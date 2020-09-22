package com.github.ipcjs.screenshottile

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.media.Image
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.*
import kotlin.math.ceil
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
    val offset =
        (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val w = image.width + offset
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return if (rect == null) {
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
        val offsetY =
            (bitmap.height - TakeScreenshotActivity.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) / 2
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
    when (uri.scheme) {
        "content" -> { // Android Q+
            return deleteContentResolver(context, uri)
        }

        "file" -> { // until Android P
            val path = uri.path
            if (path == null) {
                Log.e(UTILSIMAGEKT, "deleteImage() File path is null. uri=$uri")
                return false
            }

            val file = File(path)

            return deleteFileSystem(context, file)
        }
        else -> {
            Log.e(UTILSIMAGEKT, "deleteImage() Could not delete file. Unknown error. uri=$uri")
            return false
        }

    }
}

/**
 * Delete file via DocumentFile
 */
fun deleteDocumentFile(context: Context, uri: Uri): Boolean {
    val docDir = DocumentFile.fromSingleUri(context, uri)
    if (docDir != null) {
        if (!docDir.isFile) {
            return false
        }
        return try {
            docDir.delete()
        } catch (e: SecurityException) {
            Log.v(
                UTILSIMAGEKT,
                "SecurityException in deleteDocumentFile($context, $uri)"
            )
            false
        }
    } else {
        return false
    }
}

/**
 * Delete file via contentResolver
 */
fun deleteContentResolver(context: Context, uri: Uri): Boolean {
    val deletedRows = try {
        context.contentResolver.delete(uri, null, null)
    } catch (e: UnsupportedOperationException) {
        // Try to delete DocumentFile in custom directory
        if (App.getInstance().prefManager.screenshotDirectory != null) {
            return deleteDocumentFile(context, uri)
        }
        0
    } catch (e: SecurityException) {
        // Try to delete DocumentFile in custom directory
        if (App.getInstance().prefManager.screenshotDirectory != null) {
            return deleteDocumentFile(context, uri)
        }
        0
    }
    Log.v(
        UTILSIMAGEKT,
        "deleteImage() File deleted from MediaStore ($deletedRows rows deleted)"
    )
    return deletedRows > 0
}

/**
 * Delete file from file system and MediaStore
 */
fun deleteFileSystem(context: Context, file: File): Boolean {
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
        context.contentResolver.query(
            externalContentUri,
            projection,
            selection,
            queryArgs,
            null
        )?.apply {
            if (moveToFirst()) {
                val id = getLong(getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                context.contentResolver.delete(contentUri, null, null)
                Log.v(
                    UTILSIMAGEKT,
                    "deleteImage() File deleted from MediaStore: $contentUri"
                )
            }
            close()
        }
        return true
    } else {
        Log.w(UTILSIMAGEKT, "deleteImage() Could not delete file: ${file.absoluteFile}")
        return false
    }
}


fun statusBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        context.resources.getDimensionPixelSize(resourceId)
    } else {
        ceil(24 * context.resources.displayMetrics.density).toInt()
    }
}

/**
 * navigationBarSize, appUsableScreenSize, realScreenSize
 * From: https://stackoverflow.com/a/29609679/
 *
 */
fun navigationBarSize(context: Context): Point {
    val appUsableSize: Point = appUsableScreenSize(context)
    val realScreenSize: Point = realScreenSize(context)
    return when {
        // navigation bar on the side
        appUsableSize.x < realScreenSize.x -> Point(
            realScreenSize.x - appUsableSize.x,
            appUsableSize.y
        )
        // navigation bar at the bottom
        appUsableSize.y < realScreenSize.y -> Point(
            appUsableSize.x,
            realScreenSize.y - appUsableSize.y
        )
        // navigation bar is not present
        else -> Point()
    }
}

fun appUsableScreenSize(context: Context): Point {
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return Point().apply {
        // TODO Deprecated https://developer.android.com/reference/android/view/WindowMetrics#getBounds()
        windowManager.defaultDisplay.getSize(this)
    }
}

fun realScreenSize(activity: Activity): Point {
    val windowManager =
        activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return Point().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display?.getRealSize(this)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(this)
        }
    }
}

fun realScreenSize(context: Context): Point {
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return Point().apply {
            windowManager.defaultDisplay.getRealSize(this)
    }
}

