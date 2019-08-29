package com.github.ipcjs.screenshottile

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.icu.text.SimpleDateFormat
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore.Images
import android.util.Log

import java.io.*
import java.util.*


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


const val UTILSKT = "Utils.kt"

/**
 * Start screenshot activity and take a screenshot
 */
fun screenshot(context: Context, partial: Boolean = false) {
    TakeScreenshotActivity.start(context, partial)
}


/**
 * New image file in default "Picture" directory. (used only for Android P and lower)
 */
fun createImageFile(context: Context, filename: String): File {
    var storageDir: File?
    @Suppress("DEPRECATION")
    storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (storageDir == null) {
        // Fallback to "private" data/Package.Name/... directory
        Log.e(UTILSKT, "createImageFile() Fallback to getExternalFilesDir(Environment.DIRECTORY_PICTURES)")
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
 * Result of saveImageToFile()
 */
open class SaveImageResult(
    val errorMessage: String = "",
    val success: Boolean = false
) : Serializable {
    /**
     * Returns string representation
     */
    override fun toString(): String = "SaveImageResult($errorMessage)"
}

/**
 * Successful result of saveImageToFile()
 */
data class SaveImageResultSuccess(
    val bitmap: Bitmap,
    val mimeType: String,
    val file: File?,
    val uri: Uri? = null,
    val fileTitle: String? = null
) : SaveImageResult("", true) {
    override fun toString(): String = "SaveImageResultSuccess($file)"
}

/**
 * Result of createOutputStream()
 */
open class OutputStreamResult(
    val errorMessage: String = "",
    val success: Boolean = false
) : Serializable {
    /**
     * Returns string representation
     */
    override fun toString(): String = "OutputStreamResult($errorMessage)"
}

/**
 * Successful Result of createOutputStream()
 */
data class OutputStreamResultSuccess(
    val fileOutputStream: OutputStream,
    val imageFile: File?,
    val uri: Uri? = null,
    val contentValues: ContentValues? = null
) : OutputStreamResult("", true) {
    override fun toString(): String = "OutputStreamResultSuccess()"
}

/**
 * Get output stream for an image file
 */
fun createOutputStream(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions,
    date: Date
): OutputStreamResult {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        createOutputStreamLegacy(context, fileTitle, compressionOptions)
    } else {
        createOutputStreamMediaStore(context, fileTitle, compressionOptions, date)
    }
}

/**
 * Get output stream for an image file, until Android P
 */
fun createOutputStreamLegacy(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions
): OutputStreamResult {

    val filename = "$fileTitle.${compressionOptions.fileExtension}"

    var imageFile = createImageFile(context, filename)

    try {
        imageFile.parentFile?.mkdirs()
        imageFile.createNewFile()
    } catch (e: Exception) {
        // Try again to fallback to "private" data/Package.Name/... directory
        Log.e(UTILSKT, "createOutputStreamLegacy() Could not createNewFile() ${imageFile.absolutePath} $e")
        val directory = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File(directory, imageFile.name)
        try {
            directory?.mkdirs()
            imageFile.createNewFile()
            Log.i(UTILSKT, "createOutputStreamLegacy() Fallback to getExternalFilesDir ${imageFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(
                UTILSKT,
                "Could not createOutputStreamLegacy() for fallback file ${imageFile.absolutePath} $e"
            )
            return OutputStreamResult("Could not create new file")
        }
    }

    if (!imageFile.exists() || !imageFile.canWrite()) {
        Log.e(UTILSKT, "createOutputStreamLegacy() File ${imageFile.absolutePath} does not exist or is not writable")
        return OutputStreamResult("Cannot write to file")
    }

    val outputStream: FileOutputStream
    try {
        outputStream = imageFile.outputStream()
    } catch (e: FileNotFoundException) {
        val error = e.toString()
        Log.e(UTILSKT, "createOutputStreamLegacy() $error")
        return OutputStreamResult("Could not find output file")
    } catch (e: SecurityException) {
        val error = e.toString()
        Log.e(UTILSKT, "createOutputStreamLegacy() $error")
        return OutputStreamResult("Could not open output file because of a security exception")
    } catch (e: IOException) {
        var error = e.toString()
        Log.e(UTILSKT, "createOutputStreamLegacy() $error")
        return if (error.contains("enospc", ignoreCase = true)) {
            error = "No space left on internal device storage"
            Log.e(UTILSKT, "createOutputStreamLegacy() $error")
            OutputStreamResult("Could not open output file. No space left on internal device storage")
        } else {
            OutputStreamResult("Could not open output file. IOException")
        }
    } catch (e: NullPointerException) {
        val error = e.toString()
        Log.e(UTILSKT, "createOutputStreamLegacy() $error")
        return OutputStreamResult("Could not open output file. $error")
    }
    return OutputStreamResultSuccess(outputStream, imageFile)
}

/**
 * Get output stream for an image file, Android Q+
 */
fun createOutputStreamMediaStore(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions,
    date: Date
): OutputStreamResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return OutputStreamResult("Dummy return")
    }

    val filename = "$fileTitle.${compressionOptions.fileExtension}"

    val resolver = context.contentResolver
    val dateMilliseconds = date.time
    val dateSeconds = dateMilliseconds / 1000
    val contentValues = ContentValues().apply {
        put(Images.ImageColumns.TITLE, fileTitle)
        put(Images.ImageColumns.DISPLAY_NAME, filename)
        put(
            Images.ImageColumns.DESCRIPTION, context.getString(
                R.string.file_description,
                SimpleDateFormat(
                    context.getString(R.string.file_description_simple_date_format),
                    Locale.getDefault()
                ).format(Date())
            )
        )
        put(Images.ImageColumns.DATE_TAKEN, dateMilliseconds)
        put(Images.ImageColumns.DATE_ADDED, dateSeconds)
        put(Images.ImageColumns.DATE_MODIFIED, dateSeconds)
        put(Images.ImageColumns.MIME_TYPE, compressionOptions.mimeType)
        put(
            Images.ImageColumns.RELATIVE_PATH,
            "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}"
        )
        put(Images.ImageColumns.IS_PENDING, 1)
    }

    val uri = resolver.insert(Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: return OutputStreamResult("MediaStore failed to provide a file")
    val outputStream =
        resolver.openOutputStream(uri) ?: return OutputStreamResult("Could not open output stream from MediaStore")

    return OutputStreamResultSuccess(outputStream, null, uri, contentValues)
}

/**
 * Save image to jpg file in default "Picture" storage with filename="{$prefix}yyyyMMdd_HHmmss".
 */
fun saveImageToFile(
    context: Context,
    image: Image,
    prefix: String,
    compressionOptions: CompressionOptions = CompressionOptions(),
    cutOutRect: Rect?
): SaveImageResult {
    val date = Date()
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)
    val filename = "$prefix$timeStamp"

    val outputStreamResult = createOutputStream(context, filename, compressionOptions, date)

    if (!outputStreamResult.success && outputStreamResult !is OutputStreamResultSuccess) {
        Log.e(UTILSKT, "saveImageToFile() outputStreamResult.success is false")
        return SaveImageResult(outputStreamResult.errorMessage)
    }

    val result =
        (outputStreamResult as? OutputStreamResultSuccess?) ?: return SaveImageResult("Could not create output stream")

    val outputStream: OutputStream = result.fileOutputStream

    // Save image

    val bitmap = imageToBitmap(image, cutOutRect)
    image.close()

    if (bitmap.width == 0 || bitmap.height == 0) {
        Log.e(UTILSKT, "saveImageToFile() Bitmap width or height is 0")
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
        Log.e(UTILSKT, "saveImageToFile() $error")
    } catch (e: SecurityException) {
        error = e.toString()
        Log.e(UTILSKT, "saveImageToFile() $error")
    } catch (e: IOException) {
        error = e.toString()
        Log.e(UTILSKT, "saveImageToFile() $error")
        if (error.contains("enospc", ignoreCase = true)) {
            error = "No space left on internal device storage"
        }

    } catch (e: NullPointerException) {
        error = e.toString()
        Log.e(UTILSKT, "saveImageToFile() $error")
    } finally {
        outputStream.close()
    }

    if (!success) {
        return SaveImageResult("Could not save image file:\n$error")
    }

    // Add to gallery
    return when {
        result.imageFile != null -> {
            addImageToGallery(
                context,
                result.imageFile.absolutePath,
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
                compressionOptions.mimeType,
                date
            )
            SaveImageResultSuccess(bitmap, compressionOptions.mimeType, result.imageFile)
        }
        result.uri != null -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                result.contentValues?.run {
                    this.clear()
                    this.put(Images.ImageColumns.IS_PENDING, 0)
                    context.contentResolver.update(result.uri, this, null, null)
                }
            }
            SaveImageResultSuccess(bitmap, compressionOptions.mimeType, null, result.uri, filename)
        }
        else -> SaveImageResult("Could not save image file, no URI")
    }
}
