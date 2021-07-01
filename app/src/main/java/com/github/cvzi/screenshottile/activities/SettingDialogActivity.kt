package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.fragments.SettingDialogFragment
import com.github.cvzi.screenshottile.services.BasicForegroundService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotTileService


/**
 * Holds the dialog fragment for delaying the screenshot
 *
 * Created by ipcjs on 2017/8/16.
 * Changes by cuzi (cuzi@openmail.cc)
 */

class SettingDialogActivity : AppCompatActivity() {
    companion object {
        private const val START_SERVICE =
            BuildConfig.APPLICATION_ID + "SettingDialogActivity.START_SERVICE"

        /**
         * Get intent
         */
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

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Detect which tile was long pressed
            val componentName: ComponentName? = intent?.getParcelableExtra(EXTRA_COMPONENT_NAME)
        }
        */

        if (intent?.action == START_SERVICE) {
            // make sure that a foreground service runs
            when {
                BasicForegroundService.instance != null -> BasicForegroundService.instance?.foreground()
                ScreenshotTileService.instance != null -> ScreenshotTileService.instance?.foreground()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> BasicForegroundService.startForegroundService(
                    this
                )
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
