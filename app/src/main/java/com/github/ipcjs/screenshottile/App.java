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
import android.util.Log;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */

public class App extends Application {
    private static App sInstance;
    private PrefManager mPrefManager;
    private static Intent screenshotPermission = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sInstance = this;
    }

    public static App getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.pref, false);
        mPrefManager = new PrefManager(this);
    }

    public void screenshot(Context context) {
        int delay = mPrefManager.getDelay();
        if (mPrefManager.getShowCountDown()) {
            Intent intent;
            if (delay > 0) {
                intent = DelayScreenshotActivity.Companion.newIntent(context, delay);
            } else {
                intent = NoDisplayActivity.newIntent(context, true);
            }
            if (context instanceof TileService) {
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                ((TileService) context).startActivityAndCollapse(intent);
            } else {
                context.startActivity(intent);
            }
        } else {
            if (delay > 0) {
                mHandler.removeCallbacks(mScreenshotRunnable);
                mScreenshotRunnable = new CountDownRunnable(this, delay);
                mHandler.post(mScreenshotRunnable);
            } else {
                screenshot(this);
            }
            if (context instanceof TileService) {
                // open a activity to collapse notification bar
                NoDisplayActivity.startAndCollapse((TileService) context, false);
            }
        }
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mScreenshotRunnable;

    private class CountDownRunnable implements Runnable {
        private int mCount;
        private Context mContext;

        public CountDownRunnable(Context context, int count) {
            mCount = count;
            mContext = context;
        }

        @Override
        public void run() {
            mCount--;
            if (mCount < 0) {
                screenshot(mContext);
            } else {
                mHandler.postDelayed(this, 1000);
            }
        }
    }


    private static MediaProjection mediaProjection = null;
    public static MediaProjectionManager mediaProjectionManager = null;

    public static MediaProjection getMediaProjection() {
        if(mediaProjection == null) {

            if(screenshotPermission == null && ScreenshotTileService.Companion.getInstance() != null) {
                screenshotPermission  =ScreenshotTileService.Companion.getInstance().getScreenshotPermission();
            }

            if(screenshotPermission == null) {
                return null;
            }

            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
        }
        return mediaProjection;
    }

    protected static void aquireScreenshotPermission(Context context) {

        if(screenshotPermission == null && ScreenshotTileService.Companion.getInstance() != null) {
            screenshotPermission = ScreenshotTileService.Companion.getInstance().getScreenshotPermission();
        }



        Log.v("aquireScreenshotPermission", "screenshotPermission="+screenshotPermission);
        if (screenshotPermission != null) {
            if(null != mediaProjection) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, (Intent) screenshotPermission.clone());
            Log.v("aquireScreenshotPermission", "mediaProjection="+mediaProjection);
        } else {
            Log.v("aquireScreenshotPermission", "openScreenshotPermissionRequester(context)");
            openScreenshotPermissionRequester(context);
        }
    }

    protected static void openScreenshotPermissionRequester(Context context){
        final Intent intent = new Intent(context, AcquireScreenshotPermission.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(AcquireScreenshotPermission.EXTRA_REQUEST_PERMISSION, true);
        context.startActivity(intent);
    }


    protected static void setScreenshotPermission(final Intent permissionIntent) {
        screenshotPermission = permissionIntent;
        if(ScreenshotTileService.Companion.getInstance() != null) {
            ScreenshotTileService.Companion.getInstance().setScreenshotPermission(screenshotPermission);
        }

    }



}
