package com.github.ipcjs.screenshottile

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.github.ipcjs.screenshottile.dialog.SettingDialogFragment

/**
 * Created by ipcjs on 2017/8/16.
 */

class SettingDialogActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            SettingDialogFragment.newInstance()
                .show(supportFragmentManager, SettingDialogFragment::class.java.name)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || !App.getInstance().prefManager.useNative || ScreenshotAccessibilityService.instance == null) {
            // Only request permission on long tile press if it's probably needed
            ScreenshotTileService.instance?.let {
                App.acquireScreenshotPermission(this, it)
            }
        }
    }
}
