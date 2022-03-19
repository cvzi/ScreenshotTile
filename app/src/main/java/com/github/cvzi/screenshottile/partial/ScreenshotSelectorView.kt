/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 *
 * This file has been modified by cuzi.
 * Original file can be found at
 * https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/src/com/android/systemui/screenshot/ScreenshotSelectorView.java
 */

package com.github.cvzi.screenshottile.partial

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.github.cvzi.screenshottile.BuildConfig
import kotlin.math.max
import kotlin.math.min


/**
 * Draws a selection rectangle while taking screenshot
 */
class ScreenshotSelectorView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    companion object {
        private const val OFF = 0
        private const val LEFT = 1
        private const val TOP = 2
        private const val RIGHT = 4
        private const val BOTTOM = 8
    }

    var onShutter: ((Rect) -> Unit)? = null
    var shutter: Int? = null
    var fullScreenIcon: Int? = null
    var text: String? = null
    var bitmap: Bitmap? = null

    var defaultState = true
    private var showShutter = false
    private var startPoint: Point? = null
    private var selectionRect: Rect? = null
    private var resultRect: Rect? = null
    private var edgeMode = OFF
    private var shutterRect: Rect? = null
    private var fullscreenButtonRect: Rect? = null
    private var drawablesCache = HashMap<Int, Drawable?>(2)

    private val paintSelection = Paint(Color.TRANSPARENT).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val paintEdgeOutside = Paint(Color.RED).apply {
        alpha = 160
        style = Paint.Style.FILL
    }
    private val paintBackground = Paint(Color.BLACK).apply {
        alpha = 160
        style = Paint.Style.FILL
        if (BuildConfig.DEBUG) {
            color = Color.MAGENTA
            alpha = 200
        }
    }
    private val paintText = Paint(Color.DKGRAY).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
    }

    /**
     * Reset view to default state, i.e. no active selection
     */
    fun reset() {
        defaultState = true
        showShutter = false
        startPoint = null
        selectionRect = null
        resultRect = null
        edgeMode = OFF
        shutterRect = null
        fullscreenButtonRect = null
        invalidate()
    }

    private fun getDrawable(resource: Int): Drawable? {
        return drawablesCache.getOrPut(resource) {
            ResourcesCompat.getDrawable(this.resources, resource, null)
        }
    }

    private fun startSelection(x: Int, y: Int) {
        startPoint = Point(x, y)
        selectionRect = Rect(x, y, x, y)
        showShutter = false
        edgeMode = OFF
    }

    private fun updateSelection(x: Int, y: Int) {
        if (edgeMode != OFF) {
            resultRect?.run {
                left = if (edgeMode and LEFT > 0) x else left
                top = if (edgeMode and TOP > 0) y else top
                right = if (edgeMode and RIGHT > 0) x else right
                bottom = if (edgeMode and BOTTOM > 0) y else bottom
                invalidate()
            }
        } else {
            selectionRect?.run {
                startPoint?.let {
                    left = min(it.x, x)
                    top = min(it.y, y)
                    right = max(it.x, x)
                    bottom = max(it.y, y)
                    invalidate()
                }
            }
        }
    }

    private fun stopSelection() {
        startPoint = null
        resultRect = selectionRect
        selectionRect = null
    }

    private fun ignoreSelection() {
        startPoint = null
        selectionRect = null
    }

    private fun stopEdgeMode() {
        edgeMode = OFF
    }

    private fun showShutter() {
        if (shutter != null) {
            showShutter = true
            invalidate()
        }
    }

    private fun hideShutter() {
        showShutter = false
        invalidate()
    }

    private fun isInEdge(rect: Rect, x: Int, y: Int): Int {
        val sizeH = max(this@ScreenshotSelectorView.width / 20, 15)
        val sizeV = max(this@ScreenshotSelectorView.height / 20, 15)

        val leftEdge =
            Rect(rect.left - sizeH, rect.top - sizeV, rect.left + sizeH, rect.bottom + sizeV)
        val topEdge =
            Rect(rect.left - sizeH, rect.top - sizeV, rect.right + sizeH, rect.top + sizeV)
        val rightEdge =
            Rect(rect.right - sizeH, rect.top - sizeV, rect.right + sizeH, rect.bottom + sizeV)
        val bottomEdge =
            Rect(rect.left - sizeH, rect.bottom - sizeV, rect.right + sizeH, rect.bottom + sizeV)

        var edgeMode = OFF
        if (leftEdge.contains(x, y)) {
            edgeMode += LEFT
        }
        if (topEdge.contains(x, y)) {
            edgeMode += TOP
        }
        if (rightEdge.contains(x, y)) {
            edgeMode += RIGHT
        }
        if (bottomEdge.contains(x, y)) {
            edgeMode += BOTTOM
        }
        return edgeMode
    }

    private fun drawEdges(canvas: Canvas, rect: Rect) {
        val edgeSize = max(
            (min(this@ScreenshotSelectorView.width, this@ScreenshotSelectorView.height) / 20) - 5,
            10
        )

        val leftEdgeHalf =
            Rect(rect.left - edgeSize, rect.top - edgeSize, rect.left, rect.bottom + edgeSize)
        val topEdgeHalf =
            Rect(rect.left - edgeSize, rect.top - edgeSize, rect.right + edgeSize, rect.top)
        val rightEdgeHalf =
            Rect(rect.right, rect.top - edgeSize, rect.right + edgeSize, rect.bottom + edgeSize)
        val bottomEdgeHalf =
            Rect(rect.left - edgeSize, rect.bottom, rect.right + edgeSize, rect.bottom + edgeSize)

        canvas.drawRect(leftEdgeHalf, paintEdgeOutside)
        canvas.drawRect(topEdgeHalf, paintEdgeOutside)
        canvas.drawRect(rightEdgeHalf, paintEdgeOutside)
        canvas.drawRect(bottomEdgeHalf, paintEdgeOutside)
    }

    private fun drawFullScreenButton(canvas: Canvas, rect: Rect) {
        fullScreenIcon?.run {
            // Show image on dark rounded rect
            getDrawable(this)?.apply {
                val m2 = min(intrinsicWidth, intrinsicHeight)
                val m3 = max(min(min(rect.width(), rect.height()) / 5, m2), m2)

                // Find a suitable position for the full screen icon
                var centerX = rect.centerX()
                var centerY = rect.centerY()
                var showFullScreenIcon = true
                when {
                    rect.bottom + 10 * m2 < this@ScreenshotSelectorView.height -> {
                        centerY = rect.bottom + 4 * m2
                    }
                    rect.top - 6 * m2 > 0 -> {
                        centerY = rect.top - 4 * m2
                    }
                    rect.right + 10 * m2 < this@ScreenshotSelectorView.width -> {
                        centerX = rect.right + 4 * m2
                    }
                    rect.left - 6 * m2 > 0 -> {
                        centerX = rect.left - 4 * m2
                    }
                    else -> {
                        showFullScreenIcon = false
                    }
                }

                if (showFullScreenIcon) {
                    setBounds(
                        centerX - m2,
                        centerY - m2,
                        centerX + m2,
                        centerY + m2
                    )
                    val fullScreenRoundRect = RectF(
                        (centerX - m3).toFloat(),
                        (centerY - m3).toFloat(),
                        (centerX + m3).toFloat(),
                        (centerY + m3).toFloat()
                    )

                    canvas.drawRoundRect(
                        fullScreenRoundRect,
                        20.0f,
                        20.0f,
                        paintBackground
                    )

                    fullscreenButtonRect = Rect().apply {
                        fullScreenRoundRect.roundOut(this)
                    }
                    draw(canvas)
                } else {
                    fullscreenButtonRect = null
                }
            }
        }
    }

    private fun drawButtons(canvas: Canvas, rect: Rect) {
        shutter?.run {
            // Show shutter image on dark rounded rect
            getDrawable(this)?.apply {
                if (rect.width() > intrinsicWidth || rect.height() > intrinsicHeight) {
                    val m0 = min(intrinsicWidth / 2, intrinsicHeight / 2)
                    setBounds(
                        rect.centerX() - m0,
                        rect.centerY() - m0,
                        rect.centerX() + m0,
                        rect.centerY() + m0
                    )
                    val m1 = min(
                        min(rect.width(), rect.height()) / 5,
                        min(intrinsicWidth, intrinsicHeight)
                    )
                    val roundRect = RectF(
                        (rect.centerX() - m1).toFloat(),
                        (rect.centerY() - m1).toFloat(),
                        (rect.centerX() + m1).toFloat(),
                        (rect.centerY() + m1).toFloat()
                    )
                    canvas.drawRoundRect(
                        roundRect,
                        20.0f,
                        20.0f,
                        paintBackground
                    )
                    shutterRect = Rect().apply {
                        roundRect.roundOut(this)
                    }

                    drawFullScreenButton(canvas, rect)

                } else {
                    setBounds(rect.left, rect.top, rect.right, rect.bottom)
                }
                draw(canvas)
            }
        }

        // Show text if no shutter image was set
        if (shutter == null) {
            val textBounds = Rect()
            val mText = text ?: "Tap to save"
            paintText.getTextBounds(mText, 0, mText.length, textBounds)
            paintText.textSize = this@ScreenshotSelectorView.width / 20.0f
            val x = rect.centerX().toFloat() - textBounds.centerX()
            val y = rect.centerY().toFloat() - textBounds.centerY()
            canvas.drawText(
                mText,
                x,
                y,
                paintText
            )
            shutterRect = Rect(
                x.toInt(),
                y.toInt(),
                (x + textBounds.width()).toInt(),
                (y + textBounds.height()).toInt()
            )
        }
    }


    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val bm = bitmap
        if (bm != null && !bm.isRecycled) {
            canvas.drawBitmap(bm, 0f, 0f, null)
        }

        canvas.drawPaint(paintBackground)
        selectionRect?.let { rect ->
            if (bm != null && !bm.isRecycled) {
                canvas.drawBitmap(bm, rect, rect, null)
            } else {
                canvas.drawRect(rect, paintSelection)
            }
            return
        }
        resultRect?.let { rect ->
            if (bm != null && !bm.isRecycled) {
                canvas.drawBitmap(bm, rect, rect, null)
            } else {
                canvas.drawRect(rect, paintSelection)
            }

            drawEdges(canvas, rect)

            if (showShutter) {
                drawButtons(canvas, rect)

            } else {
                shutterRect = null
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                defaultState = false
                val x = event.x.toInt()
                val y = event.y.toInt()
                if (resultRect != null) {
                    resultRect?.run {
                        if (width() != 0 && height() != 0) {
                            if (shutterRect?.contains(x, y) == true) {
                                // Click on shutter
                                hideShutter()
                                performClick()
                                onShutter?.invoke(this)
                                return false
                            }
                            if (fullscreenButtonRect?.contains(x, y) == true) {
                                // Click on fullscreen button
                                resultRect = Rect(
                                    0,
                                    0,
                                    this@ScreenshotSelectorView.width,
                                    this@ScreenshotSelectorView.height
                                )
                                this@ScreenshotSelectorView.invalidate()
                                return false
                            }

                            edgeMode = isInEdge(this, x, y)

                            if (edgeMode == OFF) {
                                if (contains(x, y)) {
                                    // Click inside result rect and not on edge, capture screenshot
                                    hideShutter()
                                    performClick()
                                    onShutter?.invoke(this)
                                    return false
                                } else {
                                    // Click outside result rect and not on edge, start new selection
                                    startSelection(x, y)
                                }
                            }
                            return true
                        }
                    }
                } else {
                    startSelection(x, y)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                updateSelection(event.x.toInt(), event.y.toInt())
                return true
            }
            MotionEvent.ACTION_UP -> {
                selectionRect?.run {
                    if (width() != 0 && height() != 0) {
                        showShutter()
                    }
                }
                if (edgeMode == OFF) {
                    if (event.x.toInt() == startPoint?.x && event.y.toInt() == startPoint?.y) {
                        ignoreSelection()
                    } else {
                        stopSelection()
                    }
                } else {
                    stopEdgeMode()
                }
                return true
            }
        }
        return false
    }


}
