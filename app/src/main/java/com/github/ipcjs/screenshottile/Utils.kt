package com.github.ipcjs.screenshottile

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_PREVIEW_MAX_SIZE
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_PREVIEW_MIN_SIZE
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.max
import kotlin.math.min


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */

/**
 * Start screenshot activity and take a screenshot
 */
fun screenshot(context: Context) {
    TakeScreenshotActivity.start(context)
}

/**
 * Copy image content to new bitmap.
 */
fun imageToBitmap(image: Image): Bitmap {
    val offset = (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val w = image.width + offset
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
}

/**
 * Add image file and information to media store.
 */
fun addImageToGallery(
    context: Context,
    filepath: String,
    title: String,
    description: String,
    mimeType: String = "image/jpeg"
): Uri? {
    val values = ContentValues()
    values.put(Images.Media.TITLE, title)
    values.put(Images.Media.DESCRIPTION, description)
    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
    values.put(Images.Media.MIME_TYPE, mimeType)
    values.put(MediaStore.MediaColumns.DATA, filepath)
    return context.contentResolver?.insert(Images.Media.EXTERNAL_CONTENT_URI, values)
}

/**
 * New image file in default "Picture" directory.
 */
fun createImageFile(context: Context, filename: String): File {
    var storageDir: File?
    storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (storageDir == null) {
        // Fallback to "private" data/Package.Name/... directory
        Log.e("Utils.kt:createImageFile()", "Fallback to getExternalFilesDir(Environment.DIRECTORY_PICTURES)")
        storageDir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    }
    val screenshotDir = File(storageDir, TakeScreenshotActivity.SCREENSHOT_DIRECTORY)
    screenshotDir.mkdirs()
    return File(screenshotDir, filename)
}

/**
 * Represents a file format and a quality setting for compression. See https://developer.android.com/reference/kotlin/android/graphics/Bitmap#compress
 */
class CompressionOptions(var fileExtension: String = "png", val quality: Int = 100) {
    val format = when (fileExtension) {
        "jpg" -> Bitmap.CompressFormat.JPEG
        "webp" -> Bitmap.CompressFormat.WEBP
        else -> {
            fileExtension = "png"
            Bitmap.CompressFormat.PNG
        }
    }
    val mimeType = "image/$fileExtension"
}

/**
 * Get a CompressionsOptions object from the file_format setting
 */
fun compressionPreference(context: Context): CompressionOptions {
    var prefFileFormat = (context.applicationContext as? App)?.prefManager?.fileFormat
        ?: context.getString(R.string.setting_file_format_value_default)
    val parts = prefFileFormat.split("_")
    prefFileFormat = parts[0]
    val quality = if (parts.size > 1) {
        parts[1].toInt()
    } else 100
    return CompressionOptions(prefFileFormat, quality)
}

/**
 * Save image to jpg file in default "Picture" storage with filename="{$prefix}yyyyMMdd_HHmmss".
 */
fun saveImageToFile(
    context: Context,
    image: Image,
    prefix: String,
    compressionOptions: CompressionOptions = CompressionOptions()
): Pair<File, Bitmap>? {
    val date = Date()
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)
    val filename = "$prefix$timeStamp"

    var imageFile = createImageFile(context, "$filename.${compressionOptions.fileExtension}")

    try {
        imageFile.createNewFile()
    } catch (e: IOException) {
        // Try again to fallback to "private" data/Package.Name/... directory
        Log.e("Utils.kt:saveImageToFile()", "Could not createNewFile() ${imageFile.absolutePath}")
        imageFile = File(context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFile.name)
        try {
            imageFile.createNewFile()
        } catch (e: IOException) {
            Log.e("Utils.kt:saveImageToFile()", "Could not createNewFile() for fallback file ${imageFile.absolutePath}")
            return null
        }
    }

    if (!imageFile.exists() || !imageFile.canWrite()) {
        Log.e("Utils.kt:saveImageToFile()", "File ${imageFile.absolutePath} does not exist or is not writable")
        return null
    }

    // Save image
    val bitmap = imageToBitmap(image)
    image.close()

    if (bitmap.width == 0 || bitmap.height == 0) {
        Log.e("Utils.kt:saveImageToFile()", "Bitmap width or height is 0")
        return null
    }

    val bytes = ByteArrayOutputStream()
    bitmap.compress(compressionOptions.format, compressionOptions.quality, bytes)

    imageFile.outputStream().use {
        it.write(bytes.toByteArray())
    }

    // Add to g
    addImageToGallery(
        context,
        imageFile.absolutePath,
        context.getString(R.string.file_title),
        context.getString(
            R.string.file_description,
            SimpleDateFormat(context.getString(R.string.file_description_simpledateformat), Locale.getDefault()).format(
                date
            )
        ),
        compressionOptions.mimeType
    )

    return Pair(imageFile, bitmap)
}

/**
 * Create notification channel (if it does not exists) and return its name.
 */
fun createNotificationScreenshotTakenChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_title)
        val channelDescription = context.getString(R.string.notification_channel_description)

        context.applicationContext.getSystemService(NotificationManager::class.java)?.run {
            if (getNotificationChannel(NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN) == null) {
                createNotificationChannel(NotificationChannel(
                    NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN,
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
    return NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN
}

/**
 * Create notification preview icon with appropriate size according to the device screen.
 */
fun resizeToNotificationIcon(bitmap: Bitmap, screenDensity: Int): Bitmap {
    val maxSize = (min(max(screenDensity / 2, NOTIFICATION_PREVIEW_MIN_SIZE), NOTIFICATION_PREVIEW_MAX_SIZE)).toDouble()

    val ratioX = maxSize / bitmap.width
    val ratioY = maxSize / bitmap.height
    val ratio = min(ratioX, ratioY)
    val newWidth = (bitmap.width * ratio).toInt()
    val newHeight = (bitmap.height * ratio).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
}

/**
 * Create notification big picture icon, bitmap cropped (centered)
 */
fun resizeToBigPicture(bitmap: Bitmap): Bitmap {
    return if(bitmap.height > NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) {
        val offsetY = (bitmap.height - NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) / 2
        Bitmap.createBitmap(bitmap, 0, offsetY, bitmap.width, NOTIFICATION_BIG_PICTURE_MAX_HEIGHT)
    } else {
        bitmap
    }
}

/**
 * Show a notification that opens the image file on tap.
 */
fun createNotification(context: Context, path: Uri, bitmap: Bitmap, screenDensity: Int) {
    val appContext = context.applicationContext

    val bigPicture = resizeToBigPicture(bitmap)

    val largeIcon = resizeToNotificationIcon(bitmap, screenDensity)

    val uniqueId =
        (System.currentTimeMillis() and 0xfffffff).toInt() // notification id and pending intent request code must be unique for each notification

    val openImageIntent = openImageIntent(path)
    val contentPendingIntent = PendingIntent.getActivity(appContext, uniqueId + 1, openImageIntent, 0)

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
        setSmallIcon(R.drawable.stat_notify_image)
        setLargeIcon(largeIcon)
        setAutoCancel(true)
        style = Notification.BigPictureStyle().bigPicture(bigPicture).bigLargeIcon(null as Icon?)
        if (openImageIntent.resolveActivity(context.applicationContext.packageManager) != null) {
            setContentIntent(contentPendingIntent)
        } else {
            Log.e("Utils.kt:createNotification()", "resolveActivity(openImageIntent) returned null")
        }
    }

    val icon = Icon.createWithResource(
        appContext,
        R.drawable.ic_stat_name
    ) // This is not shown on Android 7+ anyways so let's just use the app icon

    val shareIntent = actionButtonIntent(path, uniqueId, NOTIFICATION_ACTION_SHARE)
    val pendingIntentShare = PendingIntent.getBroadcast(appContext, uniqueId + 3, shareIntent, 0)
    builder.addAction(
        Notification.Action.Builder(
            icon,
            appContext.getString(R.string.notification_share_screenshot),
            pendingIntentShare
        ).build()
    )

    if (editImageIntent(path).resolveActivity(context.applicationContext.packageManager) != null) {
        val editIntent = actionButtonIntent(path, uniqueId, NOTIFICATION_ACTION_EDIT)
        val pendingIntentEdit = PendingIntent.getBroadcast(appContext, uniqueId + 4, editIntent, 0)
        builder.addAction(
            Notification.Action.Builder(
                icon,
                appContext.getString(R.string.notification_edit_screenshot),
                pendingIntentEdit
            ).build()
        )
    }

    val deleteIntent = actionButtonIntent(path, uniqueId, NOTIFICATION_ACTION_DELETE)
    val pendingIntentDelete = PendingIntent.getBroadcast(appContext, uniqueId + 2, deleteIntent, 0)
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
fun actionButtonIntent(path: Uri, notificationId: Int, intentAction: String): Intent {
    return Intent().apply {
        action = intentAction
        putExtra(NOTIFICATION_ACTION_DATA_URI, path.toString())
        putExtra(NOTIFICATION_ACTION_ID, notificationId)
    }
}

/**
 * Intent to open share chooser.
 */
fun shareImageChooserIntent(context: Context, path: Uri): Intent {
    Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, path)
        return Intent.createChooser(this, context.getString(R.string.notification_app_chooser_share))
    }
}

/**
 * Intent to edit image.
 */
fun editImageIntent(path: Uri): Intent {
    return Intent(Intent.ACTION_EDIT).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        setDataAndType(path, "image/png")
    }
}

/**
 * Intent to open edit image chooser.
 */
fun editImageChooserIntent(context: Context, path: Uri): Intent {
    editImageIntent(path).apply {
        return Intent.createChooser(this, context.getString(R.string.notification_app_chooser_edit))
    }
}

/**
 * Intent to open image file on notification tap.
 */
fun openImageIntent(path: Uri): Intent {
    // Create intent for notification click
    return Intent(Intent.ACTION_VIEW).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        setDataAndType(path, "image/png")
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
 * Delete image from file system and from MediaStore.
 * Returns false if the file could not be deleted from file system,
 * otherwise true even if deleting from Media Store failed
 */
fun deleteImage(context: Context, file: File): Boolean {
    if (!file.exists()) {
        Log.w("Screenshot", "File does not exist: ${file.absoluteFile}")
        return false
    }

    if (!file.canWrite()) {
        Log.w("Screenshot", "File is not writable: ${file.absoluteFile}")
        return false
    }

    if (file.delete()) {
        Utils.p("File deleted from storage: ${file.absoluteFile}")
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = MediaStore.Images.Media.DATA + " = ?"
        val queryArgs = arrayOf(file.absolutePath)
        context.contentResolver.query(uri, projection, selection, queryArgs, null)?.apply {
            if (moveToFirst()) {
                val id = getLong(getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                context.contentResolver.delete(contentUri, null, null)
                Utils.p("File deleted from MediaStore: $contentUri")
            }
            close()
        }
    } else {
        Log.w("Screenshot", "Could not delete file: ${file.absoluteFile}")
        return false
    }

    return true
}
