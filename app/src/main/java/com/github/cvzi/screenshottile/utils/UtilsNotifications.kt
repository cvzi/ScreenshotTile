package com.github.cvzi.screenshottile.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.burhanrashid52.photoediting.EditImageActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_CROP
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_DATA_MIME_TYPE
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_DATA_URI
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_DELETE
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_DETAILS
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_EDIT
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_ID
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_PHOTO_EDITOR
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_RENAME
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_RENAME_INPUT
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_SHARE
import com.github.cvzi.screenshottile.NOTIFICATION_ACTION_STOP
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.NoDisplayActivity
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import androidx.core.net.toUri

/**
 * Created by cuzi (cuzi@openmail.cc) on 2019/08/23.
 */


const val UTILSNOTIKT = "UtilsNotifications.kt"


/**
 * Create notification channel (if it does not exists) and return its name.
 */
fun createNotificationScreenshotTakenChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getLocalizedString(R.string.notification_channel_description)
        val notificationTitle = context.getLocalizedString(R.string.notification_title)
        val channelDescription =
            context.getLocalizedString(R.string.notification_channel_description) + "\n'$notificationTitle'"

        context.applicationContext.getSystemService(NotificationManager::class.java)?.run {
            if (getNotificationChannel(TakeScreenshotActivity.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        TakeScreenshotActivity.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN,
                        channelName,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = channelDescription
                        enableVibration(false)
                        enableLights(false)
                        setSound(null, null)
                    })
            }
        }
    }
    return TakeScreenshotActivity.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN
}

/**
 * Create notification channel (if it does not exists) and return its name.
 */
fun createNotificationForegroundServiceChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getLocalizedString(R.string.notification_foreground_channel_description)
        val notificationTitle = context.getLocalizedString(R.string.notification_foreground_title)
        val channelDescription =
            context.getLocalizedString(R.string.notification_foreground_channel_description) + "\n'$notificationTitle'"

        context.applicationContext.getSystemService(NotificationManager::class.java)?.run {
            if (getNotificationChannel(TakeScreenshotActivity.NOTIFICATION_CHANNEL_FOREGROUND) == null) {
                createNotificationChannel(
                    NotificationChannel(
                        TakeScreenshotActivity.NOTIFICATION_CHANNEL_FOREGROUND,
                        channelName,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = channelDescription
                        enableVibration(false)
                        enableLights(false)
                        setSound(null, null)
                    })
            }
        }
    }
    return TakeScreenshotActivity.NOTIFICATION_CHANNEL_FOREGROUND
}


/**
 * Check if the notification channel was disabled by the user
 */
fun notificationScreenshotTakenChannelEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager?
        val notificationChannel =
            notificationManager?.getNotificationChannel(TakeScreenshotActivity.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN)
        if (notificationChannel == null) {
            true  // default to true if the channel does not yet exist
        } else {
            notificationChannel.importance != NotificationManager.IMPORTANCE_NONE
        }
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

/**
 * Return getActivity for Android Tiramisu and getBroadcast for lower Android
 */
fun createNotificationPendingIntent(
    context: Context,
    requestCode: Int,
    intent: Intent,
    flags: Int
): PendingIntent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        PendingIntent.getActivity(context, requestCode, intent, flags)
    } else {
        PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}

/**
 * Show a notification that opens the image file on tap.
 */
fun createNotification(
    context: Context,
    path: Uri,
    bitmap: Bitmap,
    screenDensity: Int,
    mimeType: String,
    dummyPath: String? = null
) {
    val appContext = context.applicationContext

    val bigPicture = resizeToBigPicture(bitmap)

    val largeIcon = resizeToNotificationIcon(bitmap, screenDensity)

    val uniqueId =
        (System.currentTimeMillis() and 0xfffffff).toInt() // notification id and pending intent request code must be unique for each notification

    val openImageIntent = openImageIntent(context, path, mimeType)
    val contentPendingIntent =
        PendingIntent.getActivity(
            appContext,
            uniqueId + 1,
            openImageIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

    // Create notification
    val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(appContext, createNotificationScreenshotTakenChannel(appContext))
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(appContext)
    }
    builder.apply {
        setWhen(Calendar.getInstance().timeInMillis)
        setShowWhen(true)
        setContentTitle(appContext.getLocalizedString(R.string.notification_title))
        setContentText(appContext.getLocalizedString(R.string.notification_body))
        //if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) { // TODO why is this crashing
        //    setSmallIcon(R.drawable.stat_notify_image)
        //} else {
        setSmallIcon(android.R.drawable.ic_menu_gallery)
        //}
        setLargeIcon(largeIcon)
        setAutoCancel(true)
        style = Notification.BigPictureStyle().bigPicture(bigPicture).bigLargeIcon(largeIcon)
        if (openImageIntent.resolveActivity(context.applicationContext.packageManager) != null) {
            setContentIntent(contentPendingIntent)
        } else {
            Log.e(
                UTILSNOTIKT,
                "createNotification() resolveActivity(openImageIntent) returned null"
            )
        }
    }

    val icon = Icon.createWithResource(
        appContext,
        R.drawable.ic_stat_name
    ) // This is not shown on Android 7+ anyways so let's just use the app icon

    val notificationActions = App.getInstance().prefManager.notificationActions

    if (appContext.getString(R.string.setting_notification_action_share) in notificationActions) {
        val shareIntent = actionButtonIntent(
            path,
            mimeType,
            uniqueId,
            NOTIFICATION_ACTION_SHARE,
            appContext
        )
        val pendingIntentShare = createNotificationPendingIntent(
            appContext,
            uniqueId + 2,
            shareIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getLocalizedString(R.string.notification_share_screenshot),
                pendingIntentShare
            ).build()
        )
    }
    if (appContext.getString(R.string.setting_notification_action_edit) in notificationActions) {
        val editIntent = actionButtonIntent(
            path,
            mimeType,
            uniqueId,
            NOTIFICATION_ACTION_EDIT,
            appContext
        )
        val pendingIntentEdit = createNotificationPendingIntent(
            appContext,
            uniqueId + 3,
            editIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getLocalizedString(R.string.notification_edit_screenshot),
                pendingIntentEdit
            ).build()
        )
    }

    if (appContext.getString(R.string.setting_notification_action_delete) in notificationActions) {
        val deleteIntent = actionButtonIntent(
            path,
            mimeType,
            uniqueId,
            NOTIFICATION_ACTION_DELETE,
            appContext
        )
        val pendingIntentDelete = createNotificationPendingIntent(
            appContext,
            uniqueId + 4,
            deleteIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getLocalizedString(R.string.notification_delete_screenshot),
                pendingIntentDelete
            ).build()
        )
    }

    if (appContext.getString(R.string.setting_notification_action_rename) in notificationActions) {

        var fileName = dummyPath
            ?: if (!path.lastPathSegment.isNullOrBlank()) {
                path.lastPathSegment
            } else {
                "New file name"
            }
        fileName = fileName?.split("/")?.last()

        val replyLabel = fileName
        val remoteInput: RemoteInput = RemoteInput.Builder(NOTIFICATION_ACTION_RENAME_INPUT).run {
            setLabel(replyLabel)
            build()
        }
        val renameIntent = actionButtonIntent(
            path,
            mimeType,
            uniqueId,
            NOTIFICATION_ACTION_RENAME,
            appContext
        )

        // Build a PendingIntent for the reply action to trigger.
        val replyPendingIntent: PendingIntent =
            createNotificationPendingIntent(
                appContext,
                uniqueId + 5,
                renameIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

        val action: Notification.Action =
            Notification.Action.Builder(
                icon,
                appContext.getLocalizedString(R.string.notification_rename_screenshot),
                replyPendingIntent
            )
                .addRemoteInput(remoteInput)
                .build()
        builder.addAction(action)
    }

    if (appContext.getString(R.string.setting_notification_action_details) in notificationActions) {
        val detailsIntent =
            actionButtonIntent(
                path,
                mimeType,
                uniqueId,
                NOTIFICATION_ACTION_DETAILS,
                appContext
            )
        val pendingIntentDetails = createNotificationPendingIntent(
            appContext,
            uniqueId + 6,
            detailsIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getLocalizedString(R.string.notification_screenshot_details),
                pendingIntentDetails
            ).build()
        )
    }

    if (appContext.getString(R.string.setting_notification_action_crop) in notificationActions) {
        val detailsIntent =
            actionButtonIntent(
                path,
                mimeType,
                uniqueId,
                NOTIFICATION_ACTION_CROP,
                context
            )
        val pendingIntentDetails = createNotificationPendingIntent(
            appContext,
            uniqueId + 7,
            detailsIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getLocalizedString(R.string.notification_crop_screenshot),
                pendingIntentDetails
            ).build()
        )
    }

    if (appContext.getString(R.string.setting_notification_action_photo_editor) in notificationActions) {
        val detailsIntent =
            actionButtonIntent(
                path,
                mimeType,
                uniqueId,
                NOTIFICATION_ACTION_PHOTO_EDITOR,
                context
            )
        val pendingIntentDetails = createNotificationPendingIntent(
            appContext,
            uniqueId + 8,
            detailsIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getLocalizedString(R.string.notification_photo_editor_screenshot),
                pendingIntentDetails
            ).build()
        )
    }

    // Listen for action buttons clicks
    App.registerNotificationReceiver()

    // Show notification
    (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
        notify(uniqueId, builder.build())
    }
}

/**
 * Intent for notification action button.
 */
fun actionButtonIntent(
    path: Uri,
    mimeType: String,
    notificationId: Int,
    intentAction: String,
    context: Context
): Intent {
    val intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(context, NoDisplayActivity::class.java)
        } else {
            Intent()
        }
    return intent.apply {
        action = intentAction
        setPackage(context.packageName)
        putExtra(NOTIFICATION_ACTION_DATA_URI, path.toString())
        putExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE, mimeType)
        putExtra(NOTIFICATION_ACTION_ID, notificationId)
    }
}

/**
 * Intent to open share chooser.
 */
fun shareImageChooserIntent(context: Context, path: Uri, mimeType: String): Intent {
    return Intent.createChooser(
        shareImageIntent(context, path, mimeType),
        context.getLocalizedString(R.string.notification_app_chooser_share)
    )
}

/**
 * Intent to share image
 */
fun shareImageIntent(context: Context, path: Uri, mimeType: String?): Intent {
    val uri = if (path.scheme == "file") {
        try {
            FileProvider.getUriForFile(
                context,
                EditImageActivity.FILE_PROVIDER_AUTHORITY,
                path.toFile()
            )
        } catch (e: IllegalArgumentException) {
            Log.w(UTILSNOTIKT, "Failed to get file uri for $path", e)
            path
        }
    } else {
        path
    }
    return Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}


/**
 * Intent to edit image.
 */
fun editImageIntent(context: Context, path: Uri, mimeType: String?): Intent {
    val uri = if (path.scheme == "file") {
        try {
            FileProvider.getUriForFile(
                context,
                EditImageActivity.FILE_PROVIDER_AUTHORITY,
                path.toFile()
            )
        } catch (e: IllegalArgumentException) {
            Log.w(UTILSNOTIKT, "Failed to get file uri for $path", e)
            path
        }
    } else {
        path
    }
    return Intent(Intent.ACTION_EDIT).apply {
        setDataAndTypeAndNormalize(uri, mimeType ?: "image/*")
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (uri.scheme == "content") {
            // Google Photos needs this, other apps don't need it
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
    }
}

/**
 * Intent to open edit image chooser.
 */
fun editImageChooserIntent(context: Context, path: Uri, mimeType: String?): Intent {
    editImageIntent(context, path, mimeType).apply {
        return Intent.createChooser(this, context.getLocalizedString(R.string.notification_app_chooser_edit))
    }
}

/**
 * Intent to open image file on notification tap.
 */
fun openImageIntent(context: Context, path: Uri, mimeType: String?): Intent {
    // Create intent for notification click
    val uri = if (path.scheme == "file") {
        try {
            FileProvider.getUriForFile(
                context,
                EditImageActivity.FILE_PROVIDER_AUTHORITY,
                path.toFile()
            )
        } catch (e: IllegalArgumentException) {
            Log.w(UTILSNOTIKT, "Failed to get file uri for $path", e)
            path
        }
    } else {
        path
    }
    return Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setDataAndTypeAndNormalize(uri, mimeType ?: "image/*")
    }
}

/**
 * Cancel a notification.
 */
fun hideNotification(context: Context, notificationId: Int) {
    (context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
        cancel(notificationId)
    }
}

/**
 * Intent to open the notification settings of the package in the Android system settings
 */
fun notificationSettingsIntent(packageName: String, channelId: String? = null): Intent {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
            Intent(if (channelId != null) Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS else Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                if (channelId != null) {
                    putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                }
            }
        }

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                if (channelId != null) {
                    putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                }
            }
        }

        else -> {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                data = "package:$packageName".toUri()
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        }
    }
}

/**
 * Get a builder for the foreground notification
 */
@RequiresApi(Build.VERSION_CODES.O)
fun foregroundNotification(context: Context, notificationId: Int): Notification.Builder {
    return Notification.Builder(context, createNotificationForegroundServiceChannel(context))
        .apply {
            setShowWhen(false)
            setContentTitle(context.getLocalizedString(R.string.notification_foreground_title))
            setContentText(context.getLocalizedString(R.string.notification_foreground_body))
            setAutoCancel(true)
            //if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) { // TODO why is this crashing
            //    setSmallIcon(R.drawable.transparent_icon)
            //} else {
            setSmallIcon(android.R.drawable.divider_horizontal_dark)
            //}
            val notificationIntent = Intent().apply {
                action = NOTIFICATION_ACTION_STOP
                putExtra(NOTIFICATION_ACTION_ID, notificationId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                8456,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setContentIntent(pendingIntent)
        }
}
