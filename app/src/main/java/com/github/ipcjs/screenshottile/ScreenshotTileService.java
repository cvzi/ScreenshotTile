package com.github.ipcjs.screenshottile;

import android.content.Intent;
import android.os.IBinder;
import android.service.quicksettings.TileService;

import static com.github.ipcjs.screenshottile.Utils.p;

public class ScreenshotTileService extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        p("onTileAdded");
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        p("onTileRemoved");
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        p("onStartListening");
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        p("onStopListening");
    }

    @Override
    public void onClick() {
        super.onClick();
        p("onClick");
        startActivityAndCollapse(new Intent(this, NoDisplayActivity.class));
    }

    @Override
    public IBinder onBind(Intent intent) {
        p("onBind");
        return super.onBind(intent);
    }

}
