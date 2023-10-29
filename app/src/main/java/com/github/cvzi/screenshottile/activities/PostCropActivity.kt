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
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.burhanrashid52.photoediting.EditImageActivity.Companion.ACTION_NEXTGEN_EDIT
import com.github.cvzi.screenshottile.*
import com.github.cvzi.screenshottile.databinding.ActivityPostCropBinding
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
        fun newIntentSingleImage(context: Context, uri: Uri, mimeType: String? = null): Intent {
            val intent = Intent(context, PostCropActivity::class.java)
            intent.action = Intent.ACTION_EDIT
            intent.setDataAndTypeAndNormalize(uri, mimeType ?: "image/*")
            return intent
        }

        /**
         * New Intent that opens a single image for cropping and tries to read the last bitmap
         *
         * @param context    Context
         * @param uri   Uri   of image
         * @return The intent
         */
        fun newIntentSingleImageBitmap(
            context: Context, uri: Uri, mimeType: String? = null
        ): Intent {
            val intent = Intent(context, PostCropActivity::class.java)
            intent.action = OPEN_IMAGE_FROM_URI
            intent.setDataAndTypeAndNormalize(uri, mimeType ?: "image/*")
            intent.putExtra(BITMAP_FROM_LAST_SCREENSHOT, true)
            return intent
        }
    }

    private lateinit var binding: ActivityPostCropBinding
    private val prefManager = App.getInstance().prefManager
    private var screenshotSelectorActive = false
    private var cutOutRect: Rect? = null
    private var scale: Float? = null
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
        binding = ActivityPostCropBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.textViewStatus.text = ""

        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_EDIT, ACTION_NEXTGEN_EDIT, OPEN_IMAGE_FROM_URI -> {
                val uri = intent.data ?: intent.clipData?.getItemAt(0)?.uri
                val tryLastBitmap = intent.getBooleanExtra(BITMAP_FROM_LAST_SCREENSHOT, false)
                val lastBitmap = if (tryLastBitmap) {
                    App.getInstance().lastScreenshot
                } else {
                    null
                }
                uri?.let {
                    openImageFromUri(it, lastBitmap)
                }
            }

            else -> {
                val intentType = intent.type
                if (intentType != null && intentType.startsWith("image/")) {
                    val uri = intent.data ?: intent.clipData?.getItemAt(0)?.uri
                    uri?.let {
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
            binding.layoutView.setBackgroundColor(Color.BLACK)
        } else {
            binding.layoutView.setBackgroundColor(Color.WHITE)
        }

        Log.v(
            TAG,
            "view before: ${binding.globalScreenshotSelector.measuredWidth}x${binding.globalScreenshotSelector.measuredHeight}"
        )

        var bm: Bitmap = if (BuildConfig.DEBUG && singleImage.bitmap.isMutable) {
            tintImage(singleImage.bitmap, color = 0xFF006622)
        } else {
            singleImage.bitmap
        }

        if (binding.globalScreenshotSelector.measuredWidth > 0 && binding.globalScreenshotSelector.measuredHeight > 0 && (binding.globalScreenshotSelector.measuredWidth < bm.width || binding.globalScreenshotSelector.measuredHeight < bm.height)) {
            Log.d(
                TAG,
                "View is only ${binding.globalScreenshotSelector.measuredWidth}x${binding.globalScreenshotSelector.measuredHeight}"
            )
            val scaleResult = scaleBitmap(
                bm,
                binding.globalScreenshotSelector.measuredWidth,
                binding.globalScreenshotSelector.measuredHeight
            )
            bm = scaleResult.first
            scale = scaleResult.second
            Log.d(TAG, "Scaled bitmap by $scale to ${bm.width}x${bm.height}")
        } else {
            scale = null
        }

        binding.globalScreenshotSelector.apply {
            bitmap = bm

            // Center bitmap in view
            offsetLeft = (measuredWidth - bm.width) / 2f
            offsetTop = (measuredHeight - bm.height) / 2f

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


    private fun openImageFromUri(imageUri: Uri, bitmap: Bitmap? = null) {
        SingleImage(imageUri).apply {
            loadImageInThread(contentResolver, null, bitmap, { singleImageLoaded ->
                runOnUiThread {
                    singleImage = singleImageLoaded
                    loadScreenshotSelectorView(singleImageLoaded)
                    binding.globalScreenshotSelector.setBackgroundColor(if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) Color.BLACK else Color.WHITE)
                }
            }, { error ->
                runOnUiThread {
                    Log.e(TAG, "Failed to load image:", error)
                    binding.globalScreenshotSelector.visibility = View.GONE
                    binding.textViewStatus.text = "Failed to load image\n$error"
                }
            })
        }
    }

    /**
     * Store the bitmap to file system in a separate thread
     */
    private fun storeBitmap(bitmap: Bitmap) {
        scale?.let {
            if (scale != 1f) {
                cutOutRect = cutOutRect?.run {
                    scaleRect(this, 1f / it)
                }
            }
        }

        SaveImageHandler(Looper.getMainLooper()).storeBitmap(
            this,
            bitmap,
            cutOutRect,
            prefManager.fileNamePattern,
            useAppData = false,
            directory = null
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
                    getString(R.string.screenshot_file_saved, dummyPath), ToastType.SUCCESS
                )

                createNotification(
                    this, result.uri, result.bitmap, screenDensity, result.mimeType, dummyPath
                )
                prefManager.screenshotCount++

                finish()
            }

            result.file != null -> {
                // Legacy behaviour until Android P, works with the real file path
                val uri = Uri.fromFile(result.file)
                val path = result.file.absolutePath

                toastMessage(
                    getString(R.string.screenshot_file_saved, path), ToastType.SUCCESS
                )

                createNotification(
                    this, uri, result.bitmap, screenDensity, result.mimeType
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
        binding.textViewStatus.text = "Failed to save image\n$msg"
    }


    private fun goFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION") window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.statusBarColor = Color.TRANSPARENT
            window.setDecorFitsSystemWindows(true)
        } else {
            @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
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
                onBackInvokedCallbackIsSet = true
            }
        }
    }

    private fun resetSelection() {
        Log.v(TAG, "resetSelection()")
        if (singleImage != null && !binding.globalScreenshotSelector.defaultState) {
            binding.globalScreenshotSelector.reset()
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
        Log.v(TAG, "onBackPressed()")
        // This is no longer used on Android 13+/Tiramisu
        // See onBackInvokedCallback for Android 13+
        if (screenshotSelectorActive && singleImage != null && !binding.globalScreenshotSelector.defaultState) {
            resetSelection()
        } else {
            super.onBackPressed()
        }
    }
}