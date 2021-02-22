package com.github.cvzi.screenshottile.services

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import com.github.cvzi.screenshottile.utils.foregroundNotification

/**
 * Foreground service for MediaProjection
 */
class BasicForegroundService : Service() {
    companion object {
        private const val FOREGROUND_SERVICE_ID = 7594
        const val FOREGROUND_NOTIFICATION_ID = 8140
        private const val FOREGROUND_ON_START =
            BuildConfig.APPLICATION_ID + "BasicForegroundService.FOREGROUND_ON_START"
        private const val RESUME_SCREENSHOT =
            BuildConfig.APPLICATION_ID + "BasicForegroundService.RESUME_SCREENSHOT"
        var instance: BasicForegroundService? = null

        /**
         * Start this service in the foreground
         */
        fun startForegroundService(context: Context): ComponentName? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return null
            }
            val serviceIntent = Intent(context, BasicForegroundService::class.java)
            serviceIntent.action = FOREGROUND_ON_START
            return context.startForegroundService(serviceIntent)
        }
        /**
         * Start this service in the foreground, request screenshot permission with a
         * callback to TakeScreenshotActivity
         */
        fun resumeScreenshot(context: Context): ComponentName? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return null
            }
            val serviceIntent = Intent(context, BasicForegroundService::class.java)
            serviceIntent.action = RESUME_SCREENSHOT
            return context.startForegroundService(serviceIntent)
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        if (intent?.action == FOREGROUND_ON_START) {
            foreground()
        } else if (intent?.action == RESUME_SCREENSHOT) {
            foreground()
            TakeScreenshotActivity.instance?.let {
                App.acquireScreenshotPermission(this, it)
            }
        }

        return START_STICKY
    }

    /**
     * Start foreground with sticky notification, necessary for MediaProjection
     */
    fun foreground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        startForeground(
            FOREGROUND_SERVICE_ID,
            foregroundNotification(this, FOREGROUND_NOTIFICATION_ID).build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    /**
     * Stop foreground and remove sticky notification
     */
    fun background() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }
        stopForeground(true)
    }
}