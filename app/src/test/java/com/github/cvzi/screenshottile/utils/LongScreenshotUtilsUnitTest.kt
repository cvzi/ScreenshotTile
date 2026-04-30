package com.github.cvzi.screenshottile.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class LongScreenshotUtilsUnitTest {

    @Test
    fun composeLongScreenshot_detectsSimpleOverlap() {
        val first = stripedBitmap(40, 120, start = 0)
        val second = stripedBitmap(40, 120, start = 60)

        val result = composeLongScreenshot(listOf(first, second))

        assertTrue(result.cropTops[1] in 40..80)
        assertFalse(result.requiresReview)
    }

    @Test
    fun areLongScreenshotFramesSimilar_detectsIdenticalFrames() {
        val first = stripedBitmap(30, 90, start = 0)
        val second = stripedBitmap(30, 90, start = 0)

        assertTrue(areLongScreenshotFramesSimilar(first, second))
    }

    @Test
    fun composeLongScreenshot_cropsRepeatedBottomStripWithoutReportedInset() {
        val width = 40
        val height = 120
        val bottomStripHeight = 12
        val first = stripedBitmap(width, height, start = 0)
        val second = stripedBitmap(width, height, start = 60)
        paintBottomStrip(first, bottomStripHeight, Color.LTGRAY)
        paintBottomStrip(second, bottomStripHeight, Color.LTGRAY)

        val result = composeLongScreenshot(listOf(first, second))
        val explicitCropResult = composeLongScreenshot(
            listOf(first, second),
            repeatedBottomInsetPx = bottomStripHeight
        )

        assertEquals(explicitCropResult.bitmap.height, result.bitmap.height)
        assertEquals(explicitCropResult.cropTops[1], result.cropTops[1])
    }

    @Test
    fun composeLongScreenshot_ignoresChangingLeftEdgeOverlay() {
        val width = 60
        val height = 140
        val first = stripedBitmap(width, height, start = 0)
        val second = stripedBitmap(width, height, start = 70)

        paintLeftStrip(first, stripWidth = 6, color = Color.RED)
        paintLeftStrip(second, stripWidth = 6, color = Color.BLUE)

        val cleanResult = composeLongScreenshot(
            listOf(stripedBitmap(width, height, start = 0), stripedBitmap(width, height, start = 70))
        )
        val noisyResult = composeLongScreenshot(listOf(first, second))

        assertEquals(cleanResult.cropTops[1], noisyResult.cropTops[1])
    }

    @Test
    fun composeLongScreenshot_prefersScrollableContentBounds() {
        val width = 80
        val height = 160
        val first = stripedBitmap(width, height, start = 0)
        val second = stripedBitmap(width, height, start = 70)

        paintLeftStrip(first, stripWidth = 18, color = Color.RED)
        paintLeftStrip(second, stripWidth = 18, color = Color.BLUE)

        val cleanResult = composeLongScreenshot(
            listOf(stripedBitmap(width, height, start = 0), stripedBitmap(width, height, start = 70))
        )
        val boundedResult = composeLongScreenshot(
            listOf(first, second),
            contentBounds = Rect(18, 0, width, height)
        )

        assertTrue(kotlin.math.abs(cleanResult.cropTops[1] - boundedResult.cropTops[1]) <= 1)
    }

    @Test
    fun composeLongScreenshot_ignoresFixedHeaderAboveScrollableContent() {
        val width = 80
        val height = 180
        val headerHeight = 24
        val first = stripedBitmap(width, height, start = 0)
        val second = stripedBitmap(width, height, start = 70)

        paintTopStrip(first, stripHeight = headerHeight, color = Color.DKGRAY)
        paintTopStrip(second, stripHeight = headerHeight, color = Color.DKGRAY)

        val cleanResult = composeLongScreenshot(
            listOf(stripedBitmap(width, height, start = 0), stripedBitmap(width, height, start = 70))
        )
        val boundedResult = composeLongScreenshot(
            listOf(first, second),
            contentBounds = Rect(0, headerHeight, width, height)
        )

        assertTrue(kotlin.math.abs(cleanResult.cropTops[1] - boundedResult.cropTops[1]) <= 1)
    }

    private fun stripedBitmap(width: Int, height: Int, start: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            val value = (start + y) % 255
            val color = Color.rgb(value, (value * 2) % 255, (value * 3) % 255)
            for (x in 0 until width) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    private fun paintBottomStrip(bitmap: Bitmap, stripHeight: Int, color: Int) {
        for (y in (bitmap.height - stripHeight).coerceAtLeast(0) until bitmap.height) {
            for (x in 0 until bitmap.width) {
                bitmap.setPixel(x, y, color)
            }
        }
    }

    private fun paintLeftStrip(bitmap: Bitmap, stripWidth: Int, color: Int) {
        for (x in 0 until stripWidth.coerceAtMost(bitmap.width)) {
            for (y in 0 until bitmap.height) {
                bitmap.setPixel(x, y, color)
            }
        }
    }

    private fun paintTopStrip(bitmap: Bitmap, stripHeight: Int, color: Int) {
        for (y in 0 until stripHeight.coerceAtMost(bitmap.height)) {
            for (x in 0 until bitmap.width) {
                bitmap.setPixel(x, y, color)
            }
        }
    }
}
