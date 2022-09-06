package com.github.cvzi.screenshottile.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.core.database.getLongOrNull
import androidx.core.net.toFile
import com.github.cvzi.screenshottile.BuildConfig
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*


class SingleImageLoaded(
    uri: Uri,
    override val fileName: String,
    override val mimeType: String,
    override val bitmap: Bitmap,
    override val lastModified: Date,
    val size: Long?,
    file: File? = null,
    folder: String? = null,
    isAppData: Boolean = false
) : SingleImage(uri, file, folder, isAppData = isAppData)

open class SingleImage(
    val uri: Uri,
    val file: File? = null,
    val folder: String? = null,
    open val lastModified: Date? = null,
    val isAppData: Boolean = false
) {
    open val fileName: String? = null
    open val mimeType: String? = null
    open val bitmap: Bitmap? = null

    companion object {
        private const val TAG = "SingleImage"

        /**
         * @throws FileNotFoundException
         * @throws IOException
         */
        private fun loadThumbnailFromDisk(
            contentResolver: ContentResolver,
            uri: Uri,
            size: Size
        ): Bitmap {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(uri, size, null)
            } else {
                @Suppress("DEPRECATION")
                val bm = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                Bitmap.createScaledBitmap(bm, size.width, size.height, true)
            }
        }

        /**
         * @throws FileNotFoundException
         * @throws IOException
         */
        fun loadBitmapFromDisk(contentResolver: ContentResolver, uri: Uri, mutable: Boolean?=null): Bitmap {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val onHeaderListener = ImageDecoder.OnHeaderDecodedListener { decoder, _, _ ->
                    decoder.isMutableRequired = BuildConfig.DEBUG || mutable == true
                }
                ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(contentResolver, uri),
                    onHeaderListener
                )
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        }

    }

    /**
     * Load image and display name of image
     * If dimensions is set, it will try to load a thumbnail or scale the bitmap down
     */
    fun loadImageInThread(
        contentResolver: ContentResolver,
        dimensions: Size?,
        bitmap: Bitmap?,
        onLoad: ((SingleImageLoaded) -> Unit),
        onError: (Exception?) -> Unit
    ) {
        Thread {
            try {
                onLoad(loadImage(contentResolver, dimensions, bitmap))
            } catch (e: Exception) {
                onError(e)
            }
        }.start()
    }

    /**
     * Load image and display name of image
     * If dimensions is set, it will try to load a thumbnail or scale the bitmap down
     */
    fun loadImage(
        contentResolver: ContentResolver,
        dimensions: Size?,
        bitmap: Bitmap? = null
    ): SingleImageLoaded {
        if (BuildConfig.DEBUG) Log.v(TAG, "loadImage() from: $uri")
        var displayName: String? = null
        var mime: String? = null
        var size: Long? = null
        var date: Date? = null
        val bm = if (bitmap != null) {
            if (dimensions != null && dimensions.height > 0 && dimensions.width > 0) {
                Bitmap.createScaledBitmap(bitmap, dimensions.width, dimensions.height, true)
            } else {
                bitmap
            }
        } else {
            if (dimensions != null && dimensions.height > 0 && dimensions.width > 0) {
                loadThumbnailFromDisk(contentResolver, uri, dimensions)
            } else {
                loadBitmapFromDisk(contentResolver, uri)
            }
        }

        contentResolver.query(uri, null, null, null, null)?.apply {
            moveToFirst()

            displayName =
                getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let {
                    getString(it)
                } ?: uri.lastPathSegment?.split("/")?.last()

            mime = getColumnIndex(MediaStore.MediaColumns.MIME_TYPE).takeIf { it >= 0 }?.let {
                getString(it)
            }

            size = getColumnIndex(MediaStore.MediaColumns.SIZE).takeIf { it >= 0 }?.let {
                getLongOrNull(it)
            }

            val dateModified =
                getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED).takeIf { it >= 0 }?.let {
                    getLongOrNull(it)
                }
            val dateAdded =
                getColumnIndex(MediaStore.MediaColumns.DATE_ADDED).takeIf { it >= 0 }?.let {
                    getLongOrNull(it)
                }
            val dateTaken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN).takeIf { it >= 0 }?.let {
                    getLongOrNull(it)
                }
            } else {
                null
            }
            val dateModifiedMilliseconds =
                getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED).takeIf { it >= 0 }
                    ?.let {
                        getLongOrNull(it)
                    }

            date = if (lastModified != null) {
                lastModified
            } else if (dateModified != null && dateModified > 1) {
                Date(dateModified * 1000)
            } else if (dateAdded != null && dateAdded > 1) {
                Date(dateAdded * 1000)
            } else if (dateTaken != null && dateTaken > 1) {
                Date(dateTaken * 1000)
            } else if (dateModifiedMilliseconds != null && dateModifiedMilliseconds > 1) {
                Date(dateModifiedMilliseconds)
            } else {
                Log.w(TAG, "No date value found in ContentResolver")
                null
            }
        }?.close()

        if (uri.normalizeScheme().scheme == "file") {
            val file = uri.toFile()
            if (displayName == null) {
                displayName = file.name
            }
            try {
                if (file.isFile) {
                    if (date == null) {
                        file.lastModified().takeIf { it > 1 }?.let {
                            date = Date(it)
                        }
                    }
                    if (size == null) {
                        size = file.length().takeIf { it > 1 }
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, e.stackTraceToString())
            }
            if (mime == null) {
                mime = mimeFromFileExtension(file.extension)
            }
        }

        val mimeType = mime ?: "image/png"

        val (fileName, _) = fileNameFileTitle(
            displayName ?: "unknown",
            mimeType.split("/").last()
        )
        return SingleImageLoaded(
            this.uri,
            fileName,
            mimeType,
            bitmap = bm,
            lastModified = date ?: lastModified ?: Date(),
            size = size,
            file = file,
            folder = folder,
            isAppData = isAppData
        )
    }

}
