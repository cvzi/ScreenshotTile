package com.github.cvzi.screenshottile.activities


import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.utils.nicePathFromUri

/**
 * Settings for what happens after a screenshot is taken
 */
class PostSettingsActivity : AppCompatActivity() {
    companion object {
        const val TAG = "PostSettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_settings)

        findViewById<Button>(R.id.buttonResetValues).setOnClickListener {
            findViewById<CompoundButton>(R.id.radioButtonEmpty).isChecked = true
            App.getInstance().prefManager.postScreenshotActionsReset()
            loadSettings()
        }

        findViewById<Button>(R.id.buttonSettings).setOnClickListener {
            SettingsActivity.start(this)
        }

        findViewById<Button>(R.id.buttonHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        loadSettings()
    }

    private fun loadSettings() {
        val prefManager = App.getInstance().prefManager

        findViewById<TextView>(R.id.textDescGeneral).apply {
            text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                getString(R.string.use_native_screenshot_option_default)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && prefManager.useSystemDefaults) {
                getString(R.string.use_native_screenshot_option_android11)
            } else {
                getString(R.string.setting_post_actions_description)
            }
        }

        findViewById<TextView>(R.id.textViewSaveImageLocation).text =
            if (prefManager.screenshotDirectory != null) {
                nicePathFromUri(prefManager.screenshotDirectory)
            } else {
                "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}"
            }

        val postScreenshotActions = prefManager.postScreenshotActions

        initSimpleActionSwitch(
            R.id.switchSaveToStorage,
            "saveToStorage",
            "saveToStorage" in postScreenshotActions
        )
        initSimpleActionSwitch(
            R.id.switchShowToast,
            "showToast",
            "showToast" in postScreenshotActions
        )
        initSimpleActionSwitch(
            R.id.switchShowNotification,
            "showNotification",
            "showNotification" in postScreenshotActions
        )
        initSimpleActionSwitch(
            R.id.radioButtonOpenInPost,
            "openInPost",
            "openInPost" in postScreenshotActions
        )
        initSimpleActionSwitch(
            R.id.radioButtonOpenInPostCrop,
            "openInPostCrop",
            "openInPostCrop" in postScreenshotActions
        )
        initSimpleActionSwitch(
            R.id.radioButtonOpenInPhotoEditor,
            "openInPhotoEditor",
            "openInPhotoEditor" in postScreenshotActions
        )
        initSimpleActionSwitch(
            R.id.radioButtonOpenInExternalEditor,
            "openInExternalEditor",
            "openInExternalEditor" in postScreenshotActions
        )
        initSimpleActionSwitch(
            R.id.radioButtonOpenInExternalViewer,
            "openInExternalViewer",
            "openInExternalViewer" in postScreenshotActions
        )
        initSimpleActionSwitch(
            R.id.radioButtonOpenShare,
            "openShare",
            "openShare" in postScreenshotActions
        )
    }

    private fun initSimpleActionSwitch(id: Int, actionKey: String, initValue: Boolean) {
        val switch = findViewById<CompoundButton>(id)
        switch.isChecked = initValue
        switch.setTag(R.id.tag_action_key, actionKey)
        switch.setOnCheckedChangeListener(onActionCheckedChange)
    }
    
    private val onActionCheckedChange =
        CompoundButton.OnCheckedChangeListener { compoundButton, isChecked ->
            val actionKey = compoundButton.getTag(R.id.tag_action_key) as String
            val postScreenshotActions = App.getInstance().prefManager.postScreenshotActions
            if (isChecked && actionKey !in postScreenshotActions) {
                postScreenshotActions.add(actionKey)
            } else if (!isChecked && actionKey in postScreenshotActions) {
                postScreenshotActions.remove(actionKey)
            }
            App.getInstance().prefManager.postScreenshotActions = postScreenshotActions
        }
}


