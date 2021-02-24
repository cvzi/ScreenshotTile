package com.github.cvzi.screenshottile.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.Intent.*
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
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceClickListener
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.MainActivity
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService.Companion.openAccessibilitySettings
import com.github.cvzi.screenshottile.utils.*
import java.lang.ref.WeakReference


/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */
class SettingFragment : PreferenceFragmentCompat() {
    companion object {
        const val TAG = "SettingFragment.kt"
        private const val DIRECTORY_CHOOSER_REQUEST_CODE = 8912
        var instance: WeakReference<SettingFragment>? = null
    }

    init {
        instance = WeakReference<SettingFragment>(this)
    }

    private var notificationPref: Preference? = null
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
    private var hideAppPref: SwitchPreference? = null
    private var storageDirectoryPref: Preference? = null
    private var broadcastSecretPref: EditTextPreference? = null
    private var tileActionPref: ListPreference? = null
    private var darkThemePref: ListPreference? = null
    private var floatingButtonHideShowClosePreventRecursion = false
    private lateinit var pref: SharedPreferences
    private val prefManager = App.getInstance().prefManager

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            when (key) {
                getString(R.string.pref_key_delay) -> updateDelaySummary(prefManager.delay.toString())
                getString(R.string.pref_key_hide_app) -> onHideApp(prefManager.hideApp)
                getString(R.string.pref_key_file_format) -> updateFileFormatSummary(prefManager.fileFormat)
                getString(R.string.pref_key_use_native) -> updateUseNative()
                getString(R.string.pref_key_floating_button) -> updateFloatingButton()
                getString(R.string.pref_key_floating_button_scale) -> updateFloatingButton(true)
                getString(R.string.pref_key_floating_button_show_close) -> updateFloatingButtonClose()
                getString(R.string.pref_key_floating_button_shutter) -> updateFloatingButtonShutterSummary(
                    true
                )
                getString(R.string.pref_key_floating_button_delay) -> updateFloatingButtonDelaySummary(
                    prefManager.floatingButtonDelay.toString()
                )
                getString(R.string.pref_key_use_system_defaults) -> updateUseNative()
                getString(R.string.pref_key_tile_action) -> updateTileActionSummary(prefManager.tileAction)
                getString(R.string.pref_key_dark_theme) -> updateDarkTheme(true)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        pref = preferenceManager.sharedPreferences

        addPreferencesFromResource(R.xml.pref)

        notificationPref =
            findPreference(getString(R.string.pref_static_field_key_notification_settings))
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
        darkThemePref = findPreference(getString(R.string.pref_key_dark_theme)) as ListPreference?

        pref.registerOnSharedPreferenceChangeListener(prefListener)

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

        makeNotificationSettingsLink()
        makeAccessibilitySettingsLink()
        makeStorageDirectoryLink()
    }


    override fun onResume() {
        super.onResume()

        delayPref?.run { updateDelaySummary(value) }
        tileActionPref?.run { updateTileActionSummary(value) }
        fileFormatPref?.run { updateFileFormatSummary(value) }
        floatingButtonDelay?.run { updateFloatingButtonDelaySummary(value) }

        floatingButtonHideShowClosePreventRecursion = false

        updateNotificationSummary()
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

    private fun makeLink(name: Int, link: Int) {
        val myActivity = activity
        myActivity?.let {
            val myPref = findPreference(getString(name)) as Preference?
            myPref?.isSelectable = true
            myPref?.onPreferenceClickListener = OnPreferenceClickListener {
                Intent(ACTION_VIEW, Uri.parse(getString(link))).apply {
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

    private fun makeAccessibilitySettingsLink() {
        val preferenceClickListener = OnPreferenceClickListener {
            /*
            Open accessibility settings if accessibility service is not running
             */
            val myActivity = activity
            myActivity?.apply {
                if ((it as? SwitchPreference)?.isChecked == true && ScreenshotAccessibilityService.instance == null) {
                    // Accessibility service is not running -> Open settings so user can enable it
                    openAccessibilitySettings(this, TAG)
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
                        startActivityForResult(
                            createChooser(this, "Choose directory"),
                            DIRECTORY_CHOOSER_REQUEST_CODE
                        )
                    }
                }
            }
            true
        }
    }

    private fun updateNotificationSummary() {
        activity?.let { myActivity ->
            notificationPref?.apply {
                when {
                    prefManager.useNative && ScreenshotAccessibilityService.instance != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> {
                        isEnabled = false
                        summary = getString(R.string.use_native_screenshot_option_default)
                    }
                    prefManager.useNative && ScreenshotAccessibilityService.instance != null && prefManager.useSystemDefaults -> {
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

    private fun updateDelaySummary(value: String) {
        delayPref?.apply {
            summary = entries[findIndexOfValue(value)]
        }
    }

    private fun updateFloatingButtonDelaySummary(value: String) {
        floatingButtonDelay?.apply {
            summary = entries[findIndexOfValue(value)]
        }
    }

    private fun updateTileActionSummary(value: String) {
        tileActionPref?.apply {
            summary = entries[findIndexOfValue(value)]
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

    private fun updateFloatingButtonShutterSummary(updateFloatingButton: Boolean = false) {
        floatingButtonShutter?.apply {
            summary =
                ShutterCollection(context, R.array.shutters, R.array.shutter_names).current().name
        }
        if (updateFloatingButton) {
            updateFloatingButton(true)
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
                Toast.makeText(
                    context,
                    myActivity.getString(R.string.toggle_app_icon_failed),
                    Toast.LENGTH_LONG
                ).show()
                prefManager.hideApp = !hide
                false
            }
        } ?: false
    }

    private fun updateUseNative() {
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
                    updateStorageDirectory()
                    updateFileFormatSummary(prefManager.fileFormat)
                }
                else -> {
                    summary = getString(R.string.use_native_screenshot_summary)
                    fileFormatPref?.isEnabled = true
                    updateFileFormatSummary(prefManager.fileFormat)
                    updateNotificationSummary()
                    updateStorageDirectory()
                }
            }
        }
        updateUseSystemDefaults()
    }

    private fun updateUseSystemDefaults() {
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
                activity?.run {
                    // Check storage permission
                    if (packageManager.checkPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            packageName
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        App.requestStoragePermission(this, false)
                    }
                }
            }
        }
    }

    private fun updateFloatingButton(forceRedraw: Boolean = false) {
        floatingButtonPref?.run {
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
                    unsupported
                }
                isChecked -> {
                    updateUseNative()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ScreenshotAccessibilityService.instance?.updateFloatingButton(forceRedraw)
        }
    }

    fun updateFloatingButtonFromService() {
        floatingButtonPref?.isChecked = prefManager.floatingButton
        updateFloatingButton()
    }

    private fun updateFloatingButtonClose() {
        if (floatingButtonHideShowClosePref?.isChecked == true && !floatingButtonHideShowClosePreventRecursion) {
            var alertDialog: AlertDialog? = null
            val relativeLayout = LayoutInflater.from(context)
                .inflate(R.layout.dialog_close_button, null) as ViewGroup
            val closeButtonEmojiInput =
                relativeLayout.findViewById<AutoCompleteTextView>(R.id.closeButtonEmojiInput)
            closeButtonEmojiInput.setText(prefManager.floatingButtonCloseEmoji)
            closeButtonEmojiInput.setOnEditorActionListener { _, actionId, _ ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_DONE -> {
                        saveFloatingButton(closeButtonEmojiInput.text.trim().toString())
                        alertDialog?.dismiss()
                        true
                    }
                    else -> false
                }
            }
            closeButtonEmojiInput.addTextChangedListener { v ->
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
            alertDialog = AlertDialog.Builder(context)
                .setTitle(R.string.setting_floating_button_show_close_dialog_title)
                .setMessage(R.string.setting_floating_button_show_close_dialog_description)
                .setView(relativeLayout)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    saveFloatingButton(closeButtonEmojiInput.text.trim().toString())
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
            closeButtonEmojiInput.postDelayed({
                closeButtonEmojiInput.showDropDown()
            }, 30)

            floatingButtonHideShowClosePreventRecursion = true
        } else {
            floatingButtonHideShowClosePreventRecursion = false
        }
        updateFloatingButton(true)
    }

    private fun saveFloatingButton(emoji: String) {
        prefManager.floatingButtonCloseEmoji = emoji
        updateFloatingButton(true)
    }


    private fun updateStorageDirectory() {
        storageDirectoryPref?.run {
            if (prefManager.useNative && ScreenshotAccessibilityService.instance != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                summary = getString(R.string.use_native_screenshot_option_default)
                isEnabled = false
            } else if (prefManager.useNative && ScreenshotAccessibilityService.instance != null && prefManager.useSystemDefaults) {
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

    private fun updateDarkTheme(switched: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            darkThemePref?.apply {
                isVisible = false
            }
        } else {
            darkThemePref?.apply {
                summary = entries[findIndexOfValue(value)]
            }
            if (switched) {
                App.getInstance().applyDayNightMode()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == DIRECTORY_CHOOSER_REQUEST_CODE && intent != null) {
            val uri = intent.data
            val takeFlags: Int = intent.flags and
                    (FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
            if (uri != null && activity != null && activity?.contentResolver != null) {
                prefManager.screenshotDirectory = uri.toString()
                activity?.contentResolver?.takePersistableUriPermission(uri, takeFlags)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pref.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
