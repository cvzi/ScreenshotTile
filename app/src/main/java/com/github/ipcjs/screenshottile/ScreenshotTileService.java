package com.github.ipcjs.screenshottile;

import android.content.Intent;
import android.os.IBinder;
import android.service.quicksettings.TileService;

public class ScreenshotTileService extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Utils.p("onTileAdded");
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        Utils.p("onTileRemoved");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        Utils.p("onStartListening");
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Utils.p("onStopListening");
    }

    @Override
    public void onClick() {
        super.onClick();
        Utils.p("onClick");
        startActivityAndCollapse(new Intent(this, NoDisplayActivity.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        Utils.p("onBind");
        return super.onBind(intent);
    }

}
