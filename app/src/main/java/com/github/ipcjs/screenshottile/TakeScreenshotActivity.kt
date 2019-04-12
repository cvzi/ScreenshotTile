package com.github.ipcjs.screenshottile

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.widget.Toast
import com.github.ipcjs.screenshottile.Utils.p
import java.io.File
import java.lang.ref.WeakReference


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


class TakeScreenshotActivity : Activity(), OnAcquireScreenshotPermissionListener {

    companion object {
        const val NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN = "notification_channel_screenshot_taken"
        const val SCREENSHOT_DIRECTORY = "Screenshots"
        const val NOTIFICATION_PREVIEW_MIN_SIZE = 50
        const val NOTIFICATION_PREVIEW_MAX_SIZE = 400
        const val THREAD_START = 1
        const val THREAD_FINISHED = 2

        /**
         * Start activity.
         */
        fun start(context: Context) {
            context.startActivity(newIntent(context))
        }

        private fun newIntent(context: Context): Intent {
            return Intent(context, TakeScreenshotActivity::class.java)
        }

    }

    private var screenDensity: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0
    private var screenSharing: Boolean = false
    private var virtualDisplay: VirtualDisplay? = null
    private var surface: Surface? = null
    private var imageReader: ImageReader? = null
    private var mediaProjection: MediaProjection? = null
    private var handler = SaveImageHandler(this)
    private var thread: Thread? = null
    private var resultPair: Pair<File, Bitmap>? = null

    private var askedForPermission = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Avoid android.os.FileUriExposedException:
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        with(DisplayMetrics()) {
            windowManager.defaultDisplay.getRealMetrics(this)
            screenDensity = densityDpi
            screenWidth = widthPixels
            screenHeight = heightPixels
        }

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
        surface = imageReader?.surface

        if (packageManager.checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                packageName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            p("TakeScreenshotActivity.onCreate(): missing WRITE_EXTERNAL_STORAGE permission")
            App.requestStoragePermission(this)
            return
        }

        if (!askedForPermission) {
            askedForPermission = true
            p("App.acquireScreenshotPermission() in TakeScreenshotActivity.onCreate()")
            App.acquireScreenshotPermission(this, this)
        } else {
            p("onCreate() else")

        }
    }

    override fun onAcquireScreenshotPermission() {
        /*
        Handler().postDelayed({
            // Wait so the notification area is really collapsed
            shareScreen()
         }, 350)
        */
        ScreenshotTileService.instance?.onAcquireScreenshotPermission()
        prepareForScreenSharing()
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mediaProjection != null) {
            mediaProjection?.stop()
            mediaProjection = null
        }
    }

    private fun prepareForScreenSharing() {
        resultPair = null
        screenSharing = true
        mediaProjection = App.createMediaProjection()
        if (surface == null) {
            p("shareScreen() surface == null")
            finish()
            return
        }
        if (mediaProjection == null) {
            p("shareScreen() mediaProjection == null")
            screenShotFailedToast()
            if (!askedForPermission) {
                askedForPermission = true
                p("App.acquireScreenshotPermission() in shareScreen()")
                App.acquireScreenshotPermission(this, this)
            }
            mediaProjection = App.createMediaProjection()
            if (mediaProjection == null) {
                p("shareScreen() mediaProjection == null")
                finish()
                return
            }
        }
        startVirtualDisplay()
    }

    private fun startVirtualDisplay() {
        virtualDisplay = createVirtualDisplay()
        imageReader?.setOnImageAvailableListener({
            p("onImageAvailable()")
            // Remove listener, after first image
            it.setOnImageAvailableListener(null, null)
            // Read and save image
            saveImage()
        }, null)
    }

    private fun saveImage() {
        if (imageReader == null) {
            p("saveImage() imageReader == null")
            stopScreenSharing()
            finish()
            return
        }

        // acquireLatestImage produces warning for  maxImages = 1: "Unable to acquire a buffer item, very likely client tried to acquire more than maxImages buffers"
        val image = imageReader?.acquireNextImage()
        stopScreenSharing()
        if (image == null) {
            p("saveImage() image == null")
            screenShotFailedToast()
            finish()
            return
        }

        if (image.width == 0 || image.height == 0) {
            Log.e("TakeScreenshotActivity.kt:saveImage()", "Image size: ${image.width}x${image.width}")
            screenShotFailedToast()
            finish()
            return
        }

        val compressionOptions = compressionPreference(applicationContext)

        thread = Thread(Runnable {
            resultPair = saveImageToFile(applicationContext, image, "Screenshot_", compressionOptions)
            image.close()

            handler.sendEmptyMessage(THREAD_FINISHED)
        })
        handler.sendEmptyMessage(THREAD_START)
    }

    class SaveImageHandler(takeScreenshotActivity: TakeScreenshotActivity) : Handler() {
        private var activity: WeakReference<TakeScreenshotActivity> = WeakReference(takeScreenshotActivity)
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            p("SaveImageHandler handleMessage() what=${msg.what}")
            if (msg.what == THREAD_START) {
                activity.get()!!.thread!!.start()
            } else if (msg.what == THREAD_FINISHED) {
                activity.get()?.onFileSaved()
            }
        }
    }

    private fun onFileSaved() {
        if (resultPair == null) {
            screenShotFailedToast()
            finish()
            return
        }

        resultPair?.run {
            val imageFile = first
            p("saveImage() imageFile.absolutePath=${imageFile.absolutePath}")
            Toast.makeText(
                this@TakeScreenshotActivity,
                getString(R.string.screenshot_file_saved, imageFile.canonicalFile), Toast.LENGTH_LONG
            ).show()
            createNotification(
                this@TakeScreenshotActivity,
                Uri.fromFile(imageFile),
                resizeToNotificationIcon(second, screenDensity)
            )
            second.recycle()
        } ?: screenShotFailedToast()

        resultPair = null
        finish()
    }

    private fun stopScreenSharing() {
        screenSharing = false
        virtualDisplay?.release()
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection?.createVirtualDisplay(
            "ScreenshotTaker",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null, null
        )
    }

    private fun screenShotFailedToast() {
        Toast.makeText(
            this,
            getString(R.string.screenshot_failed), Toast.LENGTH_LONG
        ).show()
    }

}



