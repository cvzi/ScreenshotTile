package com.github.ipcjs.screenshottile

import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
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
        try {
            super.onTileAdded()
            p("onTileAdded")

            App.acquireScreenshotPermission(this, this)

            qsTile.state = Tile.STATE_INACTIVE
        } catch (e: IllegalStateException) {
            Log.e("ScreenshotTileService", "onTileAdded: IllegalStateException", e)
        }
    }

    override fun onAcquireScreenshotPermission() {
        p("onAcquireScreenshotPermission")
        try {
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.updateTile()
        } catch (e: IllegalStateException) {
            Log.e("ScreenshotTileService", "onAcquireScreenshotPermission: IllegalStateException", e)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        p("onStopListening")

        // Here we can be sure that the notification panel has fully collapsed
        if (takeScreenshotOnStopListening) {
            takeScreenshotOnStopListening = false
            App.getInstance().takeScreenshotFromTileService(this)
        }
        try {
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.updateTile()
        } catch (e: IllegalStateException) {
            Log.e("ScreenshotTileService", "onStopListening: IllegalStateException", e)
        }
    }

    override fun onClick() {
        super.onClick()
        p("onClick")
        try {
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.updateTile()
        } catch (e: IllegalStateException) {
            Log.e("ScreenshotTileService", "onClick: IllegalStateException", e)
        }
        App.getInstance().screenshot(this)
    }

}
