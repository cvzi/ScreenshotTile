package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Point
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import com.github.cvzi.screenshottile.R
import java.io.File
import java.util.Date

/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */

class PrefManager(private val context: Context, private val pref: SharedPreferences) {
    companion object {
        const val TAG = "PrefManager.kt"
        val POST_ACTIONS = arrayOf(
            "saveToStorage", "showToast", "showNotification",
            // The following are exclusive in the sense that only the first selected will be run
            "openInPost", "openInPostCrop", "openInPhotoEditor",
            "openInExternalEditor", "openInExternalViewer", "openShare"
        )
        const val POST_ACTIONS_DEFAULT = "saveToStorage,showToast,showNotification"
        const val HISTORY_DELIMITER = "~///////~"
        const val HISTORY_ITEM_DELIMITER = "~////~"
    }

    constructor(context: Context) : this(
        context,
        PreferenceManager.getDefaultSharedPreferences(context)
    )

    private fun getBooleanRes(resId: Int, default: Boolean): Boolean {
        return try {
            context.resources.getBoolean(resId)
        } catch (e: Resources.NotFoundException) {
            default
        }
    }

    var delay: Int
        get() = pref.getString(
            context.getString(R.string.pref_key_delay),
            context.getString(R.string.setting_delay_value_default)
        )?.filter {
            it.isDigit()
        }?.toIntOrNull() ?: 0
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

    var tileLongPressAction: String
        get() = pref.getString(
            context.getString(R.string.pref_key_tile_long_press_action),
            context.getString(R.string.setting_tile_action_value_screenshot)
        ) ?: context.getString(R.string.setting_tile_action_value_options)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_tile_long_press_action),
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

    var formatQuality: Int
        get() = (pref.getString(context.getString(R.string.pref_key_format_quality), "-1")
            ?.toIntOrNull() ?: -1).coerceIn(-1, 100)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_format_quality),
            value.coerceIn(-1, 100).toString()
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
            if (value.isNullOrBlank()) {
                context.getString(R.string.setting_storage_directory_value_default)
            } else {
                value
            }
        ).apply()

    var fileNamePattern: String
        get() {
            val defaultValue = context.getString(R.string.setting_file_name_pattern_value_default)
            val value = pref.getString(
                context.getString(R.string.pref_key_file_name_pattern),
                defaultValue
            )
            return if (value.isNullOrBlank()) {
                this.fileNamePattern = defaultValue
                defaultValue
            } else {
                value
            }
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_file_name_pattern),
            value.ifBlank {
                context.getString(R.string.setting_file_name_pattern_value_default)
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

    var returnIfVoiceInteractionServiceEnabled: String?
        get() = pref.getString(
            context.getString(R.string.pref_key_return_if_voice_interaction),
            null
        )
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_return_if_voice_interaction),
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

    var floatingButtonCloseEmoji: String
        get() = pref.getString(
            context.getString(R.string.pref_key_floating_button_close_emoji),
            context.getString(R.string.close_buttons_default)
        ) ?: context.getString(R.string.close_buttons_default)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_close_emoji),
            value
        ).apply()

    var floatingButtonScale: Int
        get() {
            val d = pref.getString(
                context.getString(R.string.pref_key_floating_button_scale),
                context.getString(R.string.setting_floating_button_scale_default)
            )?.filter {
                it.isDigit()
            }?.toIntOrNull() ?: 200
            return if (d > 0) d else 200
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_scale),
            value.toString()
        ).apply()

    var floatingButtonAlpha: Float
        get() {
            val s = pref.getString(
                context.getString(R.string.pref_key_floating_button_alpha),
                context.getString(R.string.pref_floating_button_alpha_default)
            )?.replaceFirst(',', '.')?.filter {
                it.isDigit() || it == '.'
            }
            val f = s?.toFloatOrNull() ?: 1f
            return f.coerceIn(0f, 1f)
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_alpha),
            value.coerceIn(0f, 1f).toString()
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
        )?.filter {
            it.isDigit()
        }?.toIntOrNull() ?: 0
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_shutter),
            value.toString()
        ).apply()

    var floatingButtonDelay: Int
        get() = pref.getString(
            context.getString(R.string.pref_key_floating_button_delay),
            context.getString(R.string.setting_delay_value_default)
        )?.filter {
            it.isDigit()
        }?.toIntOrNull() ?: 0
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_delay),
            value.toString()
        ).apply()

    var floatingButtonAction: String
        get() = pref.getString(
            context.getString(R.string.pref_key_floating_action),
            context.getString(R.string.setting_floating_action_value_screenshot)
        ) ?: context.getString(R.string.setting_floating_action_value_screenshot)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_action),
            value
        ).apply()

    var floatingButtonColorTint: String?
        get() = pref.getString(
            context.getString(R.string.pref_key_floating_button_color_tint),
            null
        )
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_floating_button_color_tint),
            value ?: ""
        ).apply()

    var voiceInteractionAction: String
        get() = pref.getString(
            context.getString(R.string.pref_key_voice_interaction_action),
            context.getString(R.string.setting_voice_interaction_action_value_provided)
        ) ?: context.getString(R.string.setting_voice_interaction_action_value_provided)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_voice_interaction_action),
            value
        ).apply()

    var tileAction: String
        get() = pref.getString(
            context.getString(R.string.pref_key_tile_action),
            context.getString(R.string.setting_tile_action_value_screenshot)
        ) ?: context.getString(R.string.setting_tile_action_value_screenshot)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_tile_action),
            value
        ).apply()

    var darkTheme: String
        get() = pref.getString(
            context.getString(R.string.pref_key_dark_theme),
            context.getString(R.string.setting_dark_theme_value_default)
        ) ?: context.getString(R.string.setting_dark_theme_value_default)
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_dark_theme),
            value
        ).apply()

    var toasts: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_toasts), true)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_toasts),
            value
        ).apply()

    var naggingToasts: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_nagging_toasts), true)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_nagging_toasts),
            value
        ).apply()

    var successToasts: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_success_toasts), true)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_success_toasts),
            value
        ).apply()

    var errorToasts: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_error_toasts), true)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_error_toasts),
            value
        ).apply()

    var selectAreaShutterDelay: Long
        get() {
            val d = pref.getString(
                context.getString(R.string.pref_key_select_area_shutter_delay),
                context.getString(R.string.pref_select_area_shutter_delay_default)
            )?.toLongOrNull() ?: 0
            return if (d >= 0) d else 0
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_select_area_shutter_delay),
            value.toString()
        ).apply()

    var originalAfterPermissionDelay: Long
        get() {
            val d = pref.getString(
                context.getString(R.string.pref_key_original_after_permission_delay),
                context.getString(R.string.pref_original_after_permission_delay_default)
            )?.toLongOrNull() ?: 300
            return if (d >= 0) d else 0
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_original_after_permission_delay),
            value.toString()
        ).apply()

    var failedVirtualDisplayDelay: Long
        get() {
            val d = pref.getString(
                context.getString(R.string.pref_key_failed_virtual_display_delay),
                context.getString(R.string.pref_failed_virtual_display_delay_default)
            )?.toLongOrNull() ?: 0
            return if (d >= 0) d else 0
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_failed_virtual_display_delay),
            value.toString()
        ).apply()


    private var fileNamesRecent: Array<String>
        get() {
            return pref.getString(
                context.getString(R.string.pref_key_file_names_recent),
                ""
            )?.split("\n")?.filter { it.isNotBlank() }?.toTypedArray() ?: arrayOf()
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_file_names_recent),
            value.joinToString("\n")
        ).apply()

    private fun cleanFileName(name: String): String {
        return name.removeSuffix(".png")
            .removeSuffix(".gif")
            .removeSuffix(".jpg")
            .removeSuffix(".jpeg")
            .removeSuffix(".webp")
            .replace("\n", "")
    }

    fun addRecentFileName(name: String) {
        val cleanName = cleanFileName(name)
        fileNamesRecent = fileNamesRecent.toMutableList().apply {
            while (cleanName in this) {
                remove(cleanName)
            }
            add(0, cleanName)
        }.toTypedArray()
    }

    private fun removeRecentFileName(index: Int) {
        fileNamesRecent = fileNamesRecent.toMutableList().apply {
            if (index < size) {
                removeAt(index)
            }
        }.toTypedArray()
    }

    private var fileNamesStarred: Array<String>
        get() {
            return pref.getString(
                context.getString(R.string.pref_key_file_names_starred),
                ""
            )?.split("\n")?.filter { it.isNotBlank() }?.toTypedArray() ?: arrayOf()
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_file_names_starred),
            value.joinToString("\n")
        ).apply()

    fun addStarredFileName(name: String) {
        val cleanName = cleanFileName(name)
        if (cleanName !in fileNamesStarred) {
            fileNamesStarred += cleanName
        }
    }

    private fun removeStarredFileName(index: Int) {
        fileNamesStarred = fileNamesStarred.toMutableList().apply {
            if (index < size) {
                removeAt(index)
            }
        }.toTypedArray()
    }

    fun getFileNameSuggestions(): Array<FileNameSuggestion> {
        return (fileNamesStarred.mapIndexed { index, value ->
            FileNameSuggestion(value, true, index)
        } + fileNamesRecent.mapIndexed { index, value ->
            FileNameSuggestion(value, false, index)
        }).toTypedArray()
    }

    fun removeFileName(fileNameSuggestion: FileNameSuggestion) {
        return if (fileNameSuggestion.starred) {
            removeStarredFileName(fileNameSuggestion.dataIndex)
        } else {
            removeRecentFileName(fileNameSuggestion.dataIndex)
        }
    }


    private var recentFolders: Array<String>
        get() {
            return pref.getString(
                context.getString(R.string.pref_key_recent_folders),
                ""
            )?.split("\n\n")?.filter { it.isNotBlank() }?.toTypedArray() ?: arrayOf()
        }
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_recent_folders),
            value.joinToString("\n\n")
        ).apply()


    private fun addRecentFolder(uriStr: String) {
        val clean = uriStr.replace("\n\n", "")
        recentFolders = recentFolders.toMutableList().apply {
            while (clean in this) {
                remove(clean)
            }
            add(0, clean)
        }.toTypedArray()
    }

    fun addRecentFolder(uri: Uri) {
        return addRecentFolder(uri.toString())
    }

    fun getRecentFolders(): Array<RecentFolder> {
        return (recentFolders.mapIndexed { index, value ->
            RecentFolder(value, index)
        }).toTypedArray()
    }

    fun removeRecentFolder(recentFolder: RecentFolder) {
        val index = recentFolder.dataIndex
        recentFolders = recentFolders.toMutableList().apply {
            if (index < size) {
                removeAt(index)
            }
        }.toTypedArray()
    }

    var notificationActions: MutableSet<String>
        get() = pref.getStringSet(
            context.getString(R.string.pref_key_notification_actions),
            context.resources.getStringArray(R.array.setting_notification_actions_default_values)
                .toMutableSet()
        ) ?: arrayOf(
            context.getString(R.string.setting_notification_action_share),
            context.getString(R.string.setting_notification_action_edit),
            context.getString(R.string.setting_notification_action_delete)
        ).toMutableSet()
        set(value) = pref.edit()
            .putStringSet(context.getString(R.string.pref_key_notification_actions), value).apply()

    var postScreenshotActions: ArrayList<String>
        get() {
            val raw = pref.getString(
                context.getString(R.string.pref_key_post_screenshot_actions), POST_ACTIONS_DEFAULT
            ) ?: POST_ACTIONS_DEFAULT
            val result = ArrayList<String>()
            raw.split(",").forEach {
                val action = it.trim()
                if (action in POST_ACTIONS) {
                    result.add(action)
                }
            }
            return result
        }
        set(values) = pref.edit()
            .putString(
                context.getString(R.string.pref_key_post_screenshot_actions),
                LinkedHashSet(values).joinToString(",")
            ).apply()

    fun postScreenshotActionsReset() =
        pref.edit()
            .putString(
                context.getString(R.string.pref_key_post_screenshot_actions),
                POST_ACTIONS_DEFAULT
            ).apply()


    data class ScreenshotHistoryItem(val uri: Uri, val date: Date, val file: File?)

    var screenshotHistory: ArrayList<ScreenshotHistoryItem>
        get() {
            val result = ArrayList<ScreenshotHistoryItem>()
            try {
                val raw = pref.getString(
                    context.getString(R.string.pref_key_screenshot_history), ""
                ) ?: ""
                raw.split(HISTORY_DELIMITER).forEach {
                    if (it.isNotBlank()) {
                        val parts = it.split(HISTORY_ITEM_DELIMITER)
                        if (parts.size > 1) {
                            val uri = Uri.parse(parts[0])
                            val date = Date(parts[1].toLong())
                            val file: File? = if (parts.size > 2) {
                                File(parts[2])
                            } else {
                                null
                            }
                            result.add(ScreenshotHistoryItem(uri, date, file))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceToString())
            }
            return result
        }
        set(values) {
            try {
                val size = values.size
                pref.edit()
                    .putString(
                        context.getString(R.string.pref_key_screenshot_history),
                        values.filterIndexed { index, _ ->
                            // if more than 100 items, delete first 30 items
                            size < 100 || index > 30
                        }.joinToString(HISTORY_DELIMITER) {
                            if (it.file != null) {
                                "${
                                    it.uri.toString().replace(HISTORY_ITEM_DELIMITER, "")
                                }$HISTORY_ITEM_DELIMITER${
                                    it.date.time
                                }$HISTORY_ITEM_DELIMITER${
                                    it.file.toString().replace(HISTORY_ITEM_DELIMITER, "")
                                }"
                            } else {
                                "${
                                    it.uri.toString().replace(HISTORY_ITEM_DELIMITER, "")
                                }$HISTORY_ITEM_DELIMITER${
                                    it.date.time
                                }"
                            }
                        }
                    ).apply()
            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceToString())
            }
        }

    fun screenshotHistoryAdd(item: ScreenshotHistoryItem) {
        if (!keepScreenshotHistory) return
        val t = screenshotHistory
        t.add(item)
        screenshotHistory = t
    }

    fun screenshotHistoryRemove(uri: Uri) {
        screenshotHistory = ArrayList<ScreenshotHistoryItem>(screenshotHistory.filter {
            it.uri != uri
        })
    }

    fun screenshotHistoryRemove(file: File) {
        screenshotHistory = ArrayList<ScreenshotHistoryItem>(screenshotHistory.filter {
            it.file == null || it.file != file
        })
    }

    var keepAppDataMax: Int
        get() = pref.getString(
            context.getString(R.string.pref_key_keep_app_data_max),
            context.getString(R.string.pref_keep_app_data_max_default)
        )?.toIntOrNull() ?: 30
        set(value) = pref.edit().putString(
            context.getString(R.string.pref_key_keep_app_data_max),
            value.toString()
        ).apply()

    var keepScreenshotHistory: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_keep_screenshot_history), true)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_keep_screenshot_history),
            value
        ).apply()

    var preventIfLocked: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_prevent_if_locked), false)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_prevent_if_locked),
            value
        ).apply()

    var photoEditorOverwriteFile: Boolean
        get() = pref.getBoolean(context.getString(R.string.pref_key_pe_overwrite_file), false)
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_pe_overwrite_file),
            value
        ).apply()

    var photoEditorAutoRotateLandscape: Boolean
        get() = pref.getBoolean(
            context.getString(R.string.pref_key_pe_auto_rotate_landscape),
            getBooleanRes(R.bool.pref_pe_auto_rotate_landscape_default, true)
        )
        set(value) = pref.edit().putBoolean(
            context.getString(R.string.pref_key_pe_auto_rotate_landscape),
            value
        ).apply()

}
