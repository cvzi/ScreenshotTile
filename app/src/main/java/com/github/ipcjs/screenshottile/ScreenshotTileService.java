package com.github.ipcjs.screenshottile;

import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.quicksettings.TileService;

import com.github.ipcjs.screenshottile.dialog.ContainerDialogActivity;
import com.github.ipcjs.screenshottile.dialog.RootPermissionDialogFragment;
import com.github.ipcjs.screenshottile.dialog.SettingDialogFragment;

import static com.github.ipcjs.screenshottile.Utils.hasRoot;
import static com.github.ipcjs.screenshottile.Utils.p;

public class ScreenshotTileService extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        p("onTileAdded");
        if (!hasRoot()) {
            startActivityAndCollapse(ContainerDialogActivity.Companion.newIntent(this, RootPermissionDialogFragment.class, null));
        }
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
        int which = PreferenceManager.getDefaultSharedPreferences(this).getInt(SettingDialogFragment.PREF_DELAYS, 0);
        startActivityAndCollapse(DelayScreenshotActivity.Companion.newIntent(
                this, SettingDialogFragment.Companion.which2delay(which)
        ));
    }

    @Override
    public IBinder onBind(Intent intent) {
        p("onBind");
        return super.onBind(intent);
    }

}
