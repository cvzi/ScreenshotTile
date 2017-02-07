package com.github.ipcjs.screenshottile;

import android.app.Activity;
import android.os.Bundle;

import static com.github.ipcjs.screenshottile.Utils.p;

public class NoDisplayActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        p("NoDisplayActivity.onCreate");
//        Utils.runCmd("input keyevent 120", true);
        Utils.runOneCmdByRoot("input keyevent 120", false);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        p("NoDisplayActivity.onDestroy");
    }
}
