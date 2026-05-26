package com.github.cvzi.screenshottile.activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.setUserLanguage

/**
 * Exported trampoline that lets callers which can only invoke [android.content.Context.startActivity]
 * (e.g. hardware-key dispatchers like Motorola MyKey, button-mapper apps, custom launchers)
 * trigger a screenshot.
 *
 * Mirrors:
 *  - the security model of [com.github.cvzi.screenshottile.IntentHandler] — gated by the
 *    user-configured broadcast secret stored in [com.github.cvzi.screenshottile.utils.PrefManager.broadcastSecret].
 *  - the screenshot code path of
 *    [com.github.cvzi.screenshottile.services.ScreenshotTileService.onClick] — directly calls
 *    [App.screenshot] / [App.screenshotPartial] without launching an inner activity, so the
 *    task stack of the originating foreground app is not disturbed.
 *
 * Intent action: `com.github.cvzi.screenshottile.TAKE_SCREENSHOT`
 *
 * Required string extra: `"secret"` — must match the secret configured in the app settings.
 * Optional extra:        `"partial"` (boolean or string `"true"`) — opens the area selector
 *                        instead of taking a full-screen screenshot.
 *
 * Example (from adb / a launcher / a hardware-key dispatcher):
 * ```
 * adb shell am start \
 *   -a com.github.cvzi.screenshottile.TAKE_SCREENSHOT \
 *   --es secret yourPasswordFromAppSettings
 * ```
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
