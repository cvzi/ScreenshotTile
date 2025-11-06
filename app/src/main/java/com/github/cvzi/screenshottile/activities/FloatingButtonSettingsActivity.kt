package com.github.cvzi.screenshottile.activities


import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.ActivityFloatingButtonSettingsBinding
import com.github.cvzi.screenshottile.services.FloatingTileService
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import com.github.cvzi.screenshottile.services.ScreenshotTileService
import com.github.cvzi.screenshottile.utils.ShutterCollection
import com.github.cvzi.screenshottile.utils.fillTextHeight
import com.github.cvzi.screenshottile.utils.parseColorString
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.createBitmap


/**
 * Change settings of the floating button
 */
@RequiresApi(Build.VERSION_CODES.P)
class FloatingButtonSettingsActivity : BaseAppCompatActivity() {
    companion object {
        const val TAG = "FloatingButtonSettings"

        /**
         * Get intent
         */
        fun newIntent(ctx: Context): Intent =
            Intent(ctx, FloatingButtonSettingsActivity::class.java)

        /**
         * Start activity
         */
        fun start(ctx: Context) = ctx.startActivity(newIntent(ctx))

        /**
         * Start activity from service
         */
        fun startNewTask(ctx: Context) = ctx.startActivity(newIntent(ctx).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private var savedInstanceState: Bundle? = null
    private val prefManager = App.getInstance().prefManager

    private val hsv = floatArrayOf(360.0f, 1.0f, 0.5f)

    private lateinit var switchFloatingButtonEnabled: SwitchMaterial
    private lateinit var imageViewFloatingButton: ImageView
    private lateinit var textViewCloseButton: TextView
    private lateinit var switchFloatingButtonColorTint: SwitchMaterial
    private lateinit var switchFloatingButtonAlpha: SwitchMaterial
    private lateinit var radioGroupAction: RadioGroup
    private lateinit var radioGroupShutterTheme: RadioGroup
    private lateinit var switchFloatingButtonDelay: SwitchMaterial
    private lateinit var editTextFloatingButtonDelay: EditText
    private lateinit var seekBarFloatingButtonTintH: SeekBar
    private lateinit var seekBarFloatingButtonTintV: SeekBar
    private lateinit var seekBarFloatingButtonAlpha: SeekBar
    private lateinit var seekBarFloatingButtonScale: SeekBar

    private lateinit var shutterCollection: ShutterCollection

    private lateinit var binding: ActivityFloatingButtonSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityFloatingButtonSettingsBinding>(this, R.layout.activity_floating_button_settings)
        binding.setVariable(BR.strings, App.texts)

        shutterCollection = ShutterCollection(this, R.array.shutters, R.array.shutter_names)
        switchFloatingButtonEnabled = binding.switchFloatingButtonEnabled
        imageViewFloatingButton = binding.imageViewFloatingButton
        textViewCloseButton = binding.imageViewCloseButton
        switchFloatingButtonColorTint = binding.switchFloatingButtonColorTint
        switchFloatingButtonAlpha = binding.switchFloatingButtonAlpha
        radioGroupAction = binding.radioGroupAction
        radioGroupShutterTheme = binding.radioGroupShutterTheme
        switchFloatingButtonDelay = binding.switchFloatingButtonDelay
        editTextFloatingButtonDelay = binding.editTextFloatingButtonDelay
        seekBarFloatingButtonTintH = binding.seekBarFloatingButtonTintH
        seekBarFloatingButtonTintV = binding.seekBarFloatingButtonTintV
        seekBarFloatingButtonAlpha = binding.seekBarFloatingButtonAlpha
        seekBarFloatingButtonScale = binding.seekBarFloatingButtonScale

        switchFloatingButtonEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButton = isChecked
            if (isChecked && ScreenshotAccessibilityService.instance == null) {
                ScreenshotAccessibilityService.openAccessibilitySettings(this, MainActivity.TAG)
            } else {
                ScreenshotAccessibilityService.instance?.updateFloatingButton()
            }
        }

        radioGroupAction.setOnCheckedChangeListener { _, checkedId ->
            prefManager.floatingButtonAction = getString(
                when (checkedId) {
                    R.id.radioButtonActionPartial -> {
                        R.string.setting_floating_action_value_partial
                    }

                    R.id.radioButtonActionLegacy -> {
                        R.string.setting_floating_action_value_legacy
                    }

                    else -> { // R.id.radioButtonActionNative
                        R.string.setting_floating_action_value_screenshot
                    }
                }
            )
        }

        binding.radioGroupTapType.setOnCheckedChangeListener { _, checkedId ->
            prefManager.floatingButtonTapType = when (checkedId) {
                    R.id.radioButtonTapTypeDouble -> {
                        ScreenshotAccessibilityService.TAP_TYPE_DOUBLE
                    }
                    else -> { // R.id.radioButtonTapTypeSingle
                        ScreenshotAccessibilityService.TAP_TYPE_SINGLE
                    }
                }
        }

        binding.switchFloatingButtonHideAfter.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButtonHideAfter = isChecked
        }

        switchFloatingButtonDelay.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButtonDelay = if (isChecked) {
                editTextFloatingButtonDelay.text.toString().toIntOrNull() ?: 0
            } else {
                0
            }
        }
        editTextFloatingButtonDelay.doAfterTextChanged {
            val value = it.toString().toIntOrNull() ?: 0
            prefManager.floatingButtonDelay = value
            switchFloatingButtonDelay.isChecked = value > 0
        }


        binding.linearLayoutPreview.background =
            checkeredBackground().toDrawable(resources).apply {
                setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                setTargetDensity(resources.displayMetrics.densityDpi * 2)
            }

        textViewCloseButton.layoutParams =
            LinearLayout.LayoutParams(textViewCloseButton.layoutParams).apply {
                height = LayoutParams.MATCH_PARENT
            }

        seekBarFloatingButtonAlpha.max = 100
        seekBarFloatingButtonAlpha.setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
            val alpha = progress / 100f
            imageViewFloatingButton.alpha = alpha
            textViewCloseButton.alpha = alpha
            prefManager.floatingButtonAlpha = alpha
            switchFloatingButtonAlpha.isChecked = alpha < 0.98
        })
        switchFloatingButtonAlpha.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val alpha = seekBarFloatingButtonAlpha.progress / 100f
                imageViewFloatingButton.alpha = alpha
                textViewCloseButton.alpha = alpha
                prefManager.floatingButtonAlpha = alpha

            } else {
                prefManager.floatingButtonAlpha = 1f
                imageViewFloatingButton.alpha = 1f
                textViewCloseButton.alpha = 1f
            }
            ScreenshotAccessibilityService.instance?.updateFloatingButton(true)
        }

        seekBarFloatingButtonTintH.max = 3600
        seekBarFloatingButtonTintV.max = 500

        seekBarFloatingButtonTintH.setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
            onSelectColor(h = 0.1f * progress, v = null)
        })
        seekBarFloatingButtonTintV.setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
            onSelectColor(h = null, v = 0.5f + 0.001f * progress)
        })

        switchFloatingButtonColorTint.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                onSelectColor(
                    h = 0.1f * seekBarFloatingButtonTintH.progress,
                    v = 0.5f + 0.001f * seekBarFloatingButtonTintV.progress
                )
            } else {
                imageViewFloatingButton.drawable.setTintList(null)
                prefManager.floatingButtonColorTint = ""
                ScreenshotAccessibilityService.setShutterDrawable(
                    this, imageViewFloatingButton, shutterCollection.current().normal
                )
            }
            ScreenshotAccessibilityService.instance?.updateFloatingButton(true)
        }
        binding.switchFloatingButtonShowClose.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButtonShowClose = isChecked
            updateCloseButton()
            ScreenshotAccessibilityService.instance?.updateFloatingButton(true)
        }

        seekBarFloatingButtonScale.apply {
            max = 1000
            min = 1
            setOnSeekBarChangeListener(OnSeekBarProgress { progress ->
                prefManager.floatingButtonScale = progress
                updatePreviewButton()
            })
        }

        radioGroupShutterTheme.setOnCheckedChangeListener { group, checkedId ->
            val button = group.findViewById<RadioButton?>(checkedId)
            if (button != null) {
                val shutter = button.tag as ShutterCollection.Shutter
                prefManager.floatingButtonShutter = shutter.index
                ScreenshotAccessibilityService.setShutterDrawable(
                    this, imageViewFloatingButton, shutterCollection.current().normal
                )
            }
            ScreenshotAccessibilityService.instance?.updateFloatingButton(true)
        }

        binding.buttonOpenPackageFilter.setOnClickListener {
            FloatingButtonFilterActivity.start(this)
        }

        binding.buttonRefresh.setOnClickListener {
            ScreenshotAccessibilityService.instance?.updateFloatingButton(true)
        }
        binding.buttonMoreSettings.setOnClickListener {
            SettingsActivity.start(this)
        }

        binding.switchFloatingButtonOnUnLocked.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButtonWhenUnLocked = isChecked
            ScreenshotAccessibilityService.instance?.updateLockscreenSetting()
        }
        binding.switchFloatingButtonOnLocked.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButtonWhenLocked = isChecked
            ScreenshotAccessibilityService.instance?.updateLockscreenSetting()
        }
        binding.switchFloatingButtonOnPortrait.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButtonWhenPortrait = isChecked
            ScreenshotAccessibilityService.instance?.updateFloatingButton(true)
        }
        binding.switchFloatingButtonOnLandscape.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButtonWhenLandscape = isChecked
            ScreenshotAccessibilityService.instance?.updateFloatingButton(true)
        }
        binding.switchFloatingButtonOnQuickSettings.setOnCheckedChangeListener { _, isChecked ->
            prefManager.floatingButtonWhenQuickSettings = isChecked
        }
    }

    private fun onSelectColor(h: Float?, v: Float?) {
        hsv[0] = h ?: hsv[0]
        hsv[2] = v ?: hsv[2]
        val color = Color.HSVToColor(hsv)
        imageViewFloatingButton.drawable.setTint(color)
        prefManager.floatingButtonColorTint = "i$color"
        switchFloatingButtonColorTint.isChecked = true
    }

    override fun onResume() {
        super.onResume()

        switchFloatingButtonEnabled.isChecked =
            ScreenshotAccessibilityService.instance != null && prefManager.floatingButton

        findViewById<RadioButton>(
            when (prefManager.floatingButtonAction) {
                getString(R.string.setting_floating_action_value_partial) -> {
                    R.id.radioButtonActionPartial
                }

                getString(R.string.setting_floating_action_value_legacy) -> {
                    R.id.radioButtonActionLegacy
                }

                else -> R.id.radioButtonActionNative
            }
        ).isChecked = true

        findViewById<RadioButton>(
            when (prefManager.floatingButtonTapType) {
                ScreenshotAccessibilityService.TAP_TYPE_DOUBLE -> {
                    R.id.radioButtonTapTypeDouble
                }
                else -> R.id.radioButtonTapTypeSingle
            }
        ).isChecked = true

        binding.switchFloatingButtonHideAfter.isChecked =
            prefManager.floatingButtonHideAfter

        editTextFloatingButtonDelay.setText(prefManager.floatingButtonDelay.toString())
        switchFloatingButtonDelay.isChecked = prefManager.floatingButtonDelay > 0

        val alpha = prefManager.floatingButtonAlpha
        val colorInt = parseColorString(prefManager.floatingButtonColorTint)
        if (colorInt != null) {
            Color.colorToHSV(colorInt, hsv)
            seekBarFloatingButtonTintH.progress = (10 * hsv[0]).toInt()
            seekBarFloatingButtonTintV.progress = (1000 * (hsv[2] - 0.5f)).toInt()
            switchFloatingButtonColorTint.isChecked = true
        } else {
            switchFloatingButtonColorTint.isChecked = false
        }

        ScreenshotAccessibilityService.setShutterDrawable(
            this, imageViewFloatingButton, shutterCollection.current().normal
        )

        binding.switchFloatingButtonShowClose.isChecked =
            prefManager.floatingButtonShowClose

        radioGroupShutterTheme.removeAllViews()
        val selectedShutter = shutterCollection.current()
        for (shutter in shutterCollection.list) {
            radioGroupShutterTheme.addView(RadioButton(this).apply {
                isChecked = shutter == selectedShutter
                text = shutter.name
                tag = shutter
                id = View.generateViewId()
            })
        }

        imageViewFloatingButton.alpha = alpha
        textViewCloseButton.alpha = alpha

        seekBarFloatingButtonScale.progress = prefManager.floatingButtonScale

        seekBarFloatingButtonAlpha.progress = (100 * alpha).toInt()
        switchFloatingButtonAlpha.isChecked = alpha < 0.98

        binding.switchFloatingButtonOnUnLocked.isChecked = prefManager.floatingButtonWhenUnLocked
        binding.switchFloatingButtonOnLocked.isChecked = prefManager.floatingButtonWhenLocked
        binding.switchFloatingButtonOnPortrait.isChecked = prefManager.floatingButtonWhenPortrait
        binding.switchFloatingButtonOnLandscape.isChecked = prefManager.floatingButtonWhenLandscape
        binding.switchFloatingButtonOnQuickSettings.isChecked =
            prefManager.floatingButtonWhenQuickSettings

        binding.tableRowQuickSettingsWarning.visibility =
            if (!prefManager.floatingButtonWhenQuickSettings && FloatingTileService.instance == null && ScreenshotTileService.instance == null) {
                View.VISIBLE
            } else {
                View.GONE
            }

        restoreSavedInstanceValues()

        Handler(Looper.getMainLooper()).postDelayed({
            updatePreviewButton()
        }, 100)
    }

    override fun onPause() {
        super.onPause()
        ScreenshotAccessibilityService.instance?.updateFloatingButton(forceRedraw = true)
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
    }

    private fun updatePreviewButton() {
        // Scale button
        imageViewFloatingButton.handler?.removeCallbacksAndMessages(imageViewFloatingButton)
        val scale = prefManager.floatingButtonScale
        imageViewFloatingButton.handler?.postDelayed({
            imageViewFloatingButton.layoutParams = imageViewFloatingButton.layoutParams.apply {
                width = LayoutParams.MATCH_PARENT
                height = LayoutParams.WRAP_CONTENT
            }
        }, imageViewFloatingButton, 300)

        imageViewFloatingButton.handler?.postDelayed({
            imageViewFloatingButton.layoutParams = imageViewFloatingButton.layoutParams.apply {
                width = imageViewFloatingButton.measuredHeight * scale / 100 + 20
                height = imageViewFloatingButton.measuredHeight * scale / 100
            }
        }, imageViewFloatingButton, 500)

        updateCloseButton()
    }

    private fun updateCloseButton() {
        if (prefManager.floatingButtonShowClose && prefManager.floatingButtonCloseEmoji.isNotBlank()) {
            textViewCloseButton.text = prefManager.floatingButtonCloseEmoji
            textViewCloseButton.alpha = prefManager.floatingButtonAlpha
            imageViewFloatingButton.handler?.postDelayed({
                textViewCloseButton.run {
                    fillTextHeight(
                        this,
                        imageViewFloatingButton.measuredHeight * 3 / 4,
                        imageViewFloatingButton.measuredHeight * 0.8f
                    )
                }
            }, imageViewFloatingButton, 1000)
        } else {
            textViewCloseButton.text = ""
        }
    }


    /**
     * Call onProgress when the seek bar is moved by the user
     */
    class OnSeekBarProgress(val onProgress: (progress: Int) -> Unit) :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                onProgress(progress)
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    /**
     * Generate the usual checkered background to highlight transparency
     */
    private fun checkeredBackground(): Bitmap {
        val isNightMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return createBitmap(19, 19).apply {
            Canvas(this).apply {
                val dark = Paint().apply {
                    color = (if (isNightMode) 0xff000000 else 0xffCDCDCD).toInt()
                }
                val light = Paint().apply {
                    color = (if (isNightMode) 0xff555555 else 0xffF1F1F1).toInt()
                }
                drawRect(0f, 0f, 19f, 19f, dark)
                drawRect(9f, 0f, 19f, 9f, light)
                drawRect(0f, 9f, 9f, 19f, light)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("tintEnabled", switchFloatingButtonColorTint.isChecked)
        outState.putInt("tintH", seekBarFloatingButtonTintH.progress)
        outState.putInt("tintV", seekBarFloatingButtonTintV.progress)
        outState.putInt("alpha", seekBarFloatingButtonAlpha.progress)
        outState.putInt("scale", seekBarFloatingButtonScale.progress)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(mSavedInstanceState: Bundle) {
        super.onRestoreInstanceState(mSavedInstanceState)
        savedInstanceState = mSavedInstanceState
    }

    private fun restoreSavedInstanceValues() {
        savedInstanceState?.run {
            getInt("tintH", -1).let {
                if (it > -1) {
                    seekBarFloatingButtonTintH.progress = it
                    onSelectColor(h = 0.1f * it, v = null)
                }
            }
            getInt("tintV", -1).let {
                if (it > -1) {
                    seekBarFloatingButtonTintV.progress = it
                    onSelectColor(h = null, v = 0.5f + 0.001f * it)
                }
            }
            getBoolean("tintEnabled", false).let {
                // This needs to be done after the tintH/tintV, otherwise it is overwritten
                switchFloatingButtonColorTint.isChecked = it
            }
            getInt("alpha", -1).let {
                if (it > -1) {
                    seekBarFloatingButtonAlpha.progress = it
                    val alpha = it / 100f
                    imageViewFloatingButton.alpha = alpha
                    textViewCloseButton.alpha = alpha
                    prefManager.floatingButtonAlpha = alpha
                    switchFloatingButtonAlpha.isChecked = alpha < 0.98
                }
            }
            getInt("scale", -1).let {
                if (it > -1) {
                    seekBarFloatingButtonScale.progress = it
                    prefManager.floatingButtonScale = it
                    updatePreviewButton()
                }
            }
        }
    }

}


