package com.github.ipcjs.screenshottile

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.icu.util.Calendar
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.DisplayMetrics
import android.view.Surface
import android.widget.Toast
import com.github.ipcjs.screenshottile.App.getMediaProjection
import com.github.ipcjs.screenshottile.Utils.p


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


class TakeScreenshotActivity : Activity(), OnAcquireScreenshotPermissionListener {

    companion object {
        const val NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN = "notification_channel_screenshot_taken"
        const val SCREENSHOT_DIRECTORY = "Screenshots"
        const val NOTIFICATION_PREVIEW_MIN_SIZE = 50
        const val NOTIFICATION_PREVIEW_MAX_SIZE = 400

        /**
         * Start activity
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

    private var askedForPermission = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Avoid android.os.FileUriExposedException:
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        with(metrics) {
            screenDensity = densityDpi
            screenWidth = widthPixels
            screenHeight = heightPixels
        }

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)
        surface = imageReader?.surface

        if (!askedForPermission) {
            askedForPermission = true
            p("App.acquireScreenshotPermission() in TakeScreenshotActivity.onCreate()")
            App.acquireScreenshotPermission(this, this)
        }
    }

    override fun onAcquireScreenshotPermission() {
        /*
        Handler().postDelayed({
            // Wait so the notification area is really collapsed
            shareScreen()
         }, 350)
        */
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
        screenSharing = true
        mediaProjection = getMediaProjection()
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
            mediaProjection = getMediaProjection()
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
        val image = imageReader?.acquireLatestImage()
        stopScreenSharing()
        if (image == null) {
            p("saveImage() image == null")
            screenShotFailedToast()
            finish()
            return
        }
        val pair = saveImageToFile(applicationContext, image, "Screenshot_")
        val imageFile = pair.first
        p("saveImage() imageFile.absolutePath= ${imageFile.absolutePath}")
        Toast.makeText(
                this,
                getString(R.string.screenshot_file_saved, imageFile.canonicalFile), Toast.LENGTH_LONG
        ).show()
        createNotification(Uri.fromFile(imageFile), resizeToNotificationIcon(pair.second, screenDensity))
        finish()
    }

    /**
     * Show a notification that opens the image file on tap
     */
    private fun createNotification(path: Uri, bitmap: Bitmap) {
        val uniqueId = (System.currentTimeMillis() and 0xfffffff).toInt() // notification id and pending intent request code must be unique for each notification
        val pendingIntent = openImageIntent(path, uniqueId)
        // Create notification
        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, createNotificationScreenshotTakenChannel(this))
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        with(builder) {
            setWhen(Calendar.getInstance().timeInMillis)
            setShowWhen(true)
            setContentTitle(getString(R.string.notification_title))
            setContentText(getString(R.string.notification_body))
            setSmallIcon(R.drawable.ic_stat_name)
            setLargeIcon(bitmap)
            setContentIntent(pendingIntent)
            setAutoCancel(true)
        }
        // Show notification
        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(uniqueId, builder.build())
        }
    }

    private fun stopScreenSharing() {
        screenSharing = false
        virtualDisplay?.release()
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mediaProjection!!.createVirtualDisplay(
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

    private fun openImageIntent(path: Uri, uniqueId: Int): PendingIntent {
        // Create intent for notification click
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            setDataAndType(path, "image/png")
        }
        val chooser = Intent.createChooser(intent, getString(R.string.notification_app_chooser))
        return PendingIntent.getActivity(this, uniqueId, chooser, 0)
    }
}



