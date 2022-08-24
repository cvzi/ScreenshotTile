package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.os.Message


/**
 * Store a bitmap to file system in a separate thread
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
            thread?.start()
        } else if (msg.what == THREAD_FINISHED) {
            onSaved?.invoke(saveImageResult)
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