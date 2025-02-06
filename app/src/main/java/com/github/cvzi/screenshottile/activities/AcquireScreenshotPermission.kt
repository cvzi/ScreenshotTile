@file:Suppress("SpellCheckingInspection")

package com.github.cvzi.screenshottile.activities

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.github.cvzi.screenshottile.App

import com.github.cvzi.screenshottile.App.setScreenshotPermission
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.utils.getLocalizedString
import com.github.cvzi.screenshottile.utils.toastMessage


/**
 * Created by cuzi (cuzi@openmail.cc) on 2019/03/05.
 */

class AcquireScreenshotPermission : BaseActivity() {
    companion object {
        private const val TAG = "AcquireScreenshotPrmssn"
        const val EXTRA_REQUEST_PERMISSION_SCREENSHOT = "extra_request_permission_screenshot"
        const val EXTRA_REQUEST_PERMISSION_STORAGE = "extra_request_permission_storage"
        const val EXTRA_TAKE_SCREENSHOT_AFTER = "extra_take_screenshot_after"
        private const val SCREENSHOT_REQUEST_CODE = 4552
        private const val WRITE_REQUEST_CODE = 12345
        private const val NOTIFICATIONS_REQUEST_CODE = 12346
    }

    private var askedForStoragePermission = false

    override fun onNewIntent(intent: Intent?) {
        /* If the activity is already open, we need to update the intent,
        otherwise getIntent() returns the old intent in onCreate() */
        setIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If asked for storage permission, start taking a screenshot on success
        if (intent.getBooleanExtra(
                EXTRA_REQUEST_PERMISSION_STORAGE,
                false
            ) && intent.getBooleanExtra(
                EXTRA_TAKE_SCREENSHOT_AFTER, true
            )
        ) {
            askedForStoragePermission = true
        }

        // Request storage and notification permission (if missing)
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && packageManager.checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                packageName
            ) != PackageManager.PERMISSION_GRANTED)
            || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && packageManager.checkPermission(
                Manifest.permission.POST_NOTIFICATIONS,
                packageName
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissions = arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                requestPermissions(permissions, NOTIFICATIONS_REQUEST_CODE)
            } else {
                val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permissions, WRITE_REQUEST_CODE)
            }
        }

        // Request screenshot permission
        if (intent.getBooleanExtra(EXTRA_REQUEST_PERMISSION_SCREENSHOT, false)) {
            (getSystemService(MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager)?.apply {
                App.setMediaProjectionManager(this)
                try {
                    startActivityForResult(createScreenCaptureIntent(), SCREENSHOT_REQUEST_CODE)
                } catch (e: ActivityNotFoundException) {
                    Log.e(
                        TAG,
                        "startActivityForResult(createScreenCaptureIntent, ...) failed with",
                        e
                    )
                    toastMessage(
                        getLocalizedString(R.string.permission_missing_screen_capture),
                        ToastType.ERROR
                    )
                    finish()
                }
            }
        }
    }

    /**
     * Screenshot permission result.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (SCREENSHOT_REQUEST_CODE == requestCode) {
            if (RESULT_OK == resultCode) {
                if (BuildConfig.DEBUG) Log.v(
                    TAG,
                    "onActivityResult() RESULT_OK"
                )
                data?.run {
                    (data.clone() as? Intent)?.apply {
                        setScreenshotPermission(this)
                    }
                }
            } else {
                setScreenshotPermission(null)
                Log.w(
                    TAG,
                    "onActivityResult() No screen capture permission: resultCode==$resultCode"
                )
                toastMessage(getLocalizedString(R.string.permission_missing_screen_capture), ToastType.ERROR)
            }
        }
        finish()
    }

    /**
     * Storage permission result.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (WRITE_REQUEST_CODE == requestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (BuildConfig.DEBUG) Log.v(
                    TAG,
                    "onRequestPermissionsResult() WRITE_EXTERNAL_STORAGE is PERMISSION_GRANTED"
                )
                if (askedForStoragePermission) {
                    App.getInstance().screenshot(this)
                }
            } else {
                Log.w(
                    TAG,
                    "onRequestPermissionsResult() Expected PERMISSION_GRANTED for WRITE_EXTERNAL_STORAGE"
                )
                toastMessage(
                    getLocalizedString(R.string.permission_missing_external_storage),
                    ToastType.ERROR
                )
            }
        } else if (NOTIFICATIONS_REQUEST_CODE == requestCode) {
            // We don't care if notifications permission was
            // granted or not.
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(
                    TAG,
                    "onRequestPermissionsResult() POST_NOTIFICATIONS is PERMISSION_GRANTED"
                )
            } else {
                Log.d(
                    TAG,
                    "onRequestPermissionsResult() POST_NOTIFICATIONS was denied"
                )
            }
            if (askedForStoragePermission) {
                App.getInstance().screenshot(this)
            }
        }
        finish()
    }
}
