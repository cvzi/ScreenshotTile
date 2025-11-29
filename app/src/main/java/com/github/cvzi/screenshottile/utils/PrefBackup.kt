package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import org.json.JSONArray
import org.json.JSONObject


class PrefBackup {
    companion object {
        const val TAG = "PrefBackup"

        fun exportPrefsToJson(context: Context): String {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val all = prefs.all  // Map<String, *>

            val root = JSONObject()
            root.put("description", "ScreenshotTile settings backup")
            root.put("version", 1)
            root.put("appVersion", "${BuildConfig.VERSION_CODE}:v${BuildConfig.VERSION_NAME}")
            root.put("time", java.time.LocalDateTime.now().toString())
            root.put(
                "device",
                "${Build.MANUFACTURER} ${Build.BRAND} ${Build.VERSION.RELEASE} ${Build.MODEL} sdk${Build.VERSION.SDK_INT}"
            )

            val prefsObj = JSONObject()

            for ((key, value) in all) {
                val entry = JSONObject()

                when (value) {
                    is Int -> {
                        entry.put("type", "int")
                        entry.put("value", value)
                    }

                    is Long -> {
                        entry.put("type", "long")
                        entry.put("value", value)
                    }

                    is Float -> {
                        entry.put("type", "float")
                        entry.put("value", value.toDouble()) // JSON has no float
                    }

                    is Boolean -> {
                        entry.put("type", "boolean")
                        entry.put("value", value)
                    }

                    is String -> {
                        entry.put("type", "string")
                        entry.put("value", value)
                    }

                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val stringSet = value as? Set<String>
                        if (stringSet != null) {
                            entry.put("type", "string_set")
                            val arr = JSONArray()
                            stringSet.forEach { arr.put(it) }
                            entry.put("value", arr)
                        } else {
                            // Ignore non-string sets
                            Log.e(TAG, "Non-string set in preferences for key $key")
                            continue
                        }
                    }

                    else -> {
                        Log.e(
                            TAG,
                            "Unknown type in preferences for key $key: ${value?.javaClass?.name}"
                        )
                        continue
                    }
                }

                prefsObj.put(key, entry)
            }

            root.put("prefs", prefsObj)
            return root.toString(2)
        }

        fun importPrefsFromJson(context: Context, json: String) {
            val root = JSONObject(json)
            val prefsObj = root.getJSONObject("prefs")

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit {
                clear()

                val keys = prefsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val entry = prefsObj.getJSONObject(key)
                    when (val type = entry.getString("type")) {
                        "int" -> putInt(key, entry.getInt("value"))
                        "long" -> putLong(key, entry.getLong("value"))
                        "float" -> putFloat(key, entry.getDouble("value").toFloat())
                        "boolean" -> putBoolean(key, entry.getBoolean("value"))
                        "string" -> putString(key, entry.getString("value"))
                        "string_set" -> {
                            val arr = entry.getJSONArray("value")
                            val set = mutableSetOf<String>()
                            for (i in 0 until arr.length()) {
                                set.add(arr.getString(i))
                            }
                            putStringSet(key, set)
                        }

                        else -> {
                            // Unknown type, ignore
                            Log.e(TAG, "Unknown type in json $type")
                        }
                    }
                }

            }
        }

        fun resetToDefaults(context: Context) {
            PreferenceManager.getDefaultSharedPreferences(context).edit { clear() }
            PreferenceManager.setDefaultValues(context, R.xml.pref, true)
            PreferenceManager.setDefaultValues(context, R.xml.pref_advanced, true)
        }

    }
}