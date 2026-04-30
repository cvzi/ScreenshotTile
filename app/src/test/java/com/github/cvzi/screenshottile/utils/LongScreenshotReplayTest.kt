package com.github.cvzi.screenshottile.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.Properties
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class LongScreenshotReplayTest {

    @Test
    fun composeLongScreenshot_replaysFramesFromLocalFolder() {
        val inputDirPath = systemPropertyOrEnv(INPUT_DIR_PROPERTY, INPUT_DIR_ENV)
        assumeTrue(
            "Set -D$INPUT_DIR_PROPERTY=<folder> or $INPUT_DIR_ENV to replay real long-screenshot frames locally.",
            !inputDirPath.isNullOrBlank()
        )

        val inputDir = File(inputDirPath!!)
        assertTrue("Replay input directory does not exist: ${inputDir.absolutePath}", inputDir.isDirectory)

        val frameFiles = loadLongScreenshotFrameFiles(inputDir)
        assertTrue(
            "Replay input directory must contain at least two PNG files: ${inputDir.absolutePath}",
            frameFiles.size >= 2
        )

        val config = loadReplayConfig(inputDir)
        val outputDir = resolveOutputDir(inputDir)
        outputDir.mkdirs()

        val result = composeLongScreenshotFromFiles(
            files = frameFiles,
            cropTopsOverride = config.cropTopsOverride,
            repeatedBottomInsetPx = config.repeatedBottomInsetPx,
            contentBounds = config.contentBounds,
            expectedCropTops = config.expectedCropTops
        )

        saveBitmap(result.bitmap, File(outputDir, "stitched.png"))
        saveBitmap(renderStitchedDebug(frameFiles, result), File(outputDir, "stitched_debug.png"))
        saveFrameDebugImages(frameFiles, result, config, outputDir)
        writeReplaySummary(frameFiles, result, config, outputDir)
        assertConfiguredExpectations(result, config)

        println("Long screenshot replay input: ${inputDir.absolutePath}")
        println("Long screenshot replay output: ${outputDir.absolutePath}")
    }

    private fun assertConfiguredExpectations(
        result: LongScreenshotComposeResult,
        config: ReplayConfig
    ) {
        config.assertRequiresReview?.let { expected ->
            assertEquals("Unexpected requiresReview value.", expected, result.requiresReview)
        }

        val expectedCropTops = config.assertCropTops ?: return
        assertEquals(
            "Configured assertCropTops length must match cropTops length.",
            expectedCropTops.size,
            result.cropTops.size
        )

        expectedCropTops.forEachIndexed { index, expected ->
            val actual = result.cropTops[index]
            assertTrue(
                "cropTops[$index] expected $expected +/- ${config.assertCropTolerancePx} but was $actual",
                abs(actual - expected) <= config.assertCropTolerancePx
            )
        }
    }

    private fun loadReplayConfig(inputDir: File): ReplayConfig {
        val configFile = File(inputDir, CONFIG_FILE_NAME)
        if (!configFile.isFile) {
            return ReplayConfig()
        }

        val properties = Properties().apply {
            configFile.inputStream().use(::load)
        }

        return ReplayConfig(
            cropTopsOverride = parseIntArray(properties.getProperty("cropTopsOverride")),
            repeatedBottomInsetPx = properties.getProperty("repeatedBottomInsetPx")?.toIntOrNull() ?: 0,
            contentBounds = parseRect(properties.getProperty("contentBounds")),
            expectedCropTops = parseIntArray(properties.getProperty("expectedCropTops")),
            assertCropTops = parseIntArray(properties.getProperty("assertCropTops")),
            assertCropTolerancePx = properties.getProperty("assertCropTolerancePx")?.toIntOrNull() ?: 0,
            assertRequiresReview = properties.getProperty("assertRequiresReview")?.toBooleanStrictOrNull()
        )
    }

    private fun parseIntArray(rawValue: String?): IntArray? {
        if (rawValue.isNullOrBlank()) {
            return null
        }
        val values = rawValue.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { value ->
                value.toIntOrNull() ?: error("Invalid integer value '$value' in replay config.")
            }
        return if (values.isEmpty()) null else values.toIntArray()
    }

    private fun parseRect(rawValue: String?): Rect? {
        val values = parseIntArray(rawValue) ?: return null
        require(values.size == 4) {
            "contentBounds must have exactly four comma-separated integers: left,top,right,bottom"
        }
        return Rect(values[0], values[1], values[2], values[3])
    }

    private fun resolveOutputDir(inputDir: File): File {
        val configuredOutputDir = systemPropertyOrEnv(OUTPUT_DIR_PROPERTY, OUTPUT_DIR_ENV)
        if (!configuredOutputDir.isNullOrBlank()) {
            return File(configuredOutputDir)
        }

        val userDir = File(requireNotNull(System.getProperty("user.dir")) { "user.dir is not set" })
        val projectRoot = when {
            File(userDir, "app").isDirectory -> userDir
            userDir.name.equals("app", ignoreCase = true) -> userDir.parentFile ?: userDir
            else -> userDir
        }
        return File(projectRoot, "app/build/reports/long-screenshot-replay/${inputDir.name}")
    }

    private fun systemPropertyOrEnv(propertyName: String, envName: String): String? {
        return System.getProperty(propertyName)?.takeIf { it.isNotBlank() }
            ?: System.getenv(envName)?.takeIf { it.isNotBlank() }
    }

    private fun renderStitchedDebug(
        frameFiles: List<File>,
        result: LongScreenshotComposeResult
    ): Bitmap {
        val stitchedDebug = result.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(stitchedDebug)
        val seamPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 4f
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
        }
        val textBgPaint = Paint().apply {
            color = Color.argb(160, 0, 0, 0)
            style = Paint.Style.FILL
        }

        var yOffset = 0
        frameFiles.forEachIndexed { index, file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@forEachIndexed
            if (index > 0) {
                canvas.drawLine(
                    0f,
                    yOffset.toFloat(),
                    stitchedDebug.width.toFloat(),
                    yOffset.toFloat(),
                    seamPaint
                )
                drawDebugLabel(
                    canvas = canvas,
                    text = "seam ${index - 1}->$index y=$yOffset cropTop=${result.cropTops[index]} conf=${result.confidences.getOrElse(index - 1) { 1f }}",
                    textPaint = textPaint,
                    textBgPaint = textBgPaint,
                    top = yOffset + 8f
                )
            }
            yOffset += (bitmap.height - result.cropTops[index] - result.bottomCrops[index]).coerceAtLeast(1)
            bitmap.recycle()
        }
        return stitchedDebug
    }

    private fun saveFrameDebugImages(
        frameFiles: List<File>,
        result: LongScreenshotComposeResult,
        config: ReplayConfig,
        outputDir: File
    ) {
        val seamPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 4f
        }
        val expectedPaint = Paint().apply {
            color = Color.YELLOW
            strokeWidth = 3f
        }
        val boundsPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isAntiAlias = true
        }
        val textBgPaint = Paint().apply {
            color = Color.argb(160, 0, 0, 0)
            style = Paint.Style.FILL
        }

        frameFiles.forEachIndexed { index, file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@forEachIndexed
            val debugBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(debugBitmap)

            config.contentBounds?.let { canvas.drawRect(it, boundsPaint) }

            val bottomCrop = result.bottomCrops.getOrElse(index) { 0 }
            if (bottomCrop > 0) {
                val y = (bitmap.height - bottomCrop).toFloat()
                canvas.drawLine(0f, y, bitmap.width.toFloat(), y, seamPaint)
            }

            val cropTop = result.cropTops.getOrElse(index) { 0 }
            if (cropTop > 0) {
                val y = cropTop.toFloat()
                canvas.drawLine(0f, y, bitmap.width.toFloat(), y, seamPaint)
            }

            config.expectedCropTops?.getOrNull(index)?.takeIf { it > 0 }?.let { expected ->
                val y = expected.toFloat()
                canvas.drawLine(0f, y, bitmap.width.toFloat(), y, expectedPaint)
            }

            val label = buildString {
                append("frame ")
                append(index)
                append(" cropTop=")
                append(cropTop)
                append(" bottomCrop=")
                append(bottomCrop)
                config.expectedCropTops?.getOrNull(index)?.let {
                    append(" expected=")
                    append(it)
                }
            }
            drawDebugLabel(canvas, label, textPaint, textBgPaint)
            saveBitmap(debugBitmap, File(outputDir, String.format(Locale.US, "frame_%03d_debug.png", index)))
            bitmap.recycle()
            debugBitmap.recycle()
        }
    }

    private fun writeReplaySummary(
        frameFiles: List<File>,
        result: LongScreenshotComposeResult,
        config: ReplayConfig,
        outputDir: File
    ) {
        val summary = buildString {
            appendLine("inputDir=${frameFiles.firstOrNull()?.parentFile?.absolutePath ?: ""}")
            appendLine("frameCount=${frameFiles.size}")
            appendLine("contentBounds=${config.contentBounds?.flattenToString().orEmpty()}")
            appendLine("repeatedBottomInsetPx=${config.repeatedBottomInsetPx}")
            appendLine("cropTopsOverride=${config.cropTopsOverride?.joinToString(",").orEmpty()}")
            appendLine("expectedCropTops=${config.expectedCropTops?.joinToString(",").orEmpty()}")
            appendLine("result.cropTops=${result.cropTops.joinToString(",")}")
            appendLine("result.bottomCrops=${result.bottomCrops.joinToString(",")}")
            appendLine("result.confidences=${result.confidences.joinToString(",")}")
            appendLine("result.requiresReview=${result.requiresReview}")
            appendLine("result.bitmap=${result.bitmap.width}x${result.bitmap.height}")
        }
        File(outputDir, "summary.txt").writeText(summary)
    }

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

    private fun drawDebugLabel(
        canvas: Canvas,
        text: String,
        textPaint: Paint,
        textBgPaint: Paint,
        top: Float = 8f
    ) {
        val padding = 8f
        val baseline = top + textPaint.textSize
        val width = textPaint.measureText(text)
        canvas.drawRect(
            0f,
            top,
            width + padding * 2,
            baseline + padding,
            textBgPaint
        )
        canvas.drawText(text, padding, baseline, textPaint)
    }

    private data class ReplayConfig(
        val cropTopsOverride: IntArray? = null,
        val repeatedBottomInsetPx: Int = 0,
        val contentBounds: Rect? = null,
        val expectedCropTops: IntArray? = null,
        val assertCropTops: IntArray? = null,
        val assertCropTolerancePx: Int = 0,
        val assertRequiresReview: Boolean? = null
    )

    companion object {
        private const val CONFIG_FILE_NAME = "stitch.properties"
        private const val INPUT_DIR_PROPERTY = "longScreenshot.inputDir"
        private const val OUTPUT_DIR_PROPERTY = "longScreenshot.outputDir"
        private const val INPUT_DIR_ENV = "LONG_SCREENSHOT_INPUT_DIR"
        private const val OUTPUT_DIR_ENV = "LONG_SCREENSHOT_OUTPUT_DIR"
    }
}
