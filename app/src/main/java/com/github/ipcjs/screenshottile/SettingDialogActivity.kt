package com.github.ipcjs.screenshottile

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
        ScreenshotTileService.instance?.let {
            App.acquireScreenshotPermission(this, it)
        }

    }
}
