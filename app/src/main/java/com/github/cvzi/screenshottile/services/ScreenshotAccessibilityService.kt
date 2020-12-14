package com.github.cvzi.screenshottile.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Animatable
import android.hardware.display.DisplayManager
import android.os.Build
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
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.ContainerActivity
import com.github.cvzi.screenshottile.activities.MainActivity
import com.github.cvzi.screenshottile.activities.NoDisplayActivity
import com.github.cvzi.screenshottile.databinding.AccessibilityBarBinding
import com.github.cvzi.screenshottile.fragments.SettingFragment
import com.github.cvzi.screenshottile.utils.ShutterCollection
import com.github.cvzi.screenshottile.utils.fillTextHeight


/**
 * Created by cuzi (cuzi@openmail.cc) on 2019/12/26.
 */
@RequiresApi(Build.VERSION_CODES.P)
class ScreenshotAccessibilityService : AccessibilityService() {
    companion object {
        var instance: ScreenshotAccessibilityService? = null
        var screenshotPermission: Intent? = null
        private const val TAG = "ScreenshotAccessService"

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
                    tileService.startActivityAndCollapse(this)
                }
            }
        }

    }

    private var floatingButtonShown = false
    private var binding: AccessibilityBarBinding? = null
    private var useThis = false

    override fun onServiceConnected() {
        instance = this
        when (App.getInstance().prefManager.returnIfAccessibilityServiceEnabled) {
            SettingFragment.TAG -> {
                // Return to settings
                App.getInstance().prefManager.returnIfAccessibilityServiceEnabled = null
                ContainerActivity.startNewTask(this, SettingFragment::class.java)
            }
            MainActivity.TAG -> {
                // Return to main activity
                App.getInstance().prefManager.returnIfAccessibilityServiceEnabled = null
                MainActivity.startNewTask(this)
            }
            NoDisplayActivity.TAG -> {
                // Return to NoDisplayActivity activity i.e. finish()
                App.getInstance().prefManager.returnIfAccessibilityServiceEnabled = null
                NoDisplayActivity.startNewTask(this, false)
            }
        }

        updateFloatingButton()
    }

    private fun getWinContext(): Context {
        var windowContext: Context = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !useThis) {
            val dm: DisplayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val primaryDisplay = dm.getDisplay(DEFAULT_DISPLAY)
            windowContext = createDisplayContext(primaryDisplay)
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
        val prefValue = App.getInstance().prefManager.floatingButton
        if (prefValue && !floatingButtonShown) {
            showFloatingButton()
        } else if (!prefValue && floatingButtonShown) {
            hideFloatingButton()
        } else if (prefValue && forceRedraw) {
            hideFloatingButton()
            showFloatingButton()
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
        buttonScreenshot.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                shutterCollection.current().normal
            )
        )
        var buttonClose: TextView? = null

        val scale = App.getInstance().prefManager.floatingButtonScale
        if (scale != 100) {
            // Scale button
            buttonScreenshot.post {
                buttonScreenshot.layoutParams = buttonScreenshot.layoutParams.apply {
                    width = buttonScreenshot.measuredWidth * scale / 100
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

        if (App.getInstance().prefManager.floatingButtonShowClose) {
            buttonClose = TextView(getWinContext())
            buttonClose.text = getString(R.string.emoji_close)
            val linearLayout = root.findViewById<LinearLayout>(R.id.linearLayoutOuter)
            linearLayout.addView(buttonClose)
            buttonClose.layoutParams = LinearLayout.LayoutParams(buttonClose.layoutParams).apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            buttonClose.setOnClickListener {
                App.getInstance().prefManager.floatingButton = false
                hideFloatingButton()
            }
        }
        buttonScreenshot.setOnClickListener {
            (buttonScreenshot.drawable as? Animatable)?.start()
            root.visibility = View.GONE
            root.invalidate()
            root.postDelayed({
                simulateScreenshotButton(autoHideButton = false, autoUnHideButton = false)
                if (App.getInstance().prefManager.floatingButtonHideAfter) {
                    App.getInstance().prefManager.floatingButton = false
                    hideFloatingButton()
                } else {
                    Handler(Looper.getMainLooper()).postDelayed({
                        showTemporaryHiddenFloatingButton()
                    }, 1000)
                }
            }, 5)
        }

        var dragDone = false
        buttonScreenshot.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DROP, DragEvent.ACTION_DRAG_ENDED -> {
                    var x = (event.x - v.measuredWidth / 2.0).toInt()
                    var y = (event.y - v.measuredHeight).toInt()
                    if (event.action == DragEvent.ACTION_DROP) {
                        // x and y are relative to the inside of the view's bounding box
                        x =
                            (App.getInstance().prefManager.floatingButtonPosition.x - v.measuredWidth / 2.0 + event.x).toInt()
                        y =
                            (App.getInstance().prefManager.floatingButtonPosition.y - v.measuredHeight / 2.0 + event.y).toInt()
                    }
                    root.let {
                        if (!dragDone) {
                            dragDone = true
                            updateWindowViewPosition(it, x, y)
                            App.getInstance().prefManager.floatingButtonPosition =
                                Point(x, y)
                        }
                        buttonScreenshot.setImageDrawable(
                            ContextCompat.getDrawable(
                                this,
                                shutterCollection.current().normal
                            )
                        )
                    }
                    true
                }
                else -> true
            }
        }

        buttonScreenshot.setOnLongClickListener {
            dragDone = false
            (buttonScreenshot as ImageView).setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    shutterCollection.current().move
                )
            )
            (buttonScreenshot.drawable as Animatable).start()
            it.startDragAndDrop(null, View.DragShadowBuilder(root), null, 0)
        }

        (buttonScreenshot.drawable as? Animatable)?.start()
    }

    /**
     * Remove view if it exists
     */
    private fun hideFloatingButton() {
        floatingButtonShown = false
        binding?.root?.let {
            getWinManager().removeView(it)
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
        autoHideButton: Boolean = true,
        autoUnHideButton: Boolean = true
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false
        }
        if (autoHideButton) {
            temporaryHideFloatingButton()
        }
        val success = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        if (success) {
            App.getInstance().prefManager.screenshotCount++
        }
        if (autoUnHideButton) {
            Handler(Looper.getMainLooper()).postDelayed({
                showTemporaryHiddenFloatingButton()
            }, 1000)
        }

        return success
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No op
    }

    override fun onInterrupt() {
        // No op
    }
}
