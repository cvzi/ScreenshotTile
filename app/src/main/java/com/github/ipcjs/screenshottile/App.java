package com.github.ipcjs.screenshottile;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.service.quicksettings.TileService;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.github.ipcjs.screenshottile.Utils.p;

/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */


public class App extends Application {
    public static MediaProjectionManager mediaProjectionManager = null;
    private static App instance;
    private static Intent screenshotPermission = null;
    private static OnAcquireScreenshotPermissionListener onAcquireScreenshotPermissionListener = null;
    private static MediaProjection mediaProjection = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PrefManager prefManager;
    private Runnable screenshotRunnable;

    public static App getInstance() {
        return instance;
    }

    /**
     * Create and return MediaProjection from stored permission
     *
     * @return MediaProjection if permission was granted or null
     */
    public static MediaProjection getMediaProjection() {
        if (mediaProjection == null) {

            if (screenshotPermission == null && ScreenshotTileService.Companion.getInstance() != null) {
                screenshotPermission = ScreenshotTileService.Companion.getInstance().getScreenshotPermission();
            }

            if (screenshotPermission == null) {
                return null;
            }

            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
        }
        return mediaProjection;
    }

    /**
     * Acquire screenshot permission, call listener on positive result
     *
     * @param context Context
     * @param onAcquireScreenshotPermissionListener Callback object
     */
    protected static void acquireScreenshotPermission(Context context, OnAcquireScreenshotPermissionListener onAcquireScreenshotPermissionListener) {
        if (screenshotPermission == null && ScreenshotTileService.Companion.getInstance() != null) {
            screenshotPermission = ScreenshotTileService.Companion.getInstance().getScreenshotPermission();
        }

        p("App.acquireScreenshotPermission screenshotPermission=" + screenshotPermission);
        if (screenshotPermission != null) {
            if (null != mediaProjection) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
            p("App.acquireScreenshotPermission mediaProjection=" + mediaProjection);
            if (onAcquireScreenshotPermissionListener != null) {
                onAcquireScreenshotPermissionListener.onAcquireScreenshotPermission();
            }

        } else {
            p("App.acquireScreenshotPermission openScreenshotPermissionRequester(context)");
            openScreenshotPermissionRequester(context);
        }
    }

    /**
     * Open new activity that asks for the permission
     *
     * @param context Context
     */
    private static void openScreenshotPermissionRequester(Context context) {
        final Intent intent = new Intent(context, AcquireScreenshotPermission.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_REQUEST_PERMISSION, true);
        context.startActivity(intent);
    }

    /**
     * Store screenshot permission
     *
     * @param permissionIntent Permission
     */
    static void setScreenshotPermission(final Intent permissionIntent) {
        screenshotPermission = permissionIntent;
        if (ScreenshotTileService.Companion.getInstance() != null) {
            ScreenshotTileService.Companion.getInstance().setScreenshotPermission(screenshotPermission);
            if (onAcquireScreenshotPermissionListener != null) {
                onAcquireScreenshotPermissionListener.onAcquireScreenshotPermission();
                onAcquireScreenshotPermissionListener = null;
            }
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.pref, false);
        prefManager = new PrefManager(this);
    }

    /**
     * If called from other activity: take a screenshot
     * If called from TileService: collapse the notification panel, the screenshot will then be
     * taken by TileService.onStopListening() when the panel is collapsed
     *
     * @param context Context
     */
    public void screenshot(Context context) {
        if (prefManager.getShowCountDown()) {
            screenshotShowCountdown(context);
        } else {
            screenshotHiddenCountdown(context);
        }
    }

    private void screenshotShowCountdown(Context context) {
        int delay = prefManager.getDelay();
        Intent intent;
        if (delay > 0) {
            intent = DelayScreenshotActivity.Companion.newIntent(context, delay);
        } else {
            intent = NoDisplayActivity.newIntent(context, false);
        }
        if (context instanceof TileService) {
            ((ScreenshotTileService) context).setTakeScreenshotOnStopListening(true);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            ((TileService) context).startActivityAndCollapse(intent);
        } else {
            context.startActivity(intent);
        }
    }

    private void screenshotHiddenCountdown(Context context) {
        int delay = prefManager.getDelay();
        if (delay > 0) {
            handler.removeCallbacks(screenshotRunnable);
            screenshotRunnable = new CountDownRunnable(this, delay);
            handler.post(screenshotRunnable);
        } else {
            if (context instanceof TileService) {
                // open a activity to collapse notification bar
                ((ScreenshotTileService) context).setTakeScreenshotOnStopListening(true);
                Intent intent = NoDisplayActivity.newIntent(context, false);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                ((TileService) context).startActivityAndCollapse(intent);
            } else {
                screenshot(this);
            }

        }
    }

    /**
     * Start new activity from tile service.
     *
     * @param context Context
     */
    public void takeScreenshotFromTileService(TileService context) {
        Intent intent = NoDisplayActivity.newIntent(context, true);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private class CountDownRunnable implements Runnable {
        private final Context ctx;
        private int count;

        CountDownRunnable(Context context, int count) {
            this.count = count;
            ctx = context;
        }

        @Override
        public void run() {
            count--;
            if (count < 0) {
                screenshot(ctx);
            } else {
                handler.postDelayed(this, 1000);
            }
        }
    }

}
