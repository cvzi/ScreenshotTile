package com.github.cvzi.screenshottile

import android.app.Notification
import android.app.NotificationManager
import android.app.RemoteInput
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
import com.burhanrashid52.photoediting.EditImageActivity
import com.github.cvzi.screenshottile.activities.PostActivity
import com.github.cvzi.screenshottile.activities.PostCropActivity
import com.github.cvzi.screenshottile.services.BasicForegroundService
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.*


const val NOTIFICATION_ACTION_SHARE = "NOTIFICATION_ACTION_SHARE"
const val NOTIFICATION_ACTION_DELETE = "NOTIFICATION_ACTION_DELETE"
const val NOTIFICATION_ACTION_EDIT = "NOTIFICATION_ACTION_EDIT"
const val NOTIFICATION_ACTION_STOP = "NOTIFICATION_ACTION_STOP"
const val NOTIFICATION_ACTION_RENAME = "NOTIFICATION_ACTION_RENAME"
const val NOTIFICATION_ACTION_DETAILS = "NOTIFICATION_ACTION_DETAILS"
const val NOTIFICATION_ACTION_CROP = "NOTIFICATION_ACTION_CROP"
const val NOTIFICATION_ACTION_PHOTO_EDITOR = "NOTIFICATION_ACTION_PHOTO_EDITOR"
const val NOTIFICATION_ACTION_DATA_URI = "NOTIFICATION_ACTION_DATA_URI"
const val NOTIFICATION_ACTION_DATA_MIME_TYPE = "NOTIFICATION_ACTION_DATA_MIME_TYPE"
const val NOTIFICATION_ACTION_ID = "NOTIFICATION_ACTION_ID"
const val NOTIFICATION_ACTION_RENAME_INPUT = "NOTIFICATION_ACTION_RENAME_INPUT"
val NOTIFICATION_ACTIONS = arrayOf(
    NOTIFICATION_ACTION_SHARE,
    NOTIFICATION_ACTION_DELETE,
    NOTIFICATION_ACTION_EDIT,
    NOTIFICATION_ACTION_STOP,
    NOTIFICATION_ACTION_RENAME,
    NOTIFICATION_ACTION_DETAILS,
    NOTIFICATION_ACTION_CROP,
    NOTIFICATION_ACTION_PHOTO_EDITOR
)

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
                        App.getInstance().startActivityAndCollapse(this, shareIntent)
                    } else {
                        Log.e(TAG, "resolveActivity(shareIntent) returned null")
                    }
                }
                NOTIFICATION_ACTION_DELETE -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))

                    if (path != null && deleteImage(this, path)) {
                        windowContext.toastMessage(
                            R.string.screenshot_deleted,
                            ToastType.SUCCESS,
                            Toast.LENGTH_SHORT
                        )
                    } else {
                        windowContext.toastMessage(
                            R.string.screenshot_delete_failed,
                            ToastType.ERROR
                        )
                    }
                }
                NOTIFICATION_ACTION_EDIT -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    val mimeType =
                        intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE) ?: "image/png"

                    val editIntent = editImageChooserIntent(this, path, mimeType)
                    editIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)

                    if (editIntent.resolveActivity(context.packageManager) != null) {
                        App.getInstance().startActivityAndCollapse(this, editIntent)
                    } else {
                        Log.e(TAG, "resolveActivity(editIntent) returned null")
                    }
                }

                NOTIFICATION_ACTION_RENAME -> {
                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path == null) {
                        windowContext.toastMessage(
                            R.string.screenshot_rename_failed,
                            ToastType.ERROR
                        )
                        Log.e(TAG, "Rename failed: path is null")
                        return
                    }
                    var newName = RemoteInput.getResultsFromIntent(intent)
                        ?.getString(NOTIFICATION_ACTION_RENAME_INPUT)
                    if (newName.isNullOrBlank()) {
                        Log.w(TAG, "Rename failed: New file name was empty or null")
                        startActivity(PostActivity.newIntentSingleImage(context, path).apply {
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        })
                        return
                    }
                    newName = newName.trim()

                    App.getInstance().prefManager.addRecentFileName(newName)

                    if (!renameImage(context, path, newName).first) {
                        windowContext.toastMessage(
                            R.string.screenshot_rename_failed,
                            ToastType.ERROR
                        )
                        return
                    }


                    // Build a new notification, which informs the user that the system
                    // handled their interaction with the previous notification.
                    val builder: Notification.Builder =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Notification.Builder(
                                context,
                                createNotificationScreenshotTakenChannel(context)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            Notification.Builder(context)
                        }

                    val repliedNotification = builder
                        .setSmallIcon(android.R.drawable.ic_menu_edit)
                        .setContentText(getString(R.string.screenshot_renamed, newName))
                        .build()
                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
                        notify(intent.getIntExtra(NOTIFICATION_ACTION_ID, 0), repliedNotification)
                    }

                }

                NOTIFICATION_ACTION_DETAILS -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path == null) {
                        Log.e(TAG, "NOTIFICATION_ACTION_DETAILS path is null")
                        return
                    }
                    App.getInstance().startActivityAndCollapse(
                        this,
                        PostActivity.newIntentSingleImage(context, path)
                    )
                }

                NOTIFICATION_ACTION_CROP -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path == null) {
                        Log.e(TAG, "NOTIFICATION_ACTION_CROP path is null")
                        return
                    }
                    val mimeType = intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE)
                    App.getInstance().startActivityAndCollapse(
                        this,
                        PostCropActivity.newIntentSingleImage(context, path, mimeType)
                    )
                }

                NOTIFICATION_ACTION_PHOTO_EDITOR -> {
                    hideNotification(this, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path == null) {
                        Log.e(TAG, "NOTIFICATION_ACTION_PHOTO_EDITOR path is null")
                        return
                    }
                    val mimeType = intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE)
                    App.getInstance().startActivityAndCollapse(
                        this,
                        Intent(context, EditImageActivity::class.java).apply {
                            action = Intent.ACTION_EDIT
                            setDataAndTypeAndNormalize(path, mimeType)
                        })
                }

                NOTIFICATION_ACTION_STOP -> {
                    ScreenshotTileService.instance?.kill()
                    BasicForegroundService.instance?.background()
                    hideNotification(this, ScreenshotTileService.FOREGROUND_NOTIFICATION_ID)
                    hideNotification(this, BasicForegroundService.FOREGROUND_NOTIFICATION_ID)
                }

            }
        }
    }

    /**
     * Start receiver for notification buttons.
     */
    fun registerReceiver(context: App) {
        val intentFilter = IntentFilter()
        for (action in NOTIFICATION_ACTIONS) {
            intentFilter.addAction(action)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(this, intentFilter)
        }
    }
}
