package com.github.cvzi.screenshottile.assist

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.*
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.NoDisplayActivity
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import com.github.cvzi.screenshottile.partial.ScreenshotSelectorView
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.utils.*
import java.lang.ref.WeakReference

/**
 * A session is started when the home button is long pressed
 */
class MyVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    companion object {
        private const val TAG = "MyVoiceInSession"
    }

    private var handler = SaveImageHandler(this, Looper.getMainLooper())
    private var thread: Thread? = null
    private var saveImageResult: SaveImageResult? = null
    private val prefManager = App.getInstance().prefManager
    private var screenshotSelectorActive = false
    private var layoutView: ConstraintLayout? = null
    private var screenshotSelectorView: ScreenshotSelectorView? = null
    private var cutOutRect: Rect? = null
    private var currentBitmap: Bitmap? = null

    override fun onBackPressed() {
        val selectorView = screenshotSelectorView
        if (screenshotSelectorActive && selectorView != null && !selectorView.defaultState) {
            selectorView.reset()
        } else {
            super.onBackPressed()
        }
    }

    override fun onHandleScreenshot(bitmap: Bitmap?) {
        if (bitmap == null) {
            Log.w(TAG, "onHandleScreenshot: bitmap is null")
        }

        screenshotSelectorActive = false

        if (thread?.isAlive == true) {
            Log.e(TAG, "onHandleScreenshot: Thread is already running")
            hide()
            return
        }

        // Partial screenshot -> Show "crop bitmap view"
        cutOutRect = null
        if (bitmap != null && prefManager.voiceInteractionAction == context.getString(R.string.setting_voice_interaction_action_value_partial)) {
            currentBitmap = bitmap
            showCropBitmapView(bitmap)
            return
        } else {
            currentBitmap = null
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
            Log.w(TAG, "onHandleScreenshot: Trying original screenshot method as last resort")
            context.startActivity(NoDisplayActivity.newIntent(context, true))
            hide()
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
            reset()
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
        thread = Thread {
            saveImageResult = saveBitmapToFile(
                context,
                bitmap,
                prefManager.fileNamePattern,
                compressionPreference(context),
                cutOutRect
            )
            handler.sendEmptyMessage(TakeScreenshotActivity.THREAD_FINISHED)
        }
        handler.sendEmptyMessage(TakeScreenshotActivity.THREAD_START)
    }

    /**
     * Handle messages from/to service/thread
     */
    class SaveImageHandler(myVoiceInteractionSession: MyVoiceInteractionSession, looper: Looper) :
        Handler(looper) {
        private var service: WeakReference<MyVoiceInteractionSession> =
            WeakReference(myVoiceInteractionSession)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == TakeScreenshotActivity.THREAD_START) {
                service.get()?.thread?.start()
            } else if (msg.what == TakeScreenshotActivity.THREAD_FINISHED) {
                service.get()?.onFileSaved()
            }
        }
    }

    private fun onFileSaved() {
        currentBitmap = null
        val saveImageResult = this.saveImageResult
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

        aMessage(context)

        val screenDensity = context.resources.configuration.densityDpi

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
                context.toastMessage(
                    context.getString(R.string.screenshot_file_saved, dummyPath),
                    ToastType.SUCCESS
                )

                createNotification(
                    context,
                    result.uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType
                )
                prefManager.screenshotCount++
            }
            result.file != null -> {
                // Legacy behaviour until Android P, works with the real file path
                val uri = Uri.fromFile(result.file)
                val path = result.file.absolutePath

                context.toastMessage(
                    context.getString(R.string.screenshot_file_saved, path),
                    ToastType.SUCCESS
                )

                createNotification(
                    context,
                    uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType
                )
                prefManager.screenshotCount++
            }
            else -> {
                screenShotFailedToast("Failed to cast SaveImageResult path/uri")
            }
        }
        hide()
    }

    private fun screenShotFailedToast(errorMessage: String? = null) {
        val message = context.getString(R.string.screenshot_failed) + if (errorMessage != null) {
            "\n$errorMessage"
        } else {
            ""
        }
        context.toastMessage(message, ToastType.ERROR)
    }


}
