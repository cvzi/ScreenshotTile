package com.github.ipcjs.screenshottile.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.github.ipcjs.screenshottile.App
import com.github.ipcjs.screenshottile.R

class SettingDialogFragment : DialogFragment(), DialogInterface.OnClickListener {
    private val pref by lazy { App.getInstance().prefManager }

    companion object {
        /**
         * Return new instance
         */
        fun newInstance(): SettingDialogFragment {
            return SettingDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val myActivity = activity
        return myActivity?.let {
            val entries = myActivity.resources.getTextArray(R.array.setting_delay_entries)
            val values = myActivity.resources.getStringArray(R.array.setting_delay_values)

            val checkedIndex = values.indexOf(pref.delay.toString())
            AlertDialog.Builder(activity, theme)
                .setSingleChoiceItems(entries, checkedIndex) { _, which: Int ->
                    val delay = values[which].toInt()
                    pref.delay = delay
                    App.getInstance().screenshot(context)
                    try {
                        dismiss()
                    } catch (e: IllegalStateException) {
                        Log.e("SettingsDialogFragment", "AlertDialog.dismiss: IllegalStateException", e)
                    }
                }
                .setPositiveButton(R.string.partial_screenshot, this)
                .setNeutralButton(R.string.more_setting, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setTitle(R.string.title_delay)
                .create()
        } ?: super.onCreateDialog(savedInstanceState)
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val myActivity = activity
        myActivity?.let {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    App.getInstance().screenshotPartial(context)
                }
                DialogInterface.BUTTON_NEUTRAL -> {
                    ContainerActivity.start(myActivity, SettingFragment::class.java)
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        activity?.finish()
    }
}
