package com.github.cvzi.screenshottile.utils

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.burhanrashid52.photoediting.EditImageActivity
import com.github.cvzi.screenshottile.R

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
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
    }
}

/**
 * Intent to open edit image chooser.
 */
fun editImageChooserIntent(context: Context, path: Uri, mimeType: String?): Intent {
    editImageIntent(context, path, mimeType).apply {
        return Intent.createChooser(
            this,
            context.getLocalizedString(R.string.notification_app_chooser_edit)
        )
    }
}

/**
 * Intent to open image file on notification tap.
 */
fun openImageIntent(context: Context, path: Uri, mimeType: String?): Intent {
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
