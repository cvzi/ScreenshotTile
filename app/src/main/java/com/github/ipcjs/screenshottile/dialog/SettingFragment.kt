package com.github.ipcjs.screenshottile.dialog

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import com.github.ipcjs.screenshottile.PrefManager
import com.github.ipcjs.screenshottile.R

/**
 * Created by ipcjs on 2017/8/17.
 */
class SettingFragment : PreferenceFragment() {
    val delayPref by lazy { findPreference(getString(R.string.pref_key_delay)) as ListPreference }
    val pref: SharedPreferences by lazy { preferenceManager.sharedPreferences }
    val prefManager by lazy { PrefManager(context, pref) }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
        when (key) {
            getString(R.string.pref_key_delay) -> updateDelaySummary(prefManager.delay.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref)
        pref.registerOnSharedPreferenceChangeListener(prefListener)
        updateDelaySummary(delayPref.value)
    }

    private fun updateDelaySummary(value: String) {
        delayPref.summary = delayPref.entries[delayPref.findIndexOfValue(value)]
    }

    override fun onDestroy() {
        super.onDestroy()
        pref.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}