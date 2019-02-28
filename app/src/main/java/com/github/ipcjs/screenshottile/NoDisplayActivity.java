package com.github.ipcjs.screenshottile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import static com.github.ipcjs.screenshottile.Utils.p;
import static com.github.ipcjs.screenshottile.UtilsKt.screenshot;

public class NoDisplayActivity extends Activity {

    private static final String EXTRA_SCREENSHOT = "screenshot";

    public static Intent newIntent(Context context, boolean screenshot) {
        Intent intent = new Intent(context, NoDisplayActivity.class);
        intent.putExtra(EXTRA_SCREENSHOT, screenshot);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        p("NoDisplayActivity.onCreate");
        if (getIntent().getBooleanExtra(EXTRA_SCREENSHOT, false)) {
            screenshot(this);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        p("NoDisplayActivity.onDestroy");
    }
}
