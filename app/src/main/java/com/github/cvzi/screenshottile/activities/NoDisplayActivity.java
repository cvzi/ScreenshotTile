package com.github.cvzi.screenshottile.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.github.cvzi.screenshottile.BuildConfig;
import com.github.cvzi.screenshottile.services.ScreenshotTileService;

import static com.github.cvzi.screenshottile.BuildConfig.APPLICATION_ID;
import static com.github.cvzi.screenshottile.services.ScreenshotTileService.FOREGROUND_ON_START;
import static com.github.cvzi.screenshottile.utils.UtilsKt.screenshot;

public class NoDisplayActivity extends Activity {

    private static final String TAG = "NoDisplayActivity.java";
    private static final String EXTRA_SCREENSHOT = APPLICATION_ID + ".NoDisplayActivity.EXTRA_SCREENSHOT";
    private static final String EXTRA_PARTIAL = APPLICATION_ID + ".NoDisplayActivity.EXTRA_PARTIAL";

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
                if (screenshotTileService != null) {
                    screenshotTileService.foreground();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent serviceIntent = new Intent(this, ScreenshotTileService.class);
                    serviceIntent.setAction(FOREGROUND_ON_START);
                    startService(serviceIntent);
                }
                screenshot(this, true);
            } else if (intent.getBooleanExtra(EXTRA_SCREENSHOT, false) || (action != null && action.equals(EXTRA_SCREENSHOT))) {
                // make sure that a foreground service runs
                ScreenshotTileService screenshotTileService = ScreenshotTileService.Companion.getInstance();
                if (screenshotTileService != null) {
                    screenshotTileService.foreground();
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent serviceIntent = new Intent(this, ScreenshotTileService.class);
                    serviceIntent.setAction(FOREGROUND_ON_START);
                    startForegroundService(serviceIntent);
                }
                screenshot(this, false);
            } else {
                if (BuildConfig.DEBUG) Log.v(TAG, "onCreate() EXTRA_SCREENSHOT=false");
            }
        }
        finish();
    }
}
