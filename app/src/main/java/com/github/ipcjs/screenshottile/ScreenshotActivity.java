package com.github.ipcjs.screenshottile;

import android.app.Activity;
import android.os.Bundle;

import static com.github.ipcjs.screenshottile.Utils.p;

public class ScreenshotActivity extends Activity {
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
