package com.github.cvzi.screenshottile.fragments

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.utils.CompressionOptions
import com.github.cvzi.screenshottile.utils.cleanUpAppData
import com.github.cvzi.screenshottile.utils.compressionPreference
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
    private var floatingButtonAlphaPref: EditTextPreference? = null
    private var formatQualityPref: EditTextPreference? = null
    private var keepAppDataMaxPref: EditTextPreference? = null
    private var pref: SharedPreferences? = null
    private val prefManager = App.getInstance().prefManager

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            when (key) {
                getString(R.string.pref_key_floating_button_alpha) -> updateFloatingButton(
                    switchEvent = true, forceRedraw = true
                )
                getString(R.string.pref_key_format_quality) -> updateFormatQualitySummary()
                getString(R.string.pref_key_keep_app_data_max) -> updateKeepAppDataMaxSummary(
                    switchEvent = true
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
        floatingButtonAlphaPref =
            findPreference(getString(R.string.pref_key_floating_button_alpha)) as EditTextPreference?
        formatQualityPref =
            findPreference(getString(R.string.pref_key_format_quality)) as EditTextPreference?
        keepAppDataMaxPref =
            findPreference(getString(R.string.pref_key_keep_app_data_max)) as EditTextPreference?
        pref?.registerOnSharedPreferenceChangeListener(prefListener)
    }


    override fun onResume() {
        super.onResume()

        updateNaggingToasts()
        updateFloatingButton(switchEvent = false, forceRedraw = false)
        updateFormatQualitySummary()
        updateKeepAppDataMaxSummary()
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
            floatingButtonAlphaPref?.apply {
                isEnabled = false
                summary = getString(R.string.use_native_screenshot_unsupported)
                isVisible = false
            }
        }
    }

    private fun updateFormatQualitySummary() {
        context?.let { context ->
            val defaultCompression = compressionPreference(context, forceDefaultQuality = true)
            val currentCompression = compressionPreference(context, forceDefaultQuality = false)

            formatQualityPref?.apply {
                summary = getString(
                    R.string.setting_format_quality_summary,
                    compressionFormatToString(currentCompression),
                    compressionFormatToString(defaultCompression)
                )
            }
        }

    }

    private fun updateKeepAppDataMaxSummary(switchEvent: Boolean = false) {
        keepAppDataMaxPref?.apply {
            val folder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            summary = getString(
                R.string.setting_keep_app_data_max_summary,
                prefManager.keepAppDataMax,
                folder ?: ""
            )
            if (switchEvent) {
                this@SettingAdvancedFragment.context?.let {
                    cleanUpAppData(it)
                }
            }
        }
    }

    private fun compressionFormatToString(compressionOptions: CompressionOptions): String {
        @Suppress("DEPRECATION", "CascadeIf")
        // Do not use when, it is exhaustive and enumerates fields that are not available on
        // all Android version, it will crashes with `No static field WEBP_LOSSY` on old Android
        return if (compressionOptions.format == Bitmap.CompressFormat.JPEG) {
            "JPEG ${compressionOptions.quality}%"
        } else if (compressionOptions.format == Bitmap.CompressFormat.PNG) {
            "PNG (quality parameter has no effect)"
        } else if (compressionOptions.format == Bitmap.CompressFormat.WEBP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && compressionOptions.quality == 100) {
                "WEBP (Lossless 100%)"
            } else {
                "WEBP (Lossy ${compressionOptions.quality}%)"
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (compressionOptions.format == Bitmap.CompressFormat.WEBP_LOSSY) {
                "WEBP (Lossy ${compressionOptions.quality}%)"
            } else if (compressionOptions.format == Bitmap.CompressFormat.WEBP_LOSSLESS) {
                "WEBP (Lossless ${compressionOptions.quality}%)"
            } else {
                "${compressionOptions.format.name} ${compressionOptions.quality}%"
            }
        } else {
            "${compressionOptions.format.name} ${compressionOptions.quality}%"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pref?.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
