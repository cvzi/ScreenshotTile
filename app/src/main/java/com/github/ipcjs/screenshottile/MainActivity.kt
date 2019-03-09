package com.github.ipcjs.screenshottile

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.github.ipcjs.screenshottile.dialog.ContainerActivity
import com.github.ipcjs.screenshottile.dialog.SettingFragment

/**
 * Shows a how-to video.
 */
class MainActivity : Activity() {

    private val images = arrayOf(
        R.drawable.screenshot_01,
        R.drawable.screenshot_02,
        R.drawable.screenshot_03,
        R.drawable.screenshot_04,
        R.drawable.screenshot_05,
        R.drawable.screenshot_06,
        R.drawable.screenshot_07,
        R.drawable.screenshot_08,
        R.drawable.screenshot_09,
        R.drawable.screenshot_10,
        R.drawable.screenshot_11,
        R.drawable.screenshot_12,
        R.drawable.screenshot_13,
        R.drawable.screenshot_14,
        R.drawable.screenshot_15
    )
    private var index = 0
    private var nextImage: Int
        get() {
            index = (index + 1) % images.size
            return images[index]
        }
        set(newIndex) {
            index = newIndex % images.size
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        actionBar?.title = getString(R.string.app_name) + getString(R.string.tutorial)
        val textViewStep = findViewById<TextView>(R.id.textViewStep)
        findViewById<ImageView>(R.id.imageView).apply {
            setOnClickListener {
                val imageView = it as ImageView
                imageView.setImageResource(nextImage)
                textViewStep.text = (index + 1).toString()
            }
        }
        findViewById<Button>(R.id.buttonSettings)?.setOnClickListener {
            ContainerActivity.start(this@MainActivity, SettingFragment::class.java)
        }
    }
}
