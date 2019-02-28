package com.github.ipcjs.screenshottile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore
import android.provider.MediaStore.Images
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
 * Copy image content to new bitmap
 */
fun imageToBitmap(image: Image): Bitmap {
    val w = image.width + (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
}

/**
 * Add image file and information to media store
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
 * New image file in default "Picture" directory
 */
fun createImageFile(filename: String): File {
    val storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)!!
    val screenshotDir = File(storageDir, TakeScreenshotActivity.SCREENSHOT_DIRECTORY)
    screenshotDir.mkdirs()
    return File(screenshotDir, filename)
}

/**
 * Save image to jpg file in default "Picture" storage with filename="{$prefix}yyyyMMdd_HHmmss"
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
 * Create notification channel (if it does not exists) and return its name
 */
fun createNotificationScreenshotTakenChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_title)
        val channelDescription = context.getString(R.string.notification_channel_description)

        val notificationManager = context.getSystemService(NotificationManager::class.java) as NotificationManager

        var channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN)
        if (channel == null) {
            channel =
                    NotificationChannel(NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN, channelName, NotificationManager.IMPORTANCE_LOW)
            with(channel) {
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
 * Create notification preview icon with appropriate size according to the device screen
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
