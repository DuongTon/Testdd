package me.aflak.ezcam;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class that simplifies the use of Camera 2 api
 *
 * @author Omar Aflak
 * @since 23/02/2017
 */

public class EZCam3 {
   /* private Context context;
    private EZCamCallback cameraCallback;

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
    private boolean mIsRecording = false;



    ///
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private ImageButton mRecordImageButton;
    private ImageButton mStillImageButton;
    private boolean mIsTimelapse = false;

    private File mVideoFolder;
    private String mVideoFileName;
    private File mImageFolder;
    private String mImageFileName;


    private MediaRecorder mMediaRecorder;
    private Chronometer mChronometer;
    private int mTotalRotation;
    private CameraCaptureSession mPreviewCaptureSession;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private CameraCaptureSession mRecordCaptureSession;

    private CameraDevice mCameraDevice;

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private String mCameraId;
    private Size mPreviewSize;
    private Size mVideoSize;
    private Size mImageSize;
    private ImageReader mImageReader;
    public EZCam3(Context context) {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    *//**
     * Set callback to receive camera states
     * @param cameraCallback callback
     *//*
    public void setCameraCallback(EZCamCallback cameraCallback) {
        this.cameraCallback = cameraCallback;
    }

    *//**
     * Get available cameras
     * @return SparseArray of available cameras ids
     *//*
    public SparseArray<String> getCamerasList(){
        camerasList = new SparseArray<>();
        try {
            String[] camerasAvailable = cameraManager.getCameraIdList();
            CameraCharacteristics cam;
            Integer characteristic;
            for (String id : camerasAvailable){
                cam = cameraManager.getCameraCharacteristics(id);
                characteristic = cam.get(CameraCharacteristics.LENS_FACING);
                if (characteristic!=null){
                    switch (characteristic){
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
            notifyError(e.getMessage());
            return null;
        }
    }

    *//**
     * Select the camera you want to open : front, back, external(s)
     * @param id Id of the camera which can be retrieved with getCamerasList().get(CameraCharacteristics.LENS_FACING_BACK)
     *//*
    public void selectCamera(String id) {
        if(camerasList == null){
            getCamerasList();
        }

        currentCamera = camerasList.indexOfValue(id)<0?null:id;
        if(currentCamera == null) {
            notifyError("Camera id not found.");
            return;
        }

        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(currentCamera);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if(map != null) {
                previewSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
                imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(onImageAvailable, backgroundHandler);
            }
            else{
                notifyError("Could not get configuration map.");
            }
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    *//**
     * Open camera to prepare preview
     * @param templateType capture mode e.g. CameraDevice.TEMPLATE_PREVIEW
     * @param textureView Surface where preview should be displayed
     *//*
    public void open(final int templateType, final TextureView textureView) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            notifyError("You don't have the required permissions.");
            return;
        }

        startBackgroundThread();

        try {
            cameraManager.openCamera(currentCamera, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    setupPreview(templateType, textureView);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    if(cameraCallback != null){
                        cameraCallback.onError("Camera device is no longer available for use.");
                        cameraCallback.onCameraDisconnected();
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    switch (error){
                        case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                            notifyError("Camera device has encountered a fatal error.");
                            break;
                        case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                            notifyError("Camera device could not be opened due to a device policy.");
                            break;
                        case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                            notifyError("Camera device is in use already.");
                            break;
                        case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                            notifyError("Camera service has encountered a fatal error.");
                            break;
                        case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                            notifyError("Camera device could not be opened because there are too many other open camera devices.");
                            break;
                    }
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    private void setupPreview_(int templateType, TextureView textureView){
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
                    if(cameraCallback != null){
                        cameraCallback.onCameraReady();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    notifyError("Could not configure capture session.");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }


    private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            *//*Log.d(TAG, "onConfigured: startPreview");*//*
                            mPreviewCaptureSession = session;
                            try {
                                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            *//*Log.d(TAG, "onConfigureFailed: startPreview");*//*

                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void startRecord(TextureView textureView) {
        try {
            if(mIsRecording) {
                setupMediaRecorder();
            } else if(mIsTimelapse) {
                setupTimelapse();
            }
            SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            Surface recordSurface = mMediaRecorder.getSurface();
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(recordSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, recordSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mRecordCaptureSession = session;
                            try {
                                mRecordCaptureSession.setRepeatingRequest(
                                        mCaptureRequestBuilder.build(), null, null
                                );
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                        }
                    }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupMediaRecorder() throws IOException {
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setVideoEncodingBitRate(1000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();
    }

    private void setupTimelapse() throws IOException {
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
        mMediaRecorder.setOutputFile(mVideoFileName);
        mMediaRecorder.setCaptureRate(2);
        mMediaRecorder.setOrientationHint(mTotalRotation);
        mMediaRecorder.prepare();
    }

    private void setupPreview(final int templateType, final TextureView outputSurface){
        if(outputSurface.isAvailable()){
            //setupPreview_(templateType, outputSurface);
            startRecord(textureView);
        }
        else{
            outputSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    setAspectRatioTextureView(outputSurface, width, height);
                   // setupPreview_(templateType, outputSurface);
                    setupCamera(width, height);

                }

                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {return false;}
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            });
        }
    }

    *//**
     * Set CaptureRequest parameters for preview e.g. flash, auto-focus, macro mode, etc.
     * @param key e.g. CaptureRequest.CONTROL_EFFECT_MODE
     * @param value e.g. CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE
     *//*
    public<T> void setCaptureSetting(CaptureRequest.Key<T> key, T value){
        if(captureRequestBuilder!=null && captureRequestBuilderImageReader!=null) {
            captureRequestBuilder.set(key, value);
            captureRequestBuilderImageReader.set(key, value);
        }
    }

    *//**
     * Get characteristic of selected camera e.g. available effects, scene modes, etc.
     * @param key e.g. CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS
     *//*
    public<T> T getCharacteristic(CameraCharacteristics.Key<T> key){
        if(cameraCharacteristics!=null) {
            return cameraCharacteristics.get(key);
        }
        return null;
    }

    private void setAspectRatioTextureView(TextureView textureView, int surfaceWidth, int surfaceHeight)
    {
        int rotation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
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
        }
        else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) ==
                        CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int deviceOrientation = ((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation = mTotalRotation == 90 || mTotalRotation == 270;
                int rotatedWidth = width;
                int rotatedHeight = height;
                if(swapRotation) {
                    rotatedWidth = height;
                    rotatedHeight = width;
                }
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class), rotatedWidth, rotatedHeight);
                mImageSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader = ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
              //  mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrienatation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrienatation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<Size>();
        for(Size option : choices) {
            if(option.getHeight() == option.getWidth() * height / width &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if(bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        } else {
            return choices[0];
        }
    }

    private void createVideoFolder() {
        File movieFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        mVideoFolder = new File(movieFile, "camera2VideoImage");
        if(!mVideoFolder.exists()) {
            mVideoFolder.mkdirs();
        }
    }

        *//**
     * start the preview, capture request is built at each call here
     *//*
    *//*public void startPreview(){
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }*//*

    *//**
     * stop the preview
     *//*
    public void stopPreview(){
        try {
            cameraCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    *//**
     * shortcut to call stopPreview() then startPreview()
     *//*
    public void restartPreview(){
        stopPreview();
        startPreview();
    }

    *//**
     * close the camera definitively
     *//*
    public void close(){
        cameraDevice.close();
        stopBackgroundThread();
    }

    *//**
     * take a picture
     *//*
    public void takePicture(){
        captureRequestBuilderImageReader.set(CaptureRequest.JPEG_ORIENTATION, cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
        try {
            cameraCaptureSession.capture(captureRequestBuilderImageReader.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            notifyError(e.getMessage());
        }
    }

    private ImageReader.OnImageAvailableListener onImageAvailable = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if(cameraCallback != null){
                cameraCallback.onPicture(imageReader.acquireLatestImage());
            }
        }
    };

    private void notifyError(String message) {
        if (cameraCallback != null) {
            cameraCallback.onError(message);
        }
    }

    *//**
     * Save image to storage
     * @param image Image object got from onPicture() callback of EZCamCallback
     * @param file File where image is going to be written
     * @return File object pointing to the file uri, null if file already exist
     *//*
    public static File saveImage(Image image, File file) throws IOException {
        if(file.exists()) {
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
            notifyError(e.getMessage());
        }
    }*/
}