package com.github.cvzi.screenshottile.activities


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.databinding.ActivityPostSettingsBinding
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.utils.MiuiCheck
import com.github.cvzi.screenshottile.utils.Sound
import com.github.cvzi.screenshottile.utils.Sound.Companion.allAudioSinks
import com.github.cvzi.screenshottile.utils.TonesRecyclerViewAdapter
import com.github.cvzi.screenshottile.utils.getLocalizedString
import com.github.cvzi.screenshottile.utils.nicePathFromUri
import com.github.cvzi.screenshottile.utils.toastMessage
import java.lang.Float.max


/**
 * Settings for what happens after a screenshot is taken
 */
class PostSettingsActivity : BaseAppCompatActivity() {
    companion object {
        const val TAG = "PostSettingsActivity"
    }

    private lateinit var binding: ActivityPostSettingsBinding
    private val audioSinkKeys = allAudioSinks.keys.toTypedArray()
    private lateinit var tonesRecyclerViewAdapter: TonesRecyclerViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_post_settings
        )
        binding.setVariable(BR.strings, App.texts)

        val prefManager = App.getInstance().prefManager

        binding.buttonResetValues.setOnClickListener {
            binding.radioButtonEmpty.isChecked = true
            prefManager.postScreenshotActionsReset()
            loadSettings()
        }

        binding.buttonSettings.setOnClickListener {
            SettingsActivity.start(this)
        }

        binding.buttonHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.switchAutoCrop.
            setOnCheckedChangeListener { _, isChecked ->
                Log.v(TAG, "switchAutoCrop: $isChecked")
                prefManager.autoCropEnabled = isChecked
            }
        binding.editAutoCropLeft.apply {
            addTextChangedListener { editable ->
                Log.v(TAG, "editAutoCropLeft: $editable")
                prefManager.autoCropLeft = editable.toString().toIntOrNull() ?: 0
                return@addTextChangedListener
            }
        }
        binding.editAutoCropTop.apply {
            addTextChangedListener { editable ->
                Log.v(TAG, "editAutoCropTop: $editable")
                prefManager.autoCropTop = editable.toString().toIntOrNull() ?: 0
                return@addTextChangedListener
            }
        }
        binding.editAutoCropRight.apply {
            addTextChangedListener { editable ->
                Log.v(TAG, "editAutoCropRight: $editable")
                prefManager.autoCropRight = editable.toString().toIntOrNull() ?: 0
                return@addTextChangedListener
            }
        }
        binding.editAutoCropBottom.apply {
            addTextChangedListener { editable ->
                Log.v(TAG, "editAutoCropBottom: $editable")
                prefManager.autoCropBottom = editable.toString().toIntOrNull() ?: 0
                return@addTextChangedListener
            }
        }

        // Create RecyclerView with all available tones
        binding.toneRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PostSettingsActivity)
            tonesRecyclerViewAdapter =
                TonesRecyclerViewAdapter(
                    this@PostSettingsActivity,
                    Sound.allTones,
                    Sound.selectedToneName()
                ) { _, _, name ->
                    onToneClick(name)
                }
            adapter = tonesRecyclerViewAdapter
        }

        // Create Spinner with all available audio sinks
        binding.spinnerAudioSink.apply {
            adapter =
                ArrayAdapter(
                    this@PostSettingsActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    audioSinkKeys
                )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    view: AdapterView<*>?,
                    itemView: View?,
                    position: Int,
                    id: Long
                ) {
                    prefManager.soundNotificationSink = audioSinkKeys[position]
                }

                override fun onNothingSelected(view: AdapterView<*>?) {
                    prefManager.soundNotificationSink = ""
                }

            }
        }

        binding.sliderAudioDuration.addOnChangeListener { _, value, _ ->
            val ms = value.toInt()
            prefManager.soundNotificationDuration = ms
            @SuppressLint("SetTextI18n")
            binding.textViewAudioDuration.text = "${ms}ms"
        }

        // Hide sound panel if playTone is deactivated
        binding.switchPlayTone.setOnClickListener { switch ->
            val cardView = binding.cardViewAudio
            if ((switch as? CompoundButton?)?.isChecked == false) {
                cardView.visibility = View.INVISIBLE
            } else {
                cardView.visibility = View.VISIBLE
                switch.postDelayed({
                    binding.scrollView.scrollTo(0, cardView.top + 100)
                }, 300)
            }
        }

        // Preview play button
        binding.imageButtonPlay.setOnClickListener {
            Sound.playTone()
        }


        // Xiaomi MIUI / HyperOS check
        if (MiuiCheck.isMiui()) {
            // Show warning about MIUI background/foreground blocking
            // "Open new windows while running in background"
            binding.cardViewMIUIWarning.visibility = View.VISIBLE
            binding.cardViewMIUIWarning.setOnClickListener {
                // Open MIUI / HyperOS security center for this app
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", packageName)
                    if (resolveActivity(packageManager) != null) {
                        startActivity(this)
                    } else {
                        Log.e(TAG, "No Xiami MIUI securitycenter found")
                        toastMessage(
                            "Please open the app info screen manually and change the permission",
                            ToastType.ACTIVITY
                        )
                    }
                }

            }
        } else {
            binding.cardViewMIUIWarning.visibility = View.INVISIBLE
        }

    }

    override fun onResume() {
        super.onResume()

        loadSettings()
    }

    private fun disableUseSystemDefaults() {
        App.getInstance().prefManager.useSystemDefaults = false
        binding.textDescGeneral.setTextColor(getColor(R.color.colorPrimary))
        binding.textDescGeneral.text = getLocalizedString(R.string.setting_post_actions_description)
    }

    private fun loadSettings() {
        val prefManager = App.getInstance().prefManager

        binding.textDescGeneral.text =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                binding.textDescGeneral.setTextColor(getColor(R.color.colorAccent))
                getLocalizedString(R.string.use_native_screenshot_option_default)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && prefManager.useSystemDefaults) {
                binding.textDescGeneral.setTextColor(getColor(R.color.colorAccent))
                binding.textDescGeneral.setOnClickListener {
                    disableUseSystemDefaults()
                }
                getLocalizedString(R.string.use_native_screenshot_option_android11)
            } else {
                getLocalizedString(R.string.setting_post_actions_description)
            }


        binding.textViewSaveImageLocation.text =
            if (prefManager.screenshotDirectory != null) {
                nicePathFromUri(prefManager.screenshotDirectory)
            } else {
                "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}"
            }

        val postScreenshotActions = prefManager.postScreenshotActions

        initSimpleActionSwitch(
            binding.switchSaveToStorage,
            "saveToStorage",
            "saveToStorage" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.switchShowToast,
            "showToast",
            "showToast" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.switchPlayTone,
            "playTone",
            "playTone" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.switchShowNotification,
            "showNotification",
            "showNotification" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInPost,
            "openInPost",
            "openInPost" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInPostCrop,
            "openInPostCrop",
            "openInPostCrop" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInPhotoEditor,
            "openInPhotoEditor",
            "openInPhotoEditor" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInExternalEditor,
            "openInExternalEditor",
            "openInExternalEditor" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInExternalViewer,
            "openInExternalViewer",
            "openInExternalViewer" in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenShare,
            "openShare",
            "openShare" in postScreenshotActions
        )

        binding.switchAutoCrop.isChecked = prefManager.autoCropEnabled

        binding.editAutoCropLeft.setText(if (prefManager.autoCropLeft != 0) prefManager.autoCropLeft.toString() else "")
        binding.editAutoCropTop.setText(if (prefManager.autoCropTop != 0) prefManager.autoCropTop.toString() else "")
        binding.editAutoCropRight.setText(if (prefManager.autoCropRight != 0) prefManager.autoCropRight.toString() else "")
        binding.editAutoCropBottom.setText(if (prefManager.autoCropBottom != 0) prefManager.autoCropBottom.toString() else "")

        binding.spinnerAudioSink.setSelection(audioSinkKeys.indexOf(App.getInstance().prefManager.soundNotificationSink))
        App.getInstance().prefManager.soundNotificationDuration.also { ms ->
            binding.sliderAudioDuration.value =
                max(ms.toFloat(), binding.sliderAudioDuration.valueFrom)
            @SuppressLint("SetTextI18n")
            binding.textViewAudioDuration.text = "${ms}ms"
        }
        binding.toneRecyclerView.scrollToPosition(tonesRecyclerViewAdapter.selectedIndex)
    }

    private fun initSimpleActionSwitch(
        compoundButton: CompoundButton,
        actionKey: String,
        initValue: Boolean
    ) {
        compoundButton.apply {
            isChecked = initValue
            setTag(R.id.tag_action_key, actionKey)
            setOnCheckedChangeListener(onActionCheckedChange)
        }
    }

    private val onActionCheckedChange =
        CompoundButton.OnCheckedChangeListener { compoundButton, isChecked ->
            val prefManager = App.getInstance().prefManager

            val actionKey = compoundButton.getTag(R.id.tag_action_key) as String
            val postScreenshotActions = App.getInstance().prefManager.postScreenshotActions
            if (isChecked && actionKey !in postScreenshotActions) {
                postScreenshotActions.add(actionKey)
            } else if (!isChecked && actionKey in postScreenshotActions) {
                postScreenshotActions.remove(actionKey)
            }
            prefManager.postScreenshotActions = postScreenshotActions

            /* Disable "use system defaults" */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && prefManager.useSystemDefaults) {
                disableUseSystemDefaults()
            }
        }

    private fun onToneClick(name: String) {
        App.getInstance().prefManager.soundNotificationTone = "tone:$name"
        Sound.playTone()
    }
}