package com.github.ipcjs.screenshottile.dialog

import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceFragment
import android.util.Log
import android.widget.Toast
import com.github.ipcjs.screenshottile.MainActivity
import com.github.ipcjs.screenshottile.PrefManager
import com.github.ipcjs.screenshottile.R

/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */
class SettingFragment : PreferenceFragment() {
    private val delayPref by lazy { findPreference(getString(R.string.pref_key_delay)) as ListPreference }
    private val pref: SharedPreferences by lazy { preferenceManager.sharedPreferences }
    private val prefManager by lazy { PrefManager(context, pref) }

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            when (key) {
                getString(R.string.pref_key_delay) -> updateDelaySummary(prefManager.delay.toString())
                getString(R.string.pref_key_hide_app) -> updateHideApp(prefManager.hideApp)
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

    private fun updateHideApp(hide: Boolean): Boolean {
        val componentName = ComponentName(context, MainActivity::class.java)
        return try {
            if (hide) {
                context.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                context.packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            true
        } catch (t: Throwable) {
            Log.e("SettingFragment", "setComponentEnabledSetting", t)
            Toast.makeText(context, context.getString(R.string.toggle_app_icon_failed), Toast.LENGTH_LONG).show()
            prefManager.hideApp = !hide
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pref.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}