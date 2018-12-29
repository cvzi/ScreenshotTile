package com.github.ipcjs.screenshottile

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.widget.Toast
import com.github.ipcjs.screenshottile.App.getMediaProjection


/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


class TakeScreenshotActivity : Activity() {


    companion object {
        private val TAG = "ScreenshotTest"
        private val IMAGE_QUALITY = 98

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

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi
        mScreenWidth = metrics.widthPixels
        mScreenHeight = metrics.heightPixels

        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1)
        mSurface = mImageReader!!.surface

        App.aquireScreenshotPermission(this)
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
            App.aquireScreenshotPermission(this)
            return
        }

        Log.v(TAG, "shareScreen() -> createVirtualDisplay()")


        mVirtualDisplay = createVirtualDisplay()
        Handler().postDelayed({
            saveImage()
        }, 500)
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


        val imageFile = saveImageToFile(applicationContext, image, "Screenshot_", IMAGE_QUALITY)

        Log.v(TAG, "saveImage() ${imageFile.absolutePath}")

        Toast.makeText(
            this,
            "Screenshot saved ${imageFile.canonicalFile}", Toast.LENGTH_LONG
        ).show()


        stopScreenSharing()
        onBackPressed()
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
            "ScreenSharingDemo",
            mScreenWidth, mScreenHeight, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mSurface, null, null
        )

    }
}



