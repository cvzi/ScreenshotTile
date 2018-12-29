package com.github.ipcjs.screenshottile

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.TileService
import com.github.ipcjs.screenshottile.Utils.p


/**
 * Created by ipcjs
 * Changes by cuzi (cuzi@openmail.cc)
 */


class ScreenshotTileService : TileService() {
    companion object {
        var instance: ScreenshotTileService? = null
    }

    var screenshotPermission: Intent? = null

    private val pref by lazy { PrefManager(this) }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this;
    }

    override fun onTileAdded() {
        super.onTileAdded()
        p("onTileAdded")
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
