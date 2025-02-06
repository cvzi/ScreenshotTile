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
import com.github.cvzi.screenshottile.utils.createNotificationScreenshotTakenChannel
import com.github.cvzi.screenshottile.utils.deleteImage
import com.github.cvzi.screenshottile.utils.editImageChooserIntent
import com.github.cvzi.screenshottile.utils.editImageIntent
import com.github.cvzi.screenshottile.utils.formatLocalizedString
import com.github.cvzi.screenshottile.utils.hideNotification
import com.github.cvzi.screenshottile.utils.renameImage
import com.github.cvzi.screenshottile.utils.shareImageChooserIntent
import com.github.cvzi.screenshottile.utils.shareImageIntent
import com.github.cvzi.screenshottile.utils.toastMessage
import com.github.cvzi.screenshottile.utils.setUserLanguage

const val NOTIFICATION_PREFIX = "NOTIFICATION"
const val NOTIFICATION_ACTION_SHARE = NOTIFICATION_PREFIX + "_ACTION_SHARE"
const val NOTIFICATION_ACTION_DELETE = NOTIFICATION_PREFIX + "_ACTION_DELETE"
const val NOTIFICATION_ACTION_EDIT = NOTIFICATION_PREFIX + "_ACTION_EDIT"
const val NOTIFICATION_ACTION_STOP = NOTIFICATION_PREFIX + "_ACTION_STOP"
const val NOTIFICATION_ACTION_RENAME = NOTIFICATION_PREFIX + "_ACTION_RENAME"
const val NOTIFICATION_ACTION_DETAILS = NOTIFICATION_PREFIX + "_ACTION_DETAILS"
const val NOTIFICATION_ACTION_CROP = NOTIFICATION_PREFIX + "_ACTION_CROP"
const val NOTIFICATION_ACTION_PHOTO_EDITOR = NOTIFICATION_PREFIX + "_ACTION_PHOTO_EDITOR"
const val NOTIFICATION_ACTION_DATA_URI = NOTIFICATION_PREFIX + "_ACTION_DATA_URI"
const val NOTIFICATION_ACTION_DATA_MIME_TYPE = NOTIFICATION_PREFIX + "_ACTION_DATA_MIME_TYPE"
const val NOTIFICATION_ACTION_ID = NOTIFICATION_PREFIX + "_ACTION_ID"
const val NOTIFICATION_ACTION_RENAME_INPUT = NOTIFICATION_PREFIX + "_ACTION_RENAME_INPUT"
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

        fun handleIntent(context: Context, intent: Intent, tag: String) {
            context.setUserLanguage()

            var windowContext: Context = context
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val dm: DisplayManager =
                    context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY)
                windowContext = context.createDisplayContext(defaultDisplay)
            }

            when (intent.action) {
                NOTIFICATION_ACTION_SHARE -> {
                    hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    val mimeType =
                        intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE) ?: "image/png"

                    val shareIntent = shareImageChooserIntent(context, path, mimeType)
                    shareIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    if (shareIntent.resolveActivity(context.packageManager) != null) {
                        App.getInstance()
                            .startActivityAndCollapseIfNotActivity(context, shareIntent)
                    } else {
                        Log.e(tag, "resolveActivity(shareIntent) returned null")
                        val noChooserIntent = shareImageIntent(context, path, mimeType)
                        noChooserIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                        noChooserIntent.setPackage(context.packageName)
                        if (noChooserIntent.resolveActivity(context.packageManager) != null) {
                            App.getInstance()
                                .startActivityAndCollapseIfNotActivity(context, noChooserIntent)
                        } else {
                            Log.e(tag, "resolveActivity(noChooserIntent) returned null")
                        }
                    }
                }

                NOTIFICATION_ACTION_DELETE -> {
                    hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path != null && deleteImage(context, path)) {
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
                    hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))

                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    val mimeType =
                        intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE) ?: "image/png"

                    val editIntent = editImageChooserIntent(context, path, mimeType)
                    editIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                    editIntent.setPackage(context.packageName)
                    if (editIntent.resolveActivity(context.packageManager) != null) {
                        App.getInstance().startActivityAndCollapseIfNotActivity(context, editIntent)
                    } else {
                        Log.e(tag, "resolveActivity(editIntent) returned null")
                        val noChooserIntent = editImageIntent(context, path, mimeType)
                        noChooserIntent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                        noChooserIntent.setPackage(context.packageName)
                        if (noChooserIntent.resolveActivity(context.packageManager) != null) {
                            App.getInstance()
                                .startActivityAndCollapseIfNotActivity(context, noChooserIntent)
                        } else {
                            Log.e(tag, "resolveActivity(noChooserIntent) returned null")
                        }
                    }
                }

                NOTIFICATION_ACTION_RENAME -> {
                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path == null) {
                        windowContext.toastMessage(
                            R.string.screenshot_rename_failed,
                            ToastType.ERROR
                        )
                        hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
                        Log.e(tag, "Rename failed: path is null")
                        return
                    }
                    var newName = RemoteInput.getResultsFromIntent(intent)
                        ?.getString(NOTIFICATION_ACTION_RENAME_INPUT)
                    if (newName.isNullOrBlank()) {
                        Log.w(tag, "Rename failed: New file name was empty or null")
                        hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
                        context.startActivity(
                            PostActivity.newIntentSingleImage(context, path).apply {
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
                        hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
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
                        .setContentText(context.formatLocalizedString(R.string.screenshot_renamed, newName))
                        .build()
                    (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
                        notify(intent.getIntExtra(NOTIFICATION_ACTION_ID, 0), repliedNotification)
                    }

                }

                NOTIFICATION_ACTION_DETAILS -> {
                    hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path == null) {
                        Log.e(tag, "NOTIFICATION_ACTION_DETAILS path is null")
                        return
                    }
                    App.getInstance().startActivityAndCollapseIfNotActivity(
                        context,
                        PostActivity.newIntentSingleImage(context, path)
                    )
                }

                NOTIFICATION_ACTION_CROP -> {
                    hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path == null) {
                        Log.e(tag, "NOTIFICATION_ACTION_CROP path is null")
                        return
                    }
                    val mimeType = intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE)
                    App.getInstance().startActivityAndCollapseIfNotActivity(
                        context,
                        PostCropActivity.newIntentSingleImage(context, path, mimeType)
                    )
                }

                NOTIFICATION_ACTION_PHOTO_EDITOR -> {
                    hideNotification(context, intent.getIntExtra(NOTIFICATION_ACTION_ID, 0))
                    val path = Uri.parse(intent.getStringExtra(NOTIFICATION_ACTION_DATA_URI))
                    if (path == null) {
                        Log.e(tag, "NOTIFICATION_ACTION_PHOTO_EDITOR path is null")
                        return
                    }
                    val mimeType = intent.getStringExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE)
                    App.getInstance().startActivityAndCollapseIfNotActivity(
                        context,
                        Intent(context, EditImageActivity::class.java).apply {
                            action = Intent.ACTION_EDIT
                            setDataAndTypeAndNormalize(path, mimeType)
                        })
                }

                NOTIFICATION_ACTION_STOP -> {
                    ScreenshotTileService.instance?.kill()
                    BasicForegroundService.instance?.background()
                    hideNotification(context, ScreenshotTileService.FOREGROUND_NOTIFICATION_ID)
                    hideNotification(context, BasicForegroundService.FOREGROUND_NOTIFICATION_ID)
                }

            }

        }

    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { c ->
            intent?.let { i ->
                handleIntent(c, i, TAG)
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
