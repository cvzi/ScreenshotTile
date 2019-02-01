package com.github.ipcjs.screenshottile

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.widget.Toast
import com.github.ipcjs.screenshottile.App.getMediaProjection
import android.os.StrictMode



/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


class TakeScreenshotActivity : Activity(), OnAcquireScreenshotPermissionListener {


    companion object {
        val NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN = "notification_channel_screenshot_taken";
        val SCREENSHOT_DIRECTORY = "Screenshots";
        private val TAG = "ScreenshotTest"

        fun start(context: Context) {
            context.startActivity(newIntent(context))
        }

        fun newIntent(context: Context): Intent {
            val intent = Intent(context, TakeScreenshotActivity::class.java)
            return intent
        }

    }

    private var mScreenDensity: Int = 0
    private var mScreenWidth: Int = 0
    private var mScreenHeight: Int = 0
    private var mScreenSharing: Boolean = false
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mSurface: Surface? = null
    private var mImageReader: ImageReader? = null
    private var mMediaProjection: MediaProjection? = null

    private var askedForPermission = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Avoid android.os.FileUriExposedException
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        with(metrics) {
            mScreenDensity = densityDpi
            mScreenWidth = widthPixels
            mScreenHeight = heightPixels
        }

        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1)
        mSurface = mImageReader!!.surface

        if(!askedForPermission) {
            askedForPermission = true
            Log.v(TAG, "TakeScreenshotActivity.onCreate() App.aquireScreenshotPermission()")
            App.aquireScreenshotPermission(this, this@TakeScreenshotActivity)
        }
    }

    override fun onAcquireScreenshotPermission() {
        shareScreen()
    }


    public override fun onDestroy() {
        super.onDestroy()
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    fun shareScreen() {
        mScreenSharing = true

        mMediaProjection = getMediaProjection()


        if (mSurface == null) {
            Log.v(TAG, "shareScreen() mSurface == null")
            finish()
            return
        }
        if (mMediaProjection == null) {
            Log.v(TAG, "shareScreen() mMediaProjection == null")

            Toast.makeText(
                    this,
                    getString(R.string.screenshot_failed), Toast.LENGTH_LONG
            ).show()
            if(!askedForPermission) {
                askedForPermission = true
                Log.v(TAG, "shareScreen() App.aquireScreenshotPermission()")
                App.aquireScreenshotPermission(this)
            }
            mMediaProjection = getMediaProjection()

            if (mMediaProjection == null) {
                Log.v(TAG, "shareScreen() still: mMediaProjection == null")
                finish()
                return
            }
        }

        Log.v(TAG, "shareScreen() -> createVirtualDisplay()")


        mVirtualDisplay = createVirtualDisplay()
        mImageReader!!.setOnImageAvailableListener({
            Log.v(TAG, "onImageAvailable()")
            // Remove listener, after first image
            it.setOnImageAvailableListener(null, null)
            // Read and save image
            saveImage()
        }, null)
    }

    private fun saveImage() {
        if (mImageReader == null) {
            Log.v(TAG, "saveImage() mImageReader is null")
            stopScreenSharing()
            finish()
            return
        }
        val image = mImageReader!!.acquireLatestImage()
        if(image == null) {
            Log.v(TAG, "saveImage() image is null")
            Toast.makeText(
                    this,
                    getString(R.string.screenshot_forbidden), Toast.LENGTH_LONG
            ).show()
            stopScreenSharing()
            finish()
            return
        }
        Log.v(TAG, "saveImage() retrieved image")


        val pair = saveImageToFile(applicationContext, image, "Screenshot_")
        val imageFile = pair.first
        val bitmap = pair.second

        Log.v(TAG, "saveImage() ${imageFile.absolutePath}")

        Toast.makeText(
            this,
            getString(R.string.screenshot_file_saved, imageFile.canonicalFile), Toast.LENGTH_LONG
        ).show()


        // Create intent for notification click
        val path = Uri.fromFile(imageFile)
        val intent = Intent(Intent.ACTION_VIEW).apply{
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            setDataAndType(path, "image/png")
        }
        val uniqueId = (System.currentTimeMillis() and 0xfffffff).toInt() // notification id and pending intent request code must be unique for each notification
        val chooser = Intent.createChooser(intent, getString(R.string.notitifaciton_app_chooser))
        val pendingIntent = PendingIntent.getActivity(this@TakeScreenshotActivity, uniqueId, chooser, 0)

        // Create notification
        var builder: Notification.Builder? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = Notification.Builder(this, createNotificationScreenshotTakenChannel(this))
        } else {
            builder = Notification.Builder(this)
        }
        with(builder) {
            setContentTitle(getString(R.string.notification_title))
                setContentText(getString(R.string.notification_body))
                setSmallIcon(R.drawable.ic_stat_name)
                setLargeIcon(bitmap)
                        setContentIntent(pendingIntent)
                        setAutoCancel(true)
        }

        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            notify(uniqueId, builder.build())
        }


        stopScreenSharing()
        //onBackPressed()
        finish()
    }


    private fun stopScreenSharing() {
        mScreenSharing = false
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay!!.release()
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay(
            "ScreenshotTaker",
            mScreenWidth, mScreenHeight, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mSurface, null, null
        )

    }
}



