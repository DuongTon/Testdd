package com.wesley.camera2.fragment;


import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v13.app.FragmentCompat;

/**
 * Created by wesley on 2016/03/02.
 */
public class Camera2PermissionDialog extends DialogFragment {

    public static final String FRAGMENT_DIALOG = "PermissionDialog";

    public static final int REQUEST_VIDEO_PERMISSIONS = 1;
    public static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    private Fragment mParentFragment;
    private String mRationaleMessage;

    public static Camera2PermissionDialog newInstance(Fragment mParentFragment, String mRationaleMessage) {
        Camera2PermissionDialog f = new Camera2PermissionDialog();
        f.mParentFragment = mParentFragment;
        f.mRationaleMessage = mRationaleMessage;
        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(mRationaleMessage)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FragmentCompat.requestPermissions(mParentFragment, VIDEO_PERMISSIONS,
                                REQUEST_VIDEO_PERMISSIONS);
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mParentFragment.getActivity().finish();
                            }
                        })
                .create();
    }

}