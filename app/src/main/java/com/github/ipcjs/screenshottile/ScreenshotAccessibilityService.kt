package com.github.ipcjs.screenshottile

import android.accessibilityservice.AccessibilityService

import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Created by cuzi (cuzi@openmail.cc) on 2019/12/26.
 */

class ScreenshotAccessibilityService : AccessibilityService() {
    companion object {
        var instance: ScreenshotAccessibilityService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }

    /**
     * Simulate screenshot button (home+power) press
     * Return true on success
     */
    fun simulateScreenshotButton(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false
        }
        return performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {}
    override fun onInterrupt() {}
}
