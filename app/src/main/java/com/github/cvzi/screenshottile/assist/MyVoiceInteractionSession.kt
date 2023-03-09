package com.github.cvzi.screenshottile.assist

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.cvzi.screenshottile.*
import com.github.cvzi.screenshottile.activities.NoDisplayActivity
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import com.github.cvzi.screenshottile.partial.ScreenshotSelectorView
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.utils.*

/**
 * A session is started when the home button is long pressed
 */
class MyVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    companion object {
        private const val TAG = "MyVoiceInSession"
    }

    private var saveImageHandler: SaveImageHandler? = null
    private val prefManager = App.getInstance().prefManager
    private var screenshotSelectorActive = false
    private var layoutView: ConstraintLayout? = null
    private var screenshotSelectorView: ScreenshotSelectorView? = null
    private var cutOutRect: Rect? = null
    private var currentBitmap: Bitmap? = null
    private var partial = false
    private val onBackInvokedCallback: OnBackInvokedCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handle back button for Android 13+
            OnBackInvokedCallback {
                resetSelection()
            }
        } else null
    private var onBackInvokedCallbackIsSet = false

    override fun onHandleScreenshot(bitmap: Bitmap?) {
        if (bitmap == null) {
            Log.w(TAG, "onHandleScreenshot: bitmap is null")
        }
        screenshotSelectorActive = false

        if (saveImageHandler?.isRunning() == true) {
            Log.e(TAG, "onHandleScreenshot: Thread is already running")
            hide()
            return
        }

        if (isDeviceLocked(App.getInstance()) && App.getInstance().prefManager.preventIfLocked) {
            toastDeviceIsLocked(App.getInstance())
            return
        }

        // Partial screenshot -> Show "crop bitmap view"
        cutOutRect = null
        if (bitmap != null && prefManager.voiceInteractionAction == context.getString(R.string.setting_voice_interaction_action_value_partial)) {
            partial = true
            currentBitmap = bitmap
            if (BuildConfig.DEBUG) Log.v(TAG, "onHandleScreenshot: showCropBitmapView")
            showCropBitmapView(bitmap)
            return
        } else {
            partial = false
            currentBitmap = null
        }

        if (prefManager.voiceInteractionAction == context.getString(R.string.setting_voice_interaction_action_value_legacy)) {
            hide()
            if (BuildConfig.DEBUG) Log.v(
                TAG,
                "onHandleScreenshot: Using legacy method by user request"
            )
            NoDisplayActivity.startNewTaskLegacyScreenshot(context)
            return
        }

        // If action is native screenshot, try to take screenshot with accessibility service
        var success = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            ScreenshotAccessibilityService.instance != null &&
            (bitmap == null || prefManager.voiceInteractionAction == context.getString(R.string.setting_voice_interaction_action_value_native))
        ) {
            if (BuildConfig.DEBUG) Log.v(TAG, "onHandleScreenshot: simulateScreenshotButton()")
            success = ScreenshotAccessibilityService.instance?.simulateScreenshotButton(
                autoHideButton = true,
                autoUnHideButton = true,
                useTakeScreenshotMethod = false
            ) == true
        }
        if (success) {
            hide()
            return
        }

        if (bitmap == null) {
            Log.w(TAG, "onHandleScreenshot: Trying legacy method as last resort")
            hide()
            if (prefManager.voiceInteractionAction == context.getString(R.string.setting_voice_interaction_action_value_partial)) {
                NoDisplayActivity.startNewTaskPartial(context)
            } else {
                NoDisplayActivity.startNewTaskLegacyScreenshot(context)
            }
        } else {
            if (BuildConfig.DEBUG) Log.v(TAG, "onHandleScreenshot: storeBitmap(bitmap)")
            storeBitmap(bitmap)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateContentView(): View {
        val constraintLayout = layoutInflater.inflate(R.layout.image_crop, null) as ConstraintLayout
        screenshotSelectorView =
            constraintLayout.findViewById(R.id.global_screenshot_selector) as ScreenshotSelectorView
        screenshotSelectorView?.run {
            visibility = View.GONE
            text = context.getString(R.string.take_screenshot)
            shutter = R.drawable.ic_stat_name
            fullScreenIcon = R.drawable.ic_fullscreen
            onShutter = {
                val bitmap = currentBitmap
                if (bitmap != null) {
                    // If there is a cutout or status bars, the view might have a offset
                    val selectorViewOffset = intArrayOf(0, 0)
                    getLocationOnScreen(selectorViewOffset)
                    it.offset(selectorViewOffset[0], selectorViewOffset[1])
                    cutOutRect = it
                    visibility = View.GONE
                    storeBitmap(bitmap)
                    screenshotSelectorActive = false
                }
            }
            onSelect = {
                addBackButtonHandler()
            }
        }


        layoutView = constraintLayout
        return constraintLayout
    }

    /**
     * Show a view that let's the user crop the screenshot after the fact
     */
    private fun showCropBitmapView(bitmap: Bitmap) {
        goFullscreen()

        if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            // Night Mode
            layoutView?.setBackgroundColor(Color.BLACK)
        } else {
            layoutView?.setBackgroundColor(Color.WHITE)
        }

        screenshotSelectorView?.run {
            screenshotSelectorActive = true
            this.bitmap = if (BuildConfig.DEBUG) {
                tintImage(bitmap, color = 0xFF006622)
            } else {
                bitmap
            }
            visibility = View.VISIBLE
            resetSelection()
        }

    }

    private fun goFullscreen() {
        closeSystemDialogs()

        window.window?.let { window ->
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
        }
    }


    /**
     * Store the bitmap to file system in a separate thread
     */
    private fun storeBitmap(bitmap: Bitmap) {
        saveImageHandler = SaveImageHandler(Looper.getMainLooper()).also { handler ->
            handler.storeBitmap(
                context,
                bitmap,
                cutOutRect,
                prefManager.fileNamePattern,
                useAppData = "saveToStorage" !in prefManager.postScreenshotActions,
                directory = null
            ) { result ->
                onFileSaved(result)
            }
        }
    }


    private fun onFileSaved(saveImageResult: SaveImageResult?) {
        currentBitmap = null
        if (saveImageResult == null) {
            screenShotFailedToast("saveImageResult is null")
            hide()
            return
        }
        if (!saveImageResult.success) {
            screenShotFailedToast(saveImageResult.errorMessage)
            hide()
            return
        }

        val screenDensity = context.resources.configuration.densityDpi

        val postScreenshotActions = App.getInstance().prefManager.postScreenshotActions

        val result = saveImageResult as? SaveImageResultSuccess?
        when {
            result == null -> {
                screenShotFailedToast("Failed to cast SaveImageResult")
            }
            result.uri != null -> {
                // Android Q+ works with MediaStore content:// URI
                var dummyPath =
                    "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}/${result.fileTitle}"
                if (result.dummyPath.isNotEmpty()) {
                    dummyPath = result.dummyPath
                }

                if ("showToast" in postScreenshotActions) {
                    context.toastMessage(
                        context.getString(R.string.screenshot_file_saved, dummyPath),
                        ToastType.SUCCESS
                    )
                }

                if ("showNotification" in postScreenshotActions) {
                    createNotification(
                        context,
                        result.uri,
                        result.bitmap,
                        screenDensity,
                        result.mimeType,
                        dummyPath
                    )
                }
                prefManager.screenshotCount++
                handlePostScreenshot(
                    context,
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
                    context.toastMessage(
                        context.getString(R.string.screenshot_file_saved, path),
                        ToastType.SUCCESS
                    )
                }

                createNotification(
                    context,
                    uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType
                )
                prefManager.screenshotCount++
                handlePostScreenshot(
                    context,
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
        hide()
    }

    override fun onHide() {
        currentBitmap = null
        cutOutRect = null
        screenshotSelectorActive = false
        super.onHide()
    }

    private fun screenShotFailedToast(errorMessage: String? = null) {
        val message = context.getString(R.string.screenshot_failed) + if (errorMessage != null) {
            "\n$errorMessage"
        } else {
            ""
        }
        context.toastMessage(message, ToastType.ERROR)
    }

    private fun addBackButtonHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !onBackInvokedCallbackIsSet) {
            onBackInvokedCallback?.let {
                window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
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
                window.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
                onBackInvokedCallbackIsSet = false
            }
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // This is no longer used on Android 13+/Tiramisu
        // See onBackInvokedCallback for Android 13+
        val selectorView = screenshotSelectorView
        if (screenshotSelectorActive && selectorView != null && !selectorView.defaultState) {
            resetSelection()
        } else {
            super.onBackPressed()
        }
    }
}
