package com.github.cvzi.screenshottile.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Toast
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.App.resetMediaProjection
import com.github.cvzi.screenshottile.App.setScreenshotPermission
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.BuildConfig.APPLICATION_ID
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.interfaces.OnAcquireScreenshotPermissionListener
import com.github.cvzi.screenshottile.partial.ScreenshotSelectorView
import com.github.cvzi.screenshottile.services.BasicForegroundService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.*
import java.lang.ref.WeakReference

/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


class TakeScreenshotActivity : Activity(),
    OnAcquireScreenshotPermissionListener {

    companion object {
        private const val TAG = "TakeScreenshotActivity"
        var instance: TakeScreenshotActivity? = null
        const val NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN = "notification_channel_screenshot_taken"
        const val NOTIFICATION_CHANNEL_FOREGROUND = "notification_channel_foreground"
        const val SCREENSHOT_DIRECTORY = "Screenshots"
        const val FILE_PREFIX = "Screenshot_"
        const val NOTIFICATION_PREVIEW_MIN_SIZE = 50
        const val NOTIFICATION_PREVIEW_MAX_SIZE = 400
        const val NOTIFICATION_BIG_PICTURE_MAX_HEIGHT = 1024
        const val THREAD_START = 1
        const val THREAD_FINISHED = 2
        const val EXTRA_PARTIAL = "$APPLICATION_ID.TakeScreenshotActivity.EXTRA_PARTIAL"

        /**
         * Start activity.
         */
        fun start(context: Context, partial: Boolean = false) {
            context.startActivity(newIntent(context, partial))
        }

        private fun newIntent(context: Context, partial: Boolean = false): Intent {
            return Intent(context, TakeScreenshotActivity::class.java).apply {
                putExtra(EXTRA_PARTIAL, partial)
            }
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
    private var cutOutRect: Rect? = null
    private var handler = SaveImageHandler(this, Looper.getMainLooper())
    private var thread: Thread? = null
    private var saveImageResult: SaveImageResult? = null
    private var partial = false
    private var isFullscreen = false

    private var askedForPermission = false

    override fun onNewIntent(intent: Intent?) {
        /* If the activity is already open, we need to update the intent,
        otherwise getIntent() returns the old intent in onCreate() */
        setIntent(intent)
        super.onNewIntent(intent)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        // Avoid android.os.FileUriExposedException:
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        when {
            BasicForegroundService.instance != null -> BasicForegroundService.instance?.foreground()
            ScreenshotTileService.instance != null -> ScreenshotTileService.instance?.foreground()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> BasicForegroundService.startForegroundService(
                this
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && App.getInstance().prefManager.floatingButton && ScreenshotAccessibilityService.instance != null) {
            ScreenshotAccessibilityService.instance?.run {
                temporaryHideFloatingButton()
            }
        }

        partial = intent?.getBooleanExtra(EXTRA_PARTIAL, false) ?: false

        with(DisplayMetrics()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display?.getRealMetrics(this)
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(this)
            }
            screenDensity = densityDpi
            screenWidth = widthPixels
            screenHeight = heightPixels
        }

        @SuppressLint("WrongConstant")
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)

        surface = imageReader?.surface

        if (packageManager.checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                packageName
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "onCreate() missing WRITE_EXTERNAL_STORAGE permission")
            App.requestStoragePermission(this, true)
            return
        }

        if (!askedForPermission) {
            askedForPermission = true
            App.acquireScreenshotPermission(this, this)
        } else {
            if (BuildConfig.DEBUG) Log.v(TAG, "onCreate() else")
        }

    }

    private fun partialScreenshot() {
        /**
         * Show partial screenshot selector
         */

        var mScreenshotSelectorView =
            findViewById<ScreenshotSelectorView?>(R.id.global_screenshot_selector)
        if (mScreenshotSelectorView != null) {
            // View is already loaded, stop here
            if (cutOutRect != null) {
                // rectangle is already selected -> start screenshot
                prepareForScreenSharing()
            } else {
                // nothing selected -> reset the view
                mScreenshotSelectorView.visibility = View.VISIBLE
                mScreenshotSelectorView.invalidate()
            }
            return
        }

        // Go fullscreen without status bar and without display notch/cutout
        // (must be called before content)
        if (!isFullscreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            isFullscreen = true
        }

        // Load layout (must be done before the window* calls)
        setContentView(R.layout.partial_screenshot)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.statusBarColor = Color.TRANSPARENT
            window.setDecorFitsSystemWindows(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        mScreenshotSelectorView =
            findViewById<ScreenshotSelectorView>(R.id.global_screenshot_selector)
        mScreenshotSelectorView.text = getString(R.string.take_screenshot)
        mScreenshotSelectorView.shutter = R.drawable.ic_stat_name
        mScreenshotSelectorView.onShutter = {
            // If there is a cutout or status bars, the view might have a offset
            val selectorViewOffset = intArrayOf(0, 0)
            mScreenshotSelectorView.getLocationOnScreen(selectorViewOffset)
            it.offset(selectorViewOffset[0], selectorViewOffset[1])
            cutOutRect = it
            mScreenshotSelectorView.visibility = View.GONE
            prepareForScreenSharing()
        }

        // make sure that a foreground service runs
        when {
            BasicForegroundService.instance != null -> BasicForegroundService.instance?.foreground()
            ScreenshotTileService.instance != null -> ScreenshotTileService.instance?.foreground()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> BasicForegroundService.startForegroundService(
                this
            )
        }
    }

    override fun onAcquireScreenshotPermission(isNewPermission: Boolean) {
        /*
        Handler().postDelayed({
            // Wait so the notification area is really collapsed
            shareScreen()
         }, 350)
        */
        ScreenshotTileService.instance?.onAcquireScreenshotPermission(isNewPermission)
        ScreenshotTileService.instance?.foreground()
        BasicForegroundService.instance?.foreground()
        if (partial) {
            partialScreenshot()
        } else {
            if (isNewPermission) {
                // Wait a little bit, so the permission dialog can fully hide itself
                Handler(Looper.getMainLooper()).postDelayed({
                    prepareForScreenSharing()
                }, 300)
            } else {
                prepareForScreenSharing()
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mediaProjection != null) {
            mediaProjection?.stop()
            mediaProjection = null
        }
    }

    private fun prepareForScreenSharing() {
        saveImageResult = null
        screenSharing = true
        mediaProjection = try {
            App.createMediaProjection()
        } catch (e: SecurityException) {
            Log.e(TAG, "prepareForScreenSharing(): SecurityException 1")
            null
        }
        if (surface == null) {
            if (BuildConfig.DEBUG) Log.v(TAG, "prepareForScreenSharing(): surface == null")
            screenShotFailedToast("Failed to create ImageReader surface")
            finish()
            return
        }
        if (mediaProjection == null) {
            if (BuildConfig.DEBUG) Log.v(TAG, "prepareForScreenSharing() mediaProjection == null")
            if (!askedForPermission) {
                askedForPermission = true
                if (BuildConfig.DEBUG) Log.v(
                    TAG,
                    "prepareForScreenSharing() -> App.acquireScreenshotPermission()"
                )
                try {
                    App.acquireScreenshotPermission(this, this)
                } catch (e: SecurityException) {
                    Log.e(TAG, "prepareForScreenSharing(): SecurityException 2")
                }
            }
            mediaProjection = try {
                App.createMediaProjection()
            } catch (e: SecurityException) {
                Log.e(TAG, "prepareForScreenSharing(): SecurityException 3")
                null
            }
            if (mediaProjection == null) {
                screenShotFailedToast("Failed to create MediaProjection")
                // Something went wrong, restart everything
                finish()
                App.getInstance().screenshot(this)
                return
            }
        }
        try {
            startVirtualDisplay()
        } catch (e: SecurityException) {
            Log.e(TAG, "startVirtualDisplay() SecurityException: $e")
            setScreenshotPermission(null)
            screenShotFailedToast("Failed to start virtual display: ${e.localizedMessage}")
            stopScreenSharing()

            // Start the foreground service and get a new media projection
            resetMediaProjection()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                BasicForegroundService.resumeScreenshot(this)
            } else {
                App.acquireScreenshotPermission(this, this)
            }
        }
    }

    private fun startVirtualDisplay() {
        virtualDisplay = createVirtualDisplay()
        imageReader?.setOnImageAvailableListener({
            if (BuildConfig.DEBUG) Log.v(TAG, "startVirtualDisplay:onImageAvailable()")
            // Remove listener, after first image
            it.setOnImageAvailableListener(null, null)
            // Read and save image
            saveImage()
        }, null)
    }

    private fun saveImage() {
        if (imageReader == null) {
            Log.w(TAG, "saveImage() imageReader == null")
            stopScreenSharing()
            screenShotFailedToast("Could not start screen capture")
            finish()
            return
        }

        // acquireLatestImage produces warning for  maxImages = 1: "Unable to acquire a buffer item, very likely client tried to acquire more than maxImages buffers"
        val image = try {
            imageReader?.acquireNextImage()
        } catch (e: UnsupportedOperationException) {
            stopScreenSharing()
            Log.e(TAG, "saveImage() acquireNextImage() UnsupportedOperationException", e)
            screenShotFailedToast("Could not acquire image.\nUnsupportedOperationException\nThis device is not supported.")
            finish()
            return
        }
        stopScreenSharing()
        if (image == null) {
            Log.e(TAG, "saveImage() image == null")
            screenShotFailedToast("Could not acquire image")
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && App.getInstance().prefManager.floatingButton && ScreenshotAccessibilityService.instance != null) {
            ScreenshotAccessibilityService.instance?.run {
                showTemporaryHiddenFloatingButton()
            }
        }

        if (image.width == 0 || image.height == 0) {
            Log.e(TAG, "saveImage() Image size: ${image.width}x${image.width}")
            screenShotFailedToast("Incorrect image dimensions: ${image.width}x${image.width}")
            finish()
            return
        }

        val compressionOptions = compressionPreference(applicationContext)

        thread = Thread {
            saveImageResult = saveImageToFile(
                applicationContext,
                image,
                FILE_PREFIX,
                compressionOptions,
                cutOutRect
            )
            image.close()

            handler.sendEmptyMessage(THREAD_FINISHED)
        }
        handler.sendEmptyMessage(THREAD_START)
    }

    /**
     * Handle messages from/to activity/thread
     */
    class SaveImageHandler(takeScreenshotActivity: TakeScreenshotActivity, looper: Looper) :
        Handler(looper) {
        private var activity: WeakReference<TakeScreenshotActivity> =
            WeakReference(takeScreenshotActivity)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == THREAD_START) {
                activity.get()?.thread?.start()
            } else if (msg.what == THREAD_FINISHED) {
                activity.get()?.onFileSaved()
            }
        }
    }

    private fun onFileSaved() {
        if (saveImageResult == null) {
            screenShotFailedToast("saveImageResult is null")
            finish()
            return
        }
        if (saveImageResult?.success != true) {
            screenShotFailedToast(saveImageResult?.errorMessage)
            finish()
            return
        }

        val result = saveImageResult as? SaveImageResultSuccess?

        when {
            result == null -> {
                screenShotFailedToast("Failed to cast SaveImageResult")
            }
            result.uri != null -> {
                // Android Q+ works with MediaStore content:// URI
                var dummyPath =
                    "${Environment.DIRECTORY_PICTURES}/$SCREENSHOT_DIRECTORY/${result.fileTitle}"
                if (result.dummyPath.isNotEmpty()) {
                    dummyPath = result.dummyPath
                }
                Toast.makeText(
                    this,
                    getString(R.string.screenshot_file_saved, dummyPath), Toast.LENGTH_LONG
                ).show()

                createNotification(
                    this,
                    result.uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType
                )
                App.getInstance().prefManager.screenshotCount++
            }
            result.file != null -> {
                // Legacy behaviour until Android P, works with the real file path
                val uri = Uri.fromFile(result.file)
                val path = result.file.absolutePath

                Toast.makeText(
                    this,
                    getString(R.string.screenshot_file_saved, path), Toast.LENGTH_LONG
                ).show()

                createNotification(
                    this,
                    uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType
                )
                App.getInstance().prefManager.screenshotCount++
            }
            else -> {
                screenShotFailedToast("Failed to cast SaveImageResult path/uri")
            }
        }

        saveImageResult = null

        // Stop foreground service for Android Q+
        ScreenshotTileService.instance?.background()
        BasicForegroundService.instance?.background()

        finish()
    }

    private fun stopScreenSharing() {
        screenSharing = false
        mediaProjection?.stop()
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

    private fun screenShotFailedToast(errorMessage: String? = null) {
        val message = getString(R.string.screenshot_failed) + if (errorMessage != null) {
            "\n$errorMessage"
        } else {
            ""
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}
