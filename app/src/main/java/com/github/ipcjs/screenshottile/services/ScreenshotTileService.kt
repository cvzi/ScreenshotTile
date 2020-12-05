package com.github.ipcjs.screenshottile.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.github.ipcjs.screenshottile.*
import com.github.ipcjs.screenshottile.BuildConfig.APPLICATION_ID
import com.github.ipcjs.screenshottile.activities.TakeScreenshotActivity
import com.github.ipcjs.screenshottile.interfaces.OnAcquireScreenshotPermissionListener
import com.github.ipcjs.screenshottile.utils.createNotificationForegroundServiceChannel


/**
 * Created by ipcjs.
 * Changes by cuzi (cuzi@openmail.cc)
 */


class ScreenshotTileService : TileService(),
    OnAcquireScreenshotPermissionListener {
    companion object {
        private const val TAG = "ScreenshotTileService"
        const val FOREGROUND_NOTIFICATION_ID = 8139
        const val FOREGROUND_ON_START = APPLICATION_ID + "ScreenshotTileService.FOREGROUND_ON_START"
        var instance: ScreenshotTileService? = null
        var screenshotPermission: Intent? = null
    }

    var takeScreenshotOnStopListening = false

    private fun setState(newState: Int) {
        try {
            qsTile?.run {
                state = newState
                updateTile()
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "setState: IllegalStateException", e)
        } catch (e: NullPointerException) {
            Log.e(TAG, "setState: NullPointerException", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "setState: IllegalArgumentException", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        if (intent?.action == FOREGROUND_ON_START) {
            foreground()
        }
        return START_STICKY
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
        if (screenshotPermission == null && App.getInstance() != null) {
            screenshotPermission = App.getScreenshotPermission()
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        if (BuildConfig.DEBUG) Log.v(TAG, "onTileAdded()")

        App.acquireScreenshotPermission(this, this)

        setState(Tile.STATE_INACTIVE)
    }

    override fun onAcquireScreenshotPermission(isNewPermission: Boolean) {
        if (BuildConfig.DEBUG) Log.v(TAG, "onAcquireScreenshotPermission()")
        setState(Tile.STATE_INACTIVE)
        foreground()
    }

    override fun onStartListening() {
        super.onStartListening()
        if (BuildConfig.DEBUG) Log.v(TAG, "onStartListening()")
        setState(Tile.STATE_INACTIVE)
    }

    override fun onStopListening() {
        super.onStopListening()
        if (BuildConfig.DEBUG) Log.v(TAG, "onStopListening()")

        // Here we can be sure that the notification panel has fully collapsed
        if (takeScreenshotOnStopListening) {
            takeScreenshotOnStopListening = false
            App.getInstance().takeScreenshotFromTileService(this)
        } else {
            background()
        }
        setState(Tile.STATE_INACTIVE)
    }

    override fun onClick() {
        super.onClick()
        if (BuildConfig.DEBUG) Log.v(TAG, "onClick()")

        foreground()
        setState(Tile.STATE_ACTIVE)

        App.getInstance().screenshot(this)
    }

    fun foreground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        val context = this
        val builder = Notification.Builder(this, createNotificationForegroundServiceChannel(this))
        builder.apply {
            setShowWhen(false)
            setContentTitle(getString(R.string.notification_foreground_title))
            setContentText(getString(R.string.notification_foreground_body))
            setAutoCancel(true)
            setSmallIcon(R.drawable.transparent_icon)
            val notificationIntent = Intent().apply {
                action = NOTIFICATION_ACTION_STOP
                putExtra(NOTIFICATION_ACTION_ID, FOREGROUND_NOTIFICATION_ID)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                8456,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            setContentIntent(pendingIntent)
        }
        startForeground(
            TakeScreenshotActivity.FOREGROUND_SERVICE_ID,
            builder.build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    fun background() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }
        stopForeground(true)
    }

    fun kill() {
        background()
        stopSelf()
    }
}
