package com.github.ipcjs.screenshottile;

import android.app.Activity;
import android.os.Bundle;

public class NoDisplayActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.runCmd("input keyevent 120", true);
        finish();
    }
}
