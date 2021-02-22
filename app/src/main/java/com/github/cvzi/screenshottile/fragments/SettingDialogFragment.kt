package com.github.cvzi.screenshottile.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.ContainerActivity
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.screenshot

/**
 * Settings dialog appears on long press on the screenshot tile.
 * Offers delay options, open partial screenshot and open more settings.
 */
class SettingDialogFragment : DialogFragment(), DialogInterface.OnClickListener {
    private val pref by lazy { App.getInstance().prefManager }

    companion object {
        private const val TAG = "SettingDialogFragment"

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
            // make sure that a foreground service runs
            val screenshotTileService = ScreenshotTileService.instance
            if (screenshotTileService != null) {
                screenshotTileService.foreground()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceIntent = Intent(myActivity, ScreenshotTileService::class.java)
                serviceIntent.action = ScreenshotTileService.FOREGROUND_ON_START
                myActivity.startForegroundService(serviceIntent)
            }

            val entries = myActivity.resources.getTextArray(R.array.setting_delay_entries)
            val values = myActivity.resources.getStringArray(R.array.setting_delay_values)

            val checkedIndex = values.indexOf(pref.delay.toString())
            AlertDialog.Builder(activity, theme)
                .setSingleChoiceItems(entries, checkedIndex) { _, which: Int ->
                    val delay = values[which].toInt()
                    pref.delay = delay
                    if (delay == 0) {
                        screenshot(requireContext(), false)
                    } else {
                        App.getInstance().screenshot(context)
                    }
                    try {
                        dismiss()
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "AlertDialog.dismiss: IllegalStateException", e)
                    }
                }
                .setPositiveButton(if (pref.tileAction == getString(R.string.setting_tile_action_value_screenshot)) R.string.partial_screenshot else R.string.take_screenshot, this)
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
                    if (pref.tileAction == getString(R.string.setting_tile_action_value_screenshot)) {
                        App.getInstance().screenshotPartial(context)
                    } else {
                        App.getInstance().screenshot(context)
                    }
                }
                DialogInterface.BUTTON_NEUTRAL -> {
                    ContainerActivity.start(myActivity, SettingFragment::class.java)
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    // no-op
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        App.stopMediaProjection()
        ScreenshotTileService.instance?.background()
        activity?.finish()
    }
}
