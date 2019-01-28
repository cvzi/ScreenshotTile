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
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.widget.Toast
import com.github.ipcjs.screenshottile.App.getMediaProjection
import java.util.*
import android.os.StrictMode



/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


class TakeScreenshotActivity : Activity() {


    companion object {
        val NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN = "notification_channel_screenshot_taken";
        val SCREENSHOT_DIRECTORY = "Screenshots";
        private val SCREENSHOT_DELAY_MILLIS = 50;
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
        mScreenDensity = metrics.densityDpi
        mScreenWidth = metrics.widthPixels
        mScreenHeight = metrics.heightPixels

        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1)
        mSurface = mImageReader!!.surface

        if(!askedForPermission) {
            askedForPermission = true
            App.aquireScreenshotPermission(this)
        }
    }

    public override fun onStart() {
        super.onStart()

        Handler().postDelayed({
            shareScreen()
        }, 500)
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
            return
        }
        if (mMediaProjection == null) {
            Log.v(TAG, "shareScreen() mMediaProjection == null")
            if(!askedForPermission) {
                askedForPermission = true
                App.aquireScreenshotPermission(this)
            }
            return
        }

        Log.v(TAG, "shareScreen() -> createVirtualDisplay()")


        mVirtualDisplay = createVirtualDisplay()
        Handler().postDelayed({
            saveImage()
        }, SCREENSHOT_DELAY_MILLIS.toLong())
    }

    private fun saveImage() {
        if (mImageReader == null) {
            Log.v(TAG, "saveImage() mImageReader is null")
            return
        }
        val image = mImageReader!!.acquireLatestImage()
        if(image == null) {
            Log.v(TAG, "saveImage() image is null")
            return
        }
        Log.v(TAG, "saveImage() retrieved image")


        val pair = saveImageToFile(applicationContext, image, "Screenshot_")
        val imageFile = pair.first
        val bitmap = pair.second

        Log.v(TAG, "saveImage() ${imageFile.absolutePath}")

        Toast.makeText(
            this,
            "Screenshot saved ${imageFile.canonicalFile}", Toast.LENGTH_LONG
        ).show()


        val path = Uri.fromFile(imageFile)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.setDataAndType(path, "image/png")

        val chooser = Intent.createChooser(intent, getString(R.string.notitifaciton_app_chooser))

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
                        setContentIntent(PendingIntent.getActivity(this@TakeScreenshotActivity, 0, chooser, 0))
                        setAutoCancel(true)
        }

        val notificationId = Random().nextInt()

        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
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



