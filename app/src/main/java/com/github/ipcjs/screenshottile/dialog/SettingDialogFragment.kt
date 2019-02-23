package com.github.ipcjs.screenshottile.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import com.github.ipcjs.screenshottile.App
import com.github.ipcjs.screenshottile.PrefManager
import com.github.ipcjs.screenshottile.R

class SettingDialogFragment : DialogFragment(), DialogInterface.OnClickListener {
    private val pref by lazy { PrefManager(context) }

    companion object {
        /**
         * Return new instance
         */
        fun newInstance(): SettingDialogFragment {
            return SettingDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val entries = context.resources.getTextArray(R.array.setting_delay_entries)
        val values = context.resources.getStringArray(R.array.setting_delay_values)
        val checkedIndex = values.indexOf(pref.delay.toString())
        return AlertDialog.Builder(activity, theme)
                .setSingleChoiceItems(entries, checkedIndex) { _, which: Int ->
                    val delay = values[which].toInt()
                    pref.delay = delay
                    App.getInstance().screenshot(context)
                    dismiss()
                }
//                .setPositiveButton(android.R.string.ok, this)
                .setNeutralButton(R.string.more_setting, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setTitle(R.string.title_delay)
                .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
            }
            DialogInterface.BUTTON_NEUTRAL -> {
                ContainerActivity.start(context, SettingFragment::class.java)
            }
            DialogInterface.BUTTON_NEGATIVE -> {
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        activity?.finish()
    }
}
