package com.github.cvzi.screenshottile.utils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.icu.text.SimpleDateFormat
import android.media.Image
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import com.github.cvzi.screenshottile.*
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import java.io.File
import java.io.InputStream
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
    if (image.format == ImageFormat.JPEG) {
        return imageJPEGToBitmap(image, rect)
    }
    val offset =
        (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val w = image.width + offset
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return if (rect != null && rect.left >= 0 && rect.top >= 0 && rect.width() > 0 && rect.height() > 0) {
        Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    } else {
        Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }
}

/**
 * Copy rectangle of image content to new bitmap or complete image if rect is null.
 * This is not JPEG data despite the format, it's just flat ARGB_8888
 */
fun imageJPEGToBitmap(image: Image, rect: Rect? = null): Bitmap {
    val w = image.width
    val h = image.height

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    image.planes[0].buffer.let {
        it.rewind()
        bitmap.copyPixelsFromBuffer(it)
    }
    return if (rect != null && rect.left >= 0 && rect.top >= 0 && rect.width() > 0 && rect.height() > 0) {
        Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    } else {
        Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
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
    val newWidth =
        max((bitmap.width * ratio).toInt(), TakeScreenshotActivity.NOTIFICATION_PREVIEW_MIN_SIZE)
    val newHeight =
        max((bitmap.height * ratio).toInt(), TakeScreenshotActivity.NOTIFICATION_PREVIEW_MIN_SIZE)
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
    mimeType: String,
    date: Date,
    dim: Point
): Uri? {
    val dateSeconds = date.time / 1000
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.TITLE, title)
        put(MediaStore.Images.Media.DESCRIPTION, description)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(MediaStore.Images.ImageColumns.DATE_ADDED, dateSeconds)
        put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateSeconds)
        if (dim.x > 0 && dim.y > 0) {
            put(MediaStore.Images.ImageColumns.WIDTH, dim.x)
            put(MediaStore.Images.ImageColumns.HEIGHT, dim.y)
        }
        put(MediaStore.MediaColumns.DATA, filepath)
    }
    return try {
        context.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    } catch (e: Exception) {
        Log.e(UTILSIMAGEKT, "Failed to add image to gallery:", e)
        null
    }
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

    when (uri.normalizeScheme().scheme) {
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
            Log.e(
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
        0
    } catch (e: SecurityException) {
        0
    }
    if (deletedRows == 0) {
        return deleteDocumentFile(context, uri)
    }
    if (BuildConfig.DEBUG) Log.v(
        UTILSIMAGEKT,
        "deleteImage() File deleted from MediaStore ($deletedRows rows deleted)"
    )
    return true
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
        if (BuildConfig.DEBUG) Log.v(
            UTILSIMAGEKT,
            "deleteImage() File deleted from storage: ${file.absoluteFile}"
        )
        val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)

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
                if (BuildConfig.DEBUG) Log.v(
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

/**
 * Move image to normal storage location. Return true on success, false on failure.
 * content:// Uris are moved via MediaStore if possible otherwise copied and original file deleted
 * file:// Uris are renamed on filesystem and removed and added to MediaStore
 */
fun moveImageToStorage(context: Context, file: File, newName: String?): Pair<Boolean, Uri?> {
    val compressionOptions = compressionPreference(context)
    val (newFileName, newFileTitle) =
        if (newName == null) {
            val date = Date()
            val name = formatFileName(App.getInstance().prefManager.fileNamePattern, date)
            val filename = if (name.endsWith(
                    ".${compressionOptions.fileExtension}",
                    true
                )
            ) name else "$name.${compressionOptions.fileExtension}"

            fileNameFileTitle(filename, compressionOptions)
        } else {
            fileNameFileTitle(newName, compressionOptions)
        }


    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android Q+
        // Try to copy the image to a new name
        val result = copyImageContentResolver(context, file.inputStream(), newFileTitle)
        if (!result.first) {
            return Pair(false, null)
        }
        // Remove the old file
        if (!deleteImage(context, Uri.fromFile(file))) {
            Log.e(UTILSIMAGEKT, "renameContentResolver() deleteImage failed")
        }
        return result

    } else { // until Android P
        val dest = createImageFileInDefaultPictureFolder(context, newFileName)
        renameFileSystem(context, file, dest)
    }

}

/**
 * Move image. Return true on success, false on failure.
 * content:// Uris are moved via MediaStore if possible otherwise copied and original file deleted
 * file:// Uris are renamed on filesystem and removed and added to MediaStore
 */
fun moveImage(
    context: Context,
    srcFileUri: Uri,
    destFolderUri: Uri,
    newName: String
): Pair<Boolean, Uri?> {
    val (newFileName, newFileTitle) = fileNameFileTitle(newName, compressionPreference(context))
    val destFolderPath = destFolderUri.path
    if (
        srcFileUri.normalizeScheme().scheme == "file" && destFolderUri.normalizeScheme().scheme == "file" && destFolderPath != null) { // until Android P
        val srcPath = srcFileUri.path
        if (srcPath == null) {
            Log.e(UTILSIMAGEKT, "moveImage() File path is null. srcFileUri=$srcFileUri")
            return Pair(false, null)
        }

        val srcFile = srcFileUri.toFile()
        val destFolderFile = File(destFolderPath)
        val dest = File(destFolderFile, newFileName)
        return renameFileSystem(context, srcFile, dest)
    } else if (srcFileUri.normalizeScheme().scheme == "content") { // Android Q+
        return moveContentResolver(context, srcFileUri, destFolderUri, newFileTitle)
    } else {
        Log.e(UTILSIMAGEKT, "moveImage() Could not move file. Unknown error. uri=$srcFileUri")
        return Pair(false, null)
    }
}


/**
 * Rename image (in folder defined in app settings). Return true on success, false on failure.
 * content:// Uris are moved via MediaStore if possible otherwise copied and original file deleted
 * file:// Uris are renamed on filesystem and removed and added to MediaStore
 */
fun renameImage(context: Context, uri: Uri?, newName: String): Pair<Boolean, Uri?> {
    if (uri == null) {
        Log.e(UTILSIMAGEKT, "Could not move file: uri is null")
        return Pair(false, null)
    }

    val (newFileName, newFileTitle) = fileNameFileTitle(newName, compressionPreference(context))

    when (uri.normalizeScheme().scheme) {
        "content" -> { // Android Q+
            return renameContentResolver(context, uri, newFileTitle, newFileName)
        }

        "file" -> { // until Android P
            val path = uri.path
            if (path == null) {
                Log.e(UTILSIMAGEKT, "renameImage() File path is null. uri=$uri")
                return Pair(false, null)
            }

            val file = File(path)
            val dest = createImageFileInDefaultPictureFolder(context, newFileName)

            return renameFileSystem(context, file, dest)
        }

        else -> {
            Log.e(UTILSIMAGEKT, "renameImage() Could not move file. Unknown error. uri=$uri")
            return Pair(false, null)
        }

    }
}

/**
 * Move file via contentResolver
 */
fun moveContentResolver(
    context: Context,
    uri: Uri,
    destFolderUri: Uri,
    newFileTitle: String
): Pair<Boolean, Uri?> {
    // Copy the image to a new name,
    val result = copyImageContentResolver(
        context,
        uri,
        newFileTitle,
        destFolderUri,
        forceCustomDirectory = true
    )
    if (!result.first) {
        return Pair(false, null)
    }
    // Remove the old file
    if (!deleteImage(context, uri)) {
        Log.e(UTILSIMAGEKT, "moveContentResolver() deleteImage failed")
    }
    return result
}


/**
 * Rename file via contentResolver
 */
fun renameContentResolver(
    context: Context,
    uri: Uri,
    newFileTitle: String,
    newFileName: String
): Pair<Boolean, Uri?> {
    // Try to rename file via contentResolver/MediaStore
    val updatedRows = try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
        }
        context.contentResolver.update(uri, contentValues, null, null)
    } catch (e: UnsupportedOperationException) {
        Log.w(
            UTILSIMAGEKT,
            "renameContentResolver() MediaStore move failed: $e\nTrying copy and delete"
        )
        0
    } catch (e: IllegalStateException) {
        Log.w(
            UTILSIMAGEKT,
            "renameContentResolver() MediaStore move failed: $e\nTrying copy and delete"
        )
        0
    }
    if (updatedRows > 0) {
        return Pair(true, uri)
    }
    // Try to copy the image to a new name
    val result = copyImageContentResolver(context, uri, newFileTitle)
    if (!result.first) {
        return Pair(false, null)
    }
    // Remove the old file
    if (!deleteImage(context, uri)) {
        Log.e(UTILSIMAGEKT, "renameContentResolver() deleteImage failed")
    }
    return result
}


/**
 * Copy file via contentResolver from uri
 */
fun copyImageContentResolver(
    context: Context,
    uri: Uri,
    newName: String,
    directory: Uri? = null,
    forceCustomDirectory: Boolean = false
): Pair<Boolean, Uri?> {
    val inputStream: InputStream?
    try {
        inputStream = context.contentResolver.openInputStream(uri)
    } catch (e: Exception) {
        Log.e(UTILSIMAGEKT, "copyImageContentResolver() Could not open input stream: $e")
        return Pair(false, null)
    }
    if (inputStream == null) {
        Log.e(UTILSIMAGEKT, "copyImageContentResolver() input stream is null")
        return Pair(false, null)
    }

    return copyImageContentResolver(context, inputStream, newName, directory, forceCustomDirectory)
}

/**
 * Copy file via contentResolver from inputStream
 */
fun copyImageContentResolver(
    context: Context,
    inputStream: InputStream,
    newName: String,
    directory: Uri? = null,
    forceCustomDirectory: Boolean = false
): Pair<Boolean, Uri?> {
    val outputStreamResult = createOutputStream(
        context,
        newName,
        compressionPreference(context),
        Date(),
        Point(0, 0),
        useAppData = false,
        directory = directory?.toString(),
        forceCustomDirectory
    )
    if (!outputStreamResult.success) {
        Log.e(
            UTILSIMAGEKT,
            "copyImageContentResolver() Could not open output stream: ${outputStreamResult.errorMessage}"
        )
        return Pair(false, null)
    }
    val outputStreamResultSuccess = (outputStreamResult as OutputStreamResultSuccess)
    val outputStream = outputStreamResultSuccess.fileOutputStream
    val success = try {
        val bytes = ByteArray(1024 * 32)
        var count = 0
        while (count != -1) {
            count = inputStream.read(bytes)
            if (count != -1) {
                outputStream.write(bytes, 0, count)
            }
        }
        outputStream.flush()
        inputStream.close()
        outputStream.close()
        true
    } catch (e: Exception) {
        Log.e(UTILSIMAGEKT, "copyImageContentResolver() Error while copying: $e")
        false
    } finally {
        inputStream.close()
        outputStream.close()
    }
    return if (success) {
        if (outputStreamResultSuccess.uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            outputStreamResultSuccess.contentValues?.run {
                this.clear()
                this.put(MediaStore.Images.ImageColumns.IS_PENDING, 0)
                try {
                    context.contentResolver.update(outputStreamResultSuccess.uri, this, null, null)
                } catch (e: UnsupportedOperationException) {
                    Log.e(UTILSKT, e.stackTraceToString())
                } catch (e: IllegalStateException) {
                    Log.e(UTILSKT, e.stackTraceToString())
                }
            }
        }
        Pair(true, outputStreamResultSuccess.uri)
    } else {
        Pair(false, null)
    }
}


/**
 * Rename file from file system and remove and add in MediaStore
 */
fun renameFileSystem(
    context: Context,
    file: File,
    dest: File,
    dimensions: Point? = null
): Pair<Boolean, Uri?> {
    if (!file.exists()) {
        Log.w(UTILSIMAGEKT, "renameFileSystem() File does not exist: ${file.absoluteFile}")
        return Pair(false, null)
    }

    if (dest.exists()) {
        Log.w(UTILSIMAGEKT, "renameFileSystem() File already exists: ${dest.absoluteFile}")
        return Pair(false, null)
    }

    if (!file.canWrite()) {
        Log.w(UTILSIMAGEKT, "renameFileSystem() File is not writable: ${file.absoluteFile}")
        return Pair(false, null)
    }

    val result = if (file.parent == dest.parent) {
        file.renameTo(dest)
    } else {
        try {
            file.copyTo(dest)
            file.delete()
            true
        } catch (e: Exception) {
            Log.e(UTILSIMAGEKT, "renameFileSystem() copyTo failed:", e)
            false
        }
    }

    if (result) {
        if (BuildConfig.DEBUG) Log.v(
            UTILSIMAGEKT,
            "renameFileSystem() File ${file.absoluteFile} moved to ${dest.absoluteFile}"
        )
        val externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)

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
                if (BuildConfig.DEBUG) Log.v(
                    UTILSIMAGEKT,
                    "deleteImage() File deleted from MediaStore: $contentUri"
                )
            }
            close()
        }

        val date = Date()
        addImageToGallery(
            context,
            dest.absolutePath,
            context.getString(R.string.file_title),
            context.getString(
                R.string.file_description,
                SimpleDateFormat(
                    context.getString(R.string.file_description_simple_date_format),
                    Locale.getDefault()
                ).format(
                    date
                )
            ),
            compressionPreference(context).mimeType,
            date,
            dimensions ?: Point(0, 0)
        )

        return Pair(true, Uri.fromFile(dest))
    } else {
        Log.w(UTILSIMAGEKT, "deleteImage() Could not delete file: ${file.absoluteFile}")
        return Pair(false, null)
    }
}

/**
 * Try to get the height of the status bar or return a fallback approximation
 */
@Suppress("unused")
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
@Suppress("unused")
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

/**
 * Screen size that can be used by windows
 */
fun appUsableScreenSize(context: Context): Point {
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.currentWindowMetrics.bounds
        Point(
            bounds.width(),
            bounds.height()
        )
    } else {
        Point().apply {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getSize(this)
        }
    }
}

/**
 * Full screen size including cutouts, adapted to screen orientation
 */
fun realScreenSize(context: Context): Point {
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    return Point().apply {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // display.mode.physical is independent of screen orientation
                context.display?.mode?.let {
                    when (context.display?.rotation) {
                        Surface.ROTATION_90, Surface.ROTATION_270 -> {
                            y = it.physicalWidth
                            x = it.physicalHeight
                        }

                        else -> {
                            x = it.physicalWidth
                            y = it.physicalHeight
                        }
                    }
                } ?: run {
                    // windowManager.currentWindowMetrics.bounds is already adapted to screen orientation
                    windowManager.currentWindowMetrics.bounds.let {
                        x = it.width()
                        y = it.height()
                    }
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                @Suppress("DEPRECATION")
                context.display?.getRealSize(this)
            }

            else -> {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealSize(this)
            }
        }
    }
}

/**
 * Tint image (for debugging)
 */
fun tintImage(bitmap: Bitmap, color: Long): Bitmap? {
    val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(newBitmap).drawBitmap(bitmap, 0f, 0f, Paint().apply {
        colorFilter = PorterDuffColorFilter(color.toInt(), PorterDuff.Mode.ADD)
    })
    return newBitmap
}

/**
 * Split into fileName and fileTitle
 */
fun fileNameFileTitle(s: String, ext: String): Pair<String, String> {
    val extWithDot = ".$ext"
    val fileTitle: String
    val fileName: String
    if (s.endsWith(extWithDot)) {
        fileTitle = s.dropLast(extWithDot.length)
        fileName = s
    } else {
        fileTitle = s
        fileName = s + extWithDot
    }
    return Pair(fileName, fileTitle)
}

/**
 * Split into fileName and fileTitle
 */
fun fileNameFileTitle(s: String, compressionOptions: CompressionOptions): Pair<String, String> {
    return fileNameFileTitle(s, compressionOptions.fileExtension)
}

/**
 * Create new scaled Bitmap with same width/height ratio
 */
fun scaleBitmap(bm: Bitmap, maxWidth: Int, maxHeight: Int): Pair<Bitmap, Float> {
    var scale =
        min(maxWidth.toFloat() / bm.width.toFloat(), maxHeight.toFloat() / bm.height.toFloat())
    var newWidth = (bm.width * scale).toInt()
    var newHeight = (bm.height * scale).toInt()
    if (newWidth <= 0 || newHeight <= 0) {
        newWidth = bm.width
        newHeight = bm.height
        scale = 1f
    }
    return Pair(
        Bitmap.createScaledBitmap(
            bm,
            newWidth,
            newHeight,
            true
        ), scale
    )
}

/**
 * Create new scaled Rect
 */
fun scaleRect(rect: Rect, factor: Float): Rect {
    return Rect(
        (rect.left * factor).toInt(),
        (rect.top * factor).toInt(),
        (rect.right * factor).toInt(),
        (rect.bottom * factor).toInt()
    )
}

/**
 * Create new rotated bitmap
 */
fun Bitmap.rotate(degrees: Float): Bitmap {
    return Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply {
        postRotate(degrees)
    }, true)
}
