package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.fragments.SettingDialogFragment
import com.github.cvzi.screenshottile.services.BasicForegroundService
import com.github.cvzi.screenshottile.services.FloatingTileService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.screenshot

/**
 * Holds the dialog fragment for delaying the screenshot
 * Opened by long pressing the screenshot-tile in the quick settings
 * Depending on the settings, this shows a dialog or performs an action directly
 *
 * Created by ipcjs on 2017/8/16.
 * Changes by cuzi (cuzi@openmail.cc)
 */

class SettingDialogActivity : BaseAppCompatActivity() {
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

    private val pref by lazy { App.getInstance().prefManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {

            when (App.getInstance().prefManager.tileLongPressAction) {
                getString(R.string.setting_tile_action_value_options) -> {
                    SettingDialogFragment.newInstance()
                        .show(supportFragmentManager, SettingDialogFragment::class.java.name)
                }

                getString(R.string.setting_tile_action_value_screenshot) -> {
                    if (pref.delay == 0) {
                        screenshot(this, false)
                    } else {
                        App.getInstance().screenshot(this)
                    }
                    finish()
                }

                getString(R.string.setting_tile_action_value_toggle_floating_button) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        FloatingTileService.toggleFloatingButton(this)
                    }
                    finish()
                }

                getString(R.string.setting_tile_action_value_partial) -> {
                    App.getInstance().screenshotPartial(this)
                    finish()
                }

                getString(R.string.setting_tile_action_value_delayed_1s_screenshot) -> {
                    App.getInstance().screenshot(this, 1)
                    finish()
                }

                getString(R.string.setting_tile_action_value_delayed_2s_screenshot) -> {
                    App.getInstance().screenshot(this, 2)
                    finish()
                }

                getString(R.string.setting_tile_action_value_delayed_5s_screenshot) -> {
                    App.getInstance().screenshot(this, 5)
                    finish()
                }
            }

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
