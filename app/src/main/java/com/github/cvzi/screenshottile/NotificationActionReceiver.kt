package com.github.cvzi.screenshottile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Display
import android.widget.Toast
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.deleteImage
import com.github.cvzi.screenshottile.utils.editImageChooserIntent
import com.github.cvzi.screenshottile.utils.hideNotification
import com.github.cvzi.screenshottile.utils.shareImageChooserIntent


const val NOTIFICATION_ACTION_SHARE = "NOTIFICATION_ACTION_SHARE"
const val NOTIFICATION_ACTION_DELETE = "NOTIFICATION_ACTION_DELETE"
const val NOTIFICATION_ACTION_EDIT = "NOTIFICATION_ACTION_EDIT"
const val NOTIFICATION_ACTION_STOP = "NOTIFICATION_ACTION_STOP"
const val NOTIFICATION_ACTION_DATA_URI = "NOTIFICATION_ACTION_DATA_URI"
const val NOTIFICATION_ACTION_DATA_MIME_TYPE = "NOTIFICATION_ACTION_DATA_MIME_TYPE"
const val NOTIFICATION_ACTION_ID = "NOTIFICATION_ACTION_ID"

/**
 * Handles notification action buttons.
 *
 * Created by cuzi (cuzi@openmail.cc) on 2019/03/07.
 */
class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationActionRcver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.apply {

            var windowContext: Context = context
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val dm: DisplayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY)
                windowContext = createDisplayContext(defaultDisplay)
            }

            when (intent?.action) {
                NOTIFICATION_ACTION_SHARE -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    val mimeType =
                        intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE) ?: "image/png"

                    val shareIntent = shareImageChooserIntent(this, path, mimeType)
                    shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)

                    if (shareIntent.resolveActivity(packageManager) != null) {
                        if (ScreenshotTileService.instance != null) {
                            ScreenshotTileService.instance?.startActivityAndCollapse(shareIntent)
                        } else {
                            startActivity(shareIntent)
                        }
                    } else {
                        Log.e(TAG, "resolveActivity(shareIntent) returned null")
                    }
                }
                NOTIFICATION_ACTION_DELETE -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))

                    if (path != null && deleteImage(this, path)) {
                        Toast.makeText(
                            windowContext,
                            context.getString(R.string.screenshot_deleted),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            windowContext,
                            context.getString(R.string.screenshot_delete_failed),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
                NOTIFICATION_ACTION_EDIT -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    val mimeType =
                        intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE) ?: "image/png"

                    val shareIntent = editImageChooserIntent(this, path, mimeType)
                    shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)

                    if (shareIntent.resolveActivity(context.packageManager) != null) {
                        if (ScreenshotTileService.instance != null) {
                            ScreenshotTileService.instance?.startActivityAndCollapse(shareIntent)
                        } else {
                            startActivity(shareIntent)
                        }
                    } else {
                        Log.e(TAG, "resolveActivity(shareIntent) returned null")
                    }
                }
                NOTIFICATION_ACTION_STOP -> {
                    ScreenshotTileService.instance?.kill()
                    hideNotification(this, ScreenshotTileService.FOREGROUND_NOTIFICATION_ID)
                }

            }
        }
    }

    /**
     * Start receiver for notification buttons.
     */
    fun registerReceiver(context: App) {
        var intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFICATION_ACTION_SHARE)
        context.registerReceiver(this, intentFilter)

        intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFICATION_ACTION_DELETE)
        context.registerReceiver(this, intentFilter)

        intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFICATION_ACTION_EDIT)
        context.registerReceiver(this, intentFilter)

        intentFilter = IntentFilter()
        intentFilter.addAction(NOTIFICATION_ACTION_STOP)
        context.registerReceiver(this, intentFilter)
    }
}
