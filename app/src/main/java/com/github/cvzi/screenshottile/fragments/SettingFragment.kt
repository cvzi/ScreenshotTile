package com.github.cvzi.screenshottile.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.content.Intent.ACTION_VIEW
import android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.Intent.createChooser
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.activities.FloatingButtonSettingsActivity
import com.github.cvzi.screenshottile.activities.MainActivity
import com.github.cvzi.screenshottile.activities.PostSettingsActivity
import com.github.cvzi.screenshottile.assist.MyVoiceInteractionService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService.Companion.openAccessibilitySettings
import com.github.cvzi.screenshottile.utils.ShutterCollection
import com.github.cvzi.screenshottile.utils.createNotificationScreenshotTakenChannel
import com.github.cvzi.screenshottile.utils.nicePathFromUri
import com.github.cvzi.screenshottile.utils.notificationScreenshotTakenChannelEnabled
import com.github.cvzi.screenshottile.utils.notificationSettingsIntent
import com.github.cvzi.screenshottile.utils.safeDismiss
import com.github.cvzi.screenshottile.utils.toastMessage
import java.lang.ref.WeakReference


/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */
class SettingFragment : PreferenceFragmentCompat() {
    companion object {
        const val TAG = "SettingFragment.kt"
        private const val FLOATING_BUTTON_SHOW_CLOSE_DIALOG_SHOWN = "closeAlertDialogShown"
        private const val FLOATING_BUTTON_SHOW_CLOSE_DIALOG_VALUE = "closeAlertDialogValue"
        var instance: WeakReference<SettingFragment>? = null
    }

    init {
        instance = WeakReference<SettingFragment>(this)
    }

    private lateinit var startForPickFolder: ActivityResultLauncher<Intent>
    private var openedAccessibilitySetting = false
    private var askedForStoragePermission = false
    private var notificationPref: Preference? = null
    private var postActionsPref: Preference? = null
    private var floatingButtonSettingsPref: Preference? = null
    private var notificationActionsPref: MultiSelectListPreference? = null
    private var delayPref: ListPreference? = null
    private var fileFormatPref: ListPreference? = null
    private var useNativePref: SwitchPreference? = null
    private var useSystemDefaultsPref: SwitchPreference? = null
    private var floatingButtonPref: SwitchPreference? = null
    private var floatingButtonScalePref: EditTextPreference? = null
    private var floatingButtonHideAfterPref: SwitchPreference? = null
    private var floatingButtonHideShowClosePref: SwitchPreference? = null
    private var floatingButtonShutter: ListPreference? = null
    private var floatingButtonDelay: ListPreference? = null
    private var floatingButtonActionPref: ListPreference? = null
    private var voiceInteractionActionPref: ListPreference? = null
    private var hideAppPref: SwitchPreference? = null
    private var storageDirectoryPref: Preference? = null
    private var fileNamePatternPref: EditTextPreference? = null
    private var fileNamePlaceholders: Preference? = null
    private var broadcastSecretPref: EditTextPreference? = null
    private var tileActionPref: ListPreference? = null
    private var tileLongPressActionPref: ListPreference? = null
    private var darkThemePref: ListPreference? = null
    private var floatingButtonHideShowClosePreventRecursion = false
    private var pref: SharedPreferences? = null
    private val prefManager = App.getInstance().prefManager
    private var floatingButtonShowCloseAlertDialog: AlertDialog? = null
    private var floatingButtonShowCloseTextValue = ""

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            when (key) {
                getString(R.string.pref_key_delay) -> updateDelaySummary(prefManager.delay.toString())
                getString(R.string.pref_key_hide_app) -> onHideApp(prefManager.hideApp)
                getString(R.string.pref_key_file_format) -> updateFileFormatSummary(prefManager.fileFormat)
                getString(R.string.pref_key_file_name_pattern) -> updateFileNamePatternSummary()
                getString(R.string.pref_key_use_native) -> updateUseNative(switchEvent = true)
                getString(R.string.pref_key_floating_button) -> updateFloatingButton(switchEvent = true)
                getString(R.string.pref_key_floating_button_scale) -> updateFloatingButton(
                    switchEvent = true, forceRedraw = true
                )

                getString(R.string.pref_key_floating_button_show_close) -> updateFloatingButtonClose()
                getString(R.string.pref_key_floating_button_shutter) -> updateFloatingButtonShutterSummary(
                    true
                )

                getString(R.string.pref_key_floating_button_delay) -> updateFloatingButtonDelaySummary(
                    prefManager.floatingButtonDelay.toString()
                )

                getString(R.string.pref_key_use_system_defaults) -> updateUseNative(switchEvent = true)
                getString(R.string.pref_key_tile_action) -> updateTileActionSummary(prefManager.tileAction)
                getString(R.string.pref_key_tile_long_press_action) -> updateTileLongPressActionSummary(
                    prefManager.tileLongPressAction
                )

                getString(R.string.pref_key_floating_action) -> updateFloatingActionSummary(
                    prefManager.floatingButtonAction
                )

                getString(R.string.pref_key_voice_interaction_action) -> updateVoiceInteractionActionSummary(
                    prefManager.voiceInteractionAction, switchEvent = true
                )

                getString(R.string.pref_key_dark_theme) -> updateDarkTheme(switchEvent = true)
                getString(R.string.pref_key_notification_actions) -> updateNotificationActions()
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        pref = preferenceManager.sharedPreferences

        addPreferencesFromResource(R.xml.pref)

        startForPickFolder =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.let { intent ->
                        val uri = intent.data
                        val takeFlags: Int = intent.flags and
                                (FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
                        @SuppressLint("WrongConstant")
                        if (uri != null && activity != null && activity?.contentResolver != null) {
                            prefManager.screenshotDirectory = uri.toString()
                            activity?.contentResolver?.takePersistableUriPermission(uri, takeFlags)
                        }
                    }
                }
            }

        notificationPref =
            findPreference(getString(R.string.pref_static_field_key_notification_settings))
        postActionsPref =
            findPreference(getString(R.string.pref_static_field_key_post_actions))
        floatingButtonSettingsPref =
            findPreference(getString(R.string.pref_static_field_key_floating_button_settings))
        notificationActionsPref =
            findPreference(getString(R.string.pref_key_notification_actions)) as MultiSelectListPreference?
        delayPref = findPreference(getString(R.string.pref_key_delay)) as ListPreference?
        fileFormatPref = findPreference(getString(R.string.pref_key_file_format)) as ListPreference?
        useNativePref = findPreference(getString(R.string.pref_key_use_native)) as SwitchPreference?
        useSystemDefaultsPref =
            findPreference(getString(R.string.pref_key_use_system_defaults)) as SwitchPreference?
        floatingButtonPref =
            findPreference(getString(R.string.pref_key_floating_button)) as SwitchPreference?
        floatingButtonScalePref =
            findPreference(getString(R.string.pref_key_floating_button_scale)) as EditTextPreference?
        floatingButtonHideAfterPref =
            findPreference(getString(R.string.pref_key_floating_button_hide_after)) as SwitchPreference?
        floatingButtonHideShowClosePref =
            findPreference(getString(R.string.pref_key_floating_button_show_close)) as SwitchPreference?
        hideAppPref = findPreference(getString(R.string.pref_key_hide_app)) as SwitchPreference?
        storageDirectoryPref = findPreference(getString(R.string.pref_key_storage_directory))
        broadcastSecretPref =
            findPreference(getString(R.string.pref_key_broadcast_secret)) as EditTextPreference?
        floatingButtonShutter =
            findPreference(getString(R.string.pref_key_floating_button_shutter)) as ListPreference?
        floatingButtonDelay =
            findPreference(getString(R.string.pref_key_floating_button_delay)) as ListPreference?
        tileActionPref = findPreference(getString(R.string.pref_key_tile_action)) as ListPreference?
        tileLongPressActionPref =
            findPreference(getString(R.string.pref_key_tile_long_press_action)) as ListPreference?
        floatingButtonActionPref =
            findPreference(getString(R.string.pref_key_floating_action)) as ListPreference?
        voiceInteractionActionPref =
            findPreference(getString(R.string.pref_key_voice_interaction_action)) as ListPreference?
        darkThemePref = findPreference(getString(R.string.pref_key_dark_theme)) as ListPreference?
        fileNamePatternPref =
            findPreference(getString(R.string.pref_key_file_name_pattern)) as EditTextPreference?
        fileNamePlaceholders =
            findPreference(getString(R.string.pref_static_field_key_file_name_placeholders)) as Preference?

        pref?.registerOnSharedPreferenceChangeListener(prefListener)

        makeLink(
            R.string.pref_static_field_key_about_app_1,
            R.string.pref_static_field_link_about_app_1
        )
        makeLink(
            R.string.pref_static_field_key_about_app_3,
            R.string.pref_static_field_link_about_app_3
        )
        makeLink(
            R.string.pref_static_field_key_about_license_1,
            R.string.pref_static_field_link_about_license_1
        )
        makeLink(
            R.string.pref_static_field_key_about_open_source,
            R.string.pref_static_field_link_about_open_source
        )
        makeLink(
            R.string.pref_static_field_key_about_privacy,
            R.string.pref_static_field_link_about_privacy
        )
        makeLink(
            R.string.pref_static_field_key_about_donate,
            R.string.pref_static_field_link_about_donate
        )
        makeLink(
            R.string.pref_static_field_key_about_updates,
            R.string.pref_static_field_link_about_updates,
            arrayOf(
                context?.packageName ?: "com.github.cvzi.screenshottile",
                BuildConfig.VERSION_CODE,
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_TYPE
            ).map { Uri.encode(it.toString()) }.toTypedArray(),
        )

        makeNotificationSettingsLink()
        makePostActionsLink()
        makeFloatingButtonSettingsLink()
        makeAccessibilitySettingsLink()
        makeStorageDirectoryLink()
        makeAdvancedSettingsLink()

        if (savedInstanceState?.getBoolean(
                FLOATING_BUTTON_SHOW_CLOSE_DIALOG_SHOWN,
                false
            ) == true
        ) {
            updateFloatingButtonClose(
                savedInstanceState.getString(
                    FLOATING_BUTTON_SHOW_CLOSE_DIALOG_VALUE
                )
            )
        }
    }


    override fun onResume() {
        super.onResume()

        delayPref?.run { updateDelaySummary(value) }
        tileActionPref?.run { updateTileActionSummary(value) }
        tileLongPressActionPref?.run { updateTileLongPressActionSummary(value) }
        floatingButtonActionPref?.run { updateFloatingActionSummary(value) }
        voiceInteractionActionPref?.run { updateVoiceInteractionActionSummary(value) }
        fileFormatPref?.run { updateFileFormatSummary(value) }
        floatingButtonDelay?.run { updateFloatingButtonDelaySummary(value) }
        fileNamePatternPref?.run { updateFileNamePatternSummary() }

        floatingButtonHideShowClosePreventRecursion = false

        updateNotificationSummary()
        updatePostActionsSummary()
        updateNotificationActions()
        updateUseNative()
        updateFloatingButton()
        updateStorageDirectory()
        updateHideApp(true)
        updateFloatingButtonShutterSummary()
        updateDarkTheme()

        if (BuildConfig.DEBUG) {
            broadcastSecretPref?.summary = getString(R.string.unavailable_on_debug)
        }
    }

    private fun makeLink(name: Int, link: Int, linkFormatArgs: Array<Any>? = null) {
        val myActivity = activity
        myActivity?.let {
            val myPref = findPreference(getString(name)) as Preference?
            myPref?.isSelectable = true
            myPref?.onPreferenceClickListener = OnPreferenceClickListener {
                val uri = Uri.parse(
                    if (linkFormatArgs != null) {
                        getString(link, *linkFormatArgs)
                    } else {
                        getString(link)
                    }
                )
                Intent(ACTION_VIEW, uri).apply {
                    if (resolveActivity(myActivity.packageManager) != null) {
                        startActivity(this)
                    }
                }
                true
            }
        }
    }

    private fun makeNotificationSettingsLink() {
        val myPref =
            findPreference(getString(R.string.pref_static_field_key_notification_settings)) as Preference?

        myPref?.isSelectable = true
        myPref?.onPreferenceClickListener = OnPreferenceClickListener {
            val myActivity = activity
            myActivity?.let {
                val intent = notificationSettingsIntent(
                    myActivity.packageName,
                    createNotificationScreenshotTakenChannel(myActivity)
                )
                intent.resolveActivity(myActivity.packageManager)?.let {
                    startActivity(intent)
                }
            }
            true
        }
    }

    private fun makePostActionsLink() {
        val myPref =
            findPreference(getString(R.string.pref_static_field_key_post_actions)) as Preference?

        myPref?.isSelectable = true
        myPref?.onPreferenceClickListener = OnPreferenceClickListener {
            val myActivity = activity
            myActivity?.let {
                val intent = Intent(myActivity, PostSettingsActivity::class.java)
                intent.resolveActivity(myActivity.packageManager)?.let {
                    startActivity(intent)
                }
            }
            true
        }
    }

    private fun makeFloatingButtonSettingsLink() {
        (findPreference(getString(R.string.pref_static_field_key_floating_button_settings)) as Preference?)?.let {
            it.isSelectable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            it.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            it.onPreferenceClickListener = OnPreferenceClickListener { pref ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    FloatingButtonSettingsActivity.start(pref.context)
                }
                true
            }
        }
    }

    private fun makeAccessibilitySettingsLink() {
        val preferenceClickListener = OnPreferenceClickListener {
            /*
            Open accessibility settings if accessibility service is not running
             */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && (it as? SwitchPreference)?.isChecked == true && ScreenshotAccessibilityService.instance == null) {
                // Accessibility service is not running -> Open settings so user can enable it
                if (!openedAccessibilitySetting) {
                    // Only open the accessibility settings once, if not enabled, do not open again
                    openedAccessibilitySetting = true
                    this.activity?.run { openAccessibilitySettings(this, TAG) }
                }
            }
            true
        }

        useNativePref?.onPreferenceClickListener = preferenceClickListener
        floatingButtonPref?.onPreferenceClickListener = preferenceClickListener
    }

    private fun makeStorageDirectoryLink() {
        storageDirectoryPref?.setOnPreferenceClickListener {
            val myActivity = activity
            val currentDir = prefManager.screenshotDirectory
            myActivity?.let {
                Intent(ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    addFlags(FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(FLAG_GRANT_WRITE_URI_PERMISSION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !currentDir.isNullOrEmpty()) {
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(currentDir))
                    }
                    if (resolveActivity(myActivity.packageManager) != null) {
                        startForPickFolder.launch(createChooser(this, "Choose directory"))
                    }
                }
            }
            true
        }
    }

    private fun makeAdvancedSettingsLink() {
        val myPref =
            findPreference(getString(R.string.pref_static_field_key_advanced_settings)) as Preference?

        myPref?.isSelectable = true
        myPref?.onPreferenceClickListener = OnPreferenceClickListener {
            activity?.apply {
                supportFragmentManager.commit {
                    replace<SettingAdvancedFragment>(android.R.id.content)
                    setReorderingAllowed(true)
                    addToBackStack(null)
                }
            }
            true
        }
    }

    private fun updateNotificationSummary() {
        activity?.let { myActivity ->
            notificationPref?.apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> {
                        isEnabled = false
                        summary = getString(R.string.use_native_screenshot_option_default)
                    }

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && prefManager.useSystemDefaults -> {
                        isEnabled = false
                        summary = getString(R.string.use_native_screenshot_option_android11)
                    }

                    notificationScreenshotTakenChannelEnabled(myActivity) -> {
                        isEnabled = true
                        summary = getString(R.string.notification_settings_on)
                    }

                    else -> {
                        isEnabled = true
                        summary = getString(R.string.notification_settings_off)
                    }
                }
            }
        }
    }

    private fun updatePostActionsSummary() {
        postActionsPref?.apply {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> {
                    isEnabled = false
                    summary = getString(R.string.use_native_screenshot_option_default)
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && prefManager.useSystemDefaults -> {
                    isEnabled = false
                    summary = getString(R.string.use_native_screenshot_option_android11)
                }

                else -> {
                    isEnabled = true
                    summary = getString(R.string.setting_post_actions_description)
                }
            }
        }

    }

    private fun updateDelaySummary(value: String) {
        delayPref?.apply {
            val index = findIndexOfValue(value)
            summary = if (index != -1) {
                entries[index]
            } else {
                "$value (${getString(R.string.about_advanced_settings_button)})"
            }
        }
    }

    private fun updateFloatingButtonDelaySummary(value: String) {
        floatingButtonDelay?.apply {
            val index = findIndexOfValue(value)
            summary = if (index != -1) {
                entries[index]
            } else {
                "$value (${getString(R.string.about_advanced_settings_button)})"
            }
        }
    }

    private fun updateTileActionSummary(value: String) {
        tileActionPref?.apply {
            summary = entries[findIndexOfValue(value)]
        }
    }

    private fun updateTileLongPressActionSummary(value: String) {
        tileLongPressActionPref?.apply {
            summary = entries[findIndexOfValue(value)]
        }
    }

    private fun updateFloatingActionSummary(value: String) {
        floatingButtonActionPref?.apply {
            summary = entries[findIndexOfValue(value)]
        }
    }

    private fun updateVoiceInteractionActionSummary(value: String, switchEvent: Boolean = false) {
        var newValue = value
        if (value == getString(R.string.setting_voice_interaction_action_value_native)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                // Not Supported below Android 9
                newValue = getString(R.string.setting_voice_interaction_action_value_provided)
                context.toastMessage(
                    getString(R.string.use_native_screenshot_unsupported),
                    ToastType.ACTIVITY
                )
            } else if (ScreenshotAccessibilityService.instance == null && switchEvent) {
                // Accessibility service is not running -> Open settings so user can enable it
                if (!openedAccessibilitySetting) {
                    // Only open the accessibility settings once, if not enabled, do not open again
                    openedAccessibilitySetting = true
                    this.activity?.run { openAccessibilitySettings(this, TAG) }
                }
            }
        } else if (MyVoiceInteractionService.instance != null && switchEvent && !askedForStoragePermission) {
            this.activity?.run {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    packageManager.checkPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        packageName
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Only ask for storage permission once
                    askedForStoragePermission = true
                    App.requestStoragePermission(this, false)
                }
            }
        }

        prefManager.voiceInteractionAction = newValue
        voiceInteractionActionPref?.apply {
            summary = entries[findIndexOfValue(newValue)]
        }
    }

    private fun updateFileFormatSummary(value: String) {
        fileFormatPref?.apply {
            if (prefManager.useSystemDefaults && prefManager.useNative) {
                isEnabled = false
                summary = getString(
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        R.string.use_native_screenshot_option_default
                    } else {
                        R.string.use_native_screenshot_option_android11
                    }
                )
            } else {
                isEnabled = true
                summary = entries[findIndexOfValue(value)]
            }
        }
    }

    private fun updateFileNamePatternSummary() {
        fileNamePatternPref?.apply {
            if (prefManager.useSystemDefaults && prefManager.useNative) {
                isEnabled = false
                summary = getString(
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        R.string.use_native_screenshot_option_default
                    } else {
                        R.string.use_native_screenshot_option_android11
                    }
                )
                fileNamePlaceholders?.isVisible = false
            } else {
                isEnabled = true
                summary = prefManager.fileNamePattern
                fileNamePlaceholders?.isVisible = true
            }
        }
    }

    private fun updateFloatingButtonShutterSummary(updateFloatingButton: Boolean = false) {
        floatingButtonShutter?.apply {
            summary =
                ShutterCollection(context, R.array.shutters, R.array.shutter_names).current().name
        }
        if (updateFloatingButton) {
            updateFloatingButton(switchEvent = false, forceRedraw = true)
        }
    }

    private fun updateHideApp(hideOptionCompletely: Boolean = false) {
        /* Do not show "hide_app_from_launcher" setting, it's no longer possible on Android 10+
          If the icon is already hidden, show the option anyway, to restore it
          If the icons was hidden and the user just restored it, show a hint that it is unsupported.
        */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !prefManager.hideApp) {
            hideAppPref?.apply {
                isChecked = false
                isEnabled = false
                summary = getString(R.string.hide_app_unsupported)
                isVisible = !hideOptionCompletely
            }
        }
    }

    private fun onHideApp(hide: Boolean): Boolean {
        val myActivity = activity
        return myActivity?.let {
            val componentName = ComponentName(myActivity, MainActivity::class.java)
            try {
                myActivity.packageManager.setComponentEnabledSetting(
                    componentName,
                    if (hide) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                updateHideApp()
                true
            } catch (e: Exception) {
                Log.e(TAG, "setComponentEnabledSetting", e)
                context.toastMessage(
                    myActivity.getString(R.string.toggle_app_icon_failed),
                    ToastType.ACTIVITY
                )
                prefManager.hideApp = !hide
                false
            }
        } ?: false
    }

    private fun updateUseNative(switchEvent: Boolean = false) {
        useNativePref?.run {
            when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> {
                    isChecked = false
                    isEnabled = false
                    summary = getString(R.string.use_native_screenshot_unsupported)
                }

                isChecked -> {
                    summary = if (ScreenshotAccessibilityService.instance == null) {
                        getString(
                            R.string.emoji_warning,
                            getString(R.string.use_native_screenshot_unavailable)
                        )
                    } else {
                        getString(R.string.use_native_screenshot_summary)
                    }
                    updateNotificationSummary()
                    updatePostActionsSummary()
                    updateNotificationActions()
                    updateStorageDirectory()
                    updateFileFormatSummary(prefManager.fileFormat)
                    updateFileNamePatternSummary()
                }

                else -> {
                    summary = getString(R.string.use_native_screenshot_summary)
                    fileFormatPref?.isEnabled = true
                    fileNamePatternPref?.isEnabled = true
                    updateFileFormatSummary(prefManager.fileFormat)
                    updateFileNamePatternSummary()
                    updateNotificationSummary()
                    updatePostActionsSummary()
                    updateNotificationActions()
                    updateStorageDirectory()
                }
            }
        }
        updateUseSystemDefaults(switchEvent)
    }

    private fun updateUseSystemDefaults(switchEvent: Boolean) {
        useSystemDefaultsPref?.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || !prefManager.useNative) {
                summary = getString(R.string.use_system_defaults_summary)
                isEnabled = false
                isVisible = false
            } else if (useNativePref?.isChecked == true && isChecked) {
                summary = getString(R.string.use_system_defaults_summary_on)
                isEnabled = true
                isVisible = true
            } else if (useNativePref?.isChecked == true && !isChecked) {
                summary = getString(R.string.use_system_defaults_summary_off)
                isEnabled = true
                isVisible = true
                if (switchEvent && !askedForStoragePermission) {
                    activity?.run {
                        // Check storage permission
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            packageManager.checkPermission(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                packageName
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            askedForStoragePermission = true
                            App.requestStoragePermission(this, false)
                        }
                    }
                }
            }
        }
    }

    private fun updateFloatingButton(switchEvent: Boolean = false, forceRedraw: Boolean = false) {
        floatingButtonPref?.run {
            isChecked = prefManager.floatingButton
            summary = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> {
                    isChecked = false
                    isEnabled = false
                    val unsupported = getString(R.string.setting_floating_button_unsupported)
                    floatingButtonScalePref?.isVisible = false
                    floatingButtonHideAfterPref?.isVisible = false
                    floatingButtonHideShowClosePref?.isVisible = false
                    floatingButtonShutter?.isVisible = false
                    floatingButtonDelay?.isVisible = false
                    floatingButtonActionPref?.isVisible = false
                    unsupported
                }

                isChecked -> {
                    updateUseNative(switchEvent)
                    if (ScreenshotAccessibilityService.instance == null) {
                        getString(
                            R.string.emoji_warning,
                            getString(R.string.setting_floating_button_unavailable)
                        )
                    } else {
                        getString(R.string.setting_floating_button_summary)
                    }
                }

                else -> {
                    getString(R.string.setting_floating_button_summary)
                }
            }
        }
        floatingButtonScalePref?.run {
            text = prefManager.floatingButtonScale.toString()
            summary =
                "${getString(R.string.setting_floating_button_scale_summary)}\n${
                    getString(
                        R.string.setting_floating_button_scale_current,
                        prefManager.floatingButtonScale,
                        getString(R.string.setting_floating_button_scale_default).toInt()
                    )
                }"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ScreenshotAccessibilityService.instance?.updateFloatingButton(forceRedraw)
        }
    }

    fun updateFloatingButtonFromService() {
        if (!isResumed || isHidden) {
            return
        }
        floatingButtonPref?.isChecked = prefManager.floatingButton
        updateFloatingButton(switchEvent = false, forceRedraw = false)
    }

    private fun updateFloatingButtonClose(openWithValue: String? = null) {
        if ((floatingButtonHideShowClosePref?.isChecked == true && !floatingButtonHideShowClosePreventRecursion) || openWithValue != null) {
            val relativeLayout = LayoutInflater.from(context)
                .inflate(R.layout.dialog_close_button, null) as ViewGroup
            val closeButtonEmojiInput =
                relativeLayout.findViewById<AutoCompleteTextView>(R.id.closeButtonEmojiInput)
            if (openWithValue != null) {
                floatingButtonShowCloseTextValue = openWithValue
                closeButtonEmojiInput.setText(openWithValue)
            } else {
                closeButtonEmojiInput.setText(prefManager.floatingButtonCloseEmoji)
            }
            closeButtonEmojiInput.setOnEditorActionListener { _, actionId, _ ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        saveFloatingButton(closeButtonEmojiInput.text.trim().toString())
                        floatingButtonShowCloseAlertDialog?.safeDismiss(TAG)
                        true
                    }

                    else -> false
                }
            }
            closeButtonEmojiInput.addTextChangedListener {
                floatingButtonShowCloseTextValue = closeButtonEmojiInput.text.toString()
                if (closeButtonEmojiInput.text.isBlank()) {
                    (requireActivity().getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager?)?.hideSoftInputFromWindow(
                        closeButtonEmojiInput.windowToken,
                        0
                    )
                    closeButtonEmojiInput.postDelayed({
                        closeButtonEmojiInput.showDropDown()
                    }, 30)
                }
            }
            val closeButtonSuggestions: Array<out String> =
                resources.getStringArray(R.array.close_buttons)
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                closeButtonSuggestions
            ).also { adapter ->
                closeButtonEmojiInput.setAdapter(adapter)
            }
            floatingButtonShowCloseAlertDialog = AlertDialog.Builder(context)
                .setTitle(R.string.setting_floating_button_show_close_dialog_title)
                .setMessage(R.string.setting_floating_button_show_close_dialog_description)
                .setView(relativeLayout)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    saveFloatingButton(closeButtonEmojiInput.text.trim().toString())
                    dialog.safeDismiss(TAG)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.safeDismiss(TAG) }
                .show()
            closeButtonEmojiInput.postDelayed({
                closeButtonEmojiInput.showDropDown()
            }, 30)

            floatingButtonHideShowClosePreventRecursion = true
        } else {
            floatingButtonHideShowClosePreventRecursion = false
        }
        updateFloatingButton(switchEvent = false, forceRedraw = true)
    }

    private fun saveFloatingButton(emoji: String) {
        prefManager.floatingButtonCloseEmoji = emoji
        updateFloatingButton(switchEvent = false, forceRedraw = true)
    }


    private fun updateStorageDirectory() {
        storageDirectoryPref?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                summary = getString(R.string.use_native_screenshot_option_default)
                isEnabled = false
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && prefManager.useSystemDefaults) {
                summary = getString(R.string.use_native_screenshot_option_android11)
                isEnabled = false
            } else if (prefManager.screenshotDirectory != null) {
                summary = nicePathFromUri(prefManager.screenshotDirectory)
                isEnabled = true
            } else {
                summary = getString(R.string.setting_storage_directory_description)
                isEnabled = true
            }
        }
    }

    private fun updateDarkTheme(switchEvent: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            darkThemePref?.apply {
                isVisible = false
            }
        } else {
            darkThemePref?.apply {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P && !entries[0].contains("Not supported on Android 9")) {
                    // Android 9 P supports dark mode but does not support the follow system setting MODE_NIGHT_FOLLOW_SYSTEM
                    entries[0] = "${entries[0]} (Not supported on Android 9)"
                }
                summary = entries[findIndexOfValue(value).takeIf { it != -1 } ?: 0]
            }
            if (switchEvent) {
                App.getInstance().applyDayNightMode()
            }
        }
    }

    private fun updateNotificationActions() {
        notificationActionsPref?.apply {
            if (prefManager.useSystemDefaults && prefManager.useNative) {
                isEnabled = false
                summary = getString(
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        R.string.use_native_screenshot_option_default
                    } else {
                        R.string.use_native_screenshot_option_android11
                    }
                )
            } else {
                isEnabled = true
                val allValues =
                    resources.getStringArray(R.array.setting_notification_actions_values)
                val selectedEntries = ArrayList<String>()
                for (v in allValues) {
                    if (v in values) {
                        selectedEntries.add(entries[findIndexOfValue(v)].toString())
                    }
                    if (selectedEntries.size == 3) {
                        break
                    }
                }
                summary =
                    getString(R.string.setting_notification_buttons_description) + "\n" + selectedEntries.joinToString(
                        ", "
                    )
                if (values.size > 3) {
                    context.toastMessage(
                        getString(R.string.setting_notification_buttons_max_three),
                        ToastType.ACTIVITY
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (floatingButtonShowCloseAlertDialog?.isShowing == true) {
            outState.putBoolean(FLOATING_BUTTON_SHOW_CLOSE_DIALOG_SHOWN, true)
            outState.putString(
                FLOATING_BUTTON_SHOW_CLOSE_DIALOG_VALUE,
                floatingButtonShowCloseTextValue
            )
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        floatingButtonShowCloseAlertDialog?.cancel()
        floatingButtonShowCloseAlertDialog = null
        instance?.clear()
        super.onDestroy()
        pref?.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
