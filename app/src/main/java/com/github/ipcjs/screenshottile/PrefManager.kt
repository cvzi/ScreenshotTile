package com.github.ipcjs.screenshottile

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Created by ipcjs on 2017/8/17.
 */

class PrefManager {
    val context: Context
    private val pref: SharedPreferences

    constructor(context: Context) : this(
        context,
        PreferenceManager.getDefaultSharedPreferences(context)
    )

    constructor(context: Context, pref: SharedPreferences) {
        this.context = context
        this.pref = pref
    }

    var delay: Int
        get() = pref.getString(
            context.getString(R.string.pref_key_delay),
            context.getString(R.string.setting_delay_value_default)
        )?.toIntOrNull() ?: 0
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_delay),
            value.toString()
        ).apply()

    var showCountDown: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_show_count_down), true)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_show_count_down),
            value
        ).apply()

    var hideApp: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_hide_app), false)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_hide_app),
            value
        ).apply()

    var fileFormat: String
        get() = pref.getString(
            context.getString(R.string.pref_key_file_format),
            context.getString(R.string.setting_file_format_value_default)
        ) ?: context.getString(R.string.setting_file_format_value_default)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_file_format),
            value
        ).apply()

    var broadcastSecret: String
        get() = pref.getString(
            context.getString(R.string.pref_key_broadcast_secret),
            context.getString(R.string.setting_broadcast_secret_value_default)
        ) ?: context.getString(R.string.setting_broadcast_secret_value_default)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_broadcast_secret),
            if (value.trim().isEmpty()) {
                context.getString(R.string.setting_broadcast_secret_value_default)
            } else {
                value
            }
        ).apply()
}
