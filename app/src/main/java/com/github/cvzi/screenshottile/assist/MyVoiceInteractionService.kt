package com.github.cvzi.screenshottile.assist

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.voice.VoiceInteractionService
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.MainActivity
import com.github.cvzi.screenshottile.activities.NoDisplayActivity
import com.github.cvzi.screenshottile.activities.SettingsActivity
import com.github.cvzi.screenshottile.fragments.SettingFragment

/**
 * Service that is started when the assist app is selected
 */
class MyVoiceInteractionService : VoiceInteractionService() {
    companion object {
        var instance: MyVoiceInteractionService? = null

        /**
         * Open assistant settings from activity
         */
        fun openVoiceInteractionSettings(context: Activity, returnTo: String? = null) {
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                if (resolveActivity(context.packageManager) != null) {
                    if (returnTo != null) {
                        App.getInstance().prefManager.returnIfVoiceInteractionServiceEnabled =
                            returnTo
                    }
                    context.startActivity(this)
                }
            }
        }
    }

    override fun onReady() {
        super.onReady()
        instance = this
        try {
            when (App.getInstance().prefManager.returnIfVoiceInteractionServiceEnabled) {
                SettingFragment.TAG -> {
                    // Return to settings
                    App.getInstance().prefManager.returnIfVoiceInteractionServiceEnabled = null
                    SettingsActivity.startNewTask(this)
                }

                MainActivity.TAG -> {
                    // Return to main activity
                    App.getInstance().prefManager.returnIfVoiceInteractionServiceEnabled = null
                    MainActivity.startNewTask(this)
                }

                NoDisplayActivity.TAG -> {
                    // Return to NoDisplayActivity activity i.e. finish()
                    App.getInstance().prefManager.returnIfVoiceInteractionServiceEnabled = null
                    NoDisplayActivity.startNewTask(this, false)
                }

                else -> {
                    // Do nothing
                }
            }
        } catch (e: ActivityNotFoundException) {
            // This seems to happen after booting
        }

        if (App.getInstance().prefManager.voiceInteractionAction != getString(R.string.setting_voice_interaction_action_value_native) &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            packageManager.checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                packageName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            try {
                App.requestStoragePermission(this, false)
            } catch (e: ActivityNotFoundException) {
                // This seems to happen after booting
            }
        }
    }
}
