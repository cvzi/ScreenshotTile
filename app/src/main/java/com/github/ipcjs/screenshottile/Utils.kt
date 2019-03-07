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
import android.icu.util.Calendar
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_PREVIEW_MAX_SIZE
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_PREVIEW_MIN_SIZE
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
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
    val w = image.width + (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
}

/**
 * Add image file and information to media store.
 */
fun addImageToGallery(context: Context, filepath: String, title: String, description: String, mimeType: String = "image/jpeg"): Uri {
    val values = ContentValues()
    values.put(Images.Media.TITLE, title)
    values.put(Images.Media.DESCRIPTION, description)
    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
    values.put(Images.Media.MIME_TYPE, mimeType)
    values.put(MediaStore.MediaColumns.DATA, filepath)
    return context.contentResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values)!!
}

/**
 * New image file in default "Picture" directory.
 */
fun createImageFile(filename: String): File {
    val storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)!!
    val screenshotDir = File(storageDir, TakeScreenshotActivity.SCREENSHOT_DIRECTORY)
    screenshotDir.mkdirs()
    return File(screenshotDir, filename)
}

/**
 * Save image to jpg file in default "Picture" storage with filename="{$prefix}yyyyMMdd_HHmmss".
 */
fun saveImageToFile(context: Context, image: Image, prefix: String): Pair<File, Bitmap> {
    val date = Date()
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(date)
    val filename = "$prefix$timeStamp"
    val imageFile = createImageFile("$filename.png")

    // Save image
    val bitmap = imageToBitmap(image)
    image.close()
    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)

    imageFile.createNewFile()
    val fileOutputStream = FileOutputStream(imageFile)
    fileOutputStream.write(bytes.toByteArray())
    fileOutputStream.close()

    // Add to g
    addImageToGallery(context, imageFile.absolutePath, context.getString(R.string.file_title), context.getString(R.string.file_description, SimpleDateFormat(context.getString(R.string.file_description_simpledateformat), Locale.getDefault()).format(date)))

    return Pair(imageFile, bitmap)
}

/**
 * Create notification channel (if it does not exists) and return its name.
 */
fun createNotificationScreenshotTakenChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_title)
        val channelDescription = context.getString(R.string.notification_channel_description)

        val notificationManager = context.applicationContext.getSystemService(NotificationManager::class.java) as NotificationManager

        var channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN)
        if (channel == null) {
            channel = NotificationChannel(NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN, channelName, NotificationManager.IMPORTANCE_LOW).apply {
                description = channelDescription
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
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
 * Show a notification that opens the image file on tap.
 */
fun createNotification(context: Context, path: Uri, bitmap: Bitmap) {
    val appContext = context.applicationContext

    val uniqueId = (System.currentTimeMillis() and 0xfffffff).toInt() // notification id and pending intent request code must be unique for each notification

    val contentPendingIntent = PendingIntent.getActivity(appContext, uniqueId + 1, openImageIntent(appContext, path), 0)

    // Create notification
    val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(appContext, createNotificationScreenshotTakenChannel(appContext))
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(appContext)
    }
    with(builder) {
        setWhen(Calendar.getInstance().timeInMillis)
        setShowWhen(true)
        setContentTitle(appContext.getString(R.string.notification_title))
        setContentText(appContext.getString(R.string.notification_body))
        setSmallIcon(R.drawable.ic_stat_name)
        setLargeIcon(bitmap)
        setContentIntent(contentPendingIntent)
        setAutoCancel(true)
    }

    val icon = Icon.createWithResource(appContext, R.drawable.ic_stat_name) // This is not shown on Android 7+ anyways so let's just use the app icon

    val deleteIntent = actionButtonIntent(path, uniqueId, NOTIFICATION_ACTION_DELETE)
    val pendingIntentDelete = PendingIntent.getBroadcast(appContext, uniqueId + 2, deleteIntent, 0)
    builder.addAction(Notification.Action.Builder(icon, appContext.getString(R.string.notification_delete_screenshot), pendingIntentDelete).build())

    val shareIntent = actionButtonIntent(path, uniqueId, NOTIFICATION_ACTION_SHARE)
    val pendingIntentShare = PendingIntent.getBroadcast(appContext, uniqueId + 3, shareIntent, 0)
    builder.addAction(Notification.Action.Builder(icon, appContext.getString(R.string.notification_share_screenshot), pendingIntentShare).build())

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
 * Intent to open image file on notification tap.
 */
fun openImageIntent(context: Context, path: Uri): Intent {
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