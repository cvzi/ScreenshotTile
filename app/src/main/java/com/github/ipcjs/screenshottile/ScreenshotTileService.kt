package com.github.ipcjs.screenshottile

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.ipcjs.screenshottile.Utils.p


/**
 * Created by ipcjs.
 * Changes by cuzi (cuzi@openmail.cc)
 */


class ScreenshotTileService : TileService(), OnAcquireScreenshotPermissionListener {
    companion object {
        var instance: ScreenshotTileService? = null
    }

    var screenshotPermission: Intent? = null
    var takeScreenshotOnStopListening = false

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    override fun onTileAdded() {
        super.onTileAdded()
        p("onTileAdded")

        qsTile.state = Tile.STATE_INACTIVE
        App.acquireScreenshotPermission(this, this)
    }

    override fun onAcquireScreenshotPermission() {
        p("onAcquireScreenshotPermission")
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        p("onStopListening")

        // Here we can be sure that the notification panel has fully collapsed
        if (takeScreenshotOnStopListening) {
            takeScreenshotOnStopListening = false
            App.getInstance().takeScreenshotFromTileService(this)
        }
    }

    override fun onClick() {
        super.onClick()
        p("onClick")

        App.getInstance().screenshot(this)
    }

}
