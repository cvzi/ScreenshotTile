package com.github.cvzi.screenshottile.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED
import android.app.StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.assist.MyVoiceInteractionService
import com.github.cvzi.screenshottile.databinding.ActivityMainBinding
import com.github.cvzi.screenshottile.services.FloatingTileService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.formatLocalizedString
import com.github.cvzi.screenshottile.utils.getLocalizedString
import com.github.cvzi.screenshottile.utils.hasFdroid
import com.github.cvzi.screenshottile.utils.isNewAppInstallation
import com.github.cvzi.screenshottile.utils.makeActivityClickableFromText
import com.github.cvzi.screenshottile.utils.toastMessage
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.function.Consumer


/**
 * Launcher activity. Explanations and selector for legacy/native method
 */
class MainActivity : BaseAppCompatActivity() {
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

    private lateinit var binding: ActivityMainBinding
    private var hintAccessibilityServiceUnavailable: TextView? = null
    private var hintAssistBugAndroid1011: TextView? = null
    private var askedForStoragePermission = false
    private var restrictedSettingsAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.setVariable(BR.strings, App.texts)

        if (!accessibilityConsent) {
            accessibilityConsent = hasFdroid(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            && accessibilityConsent
            && App.getInstance().prefManager.screenshotCount == 0
            && isNewAppInstallation(this)
        ) {
            // On Android Pie - Tiramisu, enable native method on first start
            // Don't do it on Tiramisu+ because of the "restricted settings" dialog
            App.getInstance().prefManager.screenshotCount++
            App.getInstance().prefManager.useNative = true
        }

        val textDescTranslate = binding.textDescTranslate
        textDescTranslate.movementMethod = LinkMovementMethod()
        textDescTranslate.text = Html.fromHtml(
            getLocalizedString(R.string.translate_this_app_text),
            Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        )

        val switchLegacy = binding.switchLegacy
        val switchNative = binding.switchNative
        val switchAssist = binding.switchAssist
        val switchFloatingButton = binding.switchFloatingButton

        toggleSwitchOnLabel(R.id.switchLegacy, R.id.textTitleLegacy)
        toggleSwitchOnLabel(R.id.switchNative, R.id.textTitleNative)
        toggleSwitchOnLabel(R.id.switchAssist, R.id.textTitleAssist)
        toggleSwitchOnLabel(R.id.switchFloatingButton, R.id.textTitleFloatingButton)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            binding.linearLayoutNative.let {
                val hint = TextView(this)
                it.addView(hint, 1)
                hint.text = formatLocalizedString(
                    R.string.emoji_forbidden,
                    getLocalizedString(R.string.use_native_screenshot_unsupported)
                )
            }
            switchNative.isEnabled = false
            switchNative.isChecked = false
            switchLegacy.isEnabled = false
            switchLegacy.isChecked = true
            switchFloatingButton.isEnabled = false

            binding.floatingButtonCardView.let {
                (it.parent as ViewGroup).removeView(it)
            }

        }
        binding.textDescNative.text =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                getLocalizedString(R.string.main_native_method_text).replace(
                    "{main_native_method_text_android_version}",
                    getLocalizedString(R.string.main_native_method_text_android_pre_11)
                )

            } else {
                getLocalizedString(R.string.main_native_method_text).replace(
                    "{main_native_method_text_android_version}",
                    getLocalizedString(R.string.main_native_method_text_android_since_11)
                )
            }


        updateSwitches()

        binding.buttonSettings.setOnClickListener {
            SettingsActivity.start(this)
        }
        binding.buttonSettings2.setOnClickListener {
            SettingsActivity.start(this)
        }
        binding.buttonTutorial.setOnClickListener {
            TutorialActivity.start(this)
        }

        binding.buttonAccessibilitySettings.setOnClickListener {
            // Open Accessibility settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
            }
        }

        binding.buttonFloatingButtonSettings.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setOnClickListener {
                    FloatingButtonSettingsActivity.start(this@MainActivity)
                }
            } else {
                visibility = View.GONE
            }
        }

        binding.buttonPostActions.setOnClickListener {
            startActivity(Intent(this, PostSettingsActivity::class.java))
        }

        binding.buttonHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.textDescGeneral.run {
            makeActivityClickable(this, getLocalizedString(R.string.main_general_text))
        }

        binding.buttonChangeLanguage.setOnClickListener {
            LanguageActivity.start(this)
        }

        binding.buttonUpdateCheck.setOnClickListener {
            val args = arrayOf(
                packageName ?: "com.github.cvzi.screenshottile",
                BuildConfig.VERSION_CODE,
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_TYPE
            ).map { Uri.encode(it.toString()) }.toTypedArray()

            @SuppressLint("StringFormatMatches")
            val uri = Uri.parse(
                formatLocalizedString(
                    R.string.pref_static_field_link_about_updates,
                    *args
                )
            )
            Intent(ACTION_VIEW, uri).apply {
                if (resolveActivity(packageManager) != null) {
                    startActivity(this)
                }
            }
        }

        switchLegacy.isChecked = !App.getInstance().prefManager.useNative
        switchNative.isChecked = App.getInstance().prefManager.useNative

        switchLegacy.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == App.getInstance().prefManager.useNative) {
                App.getInstance().prefManager.useNative = !isChecked
                updateFloatButton()
                switchNative.isChecked = App.getInstance().prefManager.useNative
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
        switchNative.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !accessibilityConsent && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchNative.isChecked = false
                askToEnableAccessibility()
                return@setOnCheckedChangeListener
            }

            if (isChecked != App.getInstance().prefManager.useNative) {
                App.getInstance().prefManager.useNative = isChecked
                updateFloatButton()
                switchLegacy.isChecked = !App.getInstance().prefManager.useNative
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

        switchAssist.setOnCheckedChangeListener { _, _ ->
            MyVoiceInteractionService.openVoiceInteractionSettings(this, TAG)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchFloatingButton.setOnCheckedChangeListener { _, isChecked ->
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
            binding.buttonFloatingButtonTile.setOnClickListener {
                requestAddFloatingButtonTile { resultCode ->
                    // Hide the button afterwards
                    if (resultCode == TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED || resultCode == TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                        binding.buttonFloatingButtonTile.visibility = View.GONE
                    }
                }
            }
            binding.buttonScreenshotTile1.setOnClickListener {
                requestAddScreenshotTile { resultCode ->
                    if (resultCode == TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED || resultCode == TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                        binding.buttonScreenshotTile1.visibility = View.GONE
                        binding.buttonScreenshotTile2.visibility = View.GONE
                    }
                }
            }
            binding.buttonScreenshotTile2.setOnClickListener {
                requestAddScreenshotTile { resultCode ->
                    if (resultCode == TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED || resultCode == TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                        binding.buttonScreenshotTile1.visibility = View.GONE
                        binding.buttonScreenshotTile2.visibility = View.GONE
                    }
                }
            }

            if (ScreenshotAccessibilityService.instance != null && !App.getInstance().prefManager.floatingButton) {
                // Service is running and floating button is disabled ->  scroll to floating button
                binding.scrollView.postDelayed({
                    binding.scrollView.smoothScrollTo(
                        0,
                        binding.nativeCardView.top
                    )
                }, 1000)
            }
        }

        // Show warning if app is installed on external storage
        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                ).flags
            } else {
                packageManager.getApplicationInfo(packageName, 0).flags
            }
            if (flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE != 0
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
        // Do not ask during instrument tests
        if (BuildConfig.TESTING_MODE.value) {
            return
        }

        requestAddScreenshotTile { resultCode ->
            if (resultCode == TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED || resultCode == TILE_ADD_REQUEST_RESULT_TILE_ADDED) {
                binding.buttonScreenshotTile1.visibility = View.GONE
                binding.buttonScreenshotTile2.visibility = View.GONE
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestAddScreenshotTile(resultCallback: Consumer<Int> = Consumer { }) {
        if (BuildConfig.TESTING_MODE.value) {
            return
        }
        if (ScreenshotTileService.instance == null) {
            App.askedToAddTileTime = System.currentTimeMillis()
            val statusBarManager = getSystemService(StatusBarManager::class.java)
            statusBarManager.requestAddTileService(
                ComponentName(this, ScreenshotTileService::class.java),
                getLocalizedString(R.string.tile_label),
                Icon.createWithResource(this, R.drawable.ic_stat_name),
                mainExecutor
            ) { resultCode ->
                App.askedToAddTileTime = System.currentTimeMillis()
                mainExecutor.execute {
                    resultCallback.accept(resultCode)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestAddFloatingButtonTile(resultCallback: Consumer<Int> = Consumer { }) {
        val statusBarManager = getSystemService(StatusBarManager::class.java)
        statusBarManager.requestAddTileService(
            ComponentName(this, FloatingTileService::class.java),
            getLocalizedString(R.string.tile_floating),
            Icon.createWithResource(this, R.drawable.ic_tile_float),
            mainExecutor,
            resultCallback
        )
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
            "${getLocalizedString(R.string.googleplay_consent_line_0)} " +
                    "${getLocalizedString(R.string.googleplay_consent_line_1)} " +
                    "${getLocalizedString(R.string.googleplay_consent_line_2)}\n" +
                    "${getLocalizedString(R.string.googleplay_consent_line_3)} " +
                    "${getLocalizedString(R.string.googleplay_consent_line_4)} " +
                    "${getLocalizedString(R.string.googleplay_consent_line_5)} " +
                    "${getLocalizedString(R.string.googleplay_consent_line_6)}\n" +
                    "\n" +
                    getLocalizedString(R.string.googleplay_consent_line_7)
        )
        builder.setPositiveButton(getLocalizedString(R.string.googleplay_consent_yes)) { _, _ ->
            accessibilityConsent = true
            if (binding.switchNative.isChecked && ScreenshotAccessibilityService.instance == null) {
                ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
            } else {
                binding.switchNative.isChecked = true
            }
        }
        builder.setNegativeButton(R.string.googleplay_consent_no) { _, _ ->
            val switchLegacy = binding.switchLegacy
            switchLegacy.isChecked = true
        }
        builder.show()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    /**
     * Does nothing until Android 13 Tiramisu
     * Show dialog about restricted settings:
     *   [ Displays screenshot of "App info" ]
     *  - Button to open accessibility settings
     *  - Cancel button
     *  - Button to open "App info" screen with "restricted settings"
     */
    private fun informAboutRestrictedSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        restrictedSettingsAlertDialog?.dismiss()

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.restricted_settings_title)
        builder.setMessage(R.string.restricted_settings_text)
        val imageView = ImageView(this)
        imageView.setImageResource(R.drawable.restricted_settings)
        builder.setView(imageView)
        builder.setNeutralButton(R.string.restricted_settings_open_settings) { _, _ ->
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
        builder.setPositiveButton(R.string.restricted_settings_open_accessibility) { _, _ ->
            ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        restrictedSettingsAlertDialog = builder.show()
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

        restrictedSettingsAlertDialog?.dismiss()
    }


    private fun updateSwitches() {
        val switchLegacy = binding.switchLegacy
        val switchNative = binding.switchNative
        val switchAssist = binding.switchAssist
        val switchFloatingButton = binding.switchFloatingButton

        switchLegacy.isChecked = !App.getInstance().prefManager.useNative
        switchNative.isChecked = App.getInstance().prefManager.useNative

        if (App.getInstance().prefManager.useNative && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (ScreenshotAccessibilityService.instance == null && hintAccessibilityServiceUnavailable == null) {
                binding.linearLayoutNative.let {
                    hintAccessibilityServiceUnavailable = TextView(this)
                    it.addView(hintAccessibilityServiceUnavailable, 1)
                    hintAccessibilityServiceUnavailable?.text = formatLocalizedString(
                        R.string.emoji_warning, getLocalizedString(
                            R.string.use_native_screenshot_unavailable
                        )
                    )
                    hintAccessibilityServiceUnavailable?.setOnClickListener { _ ->
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
                        } else {
                            informAboutRestrictedSettings()
                        }
                    }
                }
            } else if (ScreenshotAccessibilityService.instance != null && hintAccessibilityServiceUnavailable != null) {
                binding.linearLayoutNative.removeView(
                    hintAccessibilityServiceUnavailable
                )
                hintAccessibilityServiceUnavailable = null
            }
            // User might have returned from accessibility settings without activating the service
            // Show dialog about restricted settings
            if (ScreenshotAccessibilityService.instance == null) {
                informAboutRestrictedSettings()
            }
        }

        switchAssist.isChecked = MyVoiceInteractionService.instance != null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            switchFloatingButton.isChecked =
                ScreenshotAccessibilityService.instance != null && App.getInstance().prefManager.floatingButton

            binding.buttonFloatingButtonTile.visibility =
                if (FloatingTileService.instance != null) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
        }

        if (ScreenshotTileService.instance != null) {
            View.VISIBLE
        } else {
            View.GONE
        }.let { vis ->
            binding.buttonScreenshotTile1.visibility = vis
            binding.buttonScreenshotTile2.visibility = vis
        }

        // Warn about bug in Android 10 and 11 https://github.com/cvzi/ScreenshotTile/issues/556
        val showAssistHint =
            switchAssist.isChecked
                    && App.getInstance().prefManager.voiceInteractionAction == getString(R.string.setting_voice_interaction_action_value_provided)
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R
                    && ScreenshotAccessibilityService.instance == null

        if (showAssistHint && hintAssistBugAndroid1011 == null) {
            binding.linearLayoutAssist.let {
                hintAssistBugAndroid1011 = TextView(this)
                it.addView(hintAssistBugAndroid1011, 1)
                hintAssistBugAndroid1011?.text = formatLocalizedString(
                    R.string.emoji_warning,
                    "Please tap here to enable the Accessibility Service to avoid a bug in Android 10 and 11\n\nSee https://github.com/cvzi/ScreenshotTile/issues/556 for more information\n---"
                )
                hintAssistBugAndroid1011?.setOnClickListener { _ ->
                    ScreenshotAccessibilityService.openAccessibilitySettings(this, TAG)
                }
            }
        } else if (!showAssistHint) {
            hintAssistBugAndroid1011?.let {
                (it.parent as? ViewGroup)?.removeView(it)
            }
            hintAssistBugAndroid1011 = null
        }
    }

    private fun makeActivityClickable(textView: TextView, str: String) {
        textView.apply {
            text = makeActivityClickableFromText(str, this@MainActivity).builder
            movementMethod = LinkMovementMethod()
            highlightColor = Color.BLUE
        }
    }

}
