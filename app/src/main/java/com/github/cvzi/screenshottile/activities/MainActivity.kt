package com.github.cvzi.screenshottile.activities

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.assist.MyVoiceInteractionService
import com.github.cvzi.screenshottile.services.FloatingTileService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.*
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
    private var askedForStoragePermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!accessibilityConsent) {
            accessibilityConsent = hasFdroid(this)
        }

        if (accessibilityConsent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && App.getInstance().prefManager.screenshotCount == 0
                && isNewAppInstallation(this)
            ) {
                // On Android Pie and higher, enable native method on first start
                App.getInstance().prefManager.screenshotCount++
                App.getInstance().prefManager.useNative = true
            }
        }

        val textDescTranslate = findViewById<TextView>(R.id.textDescTranslate)
        textDescTranslate.movementMethod = LinkMovementMethod()
        textDescTranslate.text = Html.fromHtml(
            getString(R.string.translate_this_app_text),
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        )

        val switchLegacy = findViewById<SwitchMaterial>(R.id.switchLegacy)
        val switchNative = findViewById<SwitchMaterial>(R.id.switchNative)
        val switchAssist = findViewById<SwitchMaterial>(R.id.switchAssist)
        val switchFloatingButton = findViewById<SwitchMaterial>(R.id.switchFloatingButton)

        toggleSwitchOnLabel(R.id.switchLegacy, R.id.textTitleLegacy)
        toggleSwitchOnLabel(R.id.switchNative, R.id.textTitleNative)
        toggleSwitchOnLabel(R.id.switchAssist, R.id.textTitleAssist)
        toggleSwitchOnLabel(R.id.switchFloatingButton, R.id.textTitleFloatingButton)

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
            switchFloatingButton?.isEnabled = false

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
            }
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
                if (!askedForStoragePermission && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && packageManager.checkPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        packageName
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    askedForStoragePermission = true
                    App.requestStoragePermission(this, false)
                }
            }
        }
        switchNative?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !accessibilityConsent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchNative.isChecked = false
                askToEnableAccessibility()
                return@setOnCheckedChangeListener
            }

            if (isChecked != App.getInstance().prefManager.useNative) {
                App.getInstance().prefManager.useNative = isChecked
                updateFloatButton()
                switchLegacy?.isChecked = !App.getInstance().prefManager.useNative
                if (App.getInstance().prefManager.useNative) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ScreenshotAccessibilityService.instance == null) {
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

        switchAssist?.setOnCheckedChangeListener { _, _ ->
            MyVoiceInteractionService.openVoiceInteractionSettings(this, TAG)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchFloatingButton?.setOnCheckedChangeListener { _, isChecked ->
                App.getInstance().prefManager.floatingButton = isChecked
                if (isChecked && ScreenshotAccessibilityService.instance == null) {
                    if (accessibilityConsent) {
                        // Open Accessibility settings
                        ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
                    } else {
                        askToEnableAccessibility()
                    }
                } else if (ScreenshotAccessibilityService.instance != null) {
                    ScreenshotAccessibilityService.instance!!.updateFloatingButton()
                }
            }
            if (ScreenshotAccessibilityService.instance != null && !App.getInstance().prefManager.floatingButton) {
                // Service is running and floating button is disabled ->  scroll to floating button
                findViewById<ScrollView>(R.id.scrollView).postDelayed({
                    findViewById<ScrollView>(R.id.scrollView).smoothScrollTo(
                        0,
                        findViewById<View>(R.id.nativeCardView).top
                    )
                }, 1000)
            }
        }

        // Show warning if app is installed on external storage
        try {
            if (packageManager.getApplicationInfo(
                    packageName,
                    0
                ).flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0
            ) {
                toastMessage(
                    "App is installed on external storage, this can cause problems  after a reboot with the floating button and the assistant function.",
                    ToastType.ACTIVITY
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, e.toString())
        }

        // On Android 13 Tiramisu we ask the user to add the tile to the quick settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (App.getInstance().prefManager.screenshotCount == 0 || isNewAppInstallation(this))
        ) {
            askToAddTiles()
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun askToAddTiles() {
        if (ScreenshotTileService.instance == null) {
            val statusBarManager = getSystemService(Context.STATUS_BAR_SERVICE) as StatusBarManager
            // Firstly, ask for normal screenshot tile
            statusBarManager.requestAddTileService(
                ComponentName(this, ScreenshotTileService::class.java),
                getString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_stat_name),
                {
                    it.run()
                },
                {
                    // Secondly, ask for floating button tile
                    if (FloatingTileService.instance == null) {
                        statusBarManager.requestAddTileService(
                            ComponentName(this, FloatingTileService::class.java),
                            getString(R.string.tile_floating_label) + " " + getString(R.string.tile_floating_subtitle),
                            Icon.createWithResource(this, R.drawable.ic_tile_float),
                            {},
                            {})
                    }
                })
        }
    }

    private fun toggleSwitchOnLabel(switchId: Int, labelId: Int) {
        findViewById<View?>(labelId)?.let { label ->
            label.isClickable = true
            label.setOnClickListener {
                findViewById<SwitchMaterial?>(switchId)?.toggle()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun askToEnableAccessibility() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.googleplay_consent_title)
        builder.setMessage(
            "${getString(R.string.googleplay_consent_line_0)} " +
                    "${getString(R.string.googleplay_consent_line_1)} " +
                    "${getString(R.string.googleplay_consent_line_2)}\n" +
                    "${getString(R.string.googleplay_consent_line_3)} " +
                    "${getString(R.string.googleplay_consent_line_4)} " +
                    "${getString(R.string.googleplay_consent_line_5)} " +
                    "${getString(R.string.googleplay_consent_line_6)}\n" +
                    "\n" +
                    getString(R.string.googleplay_consent_line_7)
        )
        builder.setPositiveButton(getString(R.string.googleplay_consent_yes)) { _, _ ->
            accessibilityConsent = true
            val switchNative = findViewById<SwitchMaterial>(R.id.switchNative)
            if (switchNative.isChecked && ScreenshotAccessibilityService.instance == null) {
                ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
            } else {
                switchNative.isChecked = true
            }
        }
        builder.setNegativeButton(R.string.googleplay_consent_no) { _, _ ->
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
        askedForStoragePermission = true // Don't ask again on resume
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
        val switchAssist = findViewById<SwitchMaterial>(R.id.switchAssist)
        val switchFloatingButton = findViewById<SwitchMaterial>(R.id.switchFloatingButton)

        switchLegacy?.isChecked = !App.getInstance().prefManager.useNative
        switchNative?.isChecked = App.getInstance().prefManager.useNative

        if (App.getInstance().prefManager.useNative && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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

        switchAssist?.isChecked = MyVoiceInteractionService.instance != null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchFloatingButton?.isChecked =
                ScreenshotAccessibilityService.instance != null && App.getInstance().prefManager.floatingButton
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