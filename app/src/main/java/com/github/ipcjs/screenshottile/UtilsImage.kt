package com.github.ipcjs.screenshottile

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * Created by cuzi (cuzi@openmail.cc) on 2019/08/23.
 */

const val UTILSIMAGEKT = "UtilsImage.kt"

/**
 * Copy rectangle of image content to new bitmap or complete image if rect is null.
 */
fun imageToBitmap(image: Image, rect: Rect? = null): Bitmap {
    val offset = (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val w = image.width + offset
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return if (rect == null ) {
        Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    } else {
        Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }
}


/**
 * Create notification preview icon with appropriate size according to the device screen.
 */
fun resizeToNotificationIcon(bitmap: Bitmap, screenDensity: Int): Bitmap {
    val maxSize = (min(
        max(screenDensity / 2, TakeScreenshotActivity.NOTIFICATION_PREVIEW_MIN_SIZE),
        TakeScreenshotActivity.NOTIFICATION_PREVIEW_MAX_SIZE
    )).toDouble()

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
    return if (bitmap.height > TakeScreenshotActivity.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) {
        val offsetY = (bitmap.height - TakeScreenshotActivity.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) / 2
        Bitmap.createBitmap(
            bitmap, 0, offsetY, bitmap.width,
            TakeScreenshotActivity.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT
        )
    } else {
        bitmap
    }
}


/**
 * Add image file and information to media store. (used only for Android P and lower)
 */
fun addImageToGallery(
    context: Context,
    filepath: String,
    title: String,
    description: String,
    mimeType: String = "image/jpeg",
    date: Date? = null
): Uri? {
    val dateSeconds = (date?.time ?: System.currentTimeMillis()) / 1000
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.TITLE, title)
        put(MediaStore.Images.Media.DESCRIPTION, description)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(MediaStore.Images.ImageColumns.DATE_ADDED, dateSeconds)
        put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateSeconds)
        @Suppress("DEPRECATION")
        put(MediaStore.MediaColumns.DATA, filepath)
    }
    return context.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}


/**
 * Delete image. Return true on success, false on failure.
 * content:// Uris are deleted from MediaStore
 * file:// Uris are deleted from filesystem and MediaStore
 */
fun deleteImage(context: Context, uri: Uri?): Boolean {
    if (uri == null) {
        Log.e(UTILSIMAGEKT, "Could not delete file: uri is null")
        return false
    }

    uri.normalizeScheme()
    when {
        uri.scheme == "content" -> { // Android Q+
            val deletedRows = context.contentResolver.delete(uri, null, null)
            Log.v(UTILSIMAGEKT, "deleteImage() File deleted from MediaStore ($deletedRows rows deleted)")
            return deletedRows > 0
        }

        uri.scheme == "file" -> { // until Android P
            val path = uri.path
            if (path == null) {
                Log.e(UTILSIMAGEKT, "deleteImage() File path is null. uri=$uri")
                return false
            }

            val file = File(path)

            if (!file.exists()) {
                Log.w(UTILSIMAGEKT, "deleteImage() File does not exist: ${file.absoluteFile}")
                return false
            }

            if (!file.canWrite()) {
                Log.w(UTILSIMAGEKT, "deleteImage() File is not writable: ${file.absoluteFile}")
                return false
            }

            if (file.delete()) {
                Log.v(UTILSIMAGEKT, "deleteImage() File deleted from storage: ${file.absoluteFile}")
                val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Images.Media._ID)
                @Suppress("DEPRECATION")
                val selection = MediaStore.Images.Media.DATA + " = ?"
                val queryArgs = arrayOf(file.absolutePath)
                context.contentResolver.query(externalContentUri, projection, selection, queryArgs, null)?.apply {
                    if (moveToFirst()) {
                        val id = getLong(getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        context.contentResolver.delete(contentUri, null, null)
                        Log.v(UTILSIMAGEKT, "deleteImage() File deleted from MediaStore: $contentUri")
                    }
                    close()
                }
            } else {
                Log.w(UTILSIMAGEKT, "deleteImage() Could not delete file: ${file.absoluteFile}")
                return false
            }
        }
        else -> {
            Log.e(UTILSIMAGEKT, "deleteImage() Could not delete file. Unknown error. uri=$uri")
            return false
        }

    }

    return true
}
