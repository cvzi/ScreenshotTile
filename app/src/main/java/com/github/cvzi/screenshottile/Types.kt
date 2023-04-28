package com.github.cvzi.screenshottile

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.text.SpannableStringBuilder
import com.github.cvzi.screenshottile.utils.mimeFromFileExtension
import java.io.File
import java.io.OutputStream
import java.io.Serializable

/**
 * Holds a single boolean value
 */
class MutableBoolean(var value: Boolean)


/**
 * Category of a toast message, so messages can be hidden by category
 */
enum class ToastType {
    SUCCESS, ERROR, NAGGING, ACTIVITY
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
    val mimeType = mimeFromFileExtension(fileExtension)
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


data class ClickableStringResult(
    val builder: SpannableStringBuilder,
    val activities: List<String>
)