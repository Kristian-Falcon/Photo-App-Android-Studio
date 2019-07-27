package com.example.assessment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class PermissionManager  extends AppCompatActivity {
    final private int CAMERA_PERMISSION_REQUEST_CODE = 1;
    final private int WRITE_EXTERNAL_PERMISSION_REQUEST_CODE = 2;
    final private int READ_EXTERNAL_PERMISSION_REQUEST_CODE = 3;

    Context mContext;
    Activity mActivity;

    public PermissionManager(Activity a, Context c){
        mActivity = a;
        mContext = c;
    }

    public void requestCameraPermissions(){
        if (!(checkCameraPermissions())) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    public void requesStoragePermissions() {
        if (!(checkStoragePermissions())){
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_PERMISSION_REQUEST_CODE);
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_PERMISSION_REQUEST_CODE);
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION_REQUEST_CODE);
                } else {
                    Toast.makeText(mContext, "Access Denied until you allow us access to permissions.", Toast.LENGTH_LONG).show();
                }
                return;
            }
            case READ_EXTERNAL_PERMISSION_REQUEST_CODE: {

            }
        }
    }

    public boolean checkCameraPermissions() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    public boolean checkStoragePermissions() {
        if((ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED))
            return true;
        return false;
    }
}
