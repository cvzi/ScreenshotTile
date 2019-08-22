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

package com.github.ipcjs.screenshottile.partial

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.github.ipcjs.screenshottile.BuildConfig
import kotlin.math.max
import kotlin.math.min


/**
 * Draws a selection rectangle while taking screenshot
 */
class ScreenshotSelectorView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    var onShutter: ((Rect) -> Unit)? = null
    var shutter: Int? = null
    var text: String? = null

    private var showShutter = false
    private var mStartPoint: Point? = null
    private var selectionRect: Rect? = null
    private var resultRect: Rect? = null

    private val mPaintSelection = Paint(Color.TRANSPARENT).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val mPaintBackground = Paint(Color.BLACK).apply {
        alpha = 160
        style = Paint.Style.FILL
        if (BuildConfig.DEBUG) {
            color = Color.MAGENTA
            alpha = 200
        }
    }

    private fun startSelection(x: Int, y: Int) {
        mStartPoint = Point(x, y)
        selectionRect = Rect(x, y, x, y)
        showShutter = false
    }

    private fun updateSelection(x: Int, y: Int) {
        selectionRect?.run {
            mStartPoint?.let {
                left = min(it.x, x)
                right = max(it.x, x)
                top = min(it.y, y)
                bottom = max(it.y, y)
                invalidate()
            }
        }
    }

    private fun stopSelection() {
        mStartPoint = null
        resultRect = selectionRect
        selectionRect = null
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

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        canvas.drawPaint(mPaintBackground)
        selectionRect?.let { rect ->
            canvas.drawRect(rect, mPaintSelection)
            return
        }
        resultRect?.let { rect ->
            canvas.drawRect(rect, mPaintSelection)

            if (showShutter) {
                shutter?.run {
                    resources.getDrawable(this, null).apply {
                        if (rect.width() > intrinsicWidth || rect.height() > intrinsicHeight) {
                            val m = min(intrinsicWidth / 2, intrinsicHeight / 2)
                            setBounds(rect.centerX() - m, rect.centerY() - m, rect.centerX() + m, rect.centerY() + m)
                        } else {
                            setBounds(rect.left, rect.top, rect.right, rect.bottom)
                        }
                        draw(canvas)
                    }
                }

                val paint = Paint(Color.DKGRAY).apply {
                    style = Paint.Style.FILL
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
                    textSize = this@ScreenshotSelectorView.width / 20.0f
                }
                canvas.drawText(text ?: "Tap to save", rect.centerX().toFloat(), rect.centerY().toFloat(), paint)
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
                val x = event.x.toInt()
                val y = event.y.toInt()
                resultRect?.run {
                    if (width() != 0 && height() != 0 && contains(x, y)) {
                        hideShutter()
                        performClick()
                        onShutter?.invoke(this)
                        return true
                    }
                }
                startSelection(x, y)
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

                stopSelection()
                return true
            }
        }
        return false
    }


}
