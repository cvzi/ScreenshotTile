package com.github.ipcjs.screenshottile.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.preference.PreferenceManager
import com.github.ipcjs.screenshottile.DelayScreenshotActivity
import com.github.ipcjs.screenshottile.R

class SettingDialogFragment : DialogFragment() {
    companion object {
        fun newInstance(): SettingDialogFragment {
            return SettingDialogFragment()
        }

        fun which2delay(which: Int): Int {
            return when (which) {
                0 -> 0
                1 -> 1
                2 -> 2
                3 -> 5
                else -> 0
            }
        }

        const val PREF_DELAYS = "pref_delays";
    }

    private val pref by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val checkedIndex = pref.getInt(PREF_DELAYS, 0)
        return AlertDialog.Builder(activity, theme)
                .setSingleChoiceItems(R.array.setting_delays, checkedIndex, { dialog: DialogInterface, which: Int ->
                    pref.edit().putInt(PREF_DELAYS, which).apply()
                    val delay = which2delay(which)
                    DelayScreenshotActivity.start(context, delay)
                    dismiss()
                })
                .setTitle(R.string.title_delay)
                .create()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        activity.finish()
    }
}