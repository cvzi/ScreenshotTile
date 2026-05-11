package com.github.cvzi.screenshottile.utils.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.math.max

fun toggleSwitchOnLabel(label: View, switch: SwitchMaterial) {
    label.isClickable = true
    label.setOnClickListener {
        switch.toggle()
    }
}

fun View.minPaddingFromInsets() {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        v.setPadding(
            max(initialLeft, bars.left),
            max(initialTop, bars.top),
            max(initialRight, bars.right),
            max(initialBottom, bars.bottom)
        )

        insets
    }

    ViewCompat.requestApplyInsets(this)
}
