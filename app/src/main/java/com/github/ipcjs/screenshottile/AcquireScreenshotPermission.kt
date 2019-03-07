package com.github.ipcjs.screenshottile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.github.ipcjs.screenshottile.App.setScreenshotPermission
import com.github.ipcjs.screenshottile.Utils.p


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */

class AcquireScreenshotPermission : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p("AcquireScreenshotPermission onCreate()")

        if (intent.getBooleanExtra(EXTRA_REQUEST_PERMISSION, false)) {
            App.mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(App.mediaProjectionManager.createScreenCaptureIntent(), 1)
        }

        val pm = packageManager
        if (pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, packageName) != PackageManager.PERMISSION_GRANTED) {
            // Request WRITE_EXTERNAL_STORAGE permission:
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permissions, WRITE_REQUEST_CODE)
        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        p("onActivityResult called " + (Activity.RESULT_OK == resultCode))
        if (1 == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                p("AcquireScreenshotPermission RESULT_OK")
                setScreenshotPermission(data.clone() as Intent)
            }
        } else if (Activity.RESULT_CANCELED == resultCode) {
            setScreenshotPermission(null)
            Log.w("onActivityResult", "No screen capture permission: resultCode==RESULT_CANCELED")
            Toast.makeText(
                    this,
                    getString(R.string.permission_missing_screen_capture), Toast.LENGTH_LONG
            ).show()
        }
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (WRITE_REQUEST_CODE == requestCode) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                p("AcquireScreenshotPermission WRITE_EXTERNAL_STORAGE is PERMISSION_GRANTED")
            } else {
                Log.w("onRequestPermissionsResult", "Expected PERMISSION_GRANTED for WRITE_EXTERNAL_STORAGE")
                Toast.makeText(
                        this,
                        getString(R.string.permission_missing_external_storage), Toast.LENGTH_LONG
                ).show()
            }
        }
        finish()
    }

    companion object {

        val EXTRA_REQUEST_PERMISSION = "extra_request_permission"
        private val WRITE_REQUEST_CODE = 12345
    }
}
