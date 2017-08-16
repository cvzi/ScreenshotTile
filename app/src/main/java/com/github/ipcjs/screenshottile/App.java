package com.github.ipcjs.screenshottile;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.service.quicksettings.TileService;

/**
 * Created by ipcjs on 2017/8/17.
 */

public class App extends Application {
    private static App sInstance;
    private PrefManager mPrefManager;

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
                ((TileService) context).startActivityAndCollapse(intent);
            } else {
                context.startActivity(intent);
            }
        } else {
            if (delay > 0) {
                mHandler.removeCallbacks(mScreenshotRunnable);
                mScreenshotRunnable = new CountDownRunnable(delay);
                mHandler.post(mScreenshotRunnable);
            } else {
                Utils.screenshot();
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

        public CountDownRunnable(int count) {
            mCount = count;
        }

        @Override
        public void run() {
            mCount--;
            if (mCount < 0) {
                Utils.screenshot();
            } else {
                mHandler.postDelayed(this, 1000);
            }
        }
    }
}
