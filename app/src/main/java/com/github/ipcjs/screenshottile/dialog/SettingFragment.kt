package com.github.ipcjs.screenshottile.dialog

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceClickListener
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
    private val fileFormatPref by lazy { findPreference(getString(R.string.pref_key_file_format)) as ListPreference }
    private val pref: SharedPreferences by lazy { preferenceManager.sharedPreferences }
    private val prefManager by lazy { PrefManager(context, pref) }

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            when (key) {
                getString(R.string.pref_key_delay) -> updateDelaySummary(prefManager.delay.toString())
                getString(R.string.pref_key_hide_app) -> updateHideApp(prefManager.hideApp)
                getString(R.string.pref_key_file_format) -> updateFileFormatSummary(prefManager.fileFormat)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref)
        pref.registerOnSharedPreferenceChangeListener(prefListener)
        updateDelaySummary(delayPref.value)
        updateFileFormatSummary(fileFormatPref.value)

        makeLink(R.string.pref_static_field_key_about_app_1, R.string.pref_static_field_link_about_app_1)
        makeLink(R.string.pref_static_field_key_about_app_3, R.string.pref_static_field_link_about_app_3)
        makeLink(R.string.pref_static_field_key_about_license_1, R.string.pref_static_field_link_about_license_1)
    }

    private fun makeLink(name: Int, link: Int) {
        val myPref = findPreference(getString(name)) as Preference
        myPref.onPreferenceClickListener = OnPreferenceClickListener {
            Intent(Intent.ACTION_VIEW, Uri.parse(getString(link))).apply {
                if (resolveActivity(this@SettingFragment.context.packageManager) != null) {
                    startActivity(this)
                }
            }
            true
        }
    }

    private fun updateDelaySummary(value: String) {
        delayPref.summary = delayPref.entries[delayPref.findIndexOfValue(value)]
    }

    private fun updateFileFormatSummary(value: String) {
        fileFormatPref.summary = fileFormatPref.entries[fileFormatPref.findIndexOfValue(value)]
    }

    private fun updateHideApp(hide: Boolean): Boolean {
        val componentName = ComponentName(context, MainActivity::class.java)
        return try {
            context.packageManager.setComponentEnabledSetting(
                componentName,
                if (hide) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            true
        } catch (e: Exception) {
            Log.e("SettingFragment", "setComponentEnabledSetting", e)
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
