package me.aflak.libraries;

import android.content.pm.ActivityInfo;
import android.graphics.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.StatFs;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Main2Activity extends AppCompatActivity implements SurfaceHolder.Callback {
    private MediaRecorder recorder;
    private SurfaceHolder surfaceHolder;
    private CamcorderProfile camcorderProfile;
    private android.hardware.Camera camera;
    boolean recording = false;
    boolean usecamera = true;
    boolean previewRunning = false;
    SurfaceView surfaceView;
    Button btnStart, btnStop;
    File root;
    File file;
    Boolean isSDPresent;
    SimpleDateFormat simpleDateFormat;
    String timeStamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);
        initComs();
        actionListener();
    }

    private void initComs() {
        simpleDateFormat = new SimpleDateFormat("ddMMyyyyhhmmss");
        timeStamp = simpleDateFormat.format(new Date());
        camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        surfaceView = (SurfaceView) findViewById(R.id.textureView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        btnStop = (Button) findViewById(R.id.stop);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        isSDPresent = android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);

    }

    public static float megabytesAvailable(File f) {
        StatFs stat = new StatFs(f.getPath());
        long bytesAvailable = (long) stat.getBlockSize()
                * (long) stat.getAvailableBlocks();
        return bytesAvailable / (1024.f * 1024.f);
    }



    private void actionListener() {
        btnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (recording) {
                    recorder.stop();
                    if (usecamera) {
                        try {
                            camera.reconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    // recorder.release();
                    recording = false;
                    // Let's prepareRecorder so we can record again
                    prepareRecorder();
                }

            }
        });
    }

    private void prepareRecorder() {
        recorder = new MediaRecorder();
        recorder.setPreviewDisplay(surfaceHolder.getSurface());
        if (usecamera) {
            camera.unlock();
            recorder.setCamera(camera);
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);

        recorder.setProfile(camcorderProfile);

        if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            recorder.setOutputFile("/sdcard/XYZApp/" + "XYZAppVideo" + ""
                    + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date())
                    + ".mp4");
        } else if (camcorderProfile.fileFormat == MediaRecorder.OutputFormat.MPEG_4) {
            recorder.setOutputFile("/sdcard/XYZApp/" + "XYZAppVideo" + ""
                    + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date())
                    + ".mp4");
        } else {
            recorder.setOutputFile("/sdcard/XYZApp/" + "XYZAppVideo" + ""
                    + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date())
                    + ".mp4");
        }

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            finish();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        System.out.println("onsurfacecreated");

        if (usecamera) {
            camera = android.hardware.Camera.open();

            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        System.out.println("onsurface changed");

        if (!recording && usecamera) {
            if (previewRunning) {
                camera.stopPreview();
            }

            try {
                android.hardware.Camera.Parameters p = camera.getParameters();

                p.setPreviewSize(camcorderProfile.videoFrameWidth,
                        camcorderProfile.videoFrameHeight);
                p.setPreviewFrameRate(camcorderProfile.videoFrameRate);

                camera.setParameters(p);

                camera.setPreviewDisplay(holder);
                camera.startPreview();
                previewRunning = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            prepareRecorder();
            if (!recording) {
                recording = true;
                recorder.start();
            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (recording) {
            recorder.stop();
            recording = false;
        }
        recorder.release();
        if (usecamera) {
            previewRunning = false;
            // camera.lock();
            camera.release();
        }
        finish();
    }
}
