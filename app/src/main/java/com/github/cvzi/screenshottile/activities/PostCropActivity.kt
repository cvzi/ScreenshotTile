package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.constraintlayout.widget.ConstraintLayout
import com.burhanrashid52.photoediting.EditImageActivity.Companion.ACTION_NEXTGEN_EDIT
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.partial.ScreenshotSelectorView
import com.github.cvzi.screenshottile.utils.*

class PostCropActivity : GenericPostActivity() {
    companion object {
        private const val TAG = "PostCropActivity"

        /**
         * New Intent that opens a single image for cropping
         *
         * @param context    Context
         * @param uri   Uri   of image
         * @return The intent
         */
        fun newIntentSingleImage(context: Context, uri: Uri): Intent {
            val intent = Intent(context, PostCropActivity::class.java)
            intent.action = Intent.ACTION_EDIT
            intent.setDataAndNormalize(uri)
            return intent
        }

    }

    private lateinit var layoutView: ConstraintLayout
    private lateinit var screenshotSelectorView: ScreenshotSelectorView
    private lateinit var statusTextView: TextView
    private val prefManager = App.getInstance().prefManager
    private var screenshotSelectorActive = false
    private var cutOutRect: Rect? = null
    private val onBackInvokedCallback: OnBackInvokedCallback? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handle back button for Android 13+
            OnBackInvokedCallback {
                resetSelection()
            }
        } else null
    private var onBackInvokedCallbackIsSet = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.image_crop)

        layoutView = findViewById(R.id.layout_view)
        screenshotSelectorView = findViewById(R.id.global_screenshot_selector)
        statusTextView = findViewById(R.id.textViewStatus)

        statusTextView.text = ""

        when (intent.action) {
            Intent.ACTION_EDIT, ACTION_NEXTGEN_EDIT -> {
                intent.data?.let {
                    openImageFromUri(it)
                }
            }
            else -> {
                val intentType = intent.type
                if (intentType != null && intentType.startsWith("image/")) {
                    intent.data?.let {
                        openImageFromUri(it)
                    }
                }
            }
        }
    }

    private fun loadScreenshotSelectorView(singleImage: SingleImageLoaded) {
        goFullscreen()
        if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
            // Night Mode
            layoutView.setBackgroundColor(Color.BLACK)
        } else {
            layoutView.setBackgroundColor(Color.WHITE)
        }

        screenshotSelectorView.apply {
            bitmap = if (BuildConfig.DEBUG && singleImage.bitmap.isMutable) {
                tintImage(singleImage.bitmap, color = 0xFF006622)
            } else {
                singleImage.bitmap
            }

            visibility = View.VISIBLE
            text = context.getString(R.string.take_screenshot)
            shutter = R.drawable.ic_stat_name
            fullScreenIcon = R.drawable.ic_fullscreen
            onShutter = {
                // If there is a cutout or status bars, the view might have a offset
                val selectorViewOffset = intArrayOf(0, 0)
                getLocationOnScreen(selectorViewOffset)
                it.offset(selectorViewOffset[0], selectorViewOffset[1])
                cutOutRect = it
                visibility = View.GONE
                storeBitmap(singleImage.bitmap)
                screenshotSelectorActive = false
            }
            onSelect = {
                addBackButtonHandler()
                screenshotSelectorActive = true
            }
            resetSelection()
        }
    }


    private fun openImageFromUri(imageUri: Uri) {
        SingleImage(imageUri).apply {
            loadImageInThread(contentResolver, null, { singleImageLoaded ->
                runOnUiThread {
                    singleImage = singleImageLoaded
                    loadScreenshotSelectorView(singleImageLoaded)
                }
            }, { error ->
                runOnUiThread {
                    Log.e(TAG, "Failed to load image:", error)
                    screenshotSelectorView.visibility = View.GONE
                    statusTextView.text =
                        "Failed to load image\n$error"
                }
            })
        }
    }


    /**
     * Store the bitmap to file system in a separate thread
     */
    private fun storeBitmap(bitmap: Bitmap) {
        SaveImageHandler(Looper.getMainLooper()).storeBitmap(
            this,
            bitmap,
            cutOutRect,
            prefManager.fileNamePattern,
            useAppData = false
        ) {
            onFileSaved(it)
        }
    }

    private fun onFileSaved(saveImageResult: SaveImageResult?) {
        if (saveImageResult == null) {
            onFileSaveError("saveImageResult is null")
            return
        }
        if (!saveImageResult.success) {
            onFileSaveError("saveImageResult is null")
            return
        }

        val screenDensity = resources.configuration.densityDpi

        val result = saveImageResult as? SaveImageResultSuccess?

        when {
            result == null -> {
                onFileSaveError("Failed to cast SaveImageResult")
            }
            result.uri != null -> {
                // Android Q+ works with MediaStore content:// URI
                var dummyPath =
                    "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}/${result.fileTitle}"
                if (result.dummyPath.isNotEmpty()) {
                    dummyPath = result.dummyPath
                }
                toastMessage(
                    getString(R.string.screenshot_file_saved, dummyPath),
                    ToastType.SUCCESS
                )

                createNotification(
                    this,
                    result.uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType,
                    dummyPath
                )
                prefManager.screenshotCount++

                finish()
            }
            result.file != null -> {
                // Legacy behaviour until Android P, works with the real file path
                val uri = Uri.fromFile(result.file)
                val path = result.file.absolutePath

                toastMessage(
                    getString(R.string.screenshot_file_saved, path),
                    ToastType.SUCCESS
                )

                createNotification(
                    this,
                    uri,
                    result.bitmap,
                    screenDensity,
                    result.mimeType
                )
                prefManager.screenshotCount++

                finish()
            }
            else -> {
                onFileSaveError("Failed to cast SaveImageResult path/uri")
            }
        }
    }


    private fun onFileSaveError(msg: String?) {
        if (msg != null) {
            toastMessage(msg, ToastType.ERROR)
        }
        Log.e(TAG, "Failed to save image: $msg")
        statusTextView.text = "Failed to save image\n$msg"
    }


    private fun goFullscreen() {
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

    private fun addBackButtonHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !onBackInvokedCallbackIsSet) {
            onBackInvokedCallback?.let {
                window.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, onBackInvokedCallback
                )
            }
            onBackInvokedCallbackIsSet = true
        }
    }

    private fun resetSelection() {
        Log.v(TAG, "resetSelection()")
        if (singleImage != null && !screenshotSelectorView.defaultState) {
            screenshotSelectorView.reset()
        }
        // Remove handler for back button on Android 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback?.let {
                window.onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
            }
            onBackInvokedCallbackIsSet = false
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        Log.v(TAG, "onBackPressed()")
        // This is no longer used on Android 13+/Tiramisu
        // See onBackInvokedCallback for Android 13+
        if (screenshotSelectorActive && singleImage != null && !screenshotSelectorView.defaultState) {
            resetSelection()
        } else {
            super.onBackPressed()
        }
    }
}