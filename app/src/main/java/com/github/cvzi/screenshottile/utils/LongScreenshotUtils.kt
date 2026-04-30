package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.provider.MediaStore.Images
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.CompressionOptions
import com.github.cvzi.screenshottile.OutputStreamResultSuccess
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.graphics.get

data class LongScreenshotSeam(
    val cropTop: Int,
    val confidence: Float
)

data class LongScreenshotComposeResult(
    val bitmap: Bitmap,
    val cropTops: IntArray,
    val confidences: FloatArray,
    val requiresReview: Boolean,
    val bottomCrops: IntArray
)

private const val LONG_SCREENSHOT_SCALE_WIDTH = 192
private const val LONG_SCREENSHOT_EDGE_IGNORE_RATIO = 0.18f

fun saveLongScreenshotTempBitmap(directory: File, index: Int, bitmap: Bitmap): File {
    directory.mkdirs()
    val file = File(directory, String.format("%03d.png", index))
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    return file
}

fun loadLongScreenshotFrameFiles(directory: File): List<File> {
    return directory.listFiles()
        ?.filter { it.isFile && it.extension.equals("png", ignoreCase = true) }
        ?.sortedBy { it.name }
        ?: emptyList()
}

fun composeLongScreenshotFromFiles(
    files: List<File>,
    cropTopsOverride: IntArray? = null,
    repeatedBottomInsetPx: Int = 0,
    contentBounds: Rect? = null,
    expectedCropTops: IntArray? = null
): LongScreenshotComposeResult {
    val bitmaps = files.mapNotNull { file ->
        runCatching { android.graphics.BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }
    return composeLongScreenshot(bitmaps, cropTopsOverride, repeatedBottomInsetPx, contentBounds, expectedCropTops)
}

fun composeLongScreenshot(
    bitmaps: List<Bitmap>,
    cropTopsOverride: IntArray? = null,
    repeatedBottomInsetPx: Int = 0,
    contentBounds: Rect? = null,
    expectedCropTops: IntArray? = null
): LongScreenshotComposeResult {
    require(bitmaps.isNotEmpty()) { "Need at least one bitmap" }

    val cropTops = IntArray(bitmaps.size) { 0 }
    val confidences = FloatArray(max(bitmaps.size - 1, 0)) { 1f }
    val bottomCrops = IntArray(bitmaps.size) { 0 }
    var requiresReview = false
    val effectiveRepeatedBottomInsetPx = max(
        repeatedBottomInsetPx,
        estimateRepeatedBottomInsetPx(bitmaps)
    )

    if (effectiveRepeatedBottomInsetPx > 0) {
        for (index in 0 until bitmaps.lastIndex) {
            bottomCrops[index] = effectiveRepeatedBottomInsetPx.coerceAtMost(bitmaps[index].height - 1)
        }
    }

    for (index in 1 until bitmaps.size) {
        if (cropTopsOverride != null && index < cropTopsOverride.size) {
            cropTops[index] = cropTopsOverride[index].coerceIn(0, bitmaps[index].height - 1)
            confidences[index - 1] = 1f
            continue
        }
        val seam = findBestSeam(
            bitmaps[index - 1],
            bitmaps[index],
            bottomCrops[index - 1],
            contentBounds,
            expectedCropTops?.getOrNull(index)
        )
        cropTops[index] = seam.cropTop.coerceIn(0, bitmaps[index].height - 1)
        confidences[index - 1] = seam.confidence
        if (seam.confidence < 0.82f) {
            requiresReview = true
        }
    }

    val width = bitmaps.maxOf { it.width }
    var totalHeight = 0
    bitmaps.forEachIndexed { index, bitmap ->
        val bottomCrop = bottomCrops[index]
        totalHeight += max(1, bitmap.height - cropTops[index] - bottomCrop)
    }

    val result = createBitmap(width, totalHeight)
    val canvas = Canvas(result)
    var yOffset = 0
    bitmaps.forEachIndexed { index, bitmap ->
        val cropTop = cropTops[index]
        val bottomCrop = bottomCrops[index]
        val src = Rect(0, cropTop, bitmap.width, bitmap.height - bottomCrop)
        val dst = Rect(0, yOffset, bitmap.width, yOffset + src.height())
        canvas.drawBitmap(bitmap, src, dst, null)
        yOffset += src.height()
    }

    return LongScreenshotComposeResult(
        bitmap = result,
        cropTops = cropTops,
        confidences = confidences,
        requiresReview = requiresReview,
        bottomCrops = bottomCrops
    )
}

fun saveLongScreenshotDebugArtifacts(
    context: Context,
    files: List<File>,
    filePrefix: String,
    composeResult: LongScreenshotComposeResult,
    contentBounds: Rect? = null,
    expectedCropTops: IntArray? = null
): List<String> {
    if (!BuildConfig.DEBUG) {
        return emptyList()
    }
    val outputFiles = mutableListOf<String>()
    val frameBitmaps = files.mapNotNull { file ->
        android.graphics.BitmapFactory.decodeFile(file.absolutePath)
    }
    if (frameBitmaps.isEmpty()) {
        return emptyList()
    }

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

    frameBitmaps.forEachIndexed { index, bitmap ->
        val debugBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(debugBitmap)
        contentBounds?.let { bounds ->
            canvas.drawRect(bounds, boundsPaint)
        }
        if (index < composeResult.bottomCrops.size) {
            val bottomCrop = composeResult.bottomCrops[index]
            if (bottomCrop > 0) {
                val y = (bitmap.height - bottomCrop).toFloat()
                canvas.drawLine(0f, y, bitmap.width.toFloat(), y, seamPaint)
            }
        }
        if (index < composeResult.cropTops.size && composeResult.cropTops[index] > 0) {
            val y = composeResult.cropTops[index].toFloat()
            canvas.drawLine(0f, y, bitmap.width.toFloat(), y, seamPaint)
        }
        expectedCropTops?.getOrNull(index)?.takeIf { it > 0 }?.let { expected ->
            val y = expected.toFloat()
            canvas.drawLine(0f, y, bitmap.width.toFloat(), y, expectedPaint)
        }
        val label = buildString {
            append("frame ")
            append(index)
            append(" cropTop=")
            append(composeResult.cropTops.getOrElse(index) { 0 })
            append(" bottomCrop=")
            append(composeResult.bottomCrops.getOrElse(index) { 0 })
            expectedCropTops?.getOrNull(index)?.let {
                append(" expected=")
                append(it)
            }
        }
        drawDebugLabel(canvas, label, textPaint, textBgPaint)
        saveDebugBitmapToVisibleStorage(
            context,
            debugBitmap,
            "${filePrefix}_frame_${String.format("%03d", index)}_debug"
        )?.let(outputFiles::add)
        debugBitmap.recycle()
    }

    val stitchedDebug = composeResult.bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val stitchedCanvas = Canvas(stitchedDebug)
    var yOffset = 0
    frameBitmaps.forEachIndexed { index, bitmap ->
        if (index > 0) {
            stitchedCanvas.drawLine(
                0f,
                yOffset.toFloat(),
                stitchedDebug.width.toFloat(),
                yOffset.toFloat(),
                seamPaint
            )
            drawDebugLabel(
                stitchedCanvas,
                "seam ${index - 1}->${index} y=$yOffset cropTop=${composeResult.cropTops[index]} conf=${composeResult.confidences.getOrElse(index - 1) { 1f }}",
                textPaint,
                textBgPaint,
                yOffset + 8f
            )
        }
        yOffset += (bitmap.height - composeResult.cropTops[index] - composeResult.bottomCrops[index]).coerceAtLeast(1)
    }
    saveDebugBitmapToVisibleStorage(
        context,
        stitchedDebug,
        "${filePrefix}_stitched_debug"
    )?.let(outputFiles::add)
    stitchedDebug.recycle()
    frameBitmaps.forEach { it.recycle() }
    return outputFiles
}

fun areLongScreenshotFramesSimilar(previous: Bitmap, current: Bitmap, contentBounds: Rect? = null): Boolean {
    val previousScaled = normalizeForCompare(previous)
    val currentScaled = normalizeForCompare(current)
    val scaledContentBounds = scaleContentBounds(contentBounds, previous, previousScaled)
    val topIgnore = (previousScaled.height * 0.08f).toInt()
    val bottomIgnore = (previousScaled.height * 0.08f).toInt()
    val startY = max(topIgnore, scaledContentBounds?.top ?: 0)
    val endY = min(
        previousScaled.height - bottomIgnore,
        scaledContentBounds?.bottom ?: previousScaled.height
    ).coerceAtLeast(startY + 1)
    val score = compareRegion(
        previousScaled,
        currentScaled,
        startPrev = startY,
        startNext = startY,
        height = endY - startY,
        contentBounds = scaledContentBounds
    )
    previousScaled.recycle()
    currentScaled.recycle()
    return score < 0.025f
}

private fun findBestSeam(
    previous: Bitmap,
    current: Bitmap,
    previousBottomInsetPx: Int,
    contentBounds: Rect?,
    expectedCropTopPx: Int?
): LongScreenshotSeam {
    val previousScaled = normalizeForCompare(previous)
    val currentScaled = normalizeForCompare(current)
    val scaledContentBounds = scaleContentBounds(contentBounds, previous, previousScaled)
    val scaledExpectedCropTop = expectedCropTopPx?.let {
        ((it.coerceIn(0, current.height - 1)) * (currentScaled.height.toFloat() / current.height.toFloat()))
    }

    val topIgnore = max(2, (previousScaled.height * 0.08f).toInt())
    val bottomIgnore = max(2, (previousScaled.height * 0.08f).toInt())
    val previousBottomInsetScaled = if (previousBottomInsetPx > 0) {
        (previousBottomInsetPx * (previousScaled.height.toFloat() / previous.height.toFloat())).toInt()
    } else {
        0
    }
    val previousEffectiveBottomCrop = if (previousBottomInsetScaled > 0) {
        previousBottomInsetScaled
    } else {
        bottomIgnore
    }
    val compareTop = max(topIgnore, scaledContentBounds?.top ?: 0)
    val previousCompareBottom = min(
        previousScaled.height - previousEffectiveBottomCrop,
        scaledContentBounds?.bottom ?: previousScaled.height
    )
    val currentCompareBottom = min(
        currentScaled.height - bottomIgnore,
        scaledContentBounds?.bottom ?: currentScaled.height
    )
    val maxOverlap = min(
        previousCompareBottom - compareTop,
        currentCompareBottom - compareTop
    ).coerceAtLeast(8)
    val minOverlap = max(8, (maxOverlap * 0.15f).toInt())

    var bestOverlap = minOverlap
    var bestNextStart = compareTop
    var bestScore = Float.MAX_VALUE

    val nextStartLimit = min(
        compareTop + (currentCompareBottom - compareTop) / 3,
        currentCompareBottom - minOverlap
    ).coerceAtLeast(compareTop)

    for (startNext in compareTop..nextStartLimit step 2) {
        val availableHeight = min(
            previousCompareBottom - compareTop,
            currentCompareBottom - startNext
        )
        if (availableHeight < minOverlap) {
            continue
        }
        for (overlap in minOverlap..availableHeight step 2) {
            val startPrev = previousCompareBottom - overlap
            if (startPrev < 0 || startNext + overlap > currentScaled.height) {
                continue
            }
            val score = compareRegion(
                previousScaled,
                currentScaled,
                startPrev,
                startNext,
                overlap,
                scaledContentBounds
            )
            val expectedPenalty = if (scaledExpectedCropTop != null) {
                kotlin.math.abs((startNext + overlap) - scaledExpectedCropTop) * 0.0035f
            } else {
                0f
            }
            val weightedScore = score + (startNext - compareTop) * 0.0015f + expectedPenalty
            if (weightedScore < bestScore) {
                bestScore = weightedScore
                bestOverlap = overlap
                bestNextStart = startNext
            }
        }
    }

    val fullHeight = current.height.toFloat()
    val scaledHeight = currentScaled.height.toFloat()
    val coarseCropTop = ((bestNextStart + bestOverlap) * (fullHeight / scaledHeight)).roundToInt()
    val confidence = (1f - min(bestScore, 1f)).coerceIn(0f, 1f)

    previousScaled.recycle()
    currentScaled.recycle()

    val refinedCropTop = refineCropTopNearFullResolution(
        previous = previous,
        current = current,
        previousBottomInsetPx = previousBottomInsetPx,
        contentBounds = contentBounds,
        coarseCropTop = coarseCropTop
    )

    return LongScreenshotSeam(cropTop = refinedCropTop, confidence = confidence)
}

private fun normalizeForCompare(bitmap: Bitmap): Bitmap {
    val width = min(LONG_SCREENSHOT_SCALE_WIDTH, bitmap.width.coerceAtLeast(1))
    val height = max(1, (bitmap.height * (width.toFloat() / bitmap.width.coerceAtLeast(1))).toInt())
    return bitmap.scale(width, height, false)
}

private fun estimateRepeatedBottomInsetPx(bitmaps: List<Bitmap>): Int {
    if (bitmaps.size < 2) {
        return 0
    }

    val estimates = bitmaps.zipWithNext()
        .map { (previous, current) -> estimateRepeatedBottomInsetPx(previous, current) }
        .filter { it > 0 }
        .sorted()

    if (estimates.isEmpty()) {
        return 0
    }

    return estimates[estimates.size / 2]
}

private fun estimateRepeatedBottomInsetPx(previous: Bitmap, current: Bitmap): Int {
    val previousScaled = normalizeForCompare(previous)
    val currentScaled = normalizeForCompare(current)

    val maxCandidate = min(
        min(previousScaled.height, currentScaled.height) / 6,
        36
    ).coerceAtLeast(0)
    if (maxCandidate < 4) {
        previousScaled.recycle()
        currentScaled.recycle()
        return 0
    }

    var bestMatchHeight = 0
    var consecutiveMisses = 0
    val allowedMisses = 1
    val rowMatchThreshold = 0.02f

    for (offset in 0 until maxCandidate) {
        val score = compareRow(
            previousScaled,
            currentScaled,
            previousScaled.height - 1 - offset,
            currentScaled.height - 1 - offset,
            null
        )
        if (score <= rowMatchThreshold) {
            bestMatchHeight = offset + 1
            consecutiveMisses = 0
        } else {
            consecutiveMisses += 1
            if (consecutiveMisses > allowedMisses) {
                break
            }
        }
    }

    if (bestMatchHeight < 4) {
        previousScaled.recycle()
        currentScaled.recycle()
        return 0
    }

    val transitionScore = if (bestMatchHeight < previousScaled.height && bestMatchHeight < currentScaled.height) {
        compareRow(
            previousScaled,
            currentScaled,
            previousScaled.height - 1 - bestMatchHeight,
            currentScaled.height - 1 - bestMatchHeight,
            null
        )
    } else {
        1f
    }

    previousScaled.recycle()
    currentScaled.recycle()

    if (transitionScore <= rowMatchThreshold) {
        return 0
    }

    return ((bestMatchHeight * current.height.toFloat()) / currentScaled.height.toFloat()).toInt()
}

private fun compareRegion(
    previous: Bitmap,
    current: Bitmap,
    startPrev: Int,
    startNext: Int,
    height: Int,
    contentBounds: Rect? = null
): Float {
    var score = 0f
    var samples = 0
    val columns = sampleColumns(previous.width, contentBounds)
    for (offset in 0 until height step 2) {
        for (column in columns) {
            val prevColor = previous[column.coerceIn(0, previous.width - 1), startPrev + offset]
            val nextColor = current[column.coerceIn(0, current.width - 1), startNext + offset]
            score += pixelDistance(prevColor, nextColor)
            samples++
        }
    }
    return if (samples == 0) 1f else score / samples.toFloat()
}

private fun compareRow(
    previous: Bitmap,
    current: Bitmap,
    previousY: Int,
    currentY: Int,
    contentBounds: Rect?
): Float {
    var score = 0f
    var samples = 0
    val columns = sampleColumns(previous.width, contentBounds)
    for (column in columns) {
        val prevColor = previous[column.coerceIn(0, previous.width - 1), previousY.coerceIn(
            0,
            previous.height - 1
        )]
        val nextColor = current[column.coerceIn(0, current.width - 1), currentY.coerceIn(0, current.height - 1)]
        score += pixelDistance(prevColor, nextColor)
        samples++
    }
    return if (samples == 0) 1f else score / samples.toFloat()
}

private fun sampleColumns(width: Int, contentBounds: Rect?): IntArray {
    if (width <= 1) {
        return intArrayOf(0)
    }
    val edgeInset = (width * LONG_SCREENSHOT_EDGE_IGNORE_RATIO).toInt()
    val preferredLeft = contentBounds?.left ?: 0
    val preferredRight = contentBounds?.right?.minus(1) ?: (width - 1)
    val minX = max(edgeInset, preferredLeft).coerceIn(0, width - 1)
    val maxX = min(width - 1 - edgeInset, preferredRight).coerceAtLeast(minX)
    if (maxX <= minX) {
        return intArrayOf(width / 2)
    }
    return intArrayOf(
        minX,
        minX + (maxX - minX) / 4,
        minX + (maxX - minX) / 2,
        minX + (maxX - minX) * 3 / 4,
        maxX
    ).distinct().toIntArray()
}

private fun scaleContentBounds(contentBounds: Rect?, original: Bitmap, scaled: Bitmap): Rect? {
    contentBounds ?: return null
    if (original.width <= 0 || original.height <= 0) {
        return null
    }
    val scaleX = scaled.width.toFloat() / original.width.toFloat()
    val scaleY = scaled.height.toFloat() / original.height.toFloat()
    val left = (contentBounds.left * scaleX).toInt()
    val top = (contentBounds.top * scaleY).toInt()
    val right = (contentBounds.right * scaleX).toInt()
    val bottom = (contentBounds.bottom * scaleY).toInt()
    val scaledRect = Rect(
        left.coerceIn(0, scaled.width - 1),
        top.coerceIn(0, scaled.height - 1),
        right.coerceIn(1, scaled.width),
        bottom.coerceIn(1, scaled.height)
    )
    if (scaledRect.width() <= 1 || scaledRect.height() <= 1) {
        return null
    }
    val horizontalInset = max(1, (scaledRect.width() * 0.06f).toInt())
    scaledRect.left = (scaledRect.left + horizontalInset).coerceAtMost(scaledRect.right - 1)
    scaledRect.right = (scaledRect.right - horizontalInset).coerceAtLeast(scaledRect.left + 1)
    return scaledRect
}

private fun pixelDistance(colorA: Int, colorB: Int): Float {
    val red = abs(Color.red(colorA) - Color.red(colorB)) / 255f
    val green = abs(Color.green(colorA) - Color.green(colorB)) / 255f
    val blue = abs(Color.blue(colorA) - Color.blue(colorB)) / 255f
    return (red + green + blue) / 3f
}

private fun refineCropTopNearFullResolution(
    previous: Bitmap,
    current: Bitmap,
    previousBottomInsetPx: Int,
    contentBounds: Rect?,
    coarseCropTop: Int
): Int {
    val topIgnore = max(2, (previous.height * 0.08f).toInt())
    val bottomIgnore = max(2, (previous.height * 0.08f).toInt())
    val previousEffectiveBottomCrop = if (previousBottomInsetPx > 0) {
        previousBottomInsetPx
    } else {
        bottomIgnore
    }
    val compareTop = max(topIgnore, contentBounds?.top ?: 0)
    val previousCompareBottom = min(
        previous.height - previousEffectiveBottomCrop,
        contentBounds?.bottom ?: previous.height
    )
    val currentCompareBottom = min(
        current.height - bottomIgnore,
        contentBounds?.bottom ?: current.height
    )

    val maxWindow = min(
        240,
        min(previousCompareBottom - compareTop, currentCompareBottom - compareTop) / 2
    ).coerceAtLeast(48)
    val searchRadius = 72
    val minCropTop = (compareTop + maxWindow).coerceAtMost(currentCompareBottom - 1)
    val maxCropTop = currentCompareBottom.coerceAtLeast(minCropTop)
    val startCropTop = (coarseCropTop - searchRadius).coerceIn(minCropTop, maxCropTop)
    val endCropTop = (coarseCropTop + searchRadius).coerceIn(startCropTop, maxCropTop)

    var bestCropTop = coarseCropTop.coerceIn(minCropTop, maxCropTop)
    var bestScore = Float.MAX_VALUE

    for (candidateCropTop in startCropTop..endCropTop) {
        val availableWindow = min(
            maxWindow,
            min(previousCompareBottom - compareTop, candidateCropTop - compareTop)
        )
        if (availableWindow < 32) {
            continue
        }
        val score = compareFullResolutionWindow(
            previous = previous,
            current = current,
            endPrev = previousCompareBottom,
            endCurrent = candidateCropTop,
            window = availableWindow,
            contentBounds = contentBounds
        )
        if (score < bestScore) {
            bestScore = score
            bestCropTop = candidateCropTop
        }
    }

    return bestCropTop
}

private fun compareFullResolutionWindow(
    previous: Bitmap,
    current: Bitmap,
    endPrev: Int,
    endCurrent: Int,
    window: Int,
    contentBounds: Rect?
): Float {
    var score = 0f
    var samples = 0
    val columns = sampleColumns(previous.width, contentBounds)
    val rowStep = max(1, window / 80)
    for (offset in 0 until window step rowStep) {
        val prevY = (endPrev - window + offset).coerceIn(0, previous.height - 1)
        val currentY = (endCurrent - window + offset).coerceIn(0, current.height - 1)
        for (column in columns) {
            val prevColor = previous[column.coerceIn(0, previous.width - 1), prevY]
            val nextColor = current[column.coerceIn(0, current.width - 1), currentY]
            score += pixelDistance(prevColor, nextColor)
            samples++
        }
    }
    return if (samples == 0) 1f else score / samples.toFloat()
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

private fun saveDebugBitmapToVisibleStorage(
    context: Context,
    bitmap: Bitmap,
    fileTitle: String
): String? {
    val outputStreamResult = createOutputStream(
        context,
        fileTitle,
        CompressionOptions("png", 100),
        Date(),
        android.graphics.Point(bitmap.width, bitmap.height),
        false,
        null
    )
    val result = outputStreamResult as? OutputStreamResultSuccess ?: return null
    return try {
        result.fileOutputStream.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        if (result.uri != null) {
            result.contentValues?.run {
                clear()
                put(Images.ImageColumns.IS_PENDING, 0)
                context.contentResolver.update(result.uri, this, null, null)
            }
        }
        result.dummyPath.ifEmpty {
            result.imageFile?.absolutePath ?: result.uri?.toString()
        }
    } catch (_: Exception) {
        null
    }
}
