package com.github.cvzi.screenshottile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.cvzi.screenshottile.activities.NoDisplayActivity

/**
 * Handle broadcast intents from automation apps like Tasker or MacroDroid
 */
class IntentHandler : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent != null && context != null) {
            val secret = intent.getStringExtra("secret")
            val expected = App.getInstance().prefManager.broadcastSecret

            if (secret.isNullOrEmpty()) {
                Log.e("ScreenshotReceiver", "Extra 'secret' is required.")
                return
            }

            if (expected.isEmpty() || expected == App.getInstance()
                    .getString(R.string.setting_broadcast_secret_value_default)
            ) {
                Log.e("ScreenshotReceiver", "Secret was not set in the app settings.")
                return
            }

            if (expected != secret) {
                Log.e("ScreenshotReceiver", "Wrong secret.")
                return
            }

            // Accept boolean and string value for extra "partial"
            val partial = intent.getBooleanExtra("partial", false) ||
                    intent.getStringExtra("partial")?.equals("true", true) ?: false

            val noDisplayIntent = if (partial) {
                NoDisplayActivity.newPartialIntent(context).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                NoDisplayActivity.newIntent(context, true).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(noDisplayIntent)

        }
    }
}
