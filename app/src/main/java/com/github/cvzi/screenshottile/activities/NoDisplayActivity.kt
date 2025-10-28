package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.NOTIFICATION_PREFIX
import com.github.cvzi.screenshottile.NotificationActionReceiver
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.services.BasicForegroundService
import com.github.cvzi.screenshottile.services.BasicForegroundService.Companion.startForegroundService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService.Companion.openAccessibilitySettings
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.screenshot
import com.github.cvzi.screenshottile.utils.screenshotLegacyOnly
import com.github.cvzi.screenshottile.utils.toastMessage

/**
 * Empty activity that is used to collapse the quick settings panel, finishes itself in onCreate
 */
class NoDisplayActivity : BaseActivity() {
    override fun onNewIntent(intent: Intent) {
        /* If the activity is already open, we need to update the intent,
        otherwise getIntent() returns the old intent in onCreate() */
        setIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        if (intent != null) {
            val action = intent.action
            if (action != null && action.startsWith(NOTIFICATION_PREFIX)) {
                // Trampoline for notification buttons, since BroadcastReceiver can't
                // open activities anymore in Android Tiramisu
                NotificationActionReceiver.handleIntent(this, intent, TAG)
            } else if (intent.getBooleanExtra(EXTRA_PARTIAL, false)) {
                // make sure that a foreground service runs
                val screenshotTileService = ScreenshotTileService.instance
                val basicForegroundService = BasicForegroundService.instance
                if (basicForegroundService != null) {
                    basicForegroundService.foreground()
                } else if (screenshotTileService != null) {
                    screenshotTileService.foreground()
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForegroundService(this)
                }
                screenshot(this, true)
            } else if (intent.getBooleanExtra(
                    EXTRA_SCREENSHOT,
                    false
                ) || action != null && action == EXTRA_SCREENSHOT || intent.getBooleanExtra(
                    EXTRA_LEGACY, false
                )
            ) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // make sure that a foreground service runs
                    /*
                    On Android U/14 we need to wait until we have the screenshot
                    permission before we can start the foreground service
                     */
                    val screenshotTileService = ScreenshotTileService.instance
                    val basicForegroundService = BasicForegroundService.instance
                    if (basicForegroundService != null) {
                        basicForegroundService.foreground()
                    } else if (screenshotTileService != null) {
                        screenshotTileService.foreground()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForegroundService(this)
                    }
                }
                if (intent.getBooleanExtra(EXTRA_LEGACY, false)) {
                    screenshotLegacyOnly(this)
                } else {
                    screenshot(this, false)
                }
            } else if (action != null && action == EXTRA_FLOATING_BUTTON || intent.getBooleanExtra(
                    EXTRA_FLOATING_BUTTON, false
                )
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Toggle floating button from shortcuts.xml
                    val screenshotAccessibilityService = ScreenshotAccessibilityService.instance
                    if (App.getInstance().prefManager.floatingButton) {
                        if (screenshotAccessibilityService != null) {
                            App.getInstance().prefManager.floatingButton = false
                            screenshotAccessibilityService.updateFloatingButton(false)
                        } else {
                            openAccessibilitySettings(this, TAG)
                        }
                    } else {
                        App.getInstance().prefManager.floatingButton = true
                        if (screenshotAccessibilityService != null) {
                            screenshotAccessibilityService.updateFloatingButton(false)
                        } else {
                            openAccessibilitySettings(this, TAG)
                        }
                    }
                } else {
                    this.toastMessage(
                        R.string.setting_floating_button_unsupported,
                        ToastType.ERROR,
                        Toast.LENGTH_LONG
                    )
                }
            } else if (action == EXTRA_HIDE_QUICK_SETTINGS_PANEL || intent.getBooleanExtra(
                    EXTRA_FLOATING_BUTTON,
                    false
                )
            ) {
                // no-op
            } else {
                if (BuildConfig.DEBUG) Log.v(TAG, "onCreate() no valid action or EXTRA_* found")
            }
        }
        finish()
    }

    companion object {
        const val TAG = "NoDisplayActivity"
        private const val EXTRA_SCREENSHOT =
            BuildConfig.APPLICATION_ID + ".NoDisplayActivity.EXTRA_SCREENSHOT"
        private const val EXTRA_LEGACY =
            BuildConfig.APPLICATION_ID + ".NoDisplayActivity.EXTRA_LEGACY"
        private const val EXTRA_PARTIAL =
            BuildConfig.APPLICATION_ID + ".NoDisplayActivity.EXTRA_PARTIAL"
        private const val EXTRA_FLOATING_BUTTON =
            BuildConfig.APPLICATION_ID + ".NoDisplayActivity.EXTRA_FLOATING_BUTTON"
        const val EXTRA_HIDE_QUICK_SETTINGS_PANEL =
            BuildConfig.APPLICATION_ID + ".NoDisplayActivity.EXTRA_HIDE_QUICK_SETTINGS_PANEL"

        /**
         * Open from service
         *
         * @param context    Context
         * @param screenshot Immediately start taking a screenshot
         */
        fun startNewTask(context: Context, screenshot: Boolean) {
            val intent = newIntent(context, screenshot)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        /**
         * Open from service
         *
         * @param context Context
         */
        fun startNewTaskPartial(context: Context) {
            val intent = newPartialIntent(context)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        /**
         * Open from service, take screenshot with legacy method
         *
         * @param context Context
         */
        fun startNewTaskLegacyScreenshot(context: Context) {
            val intent = newLegacyIntent(context)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        /**
         * New Intent that takes a screenshot immediately if screenshot is true.
         *
         * @param context    Context
         * @param screenshot Immediately start taking a screenshot
         * @return The intent
         */
        @JvmStatic
        fun newIntent(context: Context?, screenshot: Boolean): Intent {
            val intent = Intent(context, NoDisplayActivity::class.java)
            intent.putExtra(EXTRA_SCREENSHOT, screenshot)
            if (screenshot) {
                intent.action = EXTRA_SCREENSHOT
            }
            return intent
        }

        /**
         * New Intent that opens the partial screenshot selector.
         *
         * @param context Context
         * @return The intent
         */
        @JvmStatic
        fun newPartialIntent(context: Context?): Intent {
            val intent = Intent(context, NoDisplayActivity::class.java)
            intent.putExtra(EXTRA_PARTIAL, true)
            return intent
        }

        /**
         * New Intent that takes a screenshot with legacy method
         *
         * @param context Context
         * @return The intent
         */
        fun newLegacyIntent(context: Context?): Intent {
            val intent = Intent(context, NoDisplayActivity::class.java)
            intent.putExtra(EXTRA_LEGACY, true)
            return intent
        }

        /**
         * New Intent that toggles the floating button
         *
         * @param context Context
         * @return The intent
         */
        fun newFloatingButtonIntent(context: Context?): Intent {
            val intent = Intent(context, NoDisplayActivity::class.java)
            intent.putExtra(EXTRA_FLOATING_BUTTON, true)
            return intent
        }
    }
}