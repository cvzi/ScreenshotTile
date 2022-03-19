package com.github.cvzi.screenshottile.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.icu.text.SimpleDateFormat
import android.media.Image
import android.net.Uri
import android.os.*
import android.os.Environment.getExternalStoragePublicDirectory
import android.provider.MediaStore.Images
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import java.io.*
import java.net.URLDecoder
import java.util.*
import kotlin.math.max
import kotlin.random.Random


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


const val UTILSKT = "Utils.kt"

/**
 * Start screenshot activity and take a screenshot
 */
fun screenshot(context: Context, partial: Boolean = false) {
    if (partial || !tryNativeScreenshot()) {
        TakeScreenshotActivity.start(context, partial)
    }
}

/**
 * Try to take a screenshot from the accessibility service/system method if it's enabled and available
 */
fun tryNativeScreenshot(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && App.getInstance().prefManager.useNative) {
        return ScreenshotAccessibilityService.instance?.simulateScreenshotButton(
            autoHideButton = true,
            autoUnHideButton = true
        ) ?: false
    }
    return false
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
        Log.e(
            UTILSKT,
            "createImageFile() Fallback to getExternalFilesDir(Environment.DIRECTORY_PICTURES)"
        )
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
        "webp" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (quality == 100) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    Bitmap.CompressFormat.WEBP_LOSSY
                }
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
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
    val fileTitle: String? = null,
    val dummyPath: String = ""
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
    val contentValues: ContentValues? = null,
    val dummyPath: String = ""
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
    date: Date,
    dim: Point
): OutputStreamResult {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        createOutputStreamLegacy(context, fileTitle, compressionOptions, date, dim)
    } else {
        createOutputStreamMediaStore(context, fileTitle, compressionOptions, date, dim)
    }
}

/**
 * Get output stream for an image file, until Android P
 */
fun createOutputStreamLegacy(
    context: Context,
    fileTitle: String,
    compressionOptions: CompressionOptions,
    date: Date,
    dim: Point
): OutputStreamResult {
    val filename = if (fileTitle.endsWith(
            ".${compressionOptions.fileExtension}",
            true
        )
    ) fileTitle else "$fileTitle.${compressionOptions.fileExtension}"

    val customDirectory = App.getInstance().prefManager.screenshotDirectory

    var imageFile: File? = null
    if (customDirectory != null) {
        if (customDirectory.startsWith("content://")) {
            return createOutputStreamMediaStore(context, fileTitle, compressionOptions, date, dim)
        } else if (customDirectory.startsWith("file://")) {
            imageFile = File(customDirectory.substring(7), filename)
        }
    }
    if (imageFile == null || !imageFile.canWrite()) {
        imageFile = createImageFile(context, filename)
    }

    try {
        imageFile.parentFile?.mkdirs()
        imageFile.createNewFile()
    } catch (e: Exception) {
        // Try again to fallback to "private" data/Package.Name/... directory
        Log.e(
            UTILSKT,
            "createOutputStreamLegacy() Could not createNewFile() ${imageFile.absolutePath} $e"
        )
        val directory =
            context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File(directory, imageFile.name)
        try {
            directory?.mkdirs()
            imageFile.createNewFile()
            if (BuildConfig.DEBUG) Log.v(
                UTILSKT,
                "createOutputStreamLegacy() Fallback to getExternalFilesDir ${imageFile.absolutePath}"
            )
        } catch (e: Exception) {
            Log.e(
                UTILSKT,
                "Could not createOutputStreamLegacy() for fallback file ${imageFile.absolutePath} $e"
            )
            return OutputStreamResult("Could not create new file")
        }
    }

    if (!imageFile.exists() || !imageFile.canWrite()) {
        Log.e(
            UTILSKT,
            "createOutputStreamLegacy() File ${imageFile.absolutePath} does not exist or is not writable"
        )
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
    date: Date,
    dim: Point
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
    App.getInstance().prefManager.screenshotDirectory?.let {
        // Use DocumentFile for custom directory
        val customDirectoryUri = Uri.parse(it)
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
    }
    if (relativePath == null) {
        relativePath =
            "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}"
    }

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

        put(Images.ImageColumns.DATE_ADDED, dateSeconds)
        put(Images.ImageColumns.DATE_MODIFIED, dateSeconds)
        put(Images.ImageColumns.MIME_TYPE, compressionOptions.mimeType)
        if (dim.x > 0 && dim.y > 0) {
            put(Images.ImageColumns.WIDTH, 0)
            put(Images.ImageColumns.HEIGHT, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(Images.ImageColumns.RESOLUTION, "${dim.x}\u00d7${dim.y}")
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(Images.ImageColumns.DATE_TAKEN, dateMilliseconds)
            if (!relativePath.isNullOrBlank()) {
                put(Images.ImageColumns.RELATIVE_PATH, relativePath)
                dummyPath = "$relativePath/$filename"
            }
            put(Images.ImageColumns.IS_PENDING, 1)
        }
    }

    val uri = if (outputStream == null) {
        resolver.insert(storageUri, contentValues)
            ?: return OutputStreamResult("MediaStore failed to provide a file")
    } else {
        storageUri
    }
    if (outputStream == null) {
        outputStream = resolver.openOutputStream(uri)
            ?: return OutputStreamResult("Could not open output stream from MediaStore")
    }
    return OutputStreamResultSuccess(
        outputStream ?: return OutputStreamResult("MediaStore output stream is null"),
        null,
        uri,
        contentValues,
        dummyPath
    )
}

/**
 * Format the filename
 */
fun formatFileName(fileNamePattern: String, date: Date): String {
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)
    val counter = App.getInstance().prefManager.screenshotCount.toString()

    var fileName = fileNamePattern.replace("%timestamp%", timeStamp)
    fileName = fileName.replace("%counter%", counter.padStart(max(5, counter.length), '0'))
    while (fileName.contains("%randint%")) {
        val randInt = Random.Default.nextInt(0, Int.MAX_VALUE).toString()
            .padStart(Int.MAX_VALUE.toString().length, '0')
        fileName = fileName.replaceFirst("%randint%", randInt)
    }
    while (fileName.contains("%random%")) {
        fileName.replaceFirst("%random%", UUID.randomUUID().toString())
    }
    return fileName
}

/**
 * Save image to jpg file in default "Picture" storage.
 */
fun saveBitmapToFile(
    context: Context,
    fullBitmap: Bitmap,
    fileNamePattern: String,
    compressionOptions: CompressionOptions = CompressionOptions(),
    cutOutRect: Rect?
): SaveImageResult {
    val bitmap = if (cutOutRect != null) {
        Bitmap.createBitmap(
            fullBitmap,
            cutOutRect.left,
            cutOutRect.top,
            cutOutRect.width(),
            cutOutRect.height()
        )
    } else {
        fullBitmap
    }

    val date = Date()

    val filename = formatFileName(fileNamePattern, date)

    val outputStreamResult = createOutputStream(
        context,
        filename,
        compressionOptions,
        date,
        Point(bitmap.width, bitmap.height)
    )

    if (!outputStreamResult.success && outputStreamResult !is OutputStreamResultSuccess) {
        Log.e(UTILSKT, "saveImageToFile() outputStreamResult.success is false")
        return SaveImageResult(outputStreamResult.errorMessage)
    }

    val result =
        (outputStreamResult as? OutputStreamResultSuccess?)
            ?: return SaveImageResult("Could not create output stream")

    val outputStream: OutputStream = result.fileOutputStream

    // Save image
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
                date,
                Point(bitmap.width, bitmap.height)
            )
            SaveImageResultSuccess(bitmap, compressionOptions.mimeType, result.imageFile)
        }
        result.uri != null -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                result.contentValues?.run {
                    this.clear()
                    this.put(Images.ImageColumns.IS_PENDING, 0)
                    try {
                        context.contentResolver.update(result.uri, this, null, null)
                    } catch (e: UnsupportedOperationException) {
                        // This happens if the file was created with DocumentFile instead of the contentResolver
                        /*
                        // Seems like this is not necessary and causes issue cvzi/ScreenshotTile#94
                        addImageToGallery(
                            context,
                            result.uri.toString(),
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
                            date,
                            Point(bitmap.width, bitmap.height)
                        )
                        */
                    }
                }
            }
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
 * Save image to jpg file in default "Picture" storage.
 */
fun saveImageToFile(
    context: Context,
    image: Image,
    fileNamePattern: String,
    compressionOptions: CompressionOptions = CompressionOptions(),
    cutOutRect: Rect?
): SaveImageResult {

    val bitmap = imageToBitmap(image, cutOutRect)
    image.close()

    return saveBitmapToFile(context, bitmap, fileNamePattern, compressionOptions, null)
}


/**
 * Find the cache directory with maximum free space
 */
@Suppress("unused")
fun getCacheMaxFreeSpace(context: Context): File? {
    val cacheDirs = context.externalCacheDirs
    if (cacheDirs.isNullOrEmpty()) {
        return null
    }
    val maxIndex =
        cacheDirs.indices.maxByOrNull { index ->
            if (cacheDirs[index] == null) {
                0
            } else {
                StatFs(cacheDirs[index].path).availableBytes
            }
        } ?: -1
    if (maxIndex == -1) {
        return null
    }
    return cacheDirs[maxIndex]
}

/**
 * Try to generate a path that can be understood by humans
 */
fun nicePathFromUri(documentFile: DocumentFile): String {
    return if (documentFile.name.isNullOrEmpty()) {
        nicePathFromUri(documentFile.uri)
    } else {
        documentFile.name.toString()
    }
}

/**
 * Try to generate a path that can be understood by humans
 */
fun nicePathFromUri(uri: Uri): String {
    return nicePathFromUri(uri.toString())
}

/**
 * Try to generate a path that can be understood by humans
 */
fun nicePathFromUri(str: String?): String {
    if (str == null) {
        return "null"
    }
    var path = URLDecoder.decode(str.toString(), "UTF-8")
    path = path.split("/").last()
    if (path.startsWith("primary:")) {
        path = path.substring(8)
    }
    return path
}

/**
 * Adjust font size to fill the available space of a text view
 */
fun fillTextHeight(textView: TextView, maxHeight: Int, startSize: Float? = null) {
    var currentTextSize: Float = startSize ?: textView.textSize
    val text = textView.text.toString()
    val bounds = Rect()
    val paint = Paint().apply {
        textView.typeface
        textSize = currentTextSize
        getTextBounds(text, 0, text.length, bounds)
    }
    while (bounds.height() > maxHeight) {
        currentTextSize--
        paint.run {
            textSize = currentTextSize
            getTextBounds(text, 0, text.length, bounds)
        }
    }
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentTextSize)
}

/**
 * Was the app updated or newly installed
 */
fun isNewAppInstallation(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo(
            context.packageName,
            0
        ).firstInstallTime == context.packageManager.getPackageInfo(
            context.packageName,
            0
        ).lastUpdateTime
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e(UTILSKT, "Package not found", e)
        true
    } catch (e: java.lang.Exception) {
        Log.e(UTILSKT, "Unexpected error in isNewAppInstallation()", e)
        false
    }
}

data class ClickableStringResult(
    val builder: SpannableStringBuilder,
    val activities: List<String>
)

/**
 * Make activity links clickable. Example: "This is a link [Tutorial,.TutorialActivity] to the tutorial"
 */
fun makeActivityClickableFromText(text: String, context: Context): ClickableStringResult {
    val builder = SpannableStringBuilder("")
    val activities = ArrayList<String>()
    for (content in text.split("]")) {
        val startIndex = content.indexOf("[")
        if (startIndex == -1) {
            builder.append(content)
            continue
        }
        val value = content.subSequence(startIndex, content.length).trim()
        val labelEnd = value.indexOf(',')
        val activityName = value.subSequence(labelEnd + 1, value.length).trim()
        activities.add("com.github.cvzi.screenshottile.activities$activityName")
        val label = value.subSequence(1, labelEnd).trim()
        var newContent = content.subSequence(0, startIndex).toString()
        newContent += label
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(textView: View) {
                val intent = Intent()
                intent.setClassName(
                    context,
                    "com.github.cvzi.screenshottile.activities$activityName"
                )
                context.startActivity(intent)
            }
        }

        val spannableString = SpannableString(newContent)
        spannableString.setSpan(
            clickableSpan,
            startIndex,
            startIndex + label.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.append(spannableString)
    }
    return ClickableStringResult(builder, activities)
}

/**
 * Check if F-Droid client is installed
 */
fun hasFdroid(context: Context): Boolean {
    val packageManager = context.packageManager
    for (name in arrayOf(
        "org.fdroid.fdroid",
        "org.fdroid.basic"
    )) {
        try {
            packageManager.getPackageInfo(name, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(UTILSKT, e.toString())
        }
    }
    return false
}

enum class ToastType {
    SUCCESS, ERROR, NAGGING
}

/**
 * Show a string as a Toast message
 */
fun Context?.toastMessage(text: String, toastType: ToastType, duration: Int = Toast.LENGTH_LONG) {
    this?.run {
        val prefManager = App.getInstance().prefManager
        val showToast = prefManager.toasts && when (toastType) {
            ToastType.SUCCESS -> prefManager.successToasts
            ToastType.ERROR -> prefManager.errorToasts
            ToastType.NAGGING -> prefManager.naggingToasts
        }
        if (showToast) {
            Toast.makeText(this, text, duration).show()
        } else if (BuildConfig.DEBUG) {
            Log.v("SUPPRESSED_TOAST", text)
        }
    }
}

/**
 * Show a string from a resource as a Toast message
 */
fun Context?.toastMessage(resource: Int, toastType: ToastType, duration: Int = Toast.LENGTH_LONG) {
    this?.toastMessage(getString(resource), toastType, duration)
}

/**
 * A message
 */
fun aMessage(context: Context) {
    if (Locale.getDefault().country != "RU" ||
        hasFdroid(context) ||
        !App.getInstance().prefManager.naggingToasts ||
        App.getInstance().prefManager.screenshotCount % 3 != 0
    ) {
        return
    }
    val m0 =
        "\ud83c\uddf7\ud83c\uddfa \u0420\u043e\u0441\u0441\u0438\u044f\u043d\u0435\u003a \u0412\u0430\u0448\u0435 \u043f\u0440\u0430\u0432\u0438\u0442\u0435\u043b\u044c\u0441\u0442\u0432\u043e \u043b\u0436\u0435\u0442 \u0432\u0430\u043c \u043e\u0431 \u0423\u043a\u0440\u0430\u0438\u043d\u0435 \ud83c\uddfa\ud83c\udde6"
    val m1 =
        "\u0423\u0431\u0438\u0432\u0430\u044e\u0442 \u043d\u0435\u0432\u0438\u043d\u043d\u044b\u0445 \u043b\u044e\u0434\u0435\u0439 \u0438 \u0434\u0435\u0442\u0435\u0439"
    val m2 =
        "\u041f\u043e\u0436\u0430\u043b\u0443\u0439\u0441\u0442\u0430\u002c \u043d\u0430\u0439\u0434\u0438\u0442\u0435 \u0441\u043f\u043e\u0441\u043e\u0431 \u0443\u0437\u043d\u0430\u0442\u044c \u043f\u0440\u0430\u0432\u0434\u0443"
    Handler(Looper.getMainLooper()).postDelayed({
        context.toastMessage(m0, ToastType.NAGGING)
    }, 4000)
    Handler(Looper.getMainLooper()).postDelayed({
        context.toastMessage(m1, ToastType.NAGGING)
    }, 8000)
    Handler(Looper.getMainLooper()).postDelayed({
        context.toastMessage(m2, ToastType.NAGGING)
    }, 12000)
}
