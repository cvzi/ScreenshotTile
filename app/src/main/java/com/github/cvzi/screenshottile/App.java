package com.github.cvzi.screenshottile;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.github.cvzi.screenshottile.utils.UtilsKt.cleanUpAppData;
import static com.github.cvzi.screenshottile.utils.UtilsKt.isDeviceLocked;
import static com.github.cvzi.screenshottile.utils.UtilsKt.startActivityAndCollapseCustom;
import static com.github.cvzi.screenshottile.utils.UtilsKt.toastDeviceIsLocked;
import static com.github.cvzi.screenshottile.utils.UtilsKt.tryNativeScreenshot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.service.quicksettings.TileService;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.github.cvzi.screenshottile.activities.AcquireScreenshotPermission;
import com.github.cvzi.screenshottile.activities.DelayScreenshotActivity;
import com.github.cvzi.screenshottile.activities.NoDisplayActivity;
import com.github.cvzi.screenshottile.interfaces.OnAcquireScreenshotPermissionListener;
import com.github.cvzi.screenshottile.services.BasicForegroundService;
import com.github.cvzi.screenshottile.services.FloatingTileService;
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService;
import com.github.cvzi.screenshottile.services.ScreenshotTileService;
import com.github.cvzi.screenshottile.utils.PrefManager;

import java.lang.ref.WeakReference;

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
    private static boolean checkAccessibilityServiceOnCollapse = true;
    private static MediaProjection mediaProjection = null;
    private static volatile boolean receiverRegistered = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PrefManager prefManager;
    private Runnable screenshotRunnable;
    private WeakReference<Bitmap> lastScreenshot = null;

    public App() {
        setInstance(this);
    }

    public static App getInstance() {
        return instance;
    }

    private static void setInstance(App app) {
        instance = app;
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

        NotificationActionReceiver notificationActionReceiver = new NotificationActionReceiver();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            /*
             On Android U/14 mediaProjection cannot be re-used,
             need to create a new one with a new screenshot permission each time
             */
            mediaProjection = null;
        }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                /*
                 On Android U/14 mediaProjection cannot be re-used, so don't create it here already
                 */
                mediaProjection = null;
            } else {
                mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
            }
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
     * Open new activity that asks for the storage permission (and also notifications permission
     * since Android 13 Tiramisu).
     *
     * @param context Context
     */
    public static void requestStoragePermission(Context context, boolean screenshot) {
        final Intent intent = new Intent(context, AcquireScreenshotPermission.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_REQUEST_PERMISSION_STORAGE, true);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_TAKE_SCREENSHOT_AFTER, screenshot);
        context.startActivity(intent);
    }

    public static boolean checkAccessibilityServiceOnCollapse() {
        return checkAccessibilityServiceOnCollapse;
    }

    public static void checkAccessibilityServiceOnCollapse(boolean checkAccessibilityServiceOnCollapse) {
        App.checkAccessibilityServiceOnCollapse = checkAccessibilityServiceOnCollapse;
    }

    /**
     * Get the last bitmap and remove the weak reference
     *
     * @return last bitmap or null
     */
    public @Nullable
    Bitmap getLastScreenshot() {
        if (lastScreenshot != null) {
            Bitmap tmp = lastScreenshot.get();
            lastScreenshot = null;
            return tmp;
        }
        return null;
    }

    /**
     * Set the last bitmap
     *
     * @param lastScreenshot The last bitmap or null to remove reference
     */
    public void setLastScreenshot(@Nullable Bitmap lastScreenshot) {
        if (lastScreenshot != null) {
            this.lastScreenshot = new WeakReference<>(lastScreenshot);
        } else {
            this.lastScreenshot = null;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /*
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .detectResourceMismatches()
                    .detectUnbufferedIo()
                    .penaltyLog()
                    .penaltyDialog()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath()
                    .build());
        }
        */
        if (!BuildConfig.DEBUG) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        PreferenceManager.setDefaultValues(this, R.xml.pref, false);
        prefManager = new PrefManager(this);

        applyDayNightMode();

        cleanUpAppData(this);
    }

    /**
     * Apply the current MODE_NIGHT_FOLLOW_SYSTEM mode from the preferences
     */
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
     * @param delay Delay in seconds, if -1 the delay from the preferences will be used
     */
    public void screenshot(Context context, int delay) {
        if (isDeviceLocked(context) && prefManager.getPreventIfLocked()) {
            toastDeviceIsLocked(context);
            return;
        }

        if (prefManager.getShowCountDown()) {
            screenshotShowCountdown(context, delay);
        } else {
            screenshotHiddenCountdown(context, delay, false, false);
        }
    }

    /**
     * Take a screenshot (with delay from preferences if a delay was configured)
     **/
    public void screenshot(Context context) {
        screenshot(context, -1);
    }

    /**
     * Take a partial screenshot.
     *
     * @param context Context
     */
    public void screenshotPartial(Context context) {
        if (isDeviceLocked(context) && prefManager.getPreventIfLocked()) {
            toastDeviceIsLocked(context);
            return;
        }

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
        if (isDeviceLocked(context) && prefManager.getPreventIfLocked()) {
            toastDeviceIsLocked(context);
            return;
        }

        Intent intent = NoDisplayActivity.newPartialIntent(context);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapseCompat(context, intent);
    }


    private void screenshotShowCountdown(Context context, int delay) {
        if (delay == -1) {
            delay = prefManager.getDelay();
        }
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
                startActivityAndCollapseCompat(tileService, intent);
                startActivityAndCollapseSucceeded = true;
                // skipcq
            } catch (NullPointerException ignored) {
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

    private void screenshotHiddenCountdown(Context context, int delay, boolean now, boolean alreadyCollapsed) {
        if (delay == -1) {
            delay = prefManager.getDelay();
        }
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
                    startActivityAndCollapseCompat(tileService, intent);
                    startActivityAndCollapseSucceeded = true;
                    // skipcq
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
                    startActivityAndCollapseCompat(tileService, intent);
                    // skipcq
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

    /**
     * Try to start activity from TileService to collapse quick settings panel.
     * If no TileService is running, start from context
     *
     * @param context Context
     * @param intent  Intent
     */
    @SuppressLint({"DEPRECATION", "MissingPermission"})
    public void startActivityAndCollapseCompat(Context context, Intent intent) {
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        TileService tileService = ScreenshotTileService.Companion.getInstance();
        if (tileService == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            tileService = FloatingTileService.Companion.getInstance();
        }
        if (tileService != null) {
            startActivityAndCollapseCustom(tileService, intent);
        } else {
            context.startActivity(intent);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // skipcq
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        }
    }

    @SuppressLint({"DEPRECATION", "MissingPermission"})
    public void startActivityAndCollapseIfNotActivity(Context context, Intent intent) {
        if (context instanceof Activity) {
            context.startActivity(intent);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // skipcq
                context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
            }
        } else {
            startActivityAndCollapseCompat(context, intent);
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
                screenshotHiddenCountdown(ctx, -1,true, alreadyCollapsed);
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    }
}
