package com.github.ipcjs.screenshottile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import static com.github.ipcjs.screenshottile.App.setScreenshotPermission;


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */

public class AcquireScreenshotPermission extends Activity {

    public static final String EXTRA_REQUEST_PERMISSION = "extra_request_permission";
    private static final int WRITE_REQUEST_CODE = 12345;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v("AcquireScreenshotPermission", "onCreate()");

        if (getIntent().getBooleanExtra(EXTRA_REQUEST_PERMISSION, false)) {
            App.mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(App.mediaProjectionManager.createScreenCaptureIntent(), 1);
        }

        PackageManager pm = getPackageManager();
        if (pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getPackageName()) != PackageManager.PERMISSION_GRANTED) {
            // Request WRITE_EXTERNAL_STORAGE permission:
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, WRITE_REQUEST_CODE);
        }

    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v("onActivityResult", "called " + (Activity.RESULT_OK == resultCode));
        if (1 == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                Log.v("AcquireScreenshotPermission", "RESULT_OK");
                setScreenshotPermission((Intent) data.clone());
            }
        } else if (Activity.RESULT_CANCELED == resultCode) {
            setScreenshotPermission(null);
            Log.e("AcquireScreenshotPermission", "no access");
        }
        finish();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //Granted.
                    Log.v("AcquireScreenshotPermission", "WRITE_EXTERNAL_STORAGE is PERMISSION_GRANTED");

                }
                else{
                    throw new RuntimeException("Expected PERMISSION_GRANTED for WRITE_EXTERNAL_STORAGE");
                }
                break;
        }
        finish();
    }

}
