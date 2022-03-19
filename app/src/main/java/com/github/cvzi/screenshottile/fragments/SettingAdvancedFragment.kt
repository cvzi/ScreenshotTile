package com.github.cvzi.screenshottile.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.github.cvzi.screenshottile.R
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
    private var pref: SharedPreferences? = null

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            Log.v(TAG, "pref listener: $key")
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        pref = preferenceManager.sharedPreferences

        addPreferencesFromResource(R.xml.pref_advanced)

        floatingButtonScalePref =
            findPreference(getString(R.string.pref_key_floating_button_scale)) as EditTextPreference?
        naggingToastsPref =
            findPreference(getString(R.string.pref_key_nagging_toasts)) as SwitchPreference?

        pref?.registerOnSharedPreferenceChangeListener(prefListener)
    }


    override fun onResume() {
        super.onResume()

        updateNaggingToasts()
    }

    private fun updateNaggingToasts() {
        naggingToastsPref?.isVisible = Locale.getDefault().country == "RU"
    }

    override fun onDestroy() {
        super.onDestroy()
        pref?.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
