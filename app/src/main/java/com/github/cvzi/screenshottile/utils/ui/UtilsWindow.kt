package com.github.cvzi.screenshottile.utils.ui

import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.WindowCompat

fun Window.applyFullscreenScreenshotStyle() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        insetsController?.hide(WindowInsets.Type.statusBars())
    } else {
        @Suppress("DEPRECATION")
        setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            @Suppress("DEPRECATION")
            statusBarColor = Color.TRANSPARENT
        }
        WindowCompat.setDecorFitsSystemWindows(this, true)
    } else {
        @Suppress("DEPRECATION")
        addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
}
