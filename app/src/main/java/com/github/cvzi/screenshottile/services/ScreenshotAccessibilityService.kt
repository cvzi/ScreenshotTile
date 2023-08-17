package com.github.cvzi.screenshottile.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Animatable
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.TileService
import android.util.Log
import android.view.*
import android.view.Display.DEFAULT_DISPLAY
import android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.github.cvzi.screenshottile.*
import com.github.cvzi.screenshottile.activities.*
import com.github.cvzi.screenshottile.databinding.AccessibilityBarBinding
import com.github.cvzi.screenshottile.fragments.SettingFragment
import com.github.cvzi.screenshottile.utils.*


/**
 * Created by cuzi (cuzi@openmail.cc) on 2019/12/26.
 */
@RequiresApi(Build.VERSION_CODES.P)
class ScreenshotAccessibilityService : AccessibilityService() {
    companion object {
        var instance: ScreenshotAccessibilityService? = null
        var screenshotPermission: Intent? = null
        private const val TAG = "ScreenshotAccessService"
        private const val SETTINGS_BUTTON_TAG = "SettingsButton"

        /**
         * Open accessibility settings from activity
         */
        fun openAccessibilitySettings(context: Activity, returnTo: String? = null) {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                if (resolveActivity(context.packageManager) != null) {
                    if (returnTo != null) {
                        App.getInstance().prefManager.returnIfAccessibilityServiceEnabled = returnTo
                    }
                    context.startActivity(this)
                }
            }
        }

        /**
         * Open accessibility settings and collapse quick settings panel
         */
        fun openAccessibilitySettings(tileService: TileService, returnTo: String? = null) {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (resolveActivity(tileService.packageManager) != null) {
                    if (returnTo != null) {
                        App.getInstance().prefManager.returnIfAccessibilityServiceEnabled = returnTo
                    }
                    tileService.startActivityAndCollapseCustom(this)
                }
            }
        }

        fun setShutterDrawable(context: Context, button: ImageView, res: Int) {
            button.setImageDrawable(
                ContextCompat.getDrawable(
                    context,
                    res
                )?.apply {
                    val colorString = App.getInstance().prefManager.floatingButtonColorTint
                    val colorInt = parseColorString(colorString)
                    if (colorInt != null) {
                        setTint(colorInt)
                    } else {
                        setTintList(null)
                    }
                }
            )
        }
    }

    private var screenDensity: Int = 0
    private var floatingButtonShown = false
    private var binding: AccessibilityBarBinding? = null
    private var useThis = false

    override fun onServiceConnected() {
        instance = this
        App.getInstance()?.let { appInstance ->
            try {
                when (appInstance.prefManager.returnIfAccessibilityServiceEnabled) {
                    SettingFragment.TAG -> {
                        // Return to settings
                        appInstance.prefManager.returnIfAccessibilityServiceEnabled = null
                        SettingsActivity.startNewTask(this)
                    }

                    MainActivity.TAG -> {
                        // Return to main activity
                        appInstance.prefManager.returnIfAccessibilityServiceEnabled = null
                        MainActivity.startNewTask(this)
                    }

                    NoDisplayActivity.TAG -> {
                        // Return to NoDisplayActivity activity i.e. finish()
                        appInstance.prefManager.returnIfAccessibilityServiceEnabled = null
                        NoDisplayActivity.startNewTask(this, false)
                    }

                    else -> {
                        // Do nothing
                    }
                }
            } catch (e: ActivityNotFoundException) {
                // This seems to happen after booting
                Log.e(
                    TAG,
                    "Could not start Activity for return to '${appInstance.prefManager.returnIfAccessibilityServiceEnabled}'",
                    e
                )
            }
        }

        updateFloatingButton()
    }

    private fun getWinContext(): Context {
        var windowContext: Context = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !useThis) {
            val dm: DisplayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val defaultDisplay = dm.getDisplay(DEFAULT_DISPLAY)
            windowContext = createDisplayContext(defaultDisplay)
        }
        return windowContext
    }

    private fun getWinManager(): WindowManager {
        return getWinContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /**
     * Toggle the floating button according to the current settings
     */
    fun updateFloatingButton(forceRedraw: Boolean = false) {
        App.getInstance()?.let {
            val prefValue = it.prefManager.floatingButton
            if (prefValue && !floatingButtonShown) {
                showFloatingButton()
            } else if (!prefValue && floatingButtonShown) {
                hideFloatingButton()
            } else if (prefValue && forceRedraw) {
                hideFloatingButton()
                showFloatingButton()
            }
        }
    }

    private fun showFloatingButton() {
        floatingButtonShown = true

        binding =
            AccessibilityBarBinding.inflate(getWinContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        binding?.root?.let { root ->
            configureFloatingButton(root)
        }
    }

    private fun configureFloatingButton(root: ViewGroup) {
        val position = App.getInstance().prefManager.floatingButtonPosition

        val shutterCollection = ShutterCollection(this, R.array.shutters, R.array.shutter_names)

        addWindowViewAt(root, position.x, position.y)

        val buttonScreenshot = root.findViewById<ImageView>(R.id.buttonScreenshot)
        setShutterDrawable(this, buttonScreenshot, shutterCollection.current().normal)
        var buttonClose: TextView? = null

        val scale = App.getInstance().prefManager.floatingButtonScale
        if (scale != 100) {
            // Scale button
            buttonScreenshot.post {
                buttonScreenshot.layoutParams = buttonScreenshot.layoutParams.apply {
                    width = buttonScreenshot.measuredHeight * scale / 100
                    height = buttonScreenshot.measuredHeight * scale / 100
                }
                buttonScreenshot.post {
                    buttonClose?.run {
                        fillTextHeight(
                            this,
                            buttonScreenshot.measuredHeight * 3 / 4,
                            buttonScreenshot.measuredHeight * 0.8f
                        )
                    }
                }
            }
        }

        if (App.getInstance().prefManager.floatingButtonShowClose && App.getInstance().prefManager.floatingButtonCloseEmoji.isNotBlank()) {
            buttonClose = TextView(root.context)
            buttonClose.text = App.getInstance().prefManager.floatingButtonCloseEmoji
            val linearLayout = root.findViewById<LinearLayout>(R.id.linearLayoutOuter)
            linearLayout.addView(buttonClose)
            buttonClose.layoutParams = LinearLayout.LayoutParams(buttonClose.layoutParams).apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            buttonClose.setOnClickListener {
                App.getInstance().prefManager.floatingButton = false
                hideFloatingButton()
                SettingFragment.instance?.get()?.updateFloatingButtonFromService()
            }
        }
        val alpha = App.getInstance().prefManager.floatingButtonAlpha
        buttonScreenshot.alpha = alpha
        buttonClose?.alpha = alpha

        buttonScreenshot.setOnClickListener {
            if (isDeviceLocked(this) && App.getInstance().prefManager.preventIfLocked) {
                toastDeviceIsLocked(this)
                return@setOnClickListener
            }

            if (App.getInstance().prefManager.floatingButtonAction == getString(R.string.setting_floating_action_value_partial)) {
                App.getInstance().screenshotPartial(this)
                return@setOnClickListener
            }

            val delayInSeconds = App.getInstance().prefManager.floatingButtonDelay.toLong()
            val delayInMilliSeconds = if (delayInSeconds > 0) {
                1000L * delayInSeconds
            } else {
                5L
            }
            var countDownTextView: TextView? = null
            if (delayInMilliSeconds >= 1000L) {
                countDownTextView = showCountDown(root, buttonScreenshot, delayInSeconds)
                root.postDelayed({
                    root.visibility = View.GONE
                    root.invalidate()
                }, delayInMilliSeconds - 20L)
            } else {
                root.visibility = View.GONE
                root.invalidate()
            }
            root.postDelayed({
                val legacyMethod =
                    App.getInstance().prefManager.floatingButtonAction == getString(R.string.setting_floating_action_value_legacy)
                if (legacyMethod) {
                    NoDisplayActivity.startNewTaskLegacyScreenshot(this)
                } else {
                    simulateScreenshotButton(autoHideButton = false, autoUnHideButton = false)
                }
                if (App.getInstance().prefManager.floatingButtonHideAfter) {
                    App.getInstance().prefManager.floatingButton = false
                    hideFloatingButton()
                } else if (!legacyMethod) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        showTemporaryHiddenFloatingButton(root, countDownTextView, buttonScreenshot)
                    }, 1000L)
                }
            }, delayInMilliSeconds)
        }

        var dragDone = false
        buttonScreenshot.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DROP, DragEvent.ACTION_DRAG_ENDED -> {
                    root.let {
                        if (!dragDone) {
                            val x: Int
                            val y: Int
                            if (event.action == DragEvent.ACTION_DROP) {
                                // x and y are relative to the inside of the view's bounding box
                                val old = App.getInstance().prefManager.floatingButtonPosition
                                x = (old.x - v.measuredWidth / 2.0 + event.x).toInt()
                                y = (old.y - v.measuredHeight / 2.0 + event.y).toInt()
                            } else {
                                val parent = v.parent as View
                                x = (event.x - parent.measuredWidth / 2).toInt()
                                y = (event.y - parent.measuredHeight / 2).toInt()
                            }
                            dragDone = true
                            updateWindowViewPosition(it, x, y)
                            App.getInstance().prefManager.floatingButtonPosition =
                                Point(x, y)
                        }
                        setShutterDrawable(
                            this,
                            buttonScreenshot,
                            shutterCollection.current().normal
                        )
                        buttonScreenshot.alpha = App.getInstance().prefManager.floatingButtonAlpha
                    }
                    showSettingsButton(root, buttonScreenshot)
                    true
                }

                else -> true
            }
        }

        buttonScreenshot.setOnLongClickListener {
            dragDone = false
            setShutterDrawable(this, buttonScreenshot, shutterCollection.current().move)
            (buttonScreenshot.drawable as Animatable).start()
            buttonScreenshot.alpha = 1f
            it.startDragAndDrop(null, View.DragShadowBuilder(root), null, 0)
        }

        (buttonScreenshot.drawable as? Animatable)?.start()
    }

    private fun showCountDown(
        root: ViewGroup,
        buttonScreenshot: View,
        delayInSeconds: Long
    ): TextView {
        buttonScreenshot.visibility = View.GONE
        val textView = TextView(root.context)
        @SuppressLint("SetTextI18n")
        textView.text = delayInSeconds.toString().map {
            it + "\uFE0F\u20E3"
        }.joinToString("")
        root.findViewById<LinearLayout>(R.id.linearLayoutOuter).addView(textView, 0)
        textView.layoutParams = LinearLayout.LayoutParams(textView.layoutParams).apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        buttonScreenshot.post {
            textView.run {
                fillTextHeight(
                    this,
                    buttonScreenshot.measuredHeight * 3 / 4,
                    buttonScreenshot.measuredHeight * 0.8f
                )
            }
        }
        for (i in 1..delayInSeconds) {
            root.postDelayed({
                @SuppressLint("SetTextI18n")
                textView.text = "${delayInSeconds - i}\uFE0F\u20E3"
            }, i * 1000L)
        }
        return textView
    }

    private fun showSettingsButton(root: ViewGroup, buttonScreenshot: View) {
        val linearLayout = root.findViewById<LinearLayout>(R.id.linearLayoutOuter)
        if (linearLayout.findViewWithTag<View>(SETTINGS_BUTTON_TAG) != null) {
            return
        }
        val textView = TextView(root.context)
        textView.tag = SETTINGS_BUTTON_TAG
        textView.alpha = App.getInstance().prefManager.floatingButtonAlpha
        @SuppressLint("SetTextI18n")
        textView.text = "\u2699\uFE0F"
        linearLayout.addView(textView)
        textView.layoutParams = LinearLayout.LayoutParams(textView.layoutParams).apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        textView.setOnClickListener {
            textView.text = "\u23F3"
            FloatingButtonSettingsActivity.startNewTask(it.context)
        }
        buttonScreenshot.post {
            textView.run {
                fillTextHeight(
                    this,
                    buttonScreenshot.measuredHeight * 3 / 4,
                    buttonScreenshot.measuredHeight * 0.8f
                )
            }
        }
        textView.postDelayed({
            linearLayout.safeRemoveView(textView, TAG)
        }, 2000)

    }

    /**
     * Remove view if it exists
     */
    private fun hideFloatingButton() {
        floatingButtonShown = false
        binding?.root?.let {
            getWinManager().safeRemoveView(it, TAG)
        }
        binding = null
    }

    /**
     * Temporary hide floating button to take a screenshot
     */
    fun temporaryHideFloatingButton(maxHiddenTime: Long = 10000L) {
        binding?.root?.apply {
            visibility = View.GONE
            invalidate()
            Handler(Looper.getMainLooper()).postDelayed({
                binding?.root?.apply {
                    visibility = View.VISIBLE
                }
            }, maxHiddenTime)
        }
    }

    /**
     * Reverse temporaryHideFloatingButton()
     */
    fun showTemporaryHiddenFloatingButton() {
        binding?.root?.apply {
            visibility = View.VISIBLE
        }
    }

    /**
     * Reverse temporaryHideFloatingButton(), hide count down and start shutter animation
     */
    private fun showTemporaryHiddenFloatingButton(
        root: ViewGroup,
        countDownTextView: View?,
        buttonScreenshot: ImageView
    ) {
        root.visibility = View.VISIBLE
        countDownTextView?.let {
            root.findViewById<LinearLayout>(R.id.linearLayoutOuter).safeRemoveView(it, TAG)
            buttonScreenshot.visibility = View.VISIBLE
        }
        (buttonScreenshot.drawable as? Animatable)?.start()
    }

    private fun addWindowViewAt(
        view: View,
        x: Int = 0,
        y: Int = 0,
        tryAgainOnFailure: Boolean = true
    ) {
        try {
            getWinManager().addView(
                view,
                windowViewAbsoluteLayoutParams(x, y)
            )
        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "windowManager.addView failed for invalid token:", e)
            if (tryAgainOnFailure) {
                try {
                    getWinManager().removeView(view)
                } catch (e: Exception) {
                    Log.e(TAG, "windowManager.removeView failed as well:", e)
                }
                useThis = true
                addWindowViewAt(view, x, y, false)
            }
        }
    }

    private fun updateWindowViewPosition(view: View, x: Int, y: Int) {
        getWinManager().updateViewLayout(
            view,
            windowViewAbsoluteLayoutParams(x, y)
        )
    }

    private fun windowViewAbsoluteLayoutParams(x: Int, y: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            type = TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            @SuppressLint("RtlHardcoded")
            gravity = Gravity.TOP or Gravity.LEFT
            this.x = x
            this.y = y
        }
    }


    /**
     * Simulate screenshot button (home+power) press
     * Return true on success
     */
    fun simulateScreenshotButton(
        autoHideButton: Boolean,
        autoUnHideButton: Boolean,
        useTakeScreenshotMethod: Boolean = true
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false
        }
        if (autoHideButton) {
            temporaryHideFloatingButton()
        }

        val success: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && useTakeScreenshotMethod && !App.getInstance().prefManager.useSystemDefaults) {
            // We don't need to check storage permission first, because this permission is not
            // necessary since Android Q and this function is only available since Android R
            takeScreenshot()
            success = true
        } else {
            success = try {
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)", e)
                false
            }
            if (success) {
                App.getInstance().prefManager.screenshotCount++
            }
            if (autoUnHideButton) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showTemporaryHiddenFloatingButton()
                }, 1000)
            }
        }
        return success
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshot() {
        super.takeScreenshot(
            DEFAULT_DISPLAY,
            { r -> Thread(r).start() },
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val bitmap = Bitmap.wrapHardwareBuffer(
                        screenshot.hardwareBuffer,
                        screenshot.colorSpace
                    )?.copy(Bitmap.Config.ARGB_8888, false)
                    screenshot.hardwareBuffer.close()

                    if (bitmap == null) {
                        Log.e(
                            TAG,
                            "takeScreenshot() bitmap == null, falling back to GLOBAL_ACTION_TAKE_SCREENSHOT"
                        )
                        Handler(Looper.getMainLooper()).post {
                            fallbackToSimulateScreenshotButton()
                        }
                    } else {
                        val saveImageResult = saveBitmapToFile(
                            this@ScreenshotAccessibilityService,
                            bitmap,
                            App.getInstance().prefManager.fileNamePattern,
                            compressionPreference(applicationContext),
                            null,
                            useAppData = "saveToStorage" !in App.getInstance().prefManager.postScreenshotActions,
                            directory = null
                        )
                        Handler(Looper.getMainLooper()).post {
                            onFileSaved(saveImageResult)
                        }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    Log.e(
                        TAG,
                        "takeScreenshot() -> onFailure($errorCode), falling back to GLOBAL_ACTION_TAKE_SCREENSHOT"
                    )
                    Handler(Looper.getMainLooper()).post {
                        fallbackToSimulateScreenshotButton()
                    }
                }
            })
    }

    private fun fallbackToSimulateScreenshotButton(): Boolean {
        return simulateScreenshotButton(
            autoHideButton = false,
            autoUnHideButton = true,
            useTakeScreenshotMethod = false
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun onFileSaved(saveImageResult: SaveImageResult?) {
        if (saveImageResult == null) {
            screenShotFailedToast("saveImageResult is null")
            return
        }
        if (!saveImageResult.success) {
            screenShotFailedToast(saveImageResult.errorMessage)
            return
        }

        screenDensity = resources.configuration.densityDpi

        val postScreenshotActions = App.getInstance().prefManager.postScreenshotActions

        val result = saveImageResult as? SaveImageResultSuccess?
        when {
            result == null -> {
                screenShotFailedToast("Failed to cast SaveImageResult")
            }

            result.uri != null -> {
                // Android Q+ works with MediaStore content:// URI
                var dummyPath =
                    "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}/${result.fileTitle}"
                if (result.dummyPath.isNotEmpty()) {
                    dummyPath = result.dummyPath
                }

                if ("showToast" in postScreenshotActions) {
                    getWinContext().toastMessage(
                        getString(R.string.screenshot_file_saved, dummyPath),
                        ToastType.SUCCESS
                    )
                }

                if ("showNotification" in postScreenshotActions) {
                    createNotification(
                        this,
                        result.uri,
                        result.bitmap,
                        screenDensity,
                        result.mimeType,
                        dummyPath
                    )
                }
                App.getInstance().prefManager.screenshotCount++
                handlePostScreenshot(
                    this,
                    postScreenshotActions,
                    result.uri,
                    result.mimeType,
                    result.bitmap
                )
            }

            result.file != null -> {
                // Legacy behaviour until Android P, works with the real file path
                val uri = Uri.fromFile(result.file)
                val path = result.file.absolutePath

                if ("showToast" in postScreenshotActions) {
                    getWinContext().toastMessage(
                        getString(R.string.screenshot_file_saved, path),
                        ToastType.SUCCESS
                    )
                }

                if ("showNotification" in postScreenshotActions) {
                    createNotification(
                        this,
                        uri,
                        result.bitmap,
                        screenDensity,
                        result.mimeType
                    )
                }
                App.getInstance().prefManager.screenshotCount++
                handlePostScreenshot(
                    this,
                    postScreenshotActions,
                    uri,
                    result.mimeType,
                    result.bitmap
                )
            }

            else -> {
                screenShotFailedToast("Failed to cast SaveImageResult path/uri")
            }
        }
    }

    private fun screenShotFailedToast(errorMessage: String? = null) {
        val message = getString(R.string.screenshot_failed) + if (errorMessage != null) {
            "\n$errorMessage"
        } else {
            ""
        }
        getWinContext().toastMessage(message, ToastType.ERROR)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No op
    }

    override fun onInterrupt() {
        // No op
    }
}
