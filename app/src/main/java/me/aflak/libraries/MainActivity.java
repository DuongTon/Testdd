package me.aflak.libraries;

import android.Manifest;
import android.app.Activity;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import me.aflak.ezcam.AutoFitTextureView;
import me.aflak.ezcam.Camera2Lib;
import me.aflak.ezcam.Camera2VideoTest;

public class MainActivity extends Activity implements View.OnClickListener {
    private AutoFitTextureView textureView;

    private ImageView imgCamera;
    private ImageView imgRecord;

    private Camera2Lib camera2Lib;

    private Camera2VideoTest camera2VideoTest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       // initView();
        textureView = findViewById(R.id.textureView);
        imgCamera = findViewById(R.id.img_camera);
        imgRecord = findViewById(R.id.img_record);
        imgCamera.setOnClickListener(this);
        imgRecord.setOnClickListener(this);
        camera2VideoTest = new Camera2VideoTest(this);

        Dexter.withActivity(MainActivity.this).withPermission(Manifest.permission.CAMERA).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                camera2VideoTest.openCamera(textureView);
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {
                Log.e("TonDV", "permission denied");
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();


    }

    private void initView() {



        camera2Lib = new Camera2Lib(this);
        String id = camera2Lib.getCamerasList().get(CameraCharacteristics.LENS_FACING_BACK);
        if (camera2Lib != null) {
            camera2Lib.selectCamera(id);
            // camera2Lib.startPreview(CameraDevice.TEMPLATE_PREVIEW, textureView);
            Dexter.withActivity(MainActivity.this).withPermission(Manifest.permission.CAMERA).withListener(new PermissionListener() {
                @Override
                public void onPermissionGranted(PermissionGrantedResponse response) {
                    camera2Lib.startPreview(CameraDevice.TEMPLATE_PREVIEW, textureView);
                }

                @Override
                public void onPermissionDenied(PermissionDeniedResponse response) {
                    Log.e("TonDV", "permission denied");
                }

                @Override
                public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                    token.continuePermissionRequest();
                }
            }).check();
        }
    }


    @Override
    protected void onDestroy() {
        if (camera2Lib != null)
            camera2Lib.close();
        super.onDestroy();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.img_camera:
                /*if (camera2Lib != null)
                    camera2Lib.takePicture();*/
                camera2VideoTest.tackPicker();
                break;
            case R.id.img_record:
                /*if (camera2Lib != null)
                    camera2Lib.recordingVideo(imgRecord);*/
                camera2VideoTest.recordingVideo();
                break;
        }
    }
}
