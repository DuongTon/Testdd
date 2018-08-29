package me.aflak.libraries;

import android.app.Activity;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import me.aflak.ezcam.AutoFitTextureView;
import me.aflak.ezcam.Camera2Lib;
public class MainActivity extends Activity implements View.OnClickListener{
    private AutoFitTextureView textureView;

    private ImageView imgCamera;
    private ImageView imgRecord;

    Camera2Lib camera2Lib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    private void initView() {
        textureView = findViewById(R.id.textureView);
        imgCamera = findViewById(R.id.img_camera);
        imgRecord = findViewById(R.id.img_record);
        imgCamera.setOnClickListener(this);
        imgRecord.setOnClickListener(this);


        camera2Lib = new Camera2Lib(this);
        String id = camera2Lib.getCamerasList().get(CameraCharacteristics.LENS_FACING_BACK);
        if (camera2Lib !=null){
            camera2Lib.selectCamera(id);
            camera2Lib.startPreview(CameraDevice.TEMPLATE_PREVIEW, textureView);
        }
    }


    @Override
    protected void onDestroy() {
        camera2Lib.close();
        super.onDestroy();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.img_camera:
                if (camera2Lib !=null)
                camera2Lib.takePicture();
                break;
            case R.id.img_record:
                if (camera2Lib !=null)
                camera2Lib.recordingVideo();
                break;
        }
    }
}
