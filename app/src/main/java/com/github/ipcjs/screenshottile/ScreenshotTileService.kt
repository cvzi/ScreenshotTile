package com.github.ipcjs.screenshottile

import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.TileService
import com.github.ipcjs.screenshottile.Utils.hasRoot
import com.github.ipcjs.screenshottile.Utils.p
import com.github.ipcjs.screenshottile.dialog.RootPermissionDialogFragment
import com.github.ipcjs.screenshottile.dialog.TransparentContainerActivity

class ScreenshotTileService : TileService() {
    private val pref by lazy { PrefManager(this) }

    override fun onTileAdded() {
        super.onTileAdded()
        p("onTileAdded")
        if (!hasRoot()) {
            TransparentContainerActivity.startAndCollapse(this, RootPermissionDialogFragment::class.java, null)
        }
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        p("onTileRemoved")
    }

    override fun onStartListening() {
        super.onStartListening()
        p("onStartListening")
    }

    override fun onStopListening() {
        super.onStopListening()
        p("onStopListening")
    }

    override fun onClick() {
        super.onClick()
        p("onClick")
        App.getInstance().screenshot(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        p("onBind")
        return super.onBind(intent)
    }

}
