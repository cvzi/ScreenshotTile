package com.github.cvzi.screenshottile.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
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
import android.view.Display.DEFAULT_DISPLAY
import android.view.DragEvent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.PackageNameFilterMode
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.SaveImageResult
import com.github.cvzi.screenshottile.SaveImageResultSuccess
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.activities.FloatingButtonSettingsActivity
import com.github.cvzi.screenshottile.activities.LongScreenshotReviewActivity
import com.github.cvzi.screenshottile.activities.MainActivity
import com.github.cvzi.screenshottile.activities.NoDisplayActivity
import com.github.cvzi.screenshottile.activities.SettingsActivity
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import com.github.cvzi.screenshottile.databinding.AccessibilityBarBinding
import com.github.cvzi.screenshottile.fragments.SettingFragment
import com.github.cvzi.screenshottile.functions.AppFunctionResultStore
import com.github.cvzi.screenshottile.utils.ShutterCollection
import com.github.cvzi.screenshottile.utils.areLongScreenshotFramesSimilar
import com.github.cvzi.screenshottile.utils.compressionPreference
import com.github.cvzi.screenshottile.utils.composeLongScreenshotFromFiles
import com.github.cvzi.screenshottile.utils.createNotification
import com.github.cvzi.screenshottile.utils.fillTextHeight
import com.github.cvzi.screenshottile.utils.formatLocalizedString
import com.github.cvzi.screenshottile.utils.getLocalizedString
import com.github.cvzi.screenshottile.utils.handlePostScreenshot
import com.github.cvzi.screenshottile.utils.isDeviceLocked
import com.github.cvzi.screenshottile.utils.parseColorString
import com.github.cvzi.screenshottile.utils.navigationBarSize
import com.github.cvzi.screenshottile.utils.safeRemoveView
import com.github.cvzi.screenshottile.utils.saveLongScreenshotDebugArtifacts
import com.github.cvzi.screenshottile.utils.saveLongScreenshotTempBitmap
import com.github.cvzi.screenshottile.utils.saveBitmapToFile
import com.github.cvzi.screenshottile.utils.setUserLanguage
import com.github.cvzi.screenshottile.utils.startActivityAndCollapseCustom
import com.github.cvzi.screenshottile.utils.toastDeviceIsLocked
import com.github.cvzi.screenshottile.utils.toastMessage
import java.io.File


/**
 * Created by cuzi (cuzi@openmail.cc) on 2019/12/26.
 */
@RequiresApi(Build.VERSION_CODES.P)
class ScreenshotAccessibilityService : AccessibilityService() {
    private enum class LongScreenshotMode {
        AUTO,
        MANUAL
    }

    private data class LongScreenshotSession(
        var mode: LongScreenshotMode,
        val sessionDir: File,
        var targetBounds: Rect? = null,
        var frameCount: Int = 0,
        var waitingForCapture: Boolean = false,
        var cancelled: Boolean = false,
        val expectedCropTops: MutableList<Int?> = mutableListOf(0)
    )

    companion object {
        var instance: ScreenshotAccessibilityService? = null
        var screenshotPermission: Intent? = null
        private const val TAG = "ScreenshotAccessService"
        private const val SETTINGS_BUTTON_TAG = "SettingsButton"

        const val TAP_TYPE_SINGLE = 0
        const val TAP_TYPE_DOUBLE = 1

        /**
         * Open accessibility settings from activity
         */
        fun openAccessibilitySettings(context: Activity, returnTo: String? = null) {
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                if (resolveActivity(context.packageManager) != null) {
                    if (returnTo != null) {
                        App.getInstance().prefManager.returnIfAccessibilityServiceEnabled = returnTo
                    }
                    context.toastMessageAccessibility()
                    context.startActivity(this)
                    Handler(Looper.getMainLooper()).postDelayed({
                        context.toastMessageAccessibility()
                    }, 2000)
                    Handler(Looper.getMainLooper()).postDelayed({
                        context.toastMessageAccessibility()
                    }, 4000)
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
                    tileService.toastMessageAccessibility()
                    tileService.startActivityAndCollapseCustom(this)
                    Handler(Looper.getMainLooper()).postDelayed({
                        tileService.toastMessageAccessibility()
                    }, 2000)
                    Handler(Looper.getMainLooper()).postDelayed({
                        tileService.toastMessageAccessibility()
                    }, 4000)
                }
            }
        }

        /**
         * Inform user that they should enable the accessibility service
         */
        private fun Context.toastMessageAccessibility() {
            if (instance == null) {
                toastMessage(
                    formatLocalizedString(
                        R.string.toast_open_accessibility_settings,
                        getLocalizedString(R.string.app_name)
                    ), ToastType.ACTIVITY, Toast.LENGTH_LONG
                )
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
    private var screenOrientation: Int = 1
    private var screenLocked = false
    var floatingButtonShown = false
        private set
    private var binding: AccessibilityBarBinding? = null
    private var useThis = false
    private var onUnLockBroadcastReceiver: OnUnLockBroadcastReceiver? = null
    private var packageFilterEnabled = false
    private var packageFilterTempForceShow: Boolean = false
    private var packageFilterTempForceHide: Boolean = false
    private var packageFilterTempOverrideTime: Long = 0L
    private var packageFilterMode = PackageNameFilterMode.BLACKLIST
    private val packageFilterNameList: HashSet<String> = HashSet()
    private var lastPackageName: CharSequence = ""

    /**
     * Get the package name of the last foreground application
     */
    fun getForegroundPackageName(): String = lastPackageName.toString()

    fun startLongScreenshotSession() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            getWinContext().toastMessage(R.string.long_screenshot_unsupported, ToastType.ERROR)
            return
        }
        if (longScreenshotSession != null) {
            updateLongScreenshotPanel(getLocalizedString(R.string.long_screenshot_status_manual), manualCaptureEnabled = true)
            return
        }

        if (!floatingButtonShown) {
            temporaryOverlayForLongScreenshot = true
            showFloatingButton(animate = false)
        }

        val sessionDir = File(cacheDir, "long_screenshot_${System.currentTimeMillis()}")
        val detectedBounds = detectScrollableBounds()
        val mode = if (detectedBounds != null) LongScreenshotMode.AUTO else LongScreenshotMode.MANUAL
        longScreenshotSession = LongScreenshotSession(
            mode = mode,
            sessionDir = sessionDir,
            targetBounds = detectedBounds
        )
        updateLongScreenshotPanel(
            if (mode == LongScreenshotMode.AUTO) {
                getLocalizedString(R.string.long_screenshot_status_auto)
            } else {
                getLocalizedString(R.string.long_screenshot_status_manual)
            },
            manualCaptureEnabled = mode == LongScreenshotMode.MANUAL
        )

        if (mode == LongScreenshotMode.AUTO) {
            captureLongScreenshotFrame(afterCapture = { session ->
                if (!advanceLongScreenshot(session)) {
                    switchLongScreenshotToManual(getLocalizedString(R.string.long_screenshot_status_retry_manual))
                }
            })
        }
    }

    private fun detectScrollableBounds(): Rect? {
        val node = findBestScrollableNode(rootInActiveWindow) ?: return null
        return Rect().also {
            node.getBoundsInScreen(it)
            lastScrollableBounds = Rect(it)
        }
    }

    private fun findBestScrollableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        root ?: return null
        var bestNode: AccessibilityNodeInfo? = null
        var bestArea = 0
        fun walk(node: AccessibilityNodeInfo?) {
            node ?: return
            if (node.isScrollable && node.isVisibleToUser) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                val area = rect.width() * rect.height()
                if (area > bestArea) {
                    bestArea = area
                    bestNode = node
                }
            }
            for (index in 0 until node.childCount) {
                walk(node.getChild(index))
            }
        }
        walk(root)
        return bestNode
    }

    private fun updateLongScreenshotPanel(status: String, manualCaptureEnabled: Boolean) {
        val localBinding = binding ?: return
        val actionsEnabled = !longScreenshotFinishing
        val captureEnabled = manualCaptureEnabled && actionsEnabled
        localBinding.longScreenshotPanel.visibility = View.VISIBLE
        localBinding.textLongScreenshotStatus.text = status
        localBinding.textLongScreenshotCounter.text = formatLocalizedString(
            R.string.long_screenshot_counter,
            longScreenshotSession?.frameCount ?: 0
        )
        applyLongScreenshotButtonState(localBinding.buttonLongScreenshotCapture, captureEnabled)
        applyLongScreenshotButtonState(localBinding.buttonLongScreenshotDone, actionsEnabled)
        applyLongScreenshotButtonState(localBinding.buttonLongScreenshotCancel, actionsEnabled)
        localBinding.buttonLongScreenshotCapture.visibility = View.VISIBLE
        localBinding.buttonLongScreenshotDone.visibility = View.VISIBLE
        localBinding.buttonLongScreenshotCancel.visibility = View.VISIBLE
        localBinding.progressLongScreenshot.visibility = if (longScreenshotFinishing) View.VISIBLE else View.GONE
        localBinding.buttonLongScreenshotCancel.setText(
            if (longScreenshotCancelPending) R.string.long_screenshot_cancel_confirm
            else R.string.long_screenshot_cancel
        )
        localBinding.rowFloatingButtons.alpha = if (longScreenshotSession != null) 0.6f else 1f
    }

    private fun applyLongScreenshotButtonState(button: Button, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else 0.45f
    }

    private fun hideLongScreenshotPanel() {
        longScreenshotHandler.removeCallbacks(resetLongScreenshotCancelRunnable)
        longScreenshotCancelPending = false
        longScreenshotFinishing = false
        binding?.run {
            longScreenshotPanel.visibility = View.GONE
            rowFloatingButtons.alpha = 1f
            applyLongScreenshotButtonState(buttonLongScreenshotCapture, true)
            applyLongScreenshotButtonState(buttonLongScreenshotDone, true)
            applyLongScreenshotButtonState(buttonLongScreenshotCancel, true)
            buttonLongScreenshotCancel.setText(R.string.long_screenshot_cancel)
            progressLongScreenshot.visibility = View.GONE
        }
    }

    private fun switchLongScreenshotToManual(status: String) {
        val session = longScreenshotSession ?: return
        session.mode = LongScreenshotMode.MANUAL
        session.waitingForCapture = false
        resetLongScreenshotCancelConfirmation()
        updateLongScreenshotPanel(status, manualCaptureEnabled = true)
    }

    private fun cancelLongScreenshotSession() {
        longScreenshotHandler.removeCallbacksAndMessages(null)
        longScreenshotCancelPending = false
        longScreenshotFinishing = false
        val session = longScreenshotSession
        longScreenshotSession = null
        hideLongScreenshotPanel()
        session?.sessionDir?.let { dir ->
            dir.listFiles()?.forEach { it.delete() }
            dir.delete()
        }
        if (temporaryOverlayForLongScreenshot) {
            temporaryOverlayForLongScreenshot = false
            hideFloatingButton()
        }
    }

    private fun finishLongScreenshotSession() {
        val session = longScreenshotSession ?: return
        if (longScreenshotFinishing) {
            return
        }
        longScreenshotHandler.removeCallbacksAndMessages(null)
        longScreenshotCancelPending = false

        if (session.frameCount == 0) {
            cancelLongScreenshotSession()
            return
        }

        if (session.frameCount == 1) {

            // TOOD save normal screenshot


            cancelLongScreenshotSession()
            return
        }
        longScreenshotFinishing = true
        updateLongScreenshotPanel(getLocalizedString(R.string.long_screenshot_status_finishing), manualCaptureEnabled = false)
        val frameFiles = com.github.cvzi.screenshottile.utils.loadLongScreenshotFrameFiles(session.sessionDir)
        val repeatedBottomInsetPx = getRepeatedBottomInsetPx()
        val frameSize = readLongScreenshotFrameSize(frameFiles.firstOrNull())
        val contentBounds = normalizeLongScreenshotBoundsForCapturedFrames(session.targetBounds, frameSize)
        val expectedCropTops = normalizeLongScreenshotExpectedCropTops(session.expectedCropTops, frameSize)
        Thread {
            val composed = composeLongScreenshotFromFiles(
                frameFiles,
                repeatedBottomInsetPx = repeatedBottomInsetPx,
                contentBounds = contentBounds,
                expectedCropTops = expectedCropTops
            )
            val debugFiles = if (BuildConfig.DEBUG) {
                saveLongScreenshotDebugArtifacts(
                    this,
                    frameFiles,
                    "long_screenshot_debug_${System.currentTimeMillis()}",
                    composed,
                    contentBounds,
                    expectedCropTops
                )
            } else {
                emptyList()
            }
            val saveImageResult = if (!composed.requiresReview) {
                Handler(Looper.getMainLooper()).post {
                    if (longScreenshotSession === session) {
                        updateLongScreenshotPanel(
                            getLocalizedString(R.string.long_screenshot_status_saving),
                            manualCaptureEnabled = false
                        )
                    }
                }
                saveBitmapToFile(
                    this,
                    composed.bitmap,
                    prefManager.fileNamePattern,
                    compressionPreference(applicationContext),
                    null,
                    useAppData = "saveToStorage" !in prefManager.postScreenshotActions,
                    directory = null
                )
            } else {
                null
            }
            Handler(Looper.getMainLooper()).post {
                if (debugFiles.isNotEmpty()) {
                    Log.d(TAG, "Long screenshot debug artifacts saved: ${debugFiles.joinToString()}")
                    getWinContext().toastMessage(
                        "Long screenshot debug images saved in the normal Screenshots folder",
                        ToastType.ACTIVITY,
                        Toast.LENGTH_LONG
                    )
                }
                longScreenshotSession = null
                hideLongScreenshotPanel()
                if (composed.requiresReview) {
                    if (temporaryOverlayForLongScreenshot) {
                        temporaryOverlayForLongScreenshot = false
                        hideFloatingButton()
                    }
                    startActivity(
                        LongScreenshotReviewActivity.newIntent(
                            this,
                            session.sessionDir,
                            composed.cropTops,
                            repeatedBottomInsetPx,
                            contentBounds,
                            expectedCropTops
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } else {
                    frameFiles.forEach { it.delete() }
                    session.sessionDir.delete()
                    onFileSaved(saveImageResult)
                    if (temporaryOverlayForLongScreenshot) {
                        temporaryOverlayForLongScreenshot = false
                        hideFloatingButton()
                    }
                }
            }
        }.start()
    }

    private fun readLongScreenshotFrameSize(file: File?): Point? {
        file ?: return null
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }
        return Point(options.outWidth, options.outHeight)
    }

    private fun normalizeLongScreenshotBoundsForCapturedFrames(bounds: Rect?, frameSize: Point?): Rect? {
        bounds ?: return null
        frameSize ?: return bounds
        if (!prefManager.autoCropEnabled) {
            return Rect(bounds)
        }
        val normalized = Rect(
            bounds.left - prefManager.autoCropLeft,
            bounds.top - prefManager.autoCropTop,
            bounds.right - prefManager.autoCropLeft,
            bounds.bottom - prefManager.autoCropTop
        )
        normalized.left = normalized.left.coerceIn(0, frameSize.x - 1)
        normalized.top = normalized.top.coerceIn(0, frameSize.y - 1)
        normalized.right = normalized.right.coerceIn(normalized.left + 1, frameSize.x)
        normalized.bottom = normalized.bottom.coerceIn(normalized.top + 1, frameSize.y)
        return normalized
    }

    private fun normalizeLongScreenshotExpectedCropTops(
        expectedCropTops: List<Int?>,
        frameSize: Point?
    ): IntArray? {
        if (expectedCropTops.isEmpty()) {
            return null
        }
        val frameHeight = frameSize?.y
        return expectedCropTops.map { expectedCropTop ->
            var normalized = expectedCropTop ?: 0
            if (prefManager.autoCropEnabled) {
                normalized -= prefManager.autoCropTop
            }
            if (frameHeight != null) {
                normalized = normalized.coerceIn(0, frameHeight - 1)
            } else {
                normalized = normalized.coerceAtLeast(0)
            }
            normalized
        }.toIntArray()
    }

    private fun updateLongScreenshotCounter(frameCount: Int) {
        binding?.textLongScreenshotCounter?.text = formatLocalizedString(
            R.string.long_screenshot_counter,
            frameCount
        )
    }

    private fun requestLongScreenshotCancelConfirmation() {
        longScreenshotCancelPending = true
        binding?.buttonLongScreenshotCancel?.setText(R.string.long_screenshot_cancel_confirm)
        longScreenshotHandler.removeCallbacks(resetLongScreenshotCancelRunnable)
        longScreenshotHandler.postDelayed(resetLongScreenshotCancelRunnable, 2500L)
    }

    private fun resetLongScreenshotCancelConfirmation() {
        longScreenshotHandler.removeCallbacks(resetLongScreenshotCancelRunnable)
        longScreenshotCancelPending = false
        binding?.buttonLongScreenshotCancel?.setText(R.string.long_screenshot_cancel)
    }

    private fun getRepeatedBottomInsetPx(): Int {
        var inset = 0
        val rootInsets = binding?.root?.rootWindowInsets
        if (rootInsets != null) {
            inset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val navigation = rootInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom
                val systemGestures = rootInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemGestures()).bottom
                val tappable = rootInsets.getInsetsIgnoringVisibility(WindowInsets.Type.tappableElement()).bottom
                maxOf(navigation, systemGestures, tappable)
            } else {
                @Suppress("DEPRECATION")
                maxOf(
                    rootInsets.systemWindowInsetBottom,
                    rootInsets.stableInsetBottom
                )
            }
        }
        if (inset <= 0) {
            inset = navigationBarSize(getWinContext()).y
        }
        Log.d(TAG, "Long screenshot repeatedBottomInsetPx=$inset")
        return inset.coerceAtLeast(0)
    }

    private fun captureLongScreenshotFrame(afterCapture: (LongScreenshotSession) -> Unit) {
        val session = longScreenshotSession ?: return
        if (session.waitingForCapture) {
            return
        }
        resetLongScreenshotCancelConfirmation()
        session.waitingForCapture = true
        binding?.buttonLongScreenshotCapture?.let { applyLongScreenshotButtonState(it, false) }
        binding?.root?.visibility = View.GONE
        binding?.root?.invalidate()
        longScreenshotHandler.postDelayed({
            captureCurrentBitmap(
                onSuccess = onSuccess@{ bitmap ->
                    binding?.root?.visibility = View.VISIBLE
                    saveLongScreenshotTempBitmap(session.sessionDir, session.frameCount, bitmap)
                    if (session.frameCount > 0) {
                        val previousBitmap = BitmapFactory.decodeFile(
                            File(session.sessionDir, String.format("%03d.png", session.frameCount - 1)).absolutePath
                        )
                        if (previousBitmap != null && areLongScreenshotFramesSimilar(previousBitmap, bitmap, session.targetBounds)) {
                            previousBitmap.recycle()
                            switchLongScreenshotToManual(getLocalizedString(R.string.long_screenshot_status_retry_manual))
                            session.waitingForCapture = false
                            binding?.buttonLongScreenshotCapture?.let { applyLongScreenshotButtonState(it, true) }
                            return@onSuccess
                        }
                        previousBitmap?.recycle()
                    }
                    session.frameCount += 1
                    updateLongScreenshotCounter(session.frameCount)
                    session.waitingForCapture = false
                    binding?.buttonLongScreenshotCapture?.let { applyLongScreenshotButtonState(it, true) }
                    afterCapture(session)
                },
                onFailure = {
                    binding?.root?.visibility = View.VISIBLE
                    session.waitingForCapture = false
                    binding?.buttonLongScreenshotCapture?.let { applyLongScreenshotButtonState(it, true) }
                    switchLongScreenshotToManual(getLocalizedString(R.string.long_screenshot_status_retry_manual))
                }
            )
        }, 120L)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun captureCurrentBitmap(
        onSuccess: (Bitmap) -> Unit,
        onFailure: () -> Unit
    ) {
        super.takeScreenshot(
            DEFAULT_DISPLAY,
            { runnable -> Thread(runnable).start() },
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val bitmap = Bitmap.wrapHardwareBuffer(
                        screenshot.hardwareBuffer,
                        screenshot.colorSpace
                    )?.copy(Bitmap.Config.ARGB_8888, false)
                    screenshot.hardwareBuffer.close()
                    if (bitmap == null) {
                        Handler(Looper.getMainLooper()).post { onFailure() }
                        return
                    }
                    val cropped = if (prefManager.autoCropEnabled) {
                        val rect = Rect(
                            prefManager.autoCropLeft,
                            prefManager.autoCropTop,
                            bitmap.width - prefManager.autoCropRight,
                            bitmap.height - prefManager.autoCropBottom
                        )
                        Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
                    } else {
                        bitmap
                    }
                    Handler(Looper.getMainLooper()).post { onSuccess(cropped) }
                }

                override fun onFailure(errorCode: Int) {
                    Log.e(TAG, "captureCurrentBitmap failed: $errorCode")
                    Handler(Looper.getMainLooper()).post { onFailure() }
                }
            }
        )
    }

    private fun advanceLongScreenshot(session: LongScreenshotSession): Boolean {
        val node = findBestScrollableNode(rootInActiveWindow)
        val bounds = Rect()
        node?.getBoundsInScreen(bounds)
        val scrolled = when {
            node != null && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) -> true
            bounds.height() > 0 -> dispatchScrollGesture(bounds)
            else -> false
        }
        if (!scrolled) {
            return false
        }
        session.targetBounds = if (bounds.height() > 0) Rect(bounds) else session.targetBounds
        longScreenshotHandler.postDelayed({
            captureLongScreenshotFrame(afterCapture = { updatedSession ->
                if (!advanceLongScreenshot(updatedSession)) {
                    switchLongScreenshotToManual(getLocalizedString(R.string.long_screenshot_status_retry_manual))
                }
            })
        }, 900L)
        return true
    }

    private fun dispatchScrollGesture(bounds: Rect): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }
        val path = android.graphics.Path().apply {
            val centerX = bounds.centerX().toFloat()
            moveTo(centerX, (bounds.bottom - bounds.height() * 0.2f))
            lineTo(centerX, (bounds.top + bounds.height() * 0.2f))
        }
        return dispatchGesture(
            android.accessibilityservice.GestureDescription.Builder()
                .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 350))
                .build(),
            null,
            null
        )
    }

    private var prefManager = App.getInstance().prefManager
    private var lastClickTime = 0L
    private val doubleClickThreshold = 300L // milliseconds
    private var longScreenshotSession: LongScreenshotSession? = null
    private var lastScrollableBounds: Rect? = null
    private val longScreenshotHandler = Handler(Looper.getMainLooper())
    private var temporaryOverlayForLongScreenshot = false
    private var longScreenshotCancelPending = false
    private var longScreenshotFinishing = false
    private val resetLongScreenshotCancelRunnable = Runnable {
        longScreenshotCancelPending = false
        binding?.buttonLongScreenshotCancel?.setText(R.string.long_screenshot_cancel)
    }

    override fun onServiceConnected() {
        instance = this

        setUserLanguage()

        updateLockscreenSetting()

        updatePackageFilter()

        try {
            when (prefManager.returnIfAccessibilityServiceEnabled) {
                SettingFragment.TAG -> {
                    // Return to settings
                    prefManager.returnIfAccessibilityServiceEnabled = null
                    SettingsActivity.startNewTask(this)
                }

                MainActivity.TAG -> {
                    // Return to main activity
                    prefManager.returnIfAccessibilityServiceEnabled = null
                    MainActivity.startNewTask(this)
                }

                NoDisplayActivity.TAG -> {
                    // Return to NoDisplayActivity activity i.e. finish()
                    prefManager.returnIfAccessibilityServiceEnabled = null
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
                "Could not start Activity for return to '${prefManager.returnIfAccessibilityServiceEnabled}'",
                e
            )
        }


        updateFloatingButton()
    }

    private fun getWinContext(): Context {
        var windowContext: Context = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !useThis) {
            val dm: DisplayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
            val defaultDisplay = dm.getDisplay(DEFAULT_DISPLAY)
            windowContext = createDisplayContext(defaultDisplay)
        }
        return windowContext
    }

    private fun getWinManager(): WindowManager {
        return getWinContext().getSystemService(WINDOW_SERVICE) as WindowManager
    }

    /**
     * Toggle the floating button according to the current settings
     */
    fun updateFloatingButton(forceRedraw: Boolean = false, animate: Boolean = true) {
        val hideInThisMode =
            (screenLocked && !prefManager.floatingButtonWhenLocked)
                    || (!screenLocked && !prefManager.floatingButtonWhenUnLocked)
                    || (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !prefManager.floatingButtonWhenLandscape)
                    || (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && !prefManager.floatingButtonWhenPortrait)

        val shouldBeShown = prefManager.floatingButton && !hideInThisMode
        if (shouldBeShown && !floatingButtonShown) {
            showFloatingButton(animate)
        } else if (!shouldBeShown && floatingButtonShown) {
            hideFloatingButton()
        } else if (shouldBeShown && forceRedraw) {
            hideFloatingButton()
            showFloatingButton(animate)
        }
    }

    /**
     * Update listeners for screen lock changes
     */
    fun updateLockscreenSetting() {
        prefManager.run {
            if (!floatingButton || (floatingButtonWhenLocked && floatingButtonWhenUnLocked)) {
                stopListeningForScreenLock()
            } else {
                listenForScreenLock()
            }
        }
        updateFloatingButton(forceRedraw = true)
    }


    private fun showFloatingButton(animate: Boolean = true) {
        floatingButtonShown = true

        binding =
            AccessibilityBarBinding.inflate(getWinContext().getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)

        binding?.root?.let { root ->
            configureFloatingButton(root, animate)
        }
    }

    private fun onClickAction(root: ViewGroup) {
        if (isDeviceLocked(this) && prefManager.preventIfLocked) {
            toastDeviceIsLocked(this)
            return
        }

        if (prefManager.floatingButtonAction == getString(R.string.setting_floating_action_value_partial)) {
            App.getInstance().screenshotPartial(this)
            return
        }

        if (prefManager.floatingButtonAction == getString(R.string.setting_floating_action_value_long)) {
            startLongScreenshotSession()
            return
        }

        val delayInSeconds = prefManager.floatingButtonDelay.toLong()
        val delayInMilliSeconds = if (delayInSeconds > 0) {
            1000L * delayInSeconds
        } else {
            5L
        }
        val buttonScreenshot = root.findViewById<ImageView>(R.id.buttonScreenshot)
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
                prefManager.floatingButtonAction == getString(R.string.setting_floating_action_value_legacy)
            if (legacyMethod) {
                NoDisplayActivity.startNewTaskLegacyScreenshot(this)
            } else {
                simulateScreenshotButton(autoHideButton = false, autoUnHideButton = false)
            }
            if (prefManager.floatingButtonHideAfter) {
                prefManager.floatingButton = false
                hideFloatingButton()
            } else if (!legacyMethod) {
                Handler(Looper.getMainLooper()).postDelayed({
                    showTemporaryHiddenFloatingButton(root, countDownTextView, buttonScreenshot)
                }, 1000L)
            }
        }, delayInMilliSeconds)
    }

    private fun configureFloatingButton(root: ViewGroup, animate: Boolean = true) {
        screenOrientation = resources.configuration.orientation

        val position = prefManager.getFloatingButtonPosition(screenOrientation)
        val shutterCollection = ShutterCollection(this, R.array.shutters, R.array.shutter_names)

        addWindowViewAt(root, position.x, position.y)

        val buttonScreenshot = root.findViewById<ImageView>(R.id.buttonScreenshot)
        val buttonLongCapture = root.findViewById<Button>(R.id.buttonLongScreenshotCapture)
        val buttonLongDone = root.findViewById<Button>(R.id.buttonLongScreenshotDone)
        val buttonLongCancel = root.findViewById<Button>(R.id.buttonLongScreenshotCancel)
        setShutterDrawable(this, buttonScreenshot, shutterCollection.current().normal)
        var buttonClose: TextView? = null
        val scale = prefManager.floatingButtonScale
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

        if (prefManager.floatingButtonShowClose && prefManager.floatingButtonCloseEmoji.isNotBlank()) {
            buttonClose = TextView(root.context)
            buttonClose.text = prefManager.floatingButtonCloseEmoji
            val linearLayout = root.findViewById<LinearLayout>(R.id.linearLayoutOuter)
            linearLayout.addView(buttonClose)
            buttonClose.layoutParams = LinearLayout.LayoutParams(buttonClose.layoutParams).apply {
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            buttonClose.setOnClickListener {
                prefManager.floatingButton = false
                hideFloatingButton()
                SettingFragment.instance?.get()?.updateFloatingButtonFromService()
            }
        }
        val alpha = prefManager.floatingButtonAlpha
        buttonScreenshot.alpha = alpha
        buttonClose?.alpha = alpha

        buttonScreenshot.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - lastClickTime
            lastClickTime = currentTime

            if (prefManager.floatingButtonTapType == TAP_TYPE_DOUBLE && timeDiff <= doubleClickThreshold) {
                // Double click detected
                onClickAction(root)
            } else if (prefManager.floatingButtonTapType == TAP_TYPE_SINGLE) {
                // Single click action
                onClickAction(root)
            }
        }

        buttonLongCapture.setOnClickListener {
            if (longScreenshotFinishing) {
                return@setOnClickListener
            }
            val session = longScreenshotSession ?: return@setOnClickListener
            if (session.mode == LongScreenshotMode.MANUAL) {
                updateLongScreenshotPanel(getLocalizedString(R.string.long_screenshot_status_manual), manualCaptureEnabled = false)
                captureLongScreenshotFrame {
                    updateLongScreenshotPanel(getLocalizedString(R.string.long_screenshot_status_manual), manualCaptureEnabled = true)
                }
            }
        }
        buttonLongDone.setOnClickListener {
            if (longScreenshotFinishing) {
                return@setOnClickListener
            }
            finishLongScreenshotSession()
        }
        buttonLongCancel.setOnClickListener {
            if (longScreenshotFinishing) {
                return@setOnClickListener
            }
            if (longScreenshotCancelPending) {
                cancelLongScreenshotSession()
            } else {
                requestLongScreenshotCancelConfirmation()
            }
        }
        updateLongScreenshotCounter(longScreenshotSession?.frameCount ?: 0)

        var dragDone = false
        buttonScreenshot.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DROP, DragEvent.ACTION_DRAG_ENDED -> {
                    root.let {
                        if (!dragDone) {
                            val x: Int
                            val y: Int
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU || event.action == DragEvent.ACTION_DROP) {
                                // x and y are relative to the inside of the view's bounding box
                                val old = prefManager.getFloatingButtonPosition(
                                    resources.configuration.orientation
                                )
                                x = (old.x - v.measuredWidth / 2.0 + event.x).toInt()
                                y = (old.y - v.measuredHeight / 2.0 + event.y).toInt()
                            } else {
                                val parent = v.parent as View
                                x = (event.x - parent.measuredWidth / 2).toInt()
                                y = (event.y - parent.measuredHeight / 2).toInt()
                            }
                            dragDone = true
                            updateWindowViewPosition(it, x, y)
                            prefManager.setFloatingButtonPosition(
                                Point(x, y),
                                resources.configuration.orientation
                            )
                        }
                        setShutterDrawable(
                            this,
                            buttonScreenshot,
                            shutterCollection.current().normal
                        )
                        buttonScreenshot.alpha = prefManager.floatingButtonAlpha
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

        if (animate) {
            (buttonScreenshot.drawable as? Animatable)?.start()
        }

        // Only Android 13+:
        // Periodically check if the button is still at correct position and reposition it if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Handler(Looper.getMainLooper()).apply {
                removeCallbacks(checkPositionRunnable)
                postDelayed(checkPositionRunnable, 10000)
            }
        }
    }

    private val checkPositionRunnable = Runnable {
        checkAndCorrectPosition()
    }

    private fun checkAndCorrectPosition() {
        // Check if the floating button is still at the correct position and reposition it if necessary
        if (!floatingButtonShown) {
            return
        }
        val root = binding?.root ?: return
        val p = prefManager.getFloatingButtonPosition(screenOrientation)
        val x = p.x
        val y = p.y
        val layoutParams = root.layoutParams as WindowManager.LayoutParams
        if (layoutParams.x != x || layoutParams.y != y) {
            updateWindowViewPosition(root, x, y)
        }
        Handler(Looper.getMainLooper()).postDelayed(checkPositionRunnable, 10000)
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


    @SuppressLint("SetTextI18n")
    private fun showSettingsButton(root: ViewGroup, buttonScreenshot: View) {
        val linearLayout = root.findViewById<LinearLayout>(R.id.linearLayoutOuter)
        if (linearLayout.findViewWithTag<View>(SETTINGS_BUTTON_TAG) != null) {
            return
        }
        val textView = TextView(root.context)
        textView.tag = SETTINGS_BUTTON_TAG
        textView.alpha = prefManager.floatingButtonAlpha
        @SuppressLint("SetTextI18n")
        textView.text = "\u2699\uFE0F"
        linearLayout.addView(textView)
        textView.layoutParams = LinearLayout.LayoutParams(textView.layoutParams).apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        textView.setOnClickListener {
            @SuppressLint("SetTextI18n")
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

        if (prefManager.packageNameFilterEnabled && lastPackageName.isNotBlank()) {
            // Show Blacklist/Whitelist this app button
            val buttonBlackWhiteList = TextView(root.context).apply {
                linearLayout.addView(this)
                setPadding(2, 3, 2, 3)

                setBackgroundColor(0xffffffff.toInt())
                setTextColor(0xffaaaaaa.toInt())
                setOnClickListener { v ->
                    prefManager.packageNameFilterList = prefManager.packageNameFilterList.apply {
                        if (contains(lastPackageName)) {
                            remove(lastPackageName.toString())
                        } else {
                            add(lastPackageName.toString())
                        }
                        (v as? TextView?)?.text = "✅"
                    }
                    updatePackageFilter()
                }
                alpha = prefManager.floatingButtonAlpha
            }


            if (prefManager.packageNameFilterList.contains(lastPackageName)) {
                // This app is in the filter list
                if (prefManager.packageNameFilterMode == PackageNameFilterMode.BLACKLIST) {
                    buttonBlackWhiteList.text =
                        "➖ ${getLocalizedString(R.string.setting_filtermode_blacklist)}"
                } else {
                    buttonBlackWhiteList.text =
                        "➖ ${getLocalizedString(R.string.setting_filtermode_whitelist)}"
                }
            } else {
                // This app is not in the filter list
                if (prefManager.packageNameFilterMode == PackageNameFilterMode.BLACKLIST) {
                    buttonBlackWhiteList.text =
                        "➕ ${getLocalizedString(R.string.setting_filtermode_blacklist)}"
                } else {
                    buttonBlackWhiteList.text =
                        "➕ ${getLocalizedString(R.string.setting_filtermode_whitelist)}"
                }
            }
            buttonScreenshot.post {
                buttonBlackWhiteList.textSize = textView.textSize / 4
            }
            buttonBlackWhiteList.postDelayed({
                linearLayout.safeRemoveView(buttonBlackWhiteList, TAG)
            }, 2000)

        }

    }

    /**
     * Remove view if it exists
     */
    fun hideFloatingButton() {
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
            // Allow the floating button to cover the camera notch/cutout
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }


    /**
     * Simulate screenshot button (home+power) press
     * Return true on success
     */
    fun simulateScreenshotButton(
        autoHideButton: Boolean,
        autoUnHideButton: Boolean,
        useTakeScreenshotMethod: Boolean = true,
        useSystemDefaults: Boolean? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false
        }
        if (autoHideButton) {
            temporaryHideFloatingButton()
        }

        val isUseSystemDefaults = useSystemDefaults ?: prefManager.useSystemDefaults

        val success: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && useTakeScreenshotMethod && !isUseSystemDefaults) {
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
                prefManager.screenshotCount++
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
                        val cutOutRect: Rect? =
                            if (prefManager.autoCropEnabled) {
                                val rect = Rect(
                                    prefManager.autoCropLeft,
                                    prefManager.autoCropTop,
                                    bitmap.width - prefManager.autoCropRight,
                                    bitmap.height - prefManager.autoCropBottom
                                )
                                Log.d(TAG, "Set auto crop to $rect")
                                rect
                            } else {
                                null
                            }

                        Log.d(TAG, "saveBitmapToFile() cutOutRect = $cutOutRect")
                        val saveImageResult = saveBitmapToFile(
                            this@ScreenshotAccessibilityService,
                            bitmap,
                            prefManager.fileNamePattern,
                            compressionPreference(applicationContext),
                            cutOutRect,
                            useAppData = "saveToStorage" !in prefManager.postScreenshotActions,
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

        val postScreenshotActions = prefManager.postScreenshotActions

        val result = saveImageResult as? SaveImageResultSuccess?
        when {
            result == null -> {
                AppFunctionResultStore.setLastFailed("Failed to cast SaveImageResult path/uri")
                screenShotFailedToast("Failed to cast SaveImageResult")
            }

            result.uri != null -> {
                // Android Q+ works with MediaStore content:// URI
                var dummyPath =
                    "${Environment.DIRECTORY_PICTURES}/${TakeScreenshotActivity.SCREENSHOT_DIRECTORY}/${result.fileTitle}"
                if (result.dummyPath.isNotEmpty()) {
                    dummyPath = result.dummyPath
                }

                AppFunctionResultStore.setLastReady(
                    result.uri,
                    result.bitmap.width,
                    result.bitmap.height
                )


                if ("showToast" in postScreenshotActions) {
                    getWinContext().toastMessage(
                        formatLocalizedString(R.string.screenshot_file_saved, dummyPath),
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
                prefManager.screenshotCount++
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

                AppFunctionResultStore.setLastReady(uri, result.bitmap.width, result.bitmap.height)

                if ("showToast" in postScreenshotActions) {
                    getWinContext().toastMessage(
                        formatLocalizedString(R.string.screenshot_file_saved, path),
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
                prefManager.screenshotCount++
                handlePostScreenshot(
                    this,
                    postScreenshotActions,
                    uri,
                    result.mimeType,
                    result.bitmap
                )
            }

            else -> {
                AppFunctionResultStore.setLastFailed("Failed to cast SaveImageResult path/uri")
                screenShotFailedToast("Failed to cast SaveImageResult path/uri")
            }
        }
    }

    private fun screenShotFailedToast(errorMessage: String? = null) {
        val message = getLocalizedString(R.string.screenshot_failed) + if (errorMessage != null) {
            "\n$errorMessage"
        } else {
            ""
        }
        getWinContext().toastMessage(message, ToastType.ERROR)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (screenOrientation != newConfig.orientation) {
            screenOrientation = newConfig.orientation
            val positions = prefManager.getFloatingButtonPositions()
            if (screenOrientation !in positions &&
                ((screenOrientation == 2 && 1 in positions) || (screenOrientation == 1 && 2 in positions))
            ) {
                // Try to restore the position from the other orientation
                var x = positions[1]?.x ?: positions[2]?.x ?: 150
                var y = positions[1]?.y ?: positions[2]?.x ?: 50
                val ratio =
                    resources.displayMetrics.widthPixels.toFloat() / resources.displayMetrics.heightPixels.toFloat()
                x = (x * ratio).toInt()
                y = (y / ratio).toInt()
                prefManager.setFloatingButtonPosition(Point(x, y), 2)
            }
            updateFloatingButton(forceRedraw = true)
        }
    }


    inner class OnUnLockBroadcastReceiver : BroadcastReceiver() {

        private var registered = false
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_USER_PRESENT -> {
                    screenLocked = false
                    updateFloatingButton()
                }

                Intent.ACTION_SCREEN_OFF -> {
                    screenLocked = true
                    updateFloatingButton()
                }

                Intent.ACTION_SCREEN_ON -> {
                    screenLocked = isDeviceLocked(this@ScreenshotAccessibilityService)
                    updateFloatingButton()
                }
            }
        }

        fun register() {
            if (registered) {
                return
            }
            registerReceiver(this, IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            })
            registered = true
        }

        fun unregister() {
            try {
                unregisterReceiver(this)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "IllegalArgumentException in OnUnLockBroadcastReceiver", e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "RuntimeException in OnUnLockBroadcastReceiver", e)
            }
            registered = false
        }
    }

    fun onDeviceLockedFromTileService() {
        screenLocked = true
        updateFloatingButton()
    }


    private fun listenForScreenLock() {
        if (onUnLockBroadcastReceiver == null) {
            onUnLockBroadcastReceiver = OnUnLockBroadcastReceiver()
        }
        onUnLockBroadcastReceiver?.register()

        FloatingTileService.informAccessibilityServiceOnLocked = true
        ScreenshotTileService.informAccessibilityServiceOnLocked = true
    }

    private fun stopListeningForScreenLock() {
        onUnLockBroadcastReceiver?.unregister()
        onUnLockBroadcastReceiver = null
        FloatingTileService.informAccessibilityServiceOnLocked = false
        ScreenshotTileService.informAccessibilityServiceOnLocked = false
    }


    override fun onDestroy() {
        cancelLongScreenshotSession()
        stopListeningForScreenLock()
        instance = null
        super.onDestroy()
    }

    fun updatePackageFilter() {
        val prefManager = prefManager
        packageFilterNameList.clear()
        packageFilterEnabled = prefManager.packageNameFilterEnabled
        if (packageFilterEnabled) {
            packageFilterNameList.addAll(prefManager.packageNameFilterList)
            packageFilterMode = prefManager.packageNameFilterMode
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED || event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            event.source?.takeIf { it.isScrollable }?.let { node ->
                val rect = Rect()
                node.getBoundsInScreen(rect)
                lastScrollableBounds = rect
                longScreenshotSession?.let { session ->
                    session.targetBounds = Rect(rect)
                    if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                        val deltaY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            event.scrollDeltaY
                        } else {
                            0
                        }
                        if (deltaY > 0) {
                            val expectedCropTop = (
                                rect.top + (rect.height() - deltaY)
                                ).coerceIn(0, rect.bottom.coerceAtLeast(1) - 1)
                            while (session.expectedCropTops.size <= session.frameCount) {
                                session.expectedCropTops.add(null)
                            }
                            session.expectedCropTops[session.frameCount] = expectedCropTop
                            Log.d(
                                TAG,
                                "Long screenshot expected crop for frame ${session.frameCount}: $expectedCropTop (deltaY=$deltaY)"
                            )
                        }
                    }
                }
            }
        }
        if (event.isFullScreen &&
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.packageName != lastPackageName
        ) {
            if (event.packageName != packageName) {
                lastPackageName = event.packageName
            }

            if (!packageFilterEnabled) {
                return
            }

            if (packageFilterTempForceShow) {
                // Temporary show floating button despite filter until the next app change but at least 60s
                if (System.currentTimeMillis() - packageFilterTempOverrideTime < 60000) {
                    return
                } else {
                    packageFilterTempForceShow = false
                }
            } else if (packageFilterTempForceHide) {
                // Temporary hide floating button despite filter until the next app change but at least 60s
                if (System.currentTimeMillis() - packageFilterTempOverrideTime < 60000) {
                    return
                } else {
                    packageFilterTempForceHide = false
                }
            }
            if (packageFilterMode == PackageNameFilterMode.BLACKLIST) {
                if (floatingButtonShown && packageFilterNameList.contains(event.packageName)) {
                    hideFloatingButton()
                } else if (!floatingButtonShown) {
                    updateFloatingButton(animate = false)
                }
            } else {
                if (!floatingButtonShown && packageFilterNameList.contains(event.packageName)) {
                    updateFloatingButton(animate = false)
                } else if (floatingButtonShown) {
                    hideFloatingButton()
                }
            }
        }
    }

    fun overridePackageFilterTempShow() {
        packageFilterTempForceShow = true
        packageFilterTempOverrideTime = System.currentTimeMillis()
        showFloatingButton()
    }

    fun overridePackageFilterTempHide() {
        packageFilterTempForceShow = false
        packageFilterTempOverrideTime = System.currentTimeMillis()
        hideFloatingButton()
    }

    override fun onInterrupt() {
        // No op
    }
}
