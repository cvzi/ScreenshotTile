package com.github.ipcjs.screenshottile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.quicksettings.TileService;

import static com.github.ipcjs.screenshottile.Utils.p;

public class ScreenshotActivity extends Activity {
    public static void start(Context context) {
        context.startActivity(newIntent(context));
    }

    public static void startAndCollapse(TileService ts) {
        ts.startActivityAndCollapse(newIntent(ts));
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, ScreenshotActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        p("NoDisplayActivity.onCreate");
        Utils.screenshot();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        p("NoDisplayActivity.onDestroy");
    }
}
