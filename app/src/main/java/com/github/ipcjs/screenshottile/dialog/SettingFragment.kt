package com.github.ipcjs.screenshottile.dialog

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import com.github.ipcjs.screenshottile.*


/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */
class SettingFragment : PreferenceFragmentCompat() {
    private val notificationPref by lazy { findPreference(getString(R.string.pref_static_field_key_notification_settings)) }
    private val delayPref by lazy { findPreference(getString(R.string.pref_key_delay)) as ListPreference }
    private val fileFormatPref by lazy { findPreference(getString(R.string.pref_key_file_format)) as ListPreference }
    private val pref: SharedPreferences by lazy { preferenceManager.sharedPreferences }
    private val prefManager by lazy { App.getInstance().prefManager }

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            when (key) {
                getString(R.string.pref_key_delay) -> updateDelaySummary(prefManager.delay.toString())
                getString(R.string.pref_key_hide_app) -> updateHideApp(prefManager.hideApp)
                getString(R.string.pref_key_file_format) -> updateFileFormatSummary(prefManager.fileFormat)
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref)

        pref.registerOnSharedPreferenceChangeListener(prefListener)
        updateDelaySummary(delayPref.value)
        updateFileFormatSummary(fileFormatPref.value)
        updateNotificationSummary()

        makeLink(R.string.pref_static_field_key_about_app_1, R.string.pref_static_field_link_about_app_1)
        makeLink(R.string.pref_static_field_key_about_app_3, R.string.pref_static_field_link_about_app_3)
        makeLink(R.string.pref_static_field_key_about_license_1, R.string.pref_static_field_link_about_license_1)

        makeNotificationSettingsLink(R.string.pref_static_field_key_notification_settings)
    }


    override fun onResume() {
        super.onResume()

        updateNotificationSummary()
    }

    private fun makeLink(name: Int, link: Int) {
        val myActivity = activity
        myActivity?.let {
            val myPref = findPreference(getString(name)) as Preference
            myPref.onPreferenceClickListener = OnPreferenceClickListener {
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(link))).apply {
                    if (resolveActivity(myActivity.packageManager) != null) {
                        startActivity(this)
                    }
                }
                true
            }
        }
    }

    private fun makeNotificationSettingsLink(name: Int) {
        val myPref = findPreference(getString(name)) as Preference
        myPref.onPreferenceClickListener = OnPreferenceClickListener {
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

    private fun updateNotificationSummary() {
        val myActivity = activity
        myActivity?.let {
            notificationPref.summary = if (notificationScreenshotTakenChannelEnabled(myActivity)) {
                getString(R.string.notification_settings_on)
            } else {
                getString(R.string.notification_settings_off)
            }
        }
    }

    private fun updateDelaySummary(value: String) {
        delayPref.summary = delayPref.entries[delayPref.findIndexOfValue(value)]
    }

    private fun updateFileFormatSummary(value: String) {
        fileFormatPref.summary = fileFormatPref.entries[fileFormatPref.findIndexOfValue(value)]
    }

    private fun updateHideApp(hide: Boolean): Boolean {
        val myActivity = activity
        return myActivity?.let {
            val componentName = ComponentName(myActivity, MainActivity::class.java)
            try {
                myActivity.packageManager.setComponentEnabledSetting(
                    componentName,
                    if (hide) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                true
            } catch (e: Exception) {
                Log.e("SettingFragment", "setComponentEnabledSetting", e)
                Toast.makeText(context, myActivity.getString(R.string.toggle_app_icon_failed), Toast.LENGTH_LONG).show()
                prefManager.hideApp = !hide
                false
            }
        } ?: false
    }

    override fun onDestroy() {
        super.onDestroy()
        pref.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}
