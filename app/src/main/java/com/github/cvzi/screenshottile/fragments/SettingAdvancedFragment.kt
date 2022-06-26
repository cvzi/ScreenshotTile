package com.github.cvzi.screenshottile.fragments

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import java.util.*


/**
 * Created by cuzi (cuzi@openmail.cc) on 2022/03/19.
 */
class SettingAdvancedFragment : PreferenceFragmentCompat() {
    companion object {
        const val TAG = "SettingAdvancedFragment"
    }

    private var floatingButtonScalePref: EditTextPreference? = null
    private var naggingToastsPref: SwitchPreference? = null
    private var floatingButtonAlpha: EditTextPreference? = null
    private var pref: SharedPreferences? = null

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            when (key) {
                getString(R.string.pref_key_floating_button_alpha) -> updateFloatingButton(
                    switchEvent = true, forceRedraw = true
                )
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        pref = preferenceManager.sharedPreferences

        addPreferencesFromResource(R.xml.pref_advanced)

        floatingButtonScalePref =
            findPreference(getString(R.string.pref_key_floating_button_scale)) as EditTextPreference?
        naggingToastsPref =
            findPreference(getString(R.string.pref_key_nagging_toasts)) as SwitchPreference?
        floatingButtonAlpha =
            findPreference(getString(R.string.pref_key_floating_button_alpha)) as EditTextPreference?

        pref?.registerOnSharedPreferenceChangeListener(prefListener)
    }


    override fun onResume() {
        super.onResume()

        updateNaggingToasts()
        updateFloatingButton(switchEvent = false, forceRedraw = false)
    }

    private fun updateNaggingToasts() {
        naggingToastsPref?.isVisible = Locale.getDefault().country == "RU"
    }

    private fun updateFloatingButton(switchEvent: Boolean = false, forceRedraw: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (switchEvent) {
                ScreenshotAccessibilityService.instance?.updateFloatingButton(forceRedraw)
            }
        } else {
            floatingButtonAlpha?.apply {
                isEnabled = false
                summary = getString(R.string.use_native_screenshot_unsupported)
                isVisible = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pref?.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
