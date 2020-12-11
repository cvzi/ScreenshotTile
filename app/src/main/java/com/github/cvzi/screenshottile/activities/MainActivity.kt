package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.fragments.SettingFragment
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.utils.isNewAppInstallation
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Launcher activity. Explanations and selector for legacy/native method
 */
class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity.kt"

        /**
         * Start this activity from a service
         */
        fun startNewTask(ctx: Context, args: Bundle? = null) {
            ctx.startActivity(
                Intent(ctx, MainActivity::class.java).apply {
                    putExtra(TransparentContainerActivity.EXTRA_ARGS, args)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
        }
    }

    private var hintAccessibilityServiceUnavailable: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && App.getInstance().prefManager.screenshotCount == 0 && isNewAppInstallation(this)) {
            // On Android Pie and higher, enable native method on first start
            App.getInstance().prefManager.screenshotCount++
            App.getInstance().prefManager.useNative = true
        }

        val textDescTranslate = findViewById<TextView>(R.id.textDescTranslate)
        textDescTranslate.movementMethod = LinkMovementMethod()
        textDescTranslate.text = Html.fromHtml(
            getString(R.string.translate_this_app_text),
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        )

        val switchLegacy = findViewById<SwitchMaterial>(R.id.switchLegacy)
        val switchNative = findViewById<SwitchMaterial>(R.id.switchNative)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            findViewById<LinearLayout>(R.id.linearLayoutNative)?.let {
                val hint = TextView(this)
                it.addView(hint, 1)
                hint.text = getString(
                    R.string.emoji_forbidden,
                    getString(R.string.use_native_screenshot_unsupported)
                )
            }
            switchNative?.isEnabled = false
            switchNative?.isChecked = false
            switchLegacy?.isEnabled = false
            switchLegacy?.isChecked = true

            findViewById<View>(R.id.floatingButtonCardView).let {
                (it.parent as ViewGroup).removeView(it)
            }

        }

        updateSwitches()

        findViewById<Button>(R.id.buttonSettings)?.setOnClickListener {
            ContainerActivity.start(this, SettingFragment::class.java)
        }
        findViewById<Button>(R.id.buttonTutorial)?.setOnClickListener {
            TutorialActivity.start(this)
        }

        findViewById<Button>(R.id.buttonAccessibilitySettings)?.setOnClickListener {
            // Open Accessibility settings
            ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
        }

        findViewById<TextView>(R.id.textDescGeneral)?.run {
            makeActivityClickable(this)
        }

        switchLegacy.isChecked = !App.getInstance().prefManager.useNative
        switchNative.isChecked = App.getInstance().prefManager.useNative

        switchLegacy?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == App.getInstance().prefManager.useNative) {
                App.getInstance().prefManager.useNative = !isChecked
                updateFloatButton()
                switchNative?.isChecked = App.getInstance().prefManager.useNative
            }
            if (!App.getInstance().prefManager.useNative) {
                hintAccessibilityServiceUnavailable?.let {
                    (it.parent as? ViewGroup)?.removeView(it)
                }
            }

        }
        switchNative?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != App.getInstance().prefManager.useNative) {
                App.getInstance().prefManager.useNative = isChecked
                updateFloatButton()
                switchLegacy?.isChecked = !App.getInstance().prefManager.useNative
                if (App.getInstance().prefManager.useNative) {
                    if (ScreenshotAccessibilityService.instance == null) {
                        // Open Accessibility settings
                        ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
                    } else {
                        hintAccessibilityServiceUnavailable?.let {
                            (it.parent as? ViewGroup)?.removeView(it)
                        }
                    }
                }
            }
        }
    }

    private fun updateFloatButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ScreenshotAccessibilityService.instance?.updateFloatingButton()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSwitches()
    }

    override fun onPause() {
        super.onPause()
        hintAccessibilityServiceUnavailable?.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
        hintAccessibilityServiceUnavailable = null
    }


    private fun updateSwitches() {
        val switchLegacy = findViewById<SwitchMaterial>(R.id.switchLegacy)
        val switchNative = findViewById<SwitchMaterial>(R.id.switchNative)

        switchLegacy?.isChecked = !App.getInstance().prefManager.useNative
        switchNative?.isChecked = App.getInstance().prefManager.useNative

        if (App.getInstance().prefManager.useNative) {
            if (ScreenshotAccessibilityService.instance == null && hintAccessibilityServiceUnavailable == null) {
                findViewById<LinearLayout>(R.id.linearLayoutNative)?.let {
                    hintAccessibilityServiceUnavailable = TextView(this)
                    it.addView(hintAccessibilityServiceUnavailable, 1)
                    hintAccessibilityServiceUnavailable?.text = getString(
                        R.string.emoji_warning, getString(
                            R.string.use_native_screenshot_unavailable
                        )
                    )
                    hintAccessibilityServiceUnavailable?.setOnClickListener { _ ->
                        ScreenshotAccessibilityService.openAccessibilitySettings(
                            this,
                            TAG
                        )
                    }
                }
            } else if (ScreenshotAccessibilityService.instance != null && hintAccessibilityServiceUnavailable != null) {
                findViewById<LinearLayout>(R.id.linearLayoutNative)?.removeView(
                    hintAccessibilityServiceUnavailable
                )
                hintAccessibilityServiceUnavailable = null
            }
        }
    }

    private fun makeActivityClickable(textView: TextView) {
        val text = textView.text.toString()

        val builder = SpannableStringBuilder("")
        for (content in text.split("]")) {
            val startIndex = content.indexOf("[")
            if (startIndex == -1) {
                builder.append(content)
                continue
            }
            val value = content.subSequence(startIndex, content.length).trim()
            val labelEnd = value.indexOf(',')
            val activityName = value.subSequence(labelEnd + 1, value.length).trim()
            val label = value.subSequence(1, labelEnd).trim()
            var newContent = content.subSequence(0, startIndex).toString()
            newContent += label
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(textView: View) {
                    val intent = Intent()
                    intent.setClassName(
                        this@MainActivity,
                        "com.github.cvzi.screenshottile.activities$activityName"
                    )
                    startActivity(intent)
                }
            }

            val spannableString = SpannableString(newContent)
            spannableString.setSpan(
                clickableSpan,
                startIndex,
                startIndex + label.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(spannableString)
        }
        textView.apply {
            setText(builder)
            movementMethod = LinkMovementMethod()
            highlightColor = Color.BLUE
        }
    }

}