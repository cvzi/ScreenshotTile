package com.github.cvzi.screenshottile;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.github.cvzi.screenshottile.activities.AcquireScreenshotPermission;
import com.github.cvzi.screenshottile.activities.DelayScreenshotActivity;
import com.github.cvzi.screenshottile.activities.NoDisplayActivity;
import com.github.cvzi.screenshottile.interfaces.OnAcquireScreenshotPermissionListener;
import com.github.cvzi.screenshottile.services.BasicForegroundService;
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService;
import com.github.cvzi.screenshottile.services.ScreenshotTileService;
import com.github.cvzi.screenshottile.utils.PrefManager;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.github.cvzi.screenshottile.utils.UtilsKt.tryNativeScreenshot;

/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */


public class App extends Application {
    private static final String TAG = "App.java";
    private static MediaProjectionManager mediaProjectionManager = null;
    private static App instance;
    private static Intent screenshotPermission = null;
    private static OnAcquireScreenshotPermissionListener onAcquireScreenshotPermissionListener = null;
    private static Boolean checkAccessibilityServiceOnCollapse = true;
    private static MediaProjection mediaProjection = null;
    private static volatile boolean receiverRegistered = false;
    private static NotificationActionReceiver notificationActionReceiver;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PrefManager prefManager;
    private Runnable screenshotRunnable;

    public static App getInstance() {
        return instance;
    }

    public static Intent getScreenshotPermission() {
        return screenshotPermission;
    }

    /**
     * Store screenshot permission.
     *
     * @param permissionIntent Permission
     */
    public static void setScreenshotPermission(final Intent permissionIntent) {
        screenshotPermission = permissionIntent;
        ScreenshotTileService.Companion.setScreenshotPermission(screenshotPermission);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ScreenshotAccessibilityService.Companion.setScreenshotPermission(permissionIntent);
        }
        if (onAcquireScreenshotPermissionListener != null && permissionIntent != null) {
            onAcquireScreenshotPermissionListener.onAcquireScreenshotPermission(true);
            onAcquireScreenshotPermissionListener = null;
        }
    }

    public static void setMediaProjectionManager(MediaProjectionManager mediaProjectionManager) {
        App.mediaProjectionManager = mediaProjectionManager;
    }

    public static void stopMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
    }

    public static void registerNotificationReceiver() {
        if (receiverRegistered) {
            return;
        }

        notificationActionReceiver = new NotificationActionReceiver();
        notificationActionReceiver.registerReceiver(App.getInstance());

        receiverRegistered = true;
    }

    public static void resetMediaProjection() {
        mediaProjection = null;
    }

    /**
     * Create and return MediaProjection from stored permission.
     *
     * @return MediaProjection if permission was granted or null
     */
    @SuppressWarnings("UnusedReturnValue")
    public static MediaProjection createMediaProjection() {
        if (BuildConfig.DEBUG) Log.v(TAG, "createMediaProjection()");
        BasicForegroundService basicForegroundService = BasicForegroundService.Companion.getInstance();
        ScreenshotTileService screenshotTileService = ScreenshotTileService.Companion.getInstance();
        if (basicForegroundService != null) {
            basicForegroundService.foreground();
        } else if (screenshotTileService != null) {
            screenshotTileService.foreground();
        }
        if (mediaProjection == null) {
            if (screenshotPermission == null) {
                screenshotPermission = ScreenshotTileService.Companion.getScreenshotPermission();
            }
            if (screenshotPermission == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                screenshotPermission = ScreenshotAccessibilityService.Companion.getScreenshotPermission();
            }
            if (screenshotPermission == null) {
                return null;
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
        }
        return mediaProjection;
    }

    /**
     * Acquire screenshot permission, call listener on positive result.
     *
     * @param context                                 Context
     * @param myOnAcquireScreenshotPermissionListener Callback object
     */
    public static void acquireScreenshotPermission(Context context, OnAcquireScreenshotPermissionListener myOnAcquireScreenshotPermissionListener) {
        onAcquireScreenshotPermissionListener = myOnAcquireScreenshotPermissionListener;
        ScreenshotTileService screenshotTileService = ScreenshotTileService.Companion.getInstance();
        BasicForegroundService basicForegroundService = BasicForegroundService.Companion.getInstance();
        if (screenshotPermission == null) {
            screenshotPermission = ScreenshotTileService.Companion.getScreenshotPermission();
        }
        if (screenshotPermission == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            screenshotPermission = ScreenshotAccessibilityService.Companion.getScreenshotPermission();
        }

        if (screenshotPermission != null) {
            if (null != mediaProjection) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            if (basicForegroundService != null) {
                basicForegroundService.foreground();
            } else if (screenshotTileService != null) {
                screenshotTileService.foreground();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                BasicForegroundService.Companion.startForegroundService(context);
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
            if (onAcquireScreenshotPermissionListener != null) {
                onAcquireScreenshotPermissionListener.onAcquireScreenshotPermission(false);
            }

        } else {
            if (BuildConfig.DEBUG)
                Log.v(TAG, "acquireScreenshotPermission() -> openScreenshotPermissionRequester(context)");
            openScreenshotPermissionRequester(context);
        }
    }

    /**
     * Open new activity that asks for the permission.
     *
     * @param context Context
     */
    public static void openScreenshotPermissionRequester(Context context) {
        final Intent intent = new Intent(context, AcquireScreenshotPermission.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_REQUEST_PERMISSION_SCREENSHOT, true);
        context.startActivity(intent);
    }

    /**
     * Open new activity that asks for the storage permission.
     *
     * @param context Context
     */
    public static void requestStoragePermission(Context context, Boolean screenshot) {
        final Intent intent = new Intent(context, AcquireScreenshotPermission.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_REQUEST_PERMISSION_STORAGE, true);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_TAKE_SCREENSHOT_AFTER, screenshot);
        context.startActivity(intent);
    }

    public static Boolean checkAccessibilityServiceOnCollapse() {
        return checkAccessibilityServiceOnCollapse;
    }

    public static void checkAccessibilityServiceOnCollapse(Boolean checkAccessibilityServiceOnCollapse) {
        App.checkAccessibilityServiceOnCollapse = checkAccessibilityServiceOnCollapse;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .detectResourceMismatches()
                    .detectUnbufferedIo()
                    .penaltyLog()
                    .penaltyDialog()
                    .build());
/*
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build());

*/
        }

        PreferenceManager.setDefaultValues(this, R.xml.pref, false);
        prefManager = new PrefManager(this);

        applyDayNightMode();
    }

    public void applyDayNightMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        String setting = prefManager.getDarkTheme();
        if (setting.equals(getString(R.string.setting_dark_theme_value_on))) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (setting.equals(getString(R.string.setting_dark_theme_value_off))) {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public PrefManager getPrefManager() {
        return prefManager;
    }

    /**
     * Take a screenshot.
     * If called from other activity: take a screenshot
     * If called from TileService: collapse the notification panel, the screenshot will then be
     * taken by TileService.onStopListening() when the panel is collapsed.
     *
     * @param context Context
     */
    public void screenshot(Context context) {
        if (prefManager.getShowCountDown()) {
            screenshotShowCountdown(context);
        } else {
            screenshotHiddenCountdown(context, false, false);
        }
    }

    /**
     * Take a partial screenshot.
     *
     * @param context Context
     */
    public void screenshotPartial(Context context) {
        Intent intent = NoDisplayActivity.newPartialIntent(context);
        if (!(context instanceof Activity)) {
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * Take a partial screenshot and collapse quick settings panel.
     *
     * @param context Context
     */
    public void screenshotPartial(TileService context) {
        Intent intent = NoDisplayActivity.newPartialIntent(context);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivityAndCollapse(intent);
    }


    private void screenshotShowCountdown(Context context) {
        int delay = prefManager.getDelay();
        Intent intent;
        boolean startActivityAndCollapseSucceeded = false;
        if (context instanceof ScreenshotTileService || ScreenshotTileService.Companion.getInstance() != null) {
            ScreenshotTileService tileService = (context instanceof ScreenshotTileService) ? (ScreenshotTileService) context : ScreenshotTileService.Companion.getInstance();
            if (delay > 0) {
                intent = DelayScreenshotActivity.Companion.newIntent(context, delay);
            } else {
                intent = NoDisplayActivity.newIntent(context, false);
                // Wait for notification panel closing:
                tileService.setTakeScreenshotOnStopListening(true);
            }
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            try {
                tileService.startActivityAndCollapse(intent);
                startActivityAndCollapseSucceeded = true;
            } catch (NullPointerException e) {
                startActivityAndCollapseSucceeded = false;
            }
        }

        if (!startActivityAndCollapseSucceeded) {
            if (delay > 0) {
                intent = DelayScreenshotActivity.Companion.newIntent(context, delay);
                context.startActivity(intent);
            } else {
                if (!tryNativeScreenshot()) {
                    intent = NoDisplayActivity.newIntent(context, true);
                    context.startActivity(intent);
                }
            }
        }
    }

    private void screenshotHiddenCountdown(Context context, Boolean now, Boolean alreadyCollapsed) {
        int delay = prefManager.getDelay();
        if (now) {
            delay = 0;
        }
        if (delay > 0) {
            boolean startActivityAndCollapseSucceeded = false;
            if (!alreadyCollapsed && context instanceof ScreenshotTileService || ScreenshotTileService.Companion.getInstance() != null) {
                // Open empty activity to collapse panel as soon as possible
                ScreenshotTileService tileService = (context instanceof ScreenshotTileService) ? (ScreenshotTileService) context : ScreenshotTileService.Companion.getInstance();
                Intent intent = NoDisplayActivity.newIntent(context, false);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                try {
                    tileService.startActivityAndCollapse(intent);
                    startActivityAndCollapseSucceeded = true;
                } catch (NullPointerException e) {
                    Log.e(TAG, "screenshotHiddenCountdown() tileService was null");
                }
            }

            handler.removeCallbacks(screenshotRunnable);
            screenshotRunnable = new CountDownRunnable(this, delay, startActivityAndCollapseSucceeded);
            handler.post(screenshotRunnable);
        } else {
            if (context instanceof ScreenshotTileService || ScreenshotTileService.Companion.getInstance() != null) {
                ScreenshotTileService tileService = (context instanceof ScreenshotTileService) ? (ScreenshotTileService) context : ScreenshotTileService.Companion.getInstance();
                // Open a activity to collapse notification bar, and wait for notification panel closing
                Intent intent;
                if (alreadyCollapsed) {
                    intent = NoDisplayActivity.newIntent(context, true);
                } else {
                    tileService.setTakeScreenshotOnStopListening(true);
                    intent = NoDisplayActivity.newIntent(context, false);
                }
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                try {
                    tileService.startActivityAndCollapse(intent);
                } catch (NullPointerException e) {
                    context.startActivity(intent);
                }
            } else {
                if (!tryNativeScreenshot()) {
                    Intent intent = NoDisplayActivity.newIntent(context, true);
                    if (!(context instanceof Activity)) {
                        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(intent);
                }
            }
        }
    }

    /**
     * Start new activity from tile service.
     *
     * @param context Context
     */
    public void takeScreenshotFromTileService(TileService context) {
        boolean done = tryNativeScreenshot();
        if (!done) {
            // Use app's screenshot function
            Intent intent = NoDisplayActivity.newIntent(context, true);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else if (ScreenshotTileService.Companion.getInstance() != null) {
            ScreenshotTileService.Companion.getInstance().background();
        }
    }

    private class CountDownRunnable implements Runnable {
        private final Context ctx;
        private final boolean alreadyCollapsed;
        private int count;

        CountDownRunnable(Context context, int mCount, boolean mAlreadyCollapsed) {
            count = mCount;
            alreadyCollapsed = mAlreadyCollapsed;
            ctx = context;
        }

        @Override
        public void run() {

            count--;
            if (count < 0) {
                screenshotHiddenCountdown(ctx, true, alreadyCollapsed);
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    }
}
