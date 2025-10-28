package com.github.cvzi.screenshottile.activities

import android.Manifest
import android.annotation.SuppressLint
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
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.App.resetMediaProjection
import com.github.cvzi.screenshottile.App.setScreenshotPermission
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.BuildConfig.APPLICATION_ID
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.SaveImageResult
import com.github.cvzi.screenshottile.SaveImageResultSuccess
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.interfaces.OnAcquireScreenshotPermissionListener
import com.github.cvzi.screenshottile.partial.ScreenshotSelectorView
import com.github.cvzi.screenshottile.services.BasicForegroundService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.SaveImageHandler
import com.github.cvzi.screenshottile.utils.createNotification
import com.github.cvzi.screenshottile.utils.formatLocalizedString
import com.github.cvzi.screenshottile.utils.getLocalizedString
import com.github.cvzi.screenshottile.utils.handlePostScreenshot
import com.github.cvzi.screenshottile.utils.realScreenSize
import com.github.cvzi.screenshottile.utils.toastMessage

/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */


class TakeScreenshotActivity : BaseActivity(),
    OnAcquireScreenshotPermissionListener {

    companion object {
        private const val TAG = "TakeScreenshotActivity"
        var instance: TakeScreenshotActivity? = null
        const val NOTIFICATION_CHANNEL_SCREENSHOT_TAKEN = "notification_channel_screenshot_taken"
        const val NOTIFICATION_CHANNEL_FOREGROUND = "notification_channel_foreground"
        const val SCREENSHOT_DIRECTORY = "Screenshots"
        const val NOTIFICATION_PREVIEW_MIN_SIZE = 50
        const val NOTIFICATION_PREVIEW_MAX_SIZE = 400
        const val NOTIFICATION_BIG_PICTURE_MAX_HEIGHT = 1024
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
    private var partial = false
    private var isFullscreen = false
    private var screenshotSelectorView: ScreenshotSelectorView? = null

    private var askedForPermission = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val onBackInvokedCallback: OnBackInvokedCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handle back button for Android 13+
            OnBackInvokedCallback {
                resetSelection()
            }
        } else null
    private var onBackInvokedCallbackIsSet = false

    override fun onNewIntent(intent: Intent?) {
        /* If the activity is already open, we need to update the intent,
        otherwise getIntent() returns the old intent in onCreate() */
        setIntent(intent)
        super.onNewIntent(intent)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // This is no longer used on Android 13+/Tiramisu
        // See onBackInvokedCallback for Android 13+
        val selectorView = screenshotSelectorView
        if (partial && selectorView != null && !selectorView.defaultState) {
            resetSelection()
        } else {
            super.onBackPressed()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        // Avoid android.os.FileUriExposedException:
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            /*
            On Android U/14 we need to wait until we have the screenshot
            permission before we can start the foreground service
             */
            when {
                BasicForegroundService.instance != null -> BasicForegroundService.instance?.foreground()
                ScreenshotTileService.instance != null -> ScreenshotTileService.instance?.foreground()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> BasicForegroundService.startForegroundService(
                    this
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && App.getInstance().prefManager.floatingButton && ScreenshotAccessibilityService.instance != null) {
            ScreenshotAccessibilityService.instance?.run {
                temporaryHideFloatingButton()
            }
        }

        partial = intent?.getBooleanExtra(EXTRA_PARTIAL, false) == true
        screenDensity = resources.configuration.densityDpi
        realScreenSize(this).run {
            screenWidth = x
            screenHeight = y
        }

        @SuppressLint("WrongConstant")
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1)

        surface = imageReader?.surface

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && packageManager.checkPermission(
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

        val mScreenshotSelectorView =
            findViewById<ScreenshotSelectorView?>(R.id.global_screenshot_selector)
        if (mScreenshotSelectorView != null) {
            // View is already loaded, stop here
            if (cutOutRect != null) {
                // rectangle is already selected -> start screenshot
                prepareForScreenSharing()
            } else {
                // nothing selected -> reset the view
                mScreenshotSelectorView.visibility = View.VISIBLE
                resetSelection()
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

        findViewById<ScreenshotSelectorView>(R.id.global_screenshot_selector).apply {
            screenshotSelectorView = this
            text = getLocalizedString(R.string.take_screenshot)
            shutter = R.drawable.ic_stat_name
            fullScreenIcon = R.drawable.ic_fullscreen
            onSelect = {
                addBackButtonHandler()
            }
            onShutter = {
                // If there is a cutout or status bars, the view might have a offset
                val selectorViewOffset = intArrayOf(0, 0)
                getLocationOnScreen(selectorViewOffset)
                it.offset(selectorViewOffset[0], selectorViewOffset[1])
                cutOutRect = it
                if (App.getInstance().prefManager.selectAreaShutterDelay > 0 || shutterIsVisible) {
                    invalidate()
                    postDelayed({
                        visibility = View.GONE
                        if (shutterIsVisible) {
                            postDelayed({
                                prepareForScreenSharing()
                            }, App.getInstance().prefManager.selectAreaShutterDelay)
                        } else {
                            prepareForScreenSharing()
                        }
                    }, App.getInstance().prefManager.selectAreaShutterDelay)
                } else {
                    visibility = View.GONE
                    prepareForScreenSharing()
                }
            }
            resetSelection()
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

    private fun addBackButtonHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !onBackInvokedCallbackIsSet) {
            onBackInvokedCallback?.let {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackInvokedCallback
                )
                onBackInvokedCallbackIsSet = true
            }
        }
    }

    private fun resetSelection() {
        val selectorView = screenshotSelectorView
        if (partial && selectorView != null && !selectorView.defaultState) {
            screenshotSelectorView?.reset()
        }
        // Remove handler for back button on Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback?.let {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
                onBackInvokedCallbackIsSet = false
            }
        }
    }

    override fun onAcquireScreenshotPermission(isNewPermission: Boolean) {
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
                }, App.getInstance().prefManager.originalAfterPermissionDelay)
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
        instance = null
    }

    private fun prepareForScreenSharing() {
        screenSharing = true
        mediaProjection = try {
            App.createMediaProjection()
        } catch (_: SecurityException) {
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
                } catch (_: SecurityException) {
                    Log.e(TAG, "prepareForScreenSharing(): SecurityException 2")
                }
            }
            mediaProjection = try {
                App.createMediaProjection()
            } catch (_: SecurityException) {
                Log.e(TAG, "prepareForScreenSharing(): SecurityException 3")
                Handler(Looper.getMainLooper()).postDelayed({
                    // Something went wrong, restart everything
                    finish()
                    App.getInstance().screenshot(this)
                }, 500)
                return
            }
            if (mediaProjection == null) {
                screenShotFailedToast("Failed to create MediaProjection", Toast.LENGTH_SHORT)
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
            screenShotFailedToast(
                "Failed to start virtual display: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            )
            stopScreenSharing()

            // Start the foreground service and get a new media projection
            Handler(Looper.getMainLooper()).postDelayed({
                resetMediaProjection()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    BasicForegroundService.resumeScreenshot(this)
                } else {
                    App.acquireScreenshotPermission(this, this)
                }
            }, App.getInstance().prefManager.failedVirtualDisplayDelay)

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

        val prefManager = App.getInstance().prefManager
        val fileNamePattern = prefManager.fileNamePattern
        val useAppData = "saveToStorage" !in prefManager.postScreenshotActions
        SaveImageHandler(Looper.getMainLooper()).storeImage(
            applicationContext,
            image,
            cutOutRect,
            fileNamePattern,
            useAppData,
            directory = null
        ) {
            onFileSaved(it)
        }

    }

    private fun onFileSaved(saveImageResult: SaveImageResult?) {
        if (saveImageResult == null) {
            screenShotFailedToast("saveImageResult is null")
            finish()
            return
        }
        if (!saveImageResult.success) {
            screenShotFailedToast(saveImageResult.errorMessage)
            finish()
            return
        }

        val postScreenshotActions = App.getInstance().prefManager.postScreenshotActions

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

                if ("showToast" in postScreenshotActions) {
                    toastMessage(
                        formatLocalizedString(R.string.screenshot_file_saved, dummyPath),
                        ToastType.SUCCESS
                    )
                }
                if ("showNotification" in postScreenshotActions) {
                    createNotification(
                        this,
                        result.uri,
                        result.bitmap,
                        screenDensity,
                        result.mimeType,
                        dummyPath
                    )
                }
                App.getInstance().prefManager.screenshotCount++
                handlePostScreenshot(
                    this,
                    postScreenshotActions,
                    result.uri,
                    result.mimeType,
                    result.bitmap
                )
            }

            result.file != null -> {
                // Legacy behaviour until Android P, works with the real file path
                val uri = Uri.fromFile(result.file)
                val path = result.file.absolutePath

                if ("showToast" in postScreenshotActions) {
                    toastMessage(formatLocalizedString(R.string.screenshot_file_saved, path), ToastType.SUCCESS)
                }

                if ("showNotification" in postScreenshotActions) {
                    createNotification(
                        this,
                        uri,
                        result.bitmap,
                        screenDensity,
                        result.mimeType
                    )
                }
                App.getInstance().prefManager.screenshotCount++
                handlePostScreenshot(
                    this,
                    postScreenshotActions,
                    uri,
                    result.mimeType,
                    result.bitmap
                )
            }

            else -> {
                screenShotFailedToast("Failed to cast SaveImageResult path/uri")
            }
        }

        // Stop foreground service for Android Q+
        ScreenshotTileService.instance?.background()
        BasicForegroundService.instance?.background()

        finish()
    }

    private fun stopScreenSharing() {
        screenSharing = false
        mediaProjection?.stop()
        virtualDisplay?.release()
        surface?.release()
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                if (screenSharing) {
                    stopScreenSharing()
                }
            }
        }, null)

        val virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenshotTaker",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null, null
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // On Android U/14 the permission can only be used once, so delete it now
            setScreenshotPermission(null)
        }

        return virtualDisplay
    }

    private fun screenShotFailedToast(
        errorMessage: String? = null,
        duration: Int = Toast.LENGTH_LONG
    ) {
        val message = getLocalizedString(R.string.screenshot_failed) + if (errorMessage != null) {
            "\n$errorMessage"
        } else {
            ""
        }
        toastMessage(message, ToastType.ERROR, duration)
    }

}
