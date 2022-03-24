package com.github.cvzi.screenshottile.activities;

import static com.github.cvzi.screenshottile.BuildConfig.APPLICATION_ID;
import static com.github.cvzi.screenshottile.utils.UtilsKt.screenshot;
import static com.github.cvzi.screenshottile.utils.UtilsKt.screenshotLegacyOnly;
import static com.github.cvzi.screenshottile.utils.UtilsKt.toastMessage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.github.cvzi.screenshottile.App;
import com.github.cvzi.screenshottile.BuildConfig;
import com.github.cvzi.screenshottile.R;
import com.github.cvzi.screenshottile.services.BasicForegroundService;
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService;
import com.github.cvzi.screenshottile.services.ScreenshotTileService;
import com.github.cvzi.screenshottile.utils.ToastType;

/**
 * Empty activity that is used to collapse the quick settings panel, finishes itself in onCreate
 */
public class NoDisplayActivity extends Activity {

    public static final String TAG = "NoDisplayActivity.java";
    private static final String EXTRA_SCREENSHOT = APPLICATION_ID + ".NoDisplayActivity.EXTRA_SCREENSHOT";
    private static final String EXTRA_LEGACY = APPLICATION_ID + ".NoDisplayActivity.EXTRA_LEGACY";
    private static final String EXTRA_PARTIAL = APPLICATION_ID + ".NoDisplayActivity.EXTRA_PARTIAL";
    private static final String EXTRA_FLOATING_BUTTON = APPLICATION_ID + ".NoDisplayActivity.EXTRA_FLOATING_BUTTON";

    /**
     * Open from service
     *
     * @param context    Context
     * @param screenshot Immediately start taking a screenshot
     */
    public static void startNewTask(Context context, boolean screenshot) {
        Intent intent = newIntent(context, screenshot);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Open from service, take screenshot with legacy method
     *
     * @param context Context
     */
    public static void startNewTaskLegacyScreenshot(Context context) {
        Intent intent = newLegacyIntent(context);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * New Intent that takes a screenshot immediately if screenshot is true.
     *
     * @param context    Context
     * @param screenshot Immediately start taking a screenshot
     * @return The intent
     */
    public static Intent newIntent(Context context, boolean screenshot) {
        Intent intent = new Intent(context, NoDisplayActivity.class);
        intent.putExtra(EXTRA_SCREENSHOT, screenshot);
        if (screenshot) {
            intent.setAction(EXTRA_SCREENSHOT);
        }
        return intent;
    }

    /**
     * New Intent that opens the partial screenshot selector.
     *
     * @param context Context
     * @return The intent
     */
    public static Intent newPartialIntent(Context context) {
        Intent intent = new Intent(context, NoDisplayActivity.class);
        intent.putExtra(EXTRA_PARTIAL, true);
        return intent;
    }

    /**
     * New Intent that takes a screenshot with legacy method
     *
     * @param context Context
     * @return The intent
     */
    public static Intent newLegacyIntent(Context context) {
        Intent intent = new Intent(context, NoDisplayActivity.class);
        intent.putExtra(EXTRA_LEGACY, true);
        return intent;
    }

    /**
     * New Intent that toggles the floating button
     *
     * @param context Context
     * @return The intent
     */
    public static Intent newFloatingButtonIntent(Context context) {
        Intent intent = new Intent(context, NoDisplayActivity.class);
        intent.putExtra(EXTRA_FLOATING_BUTTON, true);
        return intent;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /* If the activity is already open, we need to update the intent,
        otherwise getIntent() returns the old intent in onCreate() */
        setIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();

            if (intent.getBooleanExtra(EXTRA_PARTIAL, false)) {
                // make sure that a foreground service runs
                ScreenshotTileService screenshotTileService = ScreenshotTileService.Companion.getInstance();
                BasicForegroundService basicForegroundService = BasicForegroundService.Companion.getInstance();
                if (basicForegroundService != null) {
                    basicForegroundService.foreground();
                } else if (screenshotTileService != null) {
                    screenshotTileService.foreground();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    BasicForegroundService.Companion.startForegroundService(this);
                }
                screenshot(this, true);
            } else if (intent.getBooleanExtra(EXTRA_SCREENSHOT, false) || (action != null && action.equals(EXTRA_SCREENSHOT)) || intent.getBooleanExtra(EXTRA_LEGACY, false)) {
                // make sure that a foreground service runs
                ScreenshotTileService screenshotTileService = ScreenshotTileService.Companion.getInstance();
                BasicForegroundService basicForegroundService = BasicForegroundService.Companion.getInstance();
                if (basicForegroundService != null) {
                    basicForegroundService.foreground();
                } else if (screenshotTileService != null) {
                    screenshotTileService.foreground();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    BasicForegroundService.Companion.startForegroundService(this);
                }
                if (intent.getBooleanExtra(EXTRA_LEGACY, false)) {
                    screenshotLegacyOnly(this);
                } else {
                    screenshot(this, false);
                }
            } else if ((action != null && action.equals(EXTRA_FLOATING_BUTTON)) || intent.getBooleanExtra(EXTRA_FLOATING_BUTTON, false)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // Toggle floating button from shortcuts.xml
                    ScreenshotAccessibilityService screenshotAccessibilityService = ScreenshotAccessibilityService.Companion.getInstance();
                    if (App.getInstance().getPrefManager().getFloatingButton()) {
                        if (screenshotAccessibilityService != null) {
                            App.getInstance().getPrefManager().setFloatingButton(false);
                            screenshotAccessibilityService.updateFloatingButton(false);
                        } else {
                            ScreenshotAccessibilityService.Companion.openAccessibilitySettings(this, NoDisplayActivity.TAG);
                        }
                    } else {
                        App.getInstance().getPrefManager().setFloatingButton(true);
                        if (screenshotAccessibilityService != null) {
                            screenshotAccessibilityService.updateFloatingButton(false);
                        } else {
                            ScreenshotAccessibilityService.Companion.openAccessibilitySettings(this, NoDisplayActivity.TAG);
                        }
                    }
                } else {
                    toastMessage(this, R.string.setting_floating_button_unsupported, ToastType.ERROR, Toast.LENGTH_LONG);
                }
            } else {
                if (BuildConfig.DEBUG) Log.v(TAG, "onCreate() no valid action or EXTRA_* found");
            }
        }
        finish();
    }
}
