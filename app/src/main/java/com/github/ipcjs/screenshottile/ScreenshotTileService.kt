package com.github.ipcjs.screenshottile

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.ipcjs.screenshottile.Utils.p


/**
 * Created by ipcjs
 * Changes by cuzi (cuzi@openmail.cc)
 */


class ScreenshotTileService : TileService(), OnAcquireScreenshotPermissionListener {
    companion object {
        var instance: ScreenshotTileService? = null
    }

    var screenshotPermission: Intent? = null
    var takeScreenshotOnStopListening = false

    private val pref by lazy { PrefManager(this) }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this;
    }

    override fun onTileAdded() {
        super.onTileAdded()

        qsTile.state = Tile.STATE_INACTIVE

        App.aquireScreenshotPermission(this, this)
        p("onTileAdded")
        qsTile.updateTile()
    }

    override fun onAcquireScreenshotPermission() {
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
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

    override fun onBind(intent: Intent): IBinder? {
        p("onBind")
        return super.onBind(intent)
    }

}
