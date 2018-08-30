package me.aflak.ezcam;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static me.aflak.ezcam.CameraUtil.chooseVideoSize;

/**
 * Class that simplifies the use of Camera 2 api
 *
 * @author Omar Aflak
 * @since 23/02/2017
 */

public class Camera2Lib implements FragmentCompat.OnRequestPermissionsResultCallback {
    private Context context;

    private SparseArray<String> camerasList;
    private String currentCamera;
    private Size previewSize;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest.Builder captureRequestBuilderImageReader;
    private ImageReader imageReader;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private TextureView textureView;

    private MediaRecorder mMediaRecorder;
    private boolean mIsRecordingVideo;
    private String mNextVideoAbsolutePath;
    private Size mVideoSize;

    private Integer mSensorOrientation;
    private static final int REQUEST_CAMERA_PERMISSION_RESULT = 0;

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "Camera2VideoFragment";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private static final String[] VIDEO_PERMISSIONS = {
            CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private Size mPreviewSize;

    public Camera2Lib(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }


    /**
     * Get available cameras
     *
     * @return SparseArray of available cameras ids
     */
    public SparseArray<String> getCamerasList() {
        camerasList = new SparseArray<>();
        try {
            String[] camerasAvailable = cameraManager.getCameraIdList();
            CameraCharacteristics cam;
            Integer characteristic;
            for (String id : camerasAvailable) {
                cam = cameraManager.getCameraCharacteristics(id);
                characteristic = cam.get(CameraCharacteristics.LENS_FACING);
                if (characteristic != null) {
                    switch (characteristic) {
                        case CameraCharacteristics.LENS_FACING_FRONT:
                            camerasList.put(CameraCharacteristics.LENS_FACING_FRONT, id);
                            break;

                        case CameraCharacteristics.LENS_FACING_BACK:
                            camerasList.put(CameraCharacteristics.LENS_FACING_BACK, id);
                            break;

                        case CameraCharacteristics.LENS_FACING_EXTERNAL:
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                camerasList.put(CameraCharacteristics.LENS_FACING_EXTERNAL, id);
                            }
                            break;
                    }
                }
            }
            return camerasList;
        } catch (CameraAccessException e) {
            return null;
        }
    }

    /**
     * Select the camera you want to open : front, back, external(s)
     *
     * @param id Id of the camera which can be retrieved with getCamerasList().get(CameraCharacteristics.LENS_FACING_BACK)
     */
    public void selectCamera(String id) {
        if (camerasList == null) {
            getCamerasList();
        }

        currentCamera = camerasList.indexOfValue(id) < 0 ? null : id;
        if (currentCamera == null) {
            return;
        }

        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(currentCamera);

            // video
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            //

            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                //video

                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(currentCamera);
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

               /* mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        textureView.getWidth(), textureView.getHeight(), mVideoSize);*/
                //

                previewSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(onImageAvailable, backgroundHandler);
            }
        } catch (CameraAccessException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        return choices[choices.length - 1];
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static final int RequestPermissionCode = 1;

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!checkAllPermission())
                requestPermission();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions((Activity) context, new String[]
                {
                        CAMERA,
                        READ_EXTERNAL_STORAGE,
                        WRITE_EXTERNAL_STORAGE,

                }, RequestPermissionCode);
    }

    public boolean checkAllPermission() {

        int FirstPermissionResult = ContextCompat.checkSelfPermission(context.getApplicationContext(), CAMERA);
        int SecondPermissionResult = ContextCompat.checkSelfPermission(context.getApplicationContext(), READ_EXTERNAL_STORAGE);
        int ThirdPermissionResult = ContextCompat.checkSelfPermission(context.getApplicationContext(), WRITE_EXTERNAL_STORAGE);

        return FirstPermissionResult == PackageManager.PERMISSION_GRANTED &&
                SecondPermissionResult == PackageManager.PERMISSION_GRANTED &&
                ThirdPermissionResult == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {

                    boolean CameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean ReadExternalStatePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean ReadWriteStatePermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;

                    if (CameraPermission && ReadExternalStatePermission && ReadWriteStatePermission) {

                        Toast.makeText(context, "Permissions acquired", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, "One or more permissions denied", Toast.LENGTH_LONG).show();

                    }
                }


        }
    }


    /**
     * Open camera to prepare preview
     *
     * @param templateType capture mode e.g. CameraDevice.TEMPLATE_PREVIEW
     * @param textureView  Surface where preview should be displayed
     */

    public void startPreview(final int templateType, final TextureView textureView) {
        this.textureView = textureView;
        checkPermissions();
        if (ActivityCompat.checkSelfPermission(context, CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startBackgroundThread();

        try {
            mMediaRecorder = new MediaRecorder();
            cameraManager.openCamera(currentCamera, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    setupPreview(templateType, textureView);

                    //video
                    mCameraOpenCloseLock.release();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                   /* mCameraOpenCloseLock.release();
                    cameraDevice.close();
                    cameraDevice = null;*/
                }


                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    /*mCameraOpenCloseLock.release();
                    cameraDevice.close();
                    cameraDevice = null;
                    Activity activity = (Activity) context;
                    if (null != activity) {
                        activity.finish();
                    }*/
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
        }
    }

    private void setupPreview_(int templateType, TextureView textureView) {
        Surface surface = new Surface(textureView.getSurfaceTexture());

        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(templateType);
            captureRequestBuilder.addTarget(surface);

            captureRequestBuilderImageReader = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilderImageReader.addTarget(imageReader.getSurface());

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    setCaptureSetting(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
                    startPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d("TonDuong", "Could not configure capture session.");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
        }
    }

    private void setupPreview(final int templateType, final TextureView outputSurface) {
        if (outputSurface.isAvailable()) {
            setupPreview_(templateType, outputSurface);
        } else {
            outputSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    setAspectRatioTextureView(outputSurface, width, height);
                    setupPreview_(templateType, outputSurface);
                }

                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    configureTransform(width, height);
                }

                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                }
            });
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = (Activity) context;
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    /**
     * Set CaptureRequest parameters for preview e.g. flash, auto-focus, macro mode, etc.
     *
     * @param key   e.g. CaptureRequest.CONTROL_EFFECT_MODE
     * @param value e.g. CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE
     */
    public <T> void setCaptureSetting(CaptureRequest.Key<T> key, T value) {
        if (captureRequestBuilder != null && captureRequestBuilderImageReader != null) {
            captureRequestBuilder.set(key, value);
            captureRequestBuilderImageReader.set(key, value);
        }
    }

    /**
     * Get characteristic of selected camera e.g. available effects, scene modes, etc.
     *
     * @param key e.g. CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS
     */
    public <T> T getCharacteristic(CameraCharacteristics.Key<T> key) {
        if (cameraCharacteristics != null) {
            return cameraCharacteristics.get(key);
        }
        return null;
    }

    private void setAspectRatioTextureView(TextureView textureView, int surfaceWidth, int surfaceHeight) {
        int rotation = ((Activity) context).getWindowManager().getDefaultDisplay().getRotation();
        int newWidth = surfaceWidth, newHeight = surfaceHeight;

        switch (rotation) {
            case Surface.ROTATION_0:
                newWidth = surfaceWidth;
                newHeight = (surfaceWidth * previewSize.getWidth() / previewSize.getHeight());
                break;

            case Surface.ROTATION_180:
                newWidth = surfaceWidth;
                newHeight = (surfaceWidth * previewSize.getWidth() / previewSize.getHeight());
                break;

            case Surface.ROTATION_90:
                newWidth = surfaceHeight;
                newHeight = (surfaceHeight * previewSize.getWidth() / previewSize.getHeight());
                break;

            case Surface.ROTATION_270:
                newWidth = surfaceHeight;
                newHeight = (surfaceHeight * previewSize.getWidth() / previewSize.getHeight());
                break;
        }

        textureView.setLayoutParams(new FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER));
        rotatePreview(textureView, rotation, newWidth, newHeight);
    }

    private void rotatePreview(TextureView mTextureView, int rotation, int viewWidth, int viewHeight) {
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * start the preview, capture request is built at each call here
     */
    public void startPreview() {
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Toast.makeText(context, e.getMessage() + "", Toast.LENGTH_SHORT).show();
            Log.d("TONDV", e.getMessage());
        }
    }

    /**
     * stop the preview
     */
    public void stopPreview() {
        try {
            cameraCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
        }
    }

    /**
     * shortcut to call stopPreview() then startPreview()
     */
    public void restartPreview() {
        stopPreview();
        startPreview();
    }

    /**
     * close the camera definitively
     */
    public void close() {
        cameraDevice.close();
        stopBackgroundThread();
    }

    /**
     * take a picture
     */
    public void takePicture() {
        captureRequestBuilderImageReader.set(CaptureRequest.JPEG_ORIENTATION, cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
        try {
            cameraCaptureSession.capture(captureRequestBuilderImageReader.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
        }
    }

    private ImageReader.OnImageAvailableListener onImageAvailable = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            stopPreview();
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault());
                String filename = "image_" + dateFormat.format(new Date()) + ".jpg";
                File file = new File(context.getFilesDir(), filename);
                saveImage(imageReader.acquireLatestImage(), file);
                Toast.makeText(context, "image save " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                startPreview();
            } catch (IOException e) {
                Log.e("TonDuong", e.getMessage());
            }
        }
    };


    /**
     * Save image to storage
     *
     * @param image Image object got from onPicture() callback of EZCamCallback
     * @param file  File where image is going to be written
     * @return File object pointing to the file uri, null if file already exist
     */
    public static File saveImage(Image image, File file) throws IOException {
        if (file.exists()) {
            image.close();
            return null;
        }
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream output = new FileOutputStream(file);
        output.write(bytes);
        image.close();
        output.close();
        return file;
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("EZCam");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
        }
    }


    private void startRecordingVideo() {
        if (null == cameraDevice || !this.textureView.isAvailable() || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            captureRequestBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            captureRequestBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    updatePreview();
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI
                            mIsRecordingVideo = true;

                            // Start recording
                            mMediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = (Activity) context;
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, backgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == cameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(captureRequestBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private void setUpMediaRecorder() throws IOException {
        final Activity activity = (Activity) context;
        if (null == activity) {
            return;
        }
        //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(context);
        }
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }

    private void closePreviewSession() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

    private void stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false;

        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        Activity activity = (Activity) context;
        if (null != activity) {
            Toast.makeText(activity, "Video saved: " + mNextVideoAbsolutePath,
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath);
        }
        mNextVideoAbsolutePath = null;
       restartPreview();
        //startPreviewVideo();
    }


    private void startPreviewVideo() {
        if (null == cameraDevice || !textureView.isAvailable() || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            captureRequestBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = (Activity) context;
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void recordingVideo(ImageView imageView) {
        if (mIsRecordingVideo) {
            stopRecordingVideo();
            imageView.setColorFilter(Color.WHITE);
        } else {
            startRecordingVideo();
            imageView.setColorFilter(Color.RED);
        }
    }
}