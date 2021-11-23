package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.utils.isNewAppInstallation
import com.github.cvzi.screenshottile.utils.makeActivityClickableFromText
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
        var accessibilityConsent = false
    }

    private var hintAccessibilityServiceUnavailable: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && App.getInstance().prefManager.screenshotCount == 0 && isNewAppInstallation(
                this
            )
        ) {
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
        findViewById<TextView>(R.id.textDescNative)?.run {
            text =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    getString(R.string.main_native_method_text).replace(
                        "{main_native_method_text_android_version}",
                        getString(R.string.main_native_method_text_android_pre_11)
                    )

                } else {
                    getString(R.string.main_native_method_text).replace(
                        "{main_native_method_text_android_version}",
                        getString(R.string.main_native_method_text_android_since_11)
                    )
                }
        }

        updateSwitches()

        findViewById<Button>(R.id.buttonSettings)?.setOnClickListener {
            SettingsActivity.start(this)
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
            if(isChecked && !accessibilityConsent) {
                switchNative.isChecked = false
                askToEnableAccessibility()
                return@setOnCheckedChangeListener
            }

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

    private fun askToEnableAccessibility() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Google Play Prominent Disclosure & Consent Requirement")
        builder.setMessage("Google Play requires prominent disclosure of the Accessibility Services API’s use for an app that is not an accessibility tool. " +
                "This app - “Screenshot Tile [No root]“ - is not an accessibility tool. " +
                "Since users may not reasonably expect that their personal and sensitive user data will be required to provide or improve the policy compliant features or functionality within this app, " +
                "a prominent disclosure of the usage of the Accessibility Services API by this app and a declaration of the data collection and data sharing within this app is required:\n" +
                "This app requires access to Accessibility Services API in order to take a screenshot when the app is not in use. " +
                "Access to Accessibility Services API will allow this app to record the screen. Screen recordings will be used to create screenshot images. " +
                "Data is neither collected nor shared by this app using the accessibility capabilities.\n" +
                "\n" +
                "Do you agree to authorize this app to gain access to the Accessibility Services API and to use the accessibility capabilities to record the screen?")
        builder.setPositiveButton("I agree") { _, _ ->
            accessibilityConsent = true
            val switchNative = findViewById<SwitchMaterial>(R.id.switchNative)
            switchNative.isChecked = true
        }
        builder.setNegativeButton("I refuse") { _, _ ->
            val switchLegacy = findViewById<SwitchMaterial>(R.id.switchLegacy)
            switchLegacy.isChecked = true
        }
        builder.show()
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
        textView.apply {
            text = makeActivityClickableFromText(text.toString(), this@MainActivity).builder
            movementMethod = LinkMovementMethod()
            highlightColor = Color.BLUE
        }
    }

}