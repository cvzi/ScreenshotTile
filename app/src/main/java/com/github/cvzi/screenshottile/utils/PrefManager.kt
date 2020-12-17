package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import androidx.preference.PreferenceManager
import com.github.cvzi.screenshottile.R

/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
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

    var useNative: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_use_native), false)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_use_native),
            value
        ).apply()

    var useSystemDefaults: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_use_system_defaults), true)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_use_system_defaults),
            value
        ).apply()

    var floatingButton: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_floating_button), false)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_floating_button),
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

    var screenshotDirectory: String?
        get() {
            val defaultValue = context.getString(R.string.setting_storage_directory_value_default)
            val value = pref.getString(
                context.getString(R.string.pref_key_storage_directory),
                null
            )
            return if (value != defaultValue) {
                value
            } else {
                null
            }
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_storage_directory),
            if (value == null || value.trim().isEmpty()) {
                context.getString(R.string.setting_storage_directory_value_default)
            } else {
                value
            }
        ).apply()

    var screenshotCount: Int
        get() = pref.getString(
            context.getString(R.string.pref_key_screenshot_count),
            "0"
        )?.toIntOrNull() ?: 0
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_screenshot_count),
            value.toString()
        ).apply()

    var returnIfAccessibilityServiceEnabled: String?
        get() = pref.getString(
            context.getString(R.string.pref_key_return_if_accessibility),
            null
        )
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_return_if_accessibility),
            value
        ).apply()

    var floatingButtonHideAfter: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_key_floating_button_hide_after),
            false
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_floating_button_hide_after),
            value
        ).apply()

    var floatingButtonShowClose: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_key_floating_button_show_close),
            false
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_floating_button_show_close),
            value
        ).apply()

    var floatingButtonScale: Int
        get() {
            val d = pref.getString(
                context.getString(R.string.pref_key_floating_button_scale),
                "200"
            )?.toIntOrNull() ?: 200
            return if (d > 0) d else 1
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_scale),
            value.toString()
        ).apply()

    var floatingButtonPosition: Point
        get() {
            val s = pref.getString(
                context.getString(R.string.pref_key_floating_button_position),
                "0,0"
            ) ?: "0,0"
            val parts = s.split(",")
            if (parts.size != 2) {
                return Point(0, 0)
            }
            val x = parts[0].toIntOrNull() ?: 0
            val y = parts[1].toIntOrNull() ?: 0
            return Point(x, y)
        }
        set(point) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_position),
            "${point.x},${point.y}"
        ).apply()

    var floatingButtonShutter: Int
        get() = pref.getString(
            context.getString(R.string.pref_key_floating_button_shutter),
            "0"
        )?.toIntOrNull() ?: 0
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_shutter),
            value.toString()
        ).apply()
}
