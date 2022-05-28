package com.github.cvzi.screenshottile.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.core.database.getLongOrNull
import androidx.core.net.toFile
import com.github.cvzi.screenshottile.BuildConfig
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*


class SingleImageLoaded(
    uri: Uri,
    val fileName: String,
    val mimeType: String,
    val thumbnail: Bitmap,
    val lastModified: Date,
    val size: Long?
) : SingleImage(uri)

open class SingleImage(val uri: Uri) {
    companion object {
        private const val TAG = "SingleImage"
    }

    /**
     * Load thumbnail and display name of image
     */
    fun loadImage(
        contentResolver: ContentResolver,
        onLoad: ((SingleImageLoaded) -> Unit),
        onError: (Exception?) -> Unit
    ) {
        Thread {
            if (BuildConfig.DEBUG) Log.v(TAG, "loadImage() from: $uri")
            var bm: Bitmap? = null
            var error: Exception? = null
            var displayName: String? = null
            var mime: String? = null
            var size: Long? = null
            var date: Date? = null
            try {
                bm = loadThumbnail(contentResolver, uri, Size(200, 400))
            } catch (e: FileNotFoundException) {
                error = e
            } catch (e: IOException) {
                error = e
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

                date = if (dateModified != null && dateModified > 1) {
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
            if (bm != null) {
                onLoad(
                    SingleImageLoaded(
                        this.uri,
                        fileName,
                        mimeType,
                        bm,
                        date ?: Date(),
                        size
                    )
                )
            } else {
                onError(error)
            }

        }.start()
    }

    /**
     * @throws FileNotFoundException
     * @throws IOException
     */
    private fun loadThumbnail(contentResolver: ContentResolver, uri: Uri, size: Size): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.loadThumbnail(uri, size, null)
        } else {
            @Suppress("DEPRECATION")
            val bm = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            Bitmap.createScaledBitmap(bm, size.width, size.height, true)
        }
    }
}
