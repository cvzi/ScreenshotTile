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
import com.github.cvzi.screenshottile.utils.adapters.TonesRecyclerViewAdapter
import com.github.cvzi.screenshottile.utils.image.nicePathFromUri
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_COPY_TO_CLIPBOARD
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_OPEN_IN_EXTERNAL_EDITOR
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_OPEN_IN_EXTERNAL_VIEWER
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_OPEN_IN_PHOTO_EDITOR
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_OPEN_IN_POST
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_OPEN_IN_POST_CROP
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_OPEN_SHARE
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_PLAY_TONE
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_SAVE_TO_STORAGE
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_SHOW_NOTIFICATION
import com.github.cvzi.screenshottile.utils.settings.PrefManager.Companion.POST_ACTION_SHOW_TOAST
import com.github.cvzi.screenshottile.utils.toastMessage
import com.github.cvzi.screenshottile.utils.ui.getLocalizedString
import com.github.cvzi.screenshottile.utils.ui.minPaddingFromInsets
import com.github.cvzi.screenshottile.utils.ui.realScreenSize
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

    private val prefManager = App.getInstance().prefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_post_settings
        )
        binding.setVariable(BR.strings, App.texts)
        binding.scrollView.minPaddingFromInsets()

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
        binding.switchAutoCrop.setOnCheckedChangeListener { _, isChecked ->
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

        val screenSize = realScreenSize(this)
        binding.textViewAutoCropScreenSize.text =
            "Screen height:\t${screenSize.y}px\nScreen width:\t${screenSize.x}px"

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
            binding.cardViewMIUIWarning.visibility = View.GONE
        }

    }

    override fun onResume() {
        super.onResume()

        loadSettings()
    }

    private fun disableUseSystemDefaults() {
        prefManager.useSystemDefaults = false
        loadSettings()
    }


    private fun setFloatingButtonLegacy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // For Android 9 and 10, the floating button action needs to be set to 'legacy method', since useNative is only supported on Android 11+
            prefManager.floatingButtonAction =
                getString(R.string.setting_floating_action_value_legacy)
            Log.v(TAG, "Set floating button action to legacy method")

            if (prefManager.voiceInteractionAction == getString(R.string.setting_voice_interaction_action_value_native)) {
                prefManager.voiceInteractionAction =
                    getString(R.string.setting_voice_interaction_action_value_provided)
            }

        }
        loadSettings()
    }


    private fun loadSettings() {

        val floatingButtonNativeMethodEnabled =
            prefManager.floatingButtonAction == getString(R.string.setting_floating_action_value_screenshot)

        Log.v(TAG, "floatingButtonNativeMethodEnabled: $floatingButtonNativeMethodEnabled")

        binding.textDescGeneral.text =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && ScreenshotAccessibilityService.instance != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                binding.textDescGeneral.setTextColor(getColor(R.color.colorAccent))
                "${getLocalizedString(R.string.use_native_screenshot_option_default)}\n\nScreenshots from the quick settings tile will not work with '${
                    getLocalizedString(
                        R.string.setting_post_actions
                    )
                }'}'. Switch to 'Legacy Method' or use the floating button or assistant action."
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.useNative && prefManager.useSystemDefaults) {
                binding.textDescGeneral.setTextColor(getColor(R.color.colorAccent))
                binding.textDescGeneral.setOnClickListener {
                    disableUseSystemDefaults()
                }
                "${getLocalizedString(R.string.use_native_screenshot_option_android11)}\n\nTap here to fix."
            } else {
                getLocalizedString(R.string.setting_post_actions_description)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {

            if (floatingButtonNativeMethodEnabled) {
                // For Android 9 and 10, the floating button action needs to be set to 'Legacy Method', since useNative is only supported on Android 11+
                binding.textDescGeneral.setTextColor(getColor(R.color.colorAccent))
                binding.textDescGeneral.text =
                    "${binding.textDescGeneral.text}\n\nSet the floating button action to '${
                        getLocalizedString(R.string.main_legacy_method_title)
                    }' to make '${getLocalizedString(R.string.setting_post_actions)}' work from the floating button.\n\nTap here to fix."
                binding.textDescGeneral.setOnClickListener {
                    setFloatingButtonLegacy()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && prefManager.voiceInteractionAction == getString(
                R.string.setting_voice_interaction_action_value_native
            ) && Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        ) {
            // For Android 9 and 10, the assist action shouldn't be set to "Native method"
            binding.textDescGeneral.setTextColor(getColor(R.color.colorAccent))
            binding.textDescGeneral.text =
                "${binding.textDescGeneral.text}\n\nAssistant action needs to be set to '${
                    getLocalizedString(R.string.setting_voice_interaction_action_value_legacy)
                }' or '${getLocalizedString(R.string.setting_voice_interaction_action_value_provided)}'\n\nTap here to fix."
            binding.textDescGeneral.setOnClickListener {
                setFloatingButtonLegacy()
            }
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
            POST_ACTION_SAVE_TO_STORAGE,
            POST_ACTION_SAVE_TO_STORAGE in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.switchShowToast,
            POST_ACTION_SHOW_TOAST,
            POST_ACTION_SHOW_TOAST in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.switchPlayTone,
            POST_ACTION_PLAY_TONE,
            POST_ACTION_PLAY_TONE in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.switchCopyClipboard,
            POST_ACTION_COPY_TO_CLIPBOARD,
            POST_ACTION_COPY_TO_CLIPBOARD in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.switchShowNotification,
            POST_ACTION_SHOW_NOTIFICATION,
            POST_ACTION_SHOW_NOTIFICATION in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInPost,
            POST_ACTION_OPEN_IN_POST,
            POST_ACTION_OPEN_IN_POST in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInPostCrop,
            POST_ACTION_OPEN_IN_POST_CROP,
            POST_ACTION_OPEN_IN_POST_CROP in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInPhotoEditor,
            POST_ACTION_OPEN_IN_PHOTO_EDITOR,
            POST_ACTION_OPEN_IN_PHOTO_EDITOR in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInExternalEditor,
            POST_ACTION_OPEN_IN_EXTERNAL_EDITOR,
            POST_ACTION_OPEN_IN_EXTERNAL_EDITOR in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenInExternalViewer,
            POST_ACTION_OPEN_IN_EXTERNAL_VIEWER,
            POST_ACTION_OPEN_IN_EXTERNAL_VIEWER in postScreenshotActions
        )
        initSimpleActionSwitch(
            binding.radioButtonOpenShare,
            POST_ACTION_OPEN_SHARE,
            POST_ACTION_OPEN_SHARE in postScreenshotActions
        )

        binding.switchAutoCrop.isChecked = prefManager.autoCropEnabled

        binding.editAutoCropLeft.setText(if (prefManager.autoCropLeft != 0) prefManager.autoCropLeft.toString() else "")
        binding.editAutoCropTop.setText(if (prefManager.autoCropTop != 0) prefManager.autoCropTop.toString() else "")
        binding.editAutoCropRight.setText(if (prefManager.autoCropRight != 0) prefManager.autoCropRight.toString() else "")
        binding.editAutoCropBottom.setText(if (prefManager.autoCropBottom != 0) prefManager.autoCropBottom.toString() else "")

        binding.spinnerAudioSink.setSelection(audioSinkKeys.indexOf(prefManager.soundNotificationSink))
        prefManager.soundNotificationDuration.also { ms ->
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
            val actionKey = compoundButton.getTag(R.id.tag_action_key) as String
            val postScreenshotActions = prefManager.postScreenshotActions
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
        prefManager.soundNotificationTone = "tone:$name"
        Sound.playTone()
    }
}
