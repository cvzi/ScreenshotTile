package com.github.cvzi.screenshottile.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.icu.text.SimpleDateFormat
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore.Images
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.CompressionOptions
import com.github.cvzi.screenshottile.OutputStreamResult
import com.github.cvzi.screenshottile.OutputStreamResultSuccess
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.SaveImageResult
import com.github.cvzi.screenshottile.SaveImageResultSuccess
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Date
import java.util.Locale
import kotlin.math.max

private const val TAG = "ImageStorage"

/**
 * New image file in default "Picture" directory. (used only for Android P and lower)
 */
fun createImageFileInDefaultPictureFolder(context: Context, filename: String): File {
    var storageDir: File?
    storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (storageDir == null) {
        // Fallback to "private" data/Package.Name/... directory
        Log.e(
            TAG,
            "createImageFile() Fallback to getExternalFilesDir(Environment.DIRECTORY_PICTURES)"
        )
        storageDir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    }
    val screenshotDir = File(storageDir, TakeScreenshotActivity.SCREENSHOT_DIRECTORY)
    screenshotDir.mkdirs()
    return File(screenshotDir, filename)
}

/**
 * New image file in /data/ "Picture" directory.
 */
fun createAppDataImageFile(context: Context, filename: String): File {
    val storageDir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File(storageDir, filename)
}

/**
 * Get a CompressionsOptions object from the file_format setting
 */
fun compressionPreference(
    context: Context,
    forceDefaultQuality: Boolean = false
): CompressionOptions {
    var prefFileFormat = (context.applicationContext as? App)?.prefManager?.fileFormat
        ?: context.getString(R.string.setting_file_format_value_default)
    val prefFormatQuality = (context.applicationContext as? App)?.prefManager?.formatQuality
        ?: -1
    val parts = prefFileFormat.split("_")
    prefFileFormat = parts[0]
    var quality = if (parts.size > 1) {
        parts[1].toInt()
    } else 100
    if (!forceDefaultQuality && prefFormatQuality > -1) {
        quality = prefFormatQuality
    }

    return CompressionOptions(prefFileFormat, quality)
}

/**
 * Get output stream for an image file
 */
fun createOutputStream(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions,
    date: Date,
    dim: Point,
    useAppData: Boolean,
    directory: String?,
    forceCustomDirectory: Boolean = false
): OutputStreamResult {
    return if (useAppData || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        createOutputStreamLegacy(
            context,
            fileTitle,
            compressionOptions,
            date,
            dim,
            useAppData,
            directory
        )
    } else {
        createOutputStreamMediaStore(
            context,
            fileTitle,
            compressionOptions,
            date,
            dim,
            directory,
            forceCustomDirectory
        )
    }
}

private const val ENOSPC = "enospc"

/**
 * Get output stream for an image file, until Android P
 */
fun createOutputStreamLegacy(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions,
    date: Date,
    dim: Point,
    useAppData: Boolean,
    directory: String?
): OutputStreamResult {
    val filename = if (fileTitle.endsWith(
            ".${compressionOptions.fileExtension}",
            true
        )
    ) fileTitle else "$fileTitle.${compressionOptions.fileExtension}"

    val customDirectory = directory ?: App.getInstance().prefManager.screenshotDirectory

    var imageFile: File? = null
    if (useAppData) {
        imageFile = createAppDataImageFile(context, filename)
    } else if (customDirectory != null) {
        if (customDirectory.startsWith("content://")) {
            return createOutputStreamMediaStore(
                context,
                fileTitle,
                compressionOptions,
                date,
                dim,
                customDirectory
            )
        } else if (customDirectory.startsWith("file://")) {
            imageFile = File(customDirectory.substring(7), filename)
        }
    }
    if (imageFile == null) {
        imageFile = createImageFileInDefaultPictureFolder(context, filename)
    }

    try {
        imageFile.parentFile?.mkdirs()
        imageFile.createNewFile()
    } catch (e: Exception) {
        Log.e(
            TAG,
            "createOutputStreamLegacy() Could not createNewFile() ${imageFile.absolutePath} $e"
        )
        val externalDirectory =
            context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File(externalDirectory, imageFile.name)
        try {
            externalDirectory?.mkdirs()
            imageFile.createNewFile()
            if (BuildConfig.DEBUG) Log.v(
                TAG,
                "createOutputStreamLegacy() Fallback to getExternalFilesDir ${imageFile.absolutePath}"
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Could not createOutputStreamLegacy() for fallback file ${imageFile.absolutePath} $e"
            )
            return OutputStreamResult("Could not create new file")
        }
    }

    if (!imageFile.exists() || !imageFile.canWrite()) {
        Log.e(
            TAG,
            "createOutputStreamLegacy() File ${imageFile.absolutePath} does not exist or is not writable"
        )
        return OutputStreamResult("Cannot write to file")
    }

    val outputStream: FileOutputStream
    try {
        outputStream = imageFile.outputStream()
    } catch (e: FileNotFoundException) {
        val error = e.toString()
        Log.e(TAG, "createOutputStreamLegacy() $error")
        return OutputStreamResult("Could not find output file")
    } catch (e: SecurityException) {
        val error = e.toString()
        Log.e(TAG, "createOutputStreamLegacy() $error")
        return OutputStreamResult("Could not open output file because of a security exception")
    } catch (e: IOException) {
        var error = e.toString()
        Log.e(TAG, "createOutputStreamLegacy() $error")
        return if (error.contains(ENOSPC, ignoreCase = true)) {
            error = context.getString(R.string.error_no_space)
            Log.e(TAG, "createOutputStreamLegacy() $error")
            OutputStreamResult("Could not open output file. $error")
        } else {
            OutputStreamResult("Could not open output file. IOException")
        }
    } catch (e: NullPointerException) {
        val error = e.toString()
        Log.e(TAG, "createOutputStreamLegacy() $error")
        return OutputStreamResult("Could not open output file. $error")
    }
    return OutputStreamResultSuccess(outputStream, imageFile)
}

/**
 * Get output stream for an image file, Android Q+
 */
@Throws(IOException::class)
fun createOutputStreamMediaStore(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions,
    date: Date,
    dim: Point,
    directory: String?,
    forceCustomDirectory: Boolean = false
): OutputStreamResult {
    val filename = if (fileTitle.endsWith(
            ".${compressionOptions.fileExtension}",
            true
        )
    ) fileTitle else "$fileTitle.${compressionOptions.fileExtension}"

    var relativePath: String? = null
    var storageUri = Images.Media.EXTERNAL_CONTENT_URI
    var outputStream: OutputStream? = null
    var dummyPath = ""

    val customDirectory = directory ?: App.getInstance().prefManager.screenshotDirectory

    customDirectory?.let {
        val customDirectoryUri = it.toUri()
        val docDir = DocumentFile.fromTreeUri(context, customDirectoryUri)
        if (docDir != null) {
            val createdFile = docDir.createFile(compressionOptions.mimeType, filename)
            if (createdFile != null && createdFile.canWrite()) {
                outputStream = context.contentResolver.openOutputStream(createdFile.uri)
                if (outputStream != null) {
                    storageUri = createdFile.uri
                    relativePath = ""
                    dummyPath = "${nicePathFromUri(docDir)}/$filename"
                }
            }
        }
        if (forceCustomDirectory && outputStream == null) {
            throw IOException("Could not create writable DocumentFile from\ndirectoryUri=$customDirectoryUri\ndocDir=$docDir")
        }
    }
    if (relativePath == null) {
        relativePath =
            "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}"
    }

    val dateMilliseconds = date.time
    val dateSeconds = dateMilliseconds / 1000
    val contentValues = ContentValues().apply {
        put(Images.ImageColumns.TITLE, fileTitle)
        put(Images.ImageColumns.DISPLAY_NAME, filename)
        put(
            Images.ImageColumns.DESCRIPTION, context.formatLocalizedString(
                R.string.file_description,
                SimpleDateFormat(
                    context.getLocalizedString(R.string.file_description_simple_date_format),
                    Locale.getDefault()
                ).format(Date())
            )
        )
        put(Images.ImageColumns.DATE_ADDED, dateSeconds)
        put(Images.ImageColumns.DATE_MODIFIED, dateSeconds)
        put(Images.ImageColumns.MIME_TYPE, compressionOptions.mimeType)
        if (dim.x > 0 && dim.y > 0) {
            put(Images.ImageColumns.WIDTH, dim.x)
            put(Images.ImageColumns.HEIGHT, dim.y)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(Images.ImageColumns.RESOLUTION, "${dim.x}\u00d7${dim.y}")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(Images.ImageColumns.DATE_TAKEN, dateMilliseconds)
            if (relativePath.isNotBlank()) {
                put(Images.ImageColumns.RELATIVE_PATH, relativePath)
                dummyPath = "$relativePath/$filename"
            }
            put(Images.ImageColumns.IS_PENDING, 1)
        }
    }

    val uri = if (outputStream == null) {
        try {
            context.contentResolver.insert(storageUri, contentValues)
        } catch (e: UnsupportedOperationException) {
            Log.e(TAG, e.stackTraceToString())
            null
        } catch (e: IllegalStateException) {
            Log.e(TAG, e.stackTraceToString())
            null
        } catch (e: SecurityException) {
            Log.e(TAG, e.stackTraceToString())
            null
        } ?: return OutputStreamResult("MediaStore failed to provide a file")
    } else {
        storageUri
    }
    if (outputStream == null) {
        outputStream = context.contentResolver.openOutputStream(uri)
            ?: return OutputStreamResult("Could not open output stream from MediaStore")
    }
    return OutputStreamResultSuccess(
        outputStream,
        null,
        uri,
        contentValues,
        dummyPath
    )
}

/**
 * Get output stream for an existing uri
 * @throws SecurityException if not allowed to write to Uri
 */
fun createOutputStreamForExistingUri(
    context: Context,
    uri: Uri
): OutputStreamResult {
    val outputStream = try {
        context.contentResolver.openOutputStream(uri)
    } catch (e: FileNotFoundException) {
        Log.e(TAG, "createOutputStreamForExistingUri(): ", e)
        null
    } catch (e: IOException) {
        Log.e(TAG, "createOutputStreamForExistingUri(): ", e)
        null
    }
        ?: return OutputStreamResult("Could not open output stream from MediaStore for uri: $uri")
    return OutputStreamResultSuccess(
        outputStream,
        null,
        uri,
        null,
        ""
    )
}

/**
 * Cut cutOutRect from bitmap and return new bitmap,
 * return old bitmap if cutOutRect is null or malformed
 */
fun cutOutBitmap(fullBitmap: Bitmap, cutOutRect: Rect?): Bitmap {
    if (cutOutRect != null
        && cutOutRect.width() > 0
        && cutOutRect.height() > 0
    ) {
        // Constrain cutOutRect to bitmap dimensions
        cutOutRect.left = max(0, cutOutRect.left)
        cutOutRect.top = max(0, cutOutRect.top)
        if (cutOutRect.left + cutOutRect.width() > fullBitmap.width) {
            cutOutRect.right = fullBitmap.width
        }
        if (cutOutRect.top + cutOutRect.height() > fullBitmap.height) {
            cutOutRect.bottom = fullBitmap.height
        }
        if (cutOutRect.width() > 0 && cutOutRect.height() > 0 && cutOutRect.left >= 0 && cutOutRect.top >= 0) {
            return Bitmap.createBitmap(
                fullBitmap,
                cutOutRect.left,
                cutOutRect.top,
                cutOutRect.width(),
                cutOutRect.height()
            )
        }
    }
    return fullBitmap
}

/**
 * Save image to jpg file in default "Picture" storage.
 */
fun saveBitmapToFile(
    context: Context,
    fullBitmap: Bitmap,
    fileNamePattern: String,
    compressionOptions: CompressionOptions = CompressionOptions(),
    cutOutRect: Rect?,
    useAppData: Boolean,
    directory: String?
): SaveImageResult {
    val bitmap = cutOutBitmap(fullBitmap, cutOutRect)

    val date = Date()

    val filename = formatFileName(fileNamePattern, date)

    val outputStreamResult = createOutputStream(
        context,
        filename,
        compressionOptions,
        date,
        Point(bitmap.width, bitmap.height),
        useAppData,
        directory
    )

    if (!outputStreamResult.success && outputStreamResult !is OutputStreamResultSuccess) {
        Log.e(TAG, "saveImageToFile() outputStreamResult.success is false")
        return SaveImageResult(outputStreamResult.errorMessage)
    }

    val result =
        (outputStreamResult as? OutputStreamResultSuccess?)
            ?: return SaveImageResult("Could not create output stream")

    val outputStream: OutputStream = result.fileOutputStream

    // Save image
    if (bitmap.width == 0 || bitmap.height == 0) {
        Log.e(TAG, "saveImageToFile() Bitmap width or height is 0")
        return SaveImageResult("Bitmap is empty")
    }

    val bytes = ByteArrayOutputStream()

    bitmap.compress(compressionOptions.format, compressionOptions.quality, bytes)

    var success = false
    var error = ""
    try {
        outputStream.write(bytes.toByteArray())
        success = true
    } catch (e: FileNotFoundException) {
        error = e.toString()
        Log.e(TAG, "saveImageToFile() $error")
    } catch (e: SecurityException) {
        error = e.toString()
        Log.e(TAG, "saveImageToFile() $error")
    } catch (e: IOException) {
        error = e.toString()
        Log.e(TAG, "saveImageToFile() $error")
        if (error.contains(ENOSPC, ignoreCase = true)) {
            error = context.getString(R.string.error_no_space)
        }
    } catch (e: NullPointerException) {
        error = e.toString()
        Log.e(TAG, "saveImageToFile() $error")
    } finally {
        outputStream.close()
    }

    if (!success) {
        return SaveImageResult("Could not save image file:\n$error")
    }

    return when {
        result.imageFile != null -> {
            if (!useAppData) {
                addImageToGallery(
                    context,
                    result.imageFile.absolutePath,
                    context.getLocalizedString(R.string.file_title),
                    context.formatLocalizedString(
                        R.string.file_description,
                        SimpleDateFormat(
                            context.getLocalizedString(R.string.file_description_simple_date_format),
                            Locale.getDefault()
                        ).format(date)
                    ),
                    compressionOptions.mimeType,
                    date,
                    Point(bitmap.width, bitmap.height)
                )
                App.getInstance().prefManager.screenshotHistoryAdd(
                    PrefManager.ScreenshotHistoryItem(
                        Uri.fromFile(result.imageFile),
                        Date(),
                        result.imageFile
                    )
                )
            }
            SaveImageResultSuccess(bitmap, compressionOptions.mimeType, result.imageFile)
        }

        result.uri != null -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                result.contentValues?.run {
                    clear()
                    put(Images.ImageColumns.IS_PENDING, 0)
                    try {
                        context.contentResolver.update(result.uri, this, null, null)
                    } catch (e: UnsupportedOperationException) {
                        Log.e(TAG, e.stackTraceToString())
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, e.stackTraceToString())
                    }
                }
            }
            App.getInstance().prefManager.screenshotHistoryAdd(
                PrefManager.ScreenshotHistoryItem(
                    result.uri,
                    Date(),
                    null
                )
            )
            SaveImageResultSuccess(
                bitmap,
                compressionOptions.mimeType,
                null,
                result.uri,
                filename,
                result.dummyPath
            )
        }

        else -> SaveImageResult("Could not save image file, no URI")
    }
}

/**
 * Save image to existing uri
 */
fun saveBitmapToFile(
    context: Context,
    fullBitmap: Bitmap,
    uri: Uri,
    compressionOptions: CompressionOptions = CompressionOptions(),
    cutOutRect: Rect?
): SaveImageResult {
    val bitmap = cutOutBitmap(fullBitmap, cutOutRect)

    val outputStreamResult = try {
        createOutputStreamForExistingUri(context, uri)
    } catch (e: SecurityException) {
        Log.e(TAG, "Failed to create output stream for $uri", e)
        null
    } ?: return SaveImageResult("Could not overwrite file, permission denied.")

    if (!outputStreamResult.success && outputStreamResult !is OutputStreamResultSuccess) {
        Log.e(TAG, "saveImageToFile() outputStreamResult.success is false")
        return SaveImageResult(outputStreamResult.errorMessage)
    }

    val result =
        (outputStreamResult as? OutputStreamResultSuccess?)
            ?: return SaveImageResult("Could not create output stream")

    val outputStream: OutputStream = result.fileOutputStream

    if (bitmap.width == 0 || bitmap.height == 0) {
        Log.e(TAG, "saveImageToFile() Bitmap width or height is 0")
        return SaveImageResult("Bitmap is empty")
    }

    val bytes = ByteArrayOutputStream()

    bitmap.compress(compressionOptions.format, compressionOptions.quality, bytes)

    var success = false
    var error = ""
    try {
        outputStream.write(bytes.toByteArray())
        success = true
    } catch (e: FileNotFoundException) {
        error = e.toString()
        Log.e(TAG, "saveImageToFile() $error")
    } catch (e: SecurityException) {
        error = e.toString()
        Log.e(TAG, "saveImageToFile() $error")
    } catch (e: IOException) {
        error = e.toString()
        Log.e(TAG, "saveImageToFile() $error")
        if (error.contains(ENOSPC, ignoreCase = true)) {
            error = context.getString(R.string.error_no_space)
        }
    } catch (e: NullPointerException) {
        error = e.toString()
        Log.e(TAG, "saveImageToFile() $error")
    } finally {
        outputStream.close()
    }

    if (!success) {
        return SaveImageResult("Could not save image file:\n$error")
    }

    return when {
        result.uri != null -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val contentValues = ContentValues().apply {
                        put(Images.ImageColumns.DATE_MODIFIED, Date().time)
                        put(Images.ImageColumns.WIDTH, bitmap.width)
                        put(Images.ImageColumns.HEIGHT, bitmap.height)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            put(
                                Images.ImageColumns.RESOLUTION,
                                "${bitmap.width}\u00d7${bitmap.height}"
                            )
                        }
                    }
                    context.contentResolver.update(result.uri, contentValues, null, null)
                    context.contentResolver.notifyChange(result.uri, null)
                } catch (e: UnsupportedOperationException) {
                    Log.e(TAG, e.stackTraceToString())
                } catch (e: IllegalStateException) {
                    Log.e(TAG, e.stackTraceToString())
                }
            }

            SaveImageResultSuccess(
                bitmap,
                compressionOptions.mimeType,
                null,
                result.uri,
                "Existing file",
                "Overwriting existing file",
            )
        }

        else -> SaveImageResult("Could not save image file, no URI")
    }
}

/**
 * Save image to jpg file in default "Picture" storage.
 */
fun saveImageToFile(
    context: Context,
    image: Image,
    fileNamePattern: String,
    compressionOptions: CompressionOptions = CompressionOptions(),
    cutOutRect: Rect?,
    useAppData: Boolean,
    directory: String? = null
): SaveImageResult {
    val bitmap = imageToBitmap(image, cutOutRect)
    image.close()

    return saveBitmapToFile(
        context,
        bitmap,
        fileNamePattern,
        compressionOptions,
        null,
        useAppData,
        directory
    )
}
