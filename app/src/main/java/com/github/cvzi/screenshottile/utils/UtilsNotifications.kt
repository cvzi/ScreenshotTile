package com.github.cvzi.screenshottile.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.github.cvzi.screenshottile.*
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity

/**
 * Created by cuzi (cuzi@openmail.cc) on 2019/08/23.
 */


const val UTILSNOTIKT = "UtilsNotifications.kt"


/**
 * Create notification channel (if it does not exists) and return its name.
 */
fun createNotificationScreenshotTakenChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_channel_description)
        val notificationTitle = context.getString(R.string.notification_title)
        val channelDescription =
            context.getString(R.string.notification_channel_description) + "\n'$notificationTitle'"

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

        val channelName = context.getString(R.string.notification_foreground_channel_description)
        val notificationTitle = context.getString(R.string.notification_foreground_title)
        val channelDescription =
            context.getString(R.string.notification_foreground_channel_description) + "\n'$notificationTitle'"

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
 * Show a notification that opens the image file on tap.
 */
fun createNotification(
    context: Context,
    path: Uri,
    bitmap: Bitmap,
    screenDensity: Int,
    mimeType: String
) {
    val appContext = context.applicationContext

    val bigPicture = resizeToBigPicture(bitmap)

    val largeIcon = resizeToNotificationIcon(bitmap, screenDensity)

    val uniqueId =
        (System.currentTimeMillis() and 0xfffffff).toInt() // notification id and pending intent request code must be unique for each notification

    val openImageIntent = openImageIntent(path, mimeType)
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
        setContentTitle(appContext.getString(R.string.notification_title))
        setContentText(appContext.getString(R.string.notification_body))
        //if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) { // TODO why is this crashing
        //    setSmallIcon(R.drawable.stat_notify_image)
        //} else {
        setSmallIcon(android.R.drawable.ic_menu_gallery)
        //}
        setLargeIcon(largeIcon)
        setAutoCancel(true)
        style = Notification.BigPictureStyle().bigPicture(bigPicture).bigLargeIcon(null as? Icon?)
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

    val shareIntent = actionButtonIntent(path, mimeType, uniqueId, NOTIFICATION_ACTION_SHARE)
    val pendingIntentShare = PendingIntent.getBroadcast(
        appContext,
        uniqueId + 3,
        shareIntent,
        PendingIntent.FLAG_IMMUTABLE
    )
    builder.addAction(
        Notification.Action.Builder(
            icon,
            appContext.getString(R.string.notification_share_screenshot),
            pendingIntentShare
        ).build()
    )

    if (editImageIntent(
            path,
            mimeType
        ).resolveActivity(context.applicationContext.packageManager) != null
    ) {
        val editIntent = actionButtonIntent(path, mimeType, uniqueId, NOTIFICATION_ACTION_EDIT)
        val pendingIntentEdit = PendingIntent.getBroadcast(
            appContext,
            uniqueId + 4,
            editIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getString(R.string.notification_edit_screenshot),
                pendingIntentEdit
            ).build()
        )
    }

    val deleteIntent = actionButtonIntent(path, mimeType, uniqueId, NOTIFICATION_ACTION_DELETE)
    val pendingIntentDelete = PendingIntent.getBroadcast(
        appContext,
        uniqueId + 2,
        deleteIntent,
        PendingIntent.FLAG_IMMUTABLE
    )
    builder.addAction(
        Notification.Action.Builder(
            icon,
            appContext.getString(R.string.notification_delete_screenshot),
            pendingIntentDelete
        ).build()
    )

    // Listen for action buttons clicks
    App.registerNotificationReceiver()

    // Show notification
    (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
        notify(uniqueId, builder.build())
    }

    largeIcon.recycle()
    bigPicture.recycle()
}

/**
 * Intent for notification action button.
 */
fun actionButtonIntent(
    path: Uri,
    mimeType: String,
    notificationId: Int,
    intentAction: String
): Intent {
    return Intent().apply {
        action = intentAction
        putExtra(NOTIFICATION_ACTION_DATA_URI, path.toString())
        putExtra(NOTIFICATION_ACTION_DATA_MIME_TYPE, mimeType)
        putExtra(NOTIFICATION_ACTION_ID, notificationId)
    }
}

/**
 * Intent to open share chooser.
 */
fun shareImageChooserIntent(context: Context, path: Uri, mimeType: String): Intent {
    Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, path)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return Intent.createChooser(
            this,
            context.getString(R.string.notification_app_chooser_share)
        )
    }
}

/**
 * Intent to edit image.
 */
fun editImageIntent(path: Uri, mimeType: String): Intent {
    return Intent(Intent.ACTION_EDIT).apply {
        setDataAndType(path, mimeType)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (path.scheme == "content") {
            // Google Photos needs this, other apps don't need it
            putExtra(MediaStore.EXTRA_OUTPUT, path)
        }
    }
}

/**
 * Intent to open edit image chooser.
 */
fun editImageChooserIntent(context: Context, path: Uri, mimeType: String): Intent {
    editImageIntent(path, mimeType).apply {
        return Intent.createChooser(this, context.getString(R.string.notification_app_chooser_edit))
    }
}

/**
 * Intent to open image file on notification tap.
 */
fun openImageIntent(path: Uri, mimeType: String): Intent {
    // Create intent for notification click
    return Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setDataAndType(path, mimeType)
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
                data = Uri.parse("package:$packageName")
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
            setContentTitle(context.getString(R.string.notification_foreground_title))
            setContentText(context.getString(R.string.notification_foreground_body))
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
