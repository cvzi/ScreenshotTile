package com.github.ipcjs.screenshottile

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.github.ipcjs.screenshottile.dialog.SettingDialogFragment

/**
 * Created by ipcjs on 2017/8/16.
 */

class SettingDialogActivity : FragmentActivity() {
    companion object {
        private const val START_SERVICE =
            BuildConfig.APPLICATION_ID + "SettingDialogActivity.START_SERVICE"

        fun newIntent(context: Context, startService: Boolean = false): Intent {
            return Intent(context, SettingDialogActivity::class.java).apply {
                if (startService) {
                    action = START_SERVICE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            SettingDialogFragment.newInstance()
                .show(supportFragmentManager, SettingDialogFragment::class.java.name)
        }

        if (intent?.action == START_SERVICE) {
            // make sure that a foreground service runs
            val screenshotTileService = ScreenshotTileService.instance
            if (screenshotTileService != null) {
                screenshotTileService.foreground()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceIntent = Intent(this, ScreenshotTileService::class.java)
                serviceIntent.action = ScreenshotTileService.FOREGROUND_ON_START
                startForegroundService(serviceIntent)
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !App.getInstance().prefManager.useNative || ScreenshotAccessibilityService.instance == null) {
            // Only request permission on long tile press if it's probably needed
            ScreenshotTileService.instance?.let {
                App.acquireScreenshotPermission(this, it)
            }
        }
    }
}
