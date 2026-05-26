package com.github.cvzi.screenshottile.activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.setUserLanguage

/**
 * Take a screenshot from apps that can only start an activity, like the Motorola AI Key.
 * Uses the same secret and partial extras as [com.github.cvzi.screenshottile.IntentHandler].
 */
class ScreenshotTriggerActivity : Activity() {
    /** Intent action and extra keys accepted by [ScreenshotTriggerActivity]. */
    companion object {
        private const val TAG = "ScreenshotTriggerAct"
        const val EXTRA_SECRET = "secret"
        const val EXTRA_PARTIAL = "partial"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUserLanguage()

        if (!validateSecret()) {
            finish()
            return
        }

        val partial = intent.getBooleanExtra(EXTRA_PARTIAL, false) ||
                intent.getStringExtra(EXTRA_PARTIAL)?.equals("true", ignoreCase = true) == true

        if (partial) {
            App.getInstance().screenshotPartial(this)
        } else {
            App.getInstance().screenshot(this)
        }

        finish()
    }

    private fun validateSecret(): Boolean {
        val secret = intent.getStringExtra(EXTRA_SECRET)
        val expected = App.getInstance().prefManager.broadcastSecret
        val default = getString(R.string.setting_broadcast_secret_value_default)

        if (secret.isNullOrEmpty()) {
            Log.e(TAG, "Extra 'secret' is required.")
            return false
        }
        if (expected.isEmpty() || expected == default) {
            Log.e(TAG, "Secret was not set in the app settings.")
            return false
        }
        if (expected != secret) {
            Log.e(TAG, "Wrong secret.")
            return false
        }
        return true
    }
}
