package com.github.cvzi.screenshottile.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.TileService
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.activities.MainActivity
import com.github.cvzi.screenshottile.databinding.AccessibilityBarBinding
import com.github.cvzi.screenshottile.activities.ContainerActivity
import com.github.cvzi.screenshottile.fragments.SettingFragment
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

    private lateinit var windowManager: WindowManager
    private var floatingButtonShown = false
    private var binding: AccessibilityBarBinding? = null

    override fun onServiceConnected() {
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (App.getInstance().prefManager.returnIfAccessibilityServiceEnabled == SettingFragment.TAG) {
            // Return to settings
            App.getInstance().prefManager.returnIfAccessibilityServiceEnabled = null
            ContainerActivity.startNewTask(this, SettingFragment::class.java)
        } else if (App.getInstance().prefManager.returnIfAccessibilityServiceEnabled == MainActivity.TAG) {
            // Return to main activity
            App.getInstance().prefManager.returnIfAccessibilityServiceEnabled = null
            MainActivity.startNewTask(this)
        }

        updateFloatingButton()
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
        binding = AccessibilityBarBinding.inflate(LayoutInflater.from(this))

        binding?.root?.let { root ->
            configureFloatingButton(root)
        }
    }

    private fun configureFloatingButton(root: ViewGroup) {

        val position = App.getInstance().prefManager.floatingButtonPosition

        addWindowViewAt(root, position.x, position.y)

        val buttonScreenshot = root.findViewById<View>(R.id.buttonScreenshot)
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
            buttonScreenshot.post {
                //buttonClose.textSize = buttonScreenshot.measuredHeight / 2f
            }
        }

        if (App.getInstance().prefManager.floatingButtonShowClose) {
            buttonClose = TextView(this)
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
            root.visibility = View.GONE
            Handler(Looper.getMainLooper()).postDelayed({
                simulateScreenshotButton()
                if (App.getInstance().prefManager.floatingButtonHideAfter) {
                    App.getInstance().prefManager.floatingButton = false
                    hideFloatingButton()
                } else {
                    root.postDelayed({
                        root.visibility = View.VISIBLE
                    }, 1000)
                }
            }, 20)
        }

        buttonScreenshot.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_ENDED -> {
                    val x = (event.x - v.measuredWidth / 2.0).toInt()
                    val y = (event.y - v.measuredHeight).toInt()
                    root.let {
                        updateWindowViewPosition(it, x, y)
                        App.getInstance().prefManager.floatingButtonPosition = Point(x, y)
                    }
                    true
                }
                else -> true
            }
        }

        buttonScreenshot.setOnLongClickListener {
            it.startDragAndDrop(null, View.DragShadowBuilder(root), null, 0)
        }

    }

    /**
     * Remove view if it exists
     */
    private fun hideFloatingButton() {
        floatingButtonShown = false
        binding?.root?.let {
            windowManager.removeView(it)
        }
        binding = null
    }

    private fun addWindowViewAt(view: View, x: Int = 0, y: Int = 0) {
        windowManager.addView(
            view,
            windowViewAbsoluteLayoutParams(x, y)
        )
    }

    private fun updateWindowViewPosition(view: View, x: Int, y: Int) {
        windowManager.updateViewLayout(
            view,
            windowViewAbsoluteLayoutParams(x, y)
        )
    }

    private fun windowViewAbsoluteLayoutParams(x: Int, y: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
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
    fun simulateScreenshotButton(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false
        }
        val success = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        if (success) {
            App.getInstance().prefManager.screenshotCount++
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
