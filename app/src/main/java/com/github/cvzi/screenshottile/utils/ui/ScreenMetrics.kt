package com.github.cvzi.screenshottile.utils.ui

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import kotlin.math.ceil

/**
 * Try to get the height of the status bar or return a fallback approximation
 */
@Suppress("unused")
fun statusBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) {
        context.resources.getDimensionPixelSize(resourceId)
    } else {
        ceil(24 * context.resources.displayMetrics.density).toInt()
    }
}

/**
 * navigationBarSize, appUsableScreenSize, realScreenSize
 * From: https://stackoverflow.com/a/29609679/
 *
 */
@Suppress("unused")
fun navigationBarSize(context: Context): Point {
    val appUsableSize: Point = appUsableScreenSize(context)
    val realScreenSize: Point = realScreenSize(context)
    return when {
        // navigation bar on the side
        appUsableSize.x < realScreenSize.x -> Point(
            realScreenSize.x - appUsableSize.x,
            appUsableSize.y
        )
        // navigation bar at the bottom
        appUsableSize.y < realScreenSize.y -> Point(
            appUsableSize.x,
            realScreenSize.y - appUsableSize.y
        )
        // navigation bar is not present
        else -> Point()
    }
}

/**
 * Screen size that can be used by windows
 */
fun appUsableScreenSize(context: Context): Point {
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.currentWindowMetrics.bounds
        Point(
            bounds.width(),
            bounds.height()
        )
    } else {
        Point().apply {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getSize(this)
        }
    }
}

/**
 * Full screen size including cutouts, adapted to screen orientation
 */
fun realScreenSize(context: Context): Point {
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    return Point().apply {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // display.mode.physical is independent of screen orientation
                context.display.mode?.let {
                    when (context.display.rotation) {
                        Surface.ROTATION_90, Surface.ROTATION_270 -> {
                            y = it.physicalWidth
                            x = it.physicalHeight
                        }

                        else -> {
                            x = it.physicalWidth
                            y = it.physicalHeight
                        }
                    }
                } ?: run {
                    // windowManager.currentWindowMetrics.bounds is already adapted to screen orientation
                    windowManager.currentWindowMetrics.bounds.let {
                        x = it.width()
                        y = it.height()
                    }
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                @Suppress("DEPRECATION")
                (context.display.getRealSize(this))
            }

            else -> {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealSize(this)
            }
        }
    }
}
