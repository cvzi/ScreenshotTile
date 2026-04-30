package com.github.cvzi.screenshottile.activities

import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.SaveImageResult
import com.github.cvzi.screenshottile.SaveImageResultSuccess
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.functions.AppFunctionResultStore
import com.github.cvzi.screenshottile.utils.compressionPreference
import com.github.cvzi.screenshottile.utils.composeLongScreenshotFromFiles
import com.github.cvzi.screenshottile.utils.createNotification
import com.github.cvzi.screenshottile.utils.formatLocalizedString
import com.github.cvzi.screenshottile.utils.handlePostScreenshot
import com.github.cvzi.screenshottile.utils.loadLongScreenshotFrameFiles
import com.github.cvzi.screenshottile.utils.saveBitmapToFile
import com.github.cvzi.screenshottile.utils.toastMessage
import java.io.File
import kotlin.concurrent.thread

class LongScreenshotReviewActivity : BaseActivity() {
    companion object {
        const val EXTRA_SESSION_DIR = "extra_session_dir"
        const val EXTRA_SUGGESTED_CROPS = "extra_suggested_crops"
        const val EXTRA_REPEATED_BOTTOM_INSET = "extra_repeated_bottom_inset"
        const val EXTRA_CONTENT_BOUNDS = "extra_content_bounds"
        const val EXTRA_EXPECTED_CROPS = "extra_expected_crops"

        fun newIntent(
            context: android.content.Context,
            sessionDir: File,
            suggestedCrops: IntArray,
            repeatedBottomInsetPx: Int,
            contentBounds: Rect?,
            expectedCropTops: IntArray?
        ): android.content.Intent {
            return android.content.Intent(context, LongScreenshotReviewActivity::class.java).apply {
                putExtra(EXTRA_SESSION_DIR, sessionDir.absolutePath)
                putExtra(EXTRA_SUGGESTED_CROPS, suggestedCrops)
                putExtra(EXTRA_REPEATED_BOTTOM_INSET, repeatedBottomInsetPx)
                putExtra(EXTRA_CONTENT_BOUNDS, contentBounds)
                putExtra(EXTRA_EXPECTED_CROPS, expectedCropTops)
            }
        }
    }

    private lateinit var sessionDir: File
    private lateinit var frameFiles: List<File>
    private lateinit var cropTops: IntArray
    private var expectedCropTops: IntArray? = null
    private var repeatedBottomInsetPx: Int = 0
    private var contentBounds: Rect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_long_screenshot_review)
        title = getString(R.string.long_screenshot_review_title)

        val sessionPath = intent.getStringExtra(EXTRA_SESSION_DIR)
        if (sessionPath.isNullOrBlank()) {
            finish()
            return
        }
        sessionDir = File(sessionPath)
        frameFiles = loadLongScreenshotFrameFiles(sessionDir)
        if (frameFiles.size < 2) {
            toastMessage(R.string.long_screenshot_not_enough_frames, ToastType.ERROR)
            finish()
            return
        }
        cropTops = intent.getIntArrayExtra(EXTRA_SUGGESTED_CROPS) ?: IntArray(frameFiles.size) { 0 }
        expectedCropTops = intent.getIntArrayExtra(EXTRA_EXPECTED_CROPS)
        repeatedBottomInsetPx = intent.getIntExtra(EXTRA_REPEATED_BOTTOM_INSET, 0)
        contentBounds = intent.getParcelableExtra(EXTRA_CONTENT_BOUNDS)
        if (cropTops.size < frameFiles.size) {
            cropTops = IntArray(frameFiles.size) { index -> cropTops.getOrElse(index) { 0 } }
        }

        buildReviewUi()

        findViewById<Button>(R.id.buttonSaveLongScreenshot).setOnClickListener {
            saveLongScreenshot()
        }
        findViewById<Button>(R.id.buttonDiscardLongScreenshot).setOnClickListener {
            cleanupSessionDir()
            finish()
        }
    }

    private fun buildReviewUi() {
        val container = findViewById<LinearLayout>(R.id.reviewContainer)
        container.removeAllViews()

        frameFiles.forEachIndexed { index, file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@forEachIndexed
            val label = TextView(this).apply {
                text = getString(R.string.long_screenshot_review_frame, index + 1)
                textSize = 16f
            }
            container.addView(label)

            val cropLabel = TextView(this).apply {
                text = getString(R.string.long_screenshot_review_crop, cropTops[index])
            }
            container.addView(cropLabel)

            if (index > 0) {
                container.addView(SeekBar(this).apply {
                    max = bitmap.height / 2
                    progress = cropTops[index].coerceIn(0, max)
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            cropTops[index] = progress
                            cropLabel.text = getString(R.string.long_screenshot_review_crop, progress)
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                    })
                })
            }

            container.addView(ImageView(this).apply {
                adjustViewBounds = true
                setImageBitmap(bitmap)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 18
                }
            })
        }
    }

    private fun saveLongScreenshot() {
        toastMessage(R.string.long_screenshot_status_finishing, ToastType.ACTIVITY)
        thread {
            val composed = composeLongScreenshotFromFiles(
                frameFiles,
                cropTops,
                repeatedBottomInsetPx,
                contentBounds,
                expectedCropTops
            )
            val result = saveBitmapToFile(
                this,
                composed.bitmap,
                App.getInstance().prefManager.fileNamePattern,
                compressionPreference(this),
                null,
                useAppData = "saveToStorage" !in App.getInstance().prefManager.postScreenshotActions,
                directory = null
            )
            Handler(Looper.getMainLooper()).post {
                handleSavedResult(result)
            }
        }
    }

    private fun handleSavedResult(saveImageResult: SaveImageResult?) {
        if (saveImageResult == null || !saveImageResult.success) {
            toastMessage(saveImageResult?.errorMessage ?: getString(R.string.screenshot_failed), ToastType.ERROR)
            return
        }
        val result = saveImageResult as? SaveImageResultSuccess ?: run {
            toastMessage(R.string.screenshot_failed, ToastType.ERROR)
            return
        }
        val postScreenshotActions = App.getInstance().prefManager.postScreenshotActions
        when {
            result.uri != null -> {
                AppFunctionResultStore.setLastReady(result.uri, result.bitmap.width, result.bitmap.height)
                if ("showToast" in postScreenshotActions) {
                    val dummyPath = if (result.dummyPath.isNotEmpty()) result.dummyPath else result.fileTitle ?: ""
                    toastMessage(formatLocalizedString(R.string.screenshot_file_saved, dummyPath), ToastType.SUCCESS)
                }
                if ("showNotification" in postScreenshotActions) {
                    createNotification(
                        this,
                        result.uri,
                        result.bitmap,
                        resources.configuration.densityDpi,
                        result.mimeType,
                        result.dummyPath
                    )
                }
                handlePostScreenshot(this, postScreenshotActions, result.uri, result.mimeType, result.bitmap)
            }

            result.file != null -> {
                val uri = Uri.fromFile(result.file)
                AppFunctionResultStore.setLastReady(uri, result.bitmap.width, result.bitmap.height)
                if ("showToast" in postScreenshotActions) {
                    toastMessage(formatLocalizedString(R.string.screenshot_file_saved, result.file.absolutePath), ToastType.SUCCESS)
                }
                if ("showNotification" in postScreenshotActions) {
                    createNotification(
                        this,
                        uri,
                        result.bitmap,
                        resources.configuration.densityDpi,
                        result.mimeType
                    )
                }
                handlePostScreenshot(this, postScreenshotActions, uri, result.mimeType, result.bitmap)
            }
        }
        App.getInstance().prefManager.screenshotCount++
        cleanupSessionDir()
        finish()
    }

    private fun cleanupSessionDir() {
        frameFiles.forEach { file ->
            runCatching { file.delete() }
        }
        runCatching { sessionDir.delete() }
    }
}
