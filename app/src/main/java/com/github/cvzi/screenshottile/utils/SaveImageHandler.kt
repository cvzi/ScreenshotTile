package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.github.cvzi.screenshottile.SaveImageResult


/**
 * Store a bitmap to file system in a separate thread
 * Can only be used once.
 */
class SaveImageHandler(looper: Looper) :
    Handler(looper) {
    companion object {
        const val THREAD_START = 1
        const val THREAD_FINISHED = 2
    }

    private var thread: Thread? = null
    private var onSaved: ((SaveImageResult?) -> Unit)? = null
    private var saveImageResult: SaveImageResult? = null

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        if (msg.what == THREAD_START) {
            try {
                thread?.start()
            } catch (_: IllegalStateException) {
                // Thread may already be started
            }
        } else if (msg.what == THREAD_FINISHED) {
            onSaved?.invoke(saveImageResult)
            thread = null
        }
    }

    /**
     * Check if thread is running
     */
    fun isRunning(): Boolean {
        return thread?.isAlive == true
    }

    /**
     * Store the bitmap to file system
     */
    fun storeBitmap(
        context: Context,
        bitmap: Bitmap,
        cutOutRect: Rect?,
        fileNamePattern: String,
        useAppData: Boolean,
        directory: String?,
        onFileSaved: (SaveImageResult?) -> Unit
    ) {
        onSaved = onFileSaved
        thread = Thread {
            saveImageResult = saveBitmapToFile(
                context,
                bitmap,
                fileNamePattern,
                compressionPreference(context),
                cutOutRect,
                useAppData,
                directory
            )
            sendEmptyMessage(THREAD_FINISHED)
        }
        sendEmptyMessage(THREAD_START)
    }

    /**
     * Store the bitmap to an existing Uri
     */
    fun storeBitmap(
        context: Context,
        bitmap: Bitmap,
        cutOutRect: Rect?,
        uri: Uri,
        onFileSaved: (SaveImageResult?) -> Unit
    ) {
        onSaved = onFileSaved
        thread = Thread {
            saveImageResult = saveBitmapToFile(
                context,
                bitmap,
                uri,
                compressionPreference(context),
                cutOutRect,
            )
            sendEmptyMessage(THREAD_FINISHED)
        }
        sendEmptyMessage(THREAD_START)
    }


    /**
     * Convert image to bitmap and store to file system
     */
    fun storeImage(
        context: Context,
        image: Image,
        cutOutRect: Rect?,
        fileNamePattern: String,
        useAppData: Boolean,
        directory: String?,
        onFileSaved: (SaveImageResult?) -> Unit
    ) {
        onSaved = onFileSaved
        thread = Thread {
            saveImageResult = saveImageToFile(
                context,
                image,
                fileNamePattern,
                compressionPreference(context),
                cutOutRect,
                useAppData,
                directory
            )
            sendEmptyMessage(THREAD_FINISHED)
        }
        sendEmptyMessage(THREAD_START)
    }

}