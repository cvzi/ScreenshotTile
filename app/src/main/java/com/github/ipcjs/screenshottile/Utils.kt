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
import android.graphics.Rect
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
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_BIG_PICTURE_MAX_HEIGHT
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_CHANNEL_FOREGROUND
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_PREVIEW_MAX_SIZE
import com.github.ipcjs.screenshottile.TakeScreenshotActivity.Companion.NOTIFICATION_PREVIEW_MIN_SIZE
import java.io.*
import java.util.*
import kotlin.math.max
import kotlin.math.min


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */

/**
 * Start screenshot activity and take a screenshot
 */
fun screenshot(context: Context, partial: Boolean = false) {
    TakeScreenshotActivity.start(context, partial)
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
 * Copy rectangle of image content to new bitmap.
 */
fun imageCutOutToBitmap(image: Image, rect: Rect): Bitmap {
    val offset = (image.planes[0].rowStride - image.planes[0].pixelStride * image.width) / image.planes[0].pixelStride
    val w = image.width + offset
    val h = image.height
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(image.planes[0].buffer)
    return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
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
        put(Images.Media.TITLE, title)
        put(Images.Media.DESCRIPTION, description)
        put(Images.Media.MIME_TYPE, mimeType)
        put(Images.ImageColumns.DATE_ADDED, dateSeconds)
        put(Images.ImageColumns.DATE_MODIFIED, dateSeconds)
        @Suppress("DEPRECATION")
        put(MediaStore.MediaColumns.DATA, filepath)
    }
    return context.contentResolver?.insert(Images.Media.EXTERNAL_CONTENT_URI, values)
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

data class SaveImageResultSuccess(
    val bitmap: Bitmap,
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
        createOutputStreamLegacy(context, fileTitle, compressionOptions, date)
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
    compressionOptions: CompressionOptions,
    date: Date
): OutputStreamResult {

    val filename = "$fileTitle.${compressionOptions.fileExtension}"

    var imageFile = createImageFile(context, filename)

    try {
        imageFile.parentFile?.mkdirs()
        imageFile.createNewFile()
    } catch (e: Exception) {
        // Try again to fallback to "private" data/Package.Name/... directory
        Log.e("Utils.kt:createOutputStreamLegacy()", "Could not createNewFile() ${imageFile.absolutePath} $e")
        val directory = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        imageFile = File(directory, imageFile.name)
        try {
            directory?.mkdirs()
            imageFile.createNewFile()
            Log.i("Utils.kt:createOutputStreamLegacy()", "Fallback to getExternalFilesDir ${imageFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(
                "Utils.kt:createOutputStreamLegacy()",
                "Could not createOutputStreamLegacy() for fallback file ${imageFile.absolutePath} $e"
            )
            return OutputStreamResult("Could not create new file")
        }
    }

    if (!imageFile.exists() || !imageFile.canWrite()) {
        Log.e("Utils.kt:createOutputStreamLegacy()", "File ${imageFile.absolutePath} does not exist or is not writable")
        return OutputStreamResult("Cannot write to file")
    }

    val outputStream: FileOutputStream
    try {
        outputStream = imageFile.outputStream()
    } catch (e: FileNotFoundException) {
        val error = e.toString()
        Log.e("Utils.kt:createOutputStreamLegacy()", error)
        return OutputStreamResult("Could not find output file")
    } catch (e: SecurityException) {
        val error = e.toString()
        Log.e("Utils.kt:createOutputStreamLegacy()", error)
        return OutputStreamResult("Could not open output file because of a security exception")
    } catch (e: IOException) {
        var error = e.toString()
        Log.e("Utils.kt:createOutputStreamLegacy()", error)
        return if (error.contains("enospc", ignoreCase = true)) {
            error = "No space left on internal device storage"
            Log.e("Utils.kt:createOutputStreamLegacy()", error)
            OutputStreamResult("Could not open output file. No space left on internal device storage")
        } else {
            OutputStreamResult("Could not open output file. IOException")
        }
    } catch (e: NullPointerException) {
        val error = e.toString()
        Log.e("Utils.kt:createOutputStreamLegacy()", error)
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
        Log.e("Utils.kt:saveImageToFile()", "outputStreamResult.success is false")
        return SaveImageResult(outputStreamResult.errorMessage)
    }

    val result =
        (outputStreamResult as? OutputStreamResultSuccess?) ?: return SaveImageResult("Could not create output stream")

    val outputStream: OutputStream = result.fileOutputStream

    // Save image

    val bitmap = if (cutOutRect == null) {
        imageToBitmap(image)
    } else {
        imageCutOutToBitmap(image, cutOutRect)
    }
    image.close()

    if (bitmap.width == 0 || bitmap.height == 0) {
        Log.e("Utils.kt:saveImageToFile()", "Bitmap width or height is 0")
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
        Log.e("Utils.kt:saveImageToFile()", error)
    } catch (e: SecurityException) {
        error = e.toString()
        Log.e("Utils.kt:saveImageToFile()", error)
    } catch (e: IOException) {
        error = e.toString()
        Log.e("Utils.kt:saveImageToFile()", error)
        if (error.contains("enospc", ignoreCase = true)) {
            error = "No space left on internal device storage"
        }

    } catch (e: NullPointerException) {
        error = e.toString()
        Log.e("Utils.kt:saveImageToFile()", error)
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
            SaveImageResultSuccess(bitmap, result.imageFile)
        }
        result.uri != null -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                result.contentValues?.run {
                    this.clear()
                    this.put(Images.ImageColumns.IS_PENDING, 0)
                    context.contentResolver.update(result.uri, this, null, null)
                }
            }
            SaveImageResultSuccess(bitmap, null, result.uri, filename)
        }
        else -> SaveImageResult("Could not save image file, no URI")
    }
}

/**
 * Create notification channel (if it does not exists) and return its name.
 */
fun createNotificationScreenshotTakenChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_channel_description)
        val notificationTitle = context.getString(R.string.notification_title)
        val channelDescription = context.getString(R.string.notification_channel_description) + "\n'$notificationTitle'"

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
 * Create notification channel (if it does not exists) and return its name.
 */
fun createNotificationForegroundServiceChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channelName = context.getString(R.string.notification_foreground_channel_description)
        val notificationTitle = context.getString(R.string.notification_foreground_title)
        val channelDescription =
            context.getString(R.string.notification_foreground_channel_description) + "\n'$notificationTitle'"

        context.applicationContext.getSystemService(NotificationManager::class.java)?.run {
            if (getNotificationChannel(NOTIFICATION_CHANNEL_FOREGROUND) == null) {
                createNotificationChannel(NotificationChannel(
                    NOTIFICATION_CHANNEL_FOREGROUND,
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
    return NOTIFICATION_CHANNEL_FOREGROUND
}


/**
 * Check if the notification channel was disabled by the user
 */
fun notificationScreenshotTakenChannelEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN)
        if (notificationChannel == null) {
            true  // default to true if the channel does not yet exist
        } else {
            notificationChannel.importance != NotificationManager.IMPORTANCE_NONE
        }
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
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
    return if (bitmap.height > NOTIFICATION_BIG_PICTURE_MAX_HEIGHT) {
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
                data = Uri.parse("package:$packageName")
                addCategory(Intent.CATEGORY_DEFAULT)
            }
        }
    }
}

/**
 * Delete image. Return true on success, false on failure.
 * content:// Uris are deleted from MediaStore
 * file:// Uris are deleted from filesystem and MediaStore
 */
fun deleteImage(context: Context, uri: Uri?): Boolean {
    if (uri == null) {
        Log.w("Screenshot", "Could not delete file: uri is null")
        return false
    }

    uri.normalizeScheme()
    when {
        uri.scheme == "content" -> { // Android Q+
            val deletedRows = context.contentResolver.delete(uri, null, null)
            Utils.p("File deleted from MediaStore ($deletedRows rows deleted)")
            return deletedRows > 0
        }

        uri.scheme == "file" -> { // until Android P
            val path = uri.path
            if (path == null) {
                Log.w("Screenshot", "File path is null. uri=$uri")
                return false
            }

            val file = File(path)

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
                val externalContentUri = Images.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(Images.Media._ID)
                @Suppress("DEPRECATION")
                val selection = Images.Media.DATA + " = ?"
                val queryArgs = arrayOf(file.absolutePath)
                context.contentResolver.query(externalContentUri, projection, selection, queryArgs, null)?.apply {
                    if (moveToFirst()) {
                        val id = getLong(getColumnIndexOrThrow(Images.Media._ID))
                        val contentUri = ContentUris.withAppendedId(Images.Media.EXTERNAL_CONTENT_URI, id)
                        context.contentResolver.delete(contentUri, null, null)
                        Utils.p("File deleted from MediaStore: $contentUri")
                    }
                    close()
                }
            } else {
                Log.w("Screenshot", "Could not delete file: ${file.absoluteFile}")
                return false
            }
        }
        else -> {
            Log.w("Screenshot", "Could not delete file. Unknown error. uri=$uri")
            return false
        }

    }

    return true
}
