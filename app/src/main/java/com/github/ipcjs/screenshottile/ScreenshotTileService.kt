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

    private fun setState(newState: Int) {
        try {
            qsTile?.run {
                state = newState
                updateTile()
            }
        } catch (e: IllegalStateException) {
            Log.e("ScreenshotTileService", "setState: IllegalStateException", e)
        } catch (e: NullPointerException) {
            Log.e("ScreenshotTileService", "setState: NullPointerException", e)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    override fun onTileAdded() {
        super.onTileAdded()
        p("ScreenshotTileService.onTileAdded")

        App.acquireScreenshotPermission(this, this)

        setState(Tile.STATE_INACTIVE)
    }

    override fun onAcquireScreenshotPermission() {
        p("ScreenshotTileService.onAcquireScreenshotPermission")
        setState(Tile.STATE_INACTIVE)
    }

    override fun onStartListening() {
        super.onStopListening()
        p("ScreenshotTileService.onStartListening")
        setState(Tile.STATE_INACTIVE)
    }

    override fun onStopListening() {
        super.onStopListening()
        p("ScreenshotTileService.onStopListening")

        // Here we can be sure that the notification panel has fully collapsed
        if (takeScreenshotOnStopListening) {
            takeScreenshotOnStopListening = false
            App.getInstance().takeScreenshotFromTileService(this)
        }
        setState(Tile.STATE_INACTIVE)
    }

    override fun onClick() {
        super.onClick()
        p("ScreenshotTileService.onClick")

        setState(Tile.STATE_ACTIVE)
        App.getInstance().screenshot(this)
    }

}
