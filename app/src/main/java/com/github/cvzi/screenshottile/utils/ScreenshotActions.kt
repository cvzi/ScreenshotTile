package com.github.cvzi.screenshottile.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import com.burhanrashid52.photoediting.EditImageActivity
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.activities.GenericPostActivity
import com.github.cvzi.screenshottile.activities.PostActivity
import com.github.cvzi.screenshottile.activities.PostCropActivity
import com.github.cvzi.screenshottile.activities.TakeScreenshotActivity
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService

/**
 * Start screenshot activity and take a screenshot
 */
fun screenshotLegacyOnly(context: Context) {
    TakeScreenshotActivity.start(context, false)
}

/**
 * Start screenshot activity and take a screenshot
 */
fun screenshot(context: Context, partial: Boolean = false) {
    if (partial || !tryNativeScreenshot()) {
        TakeScreenshotActivity.start(context, partial)
    }
}

/**
 * Try to take a screenshot from the accessibility service/system method if it's enabled and available
 */
fun tryNativeScreenshot(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && App.getInstance().prefManager.useNative) {
        return ScreenshotAccessibilityService.instance?.simulateScreenshotButton(
            autoHideButton = true,
            autoUnHideButton = true
        ) == true
    }
    return false
}

/**
 * Handle the following post screenshot actions:
 * "openInPost", "openInPostCrop", "openInPhotoEditor", "openInExternalEditor", "openInExternalViewer", "openShare"
 */
fun handlePostScreenshot(
    context: Context,
    postScreenshotActions: ArrayList<String>,
    uri: Uri,
    mimeType: String? = null,
    fullBitmap: Bitmap? = null
) {
    val app = App.getInstance()
    app.lastScreenshot = null
    val mimeTypeNullSafe = mimeType ?: "image/*"
    if ("playTone" in postScreenshotActions) {
        Sound.playTone()
    }
    when {
        "openInPost" in postScreenshotActions -> {
            app.lastScreenshot = fullBitmap
            App.getInstance().startActivityAndCollapseIfNotActivity(
                context,
                PostActivity.newIntentSingleImageBitmap(context, uri)
            )
        }

        "openInPostCrop" in postScreenshotActions -> {
            app.lastScreenshot = fullBitmap
            App.getInstance().startActivityAndCollapseIfNotActivity(
                context,
                PostCropActivity.newIntentSingleImageBitmap(context, uri, mimeTypeNullSafe)
            )
        }

        "openInPhotoEditor" in postScreenshotActions -> {
            app.lastScreenshot = fullBitmap
            App.getInstance().startActivityAndCollapseIfNotActivity(
                context,
                Intent(context, EditImageActivity::class.java).apply {
                    action = Intent.ACTION_EDIT
                    putExtra(GenericPostActivity.BITMAP_FROM_LAST_SCREENSHOT, true)
                    setDataAndNormalize(uri)
                })
        }

        "openInExternalEditor" in postScreenshotActions -> {
            val editIntent = editImageChooserIntent(context, uri, mimeTypeNullSafe)
            editIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (editIntent.resolveActivity(context.packageManager) != null) {
                App.getInstance().startActivityAndCollapseIfNotActivity(context, editIntent)
            } else {
                Log.e(UTILSKT, "openInExternalEditor: resolveActivity(editIntent) returned null")
                context.toastMessage("No suitable external photo editor found", ToastType.ERROR)
            }
        }

        "openInExternalViewer" in postScreenshotActions -> {
            val openImageIntent = openImageIntent(context, uri, mimeTypeNullSafe)
            if (openImageIntent.resolveActivity(context.packageManager) != null) {
                App.getInstance().startActivityAndCollapseIfNotActivity(context, openImageIntent)
            } else {
                Log.e(
                    UTILSKT,
                    "openInExternalViewer: resolveActivity(openImageIntent) returned null"
                )
                context.toastMessage("No suitable external photo viewer found", ToastType.ERROR)
            }
        }

        "openShare" in postScreenshotActions -> {
            val shareIntent = shareImageChooserIntent(context, uri, mimeTypeNullSafe)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (shareIntent.resolveActivity(context.packageManager) != null) {
                App.getInstance().startActivityAndCollapseIfNotActivity(context, shareIntent)
            } else {
                Log.e(UTILSKT, "openShare: resolveActivity(shareIntent) returned null")
                context.toastMessage("No suitable app for sharing found", ToastType.ERROR)
            }
        }
    }
}

/**
 * Show toast error that the phone is locked and no screenshot was taken
 * (Toast messages are probably not shown on lock-screen though)
 */
fun toastDeviceIsLocked(context: Context) {
    context.toastMessage("Screenshot prevented: Device is locked", ToastType.ERROR)
}

/**
 * Check if the phone is currently locked
 */
fun isDeviceLocked(context: Context): Boolean {
    return (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked
}
