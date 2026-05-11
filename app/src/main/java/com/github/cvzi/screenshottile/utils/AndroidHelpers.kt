package com.github.cvzi.screenshottile.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.LocaleManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.VersionedPackage
import android.os.Build
import android.os.LocaleList
import android.service.quicksettings.TileService
import android.util.Log
import android.view.View
import android.view.ViewManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.DialogFragment
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.ToastType

private const val TAG = "AndroidHelpers"

/**
 * Was the app updated or newly installed
 */
fun isNewAppInstallation(context: Context): Boolean {
    return try {
        return context.packageManager.getPackageInfo(context.packageName)?.run {
            firstInstallTime == lastUpdateTime
        } != false
    } catch (e: PackageManager.NameNotFoundException) {
        Log.e(TAG, "Package not found", e)
        true
    } catch (e: java.lang.Exception) {
        Log.e(TAG, "Unexpected error in isNewAppInstallation()", e)
        false
    }
}

/**
 * Check if F-Droid client is installed
 */
fun hasFdroid(context: Context): Boolean {
    val packageManager = context.packageManager
    for (name in arrayOf(
        "org.fdroid.fdroid",
        "org.fdroid.basic"
    )) {
        try {
            packageManager.getPackageInfo(name)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, e.toString())
        }
    }
    return false
}

/**
 * Show a string as a Toast message
 */
fun Context?.toastMessage(text: String, toastType: ToastType, duration: Int = Toast.LENGTH_LONG) {
    this?.run {
        val prefManager = App.getInstance().prefManager
        val showToast = prefManager.toasts && when (toastType) {
            ToastType.SUCCESS -> prefManager.successToasts
            ToastType.ERROR -> prefManager.errorToasts
            ToastType.NAGGING -> prefManager.naggingToasts
            else -> true
        }
        if (showToast) {
            Toast.makeText(this, text, duration).show()
        } else if (BuildConfig.DEBUG) {
            Log.v("SUPPRESSED_TOAST", text)
        }
    }
}

/**
 * Show a string from a resource as a Toast message
 */
fun Context?.toastMessage(resource: Int, toastType: ToastType, duration: Int = Toast.LENGTH_LONG) {
    this?.toastMessage(getLocalizedString(resource), toastType, duration)
}

/**
 * Call dismiss() on a Dialog and catch the Exceptions that is thrown if the context
 * of the dialog was already destroyed or the fragment is in the background
 */
fun DialogInterface.safeDismiss(tag: String = TAG) {
    if (this is Dialog && !isShowing) {
        return
    }
    try {
        this.dismiss()
    } catch (e0: IllegalArgumentException) {
        Log.e(tag, "safeDismiss() of $this threw e0: $e0")
    } catch (e1: IllegalStateException) {
        Log.e(tag, "safeDismiss() of $this threw e1: $e1")
    }
}

/**
 * Call dismiss() on a DialogFragment and catch the Exceptions
 */
fun DialogFragment.safeDismiss(tag: String = TAG) {
    if (dialog?.isShowing != true) {
        return
    }
    try {
        this.dismiss()
    } catch (e0: IllegalArgumentException) {
        Log.e(tag, "safeDismiss() of $this threw e0: $e0")
    } catch (e1: IllegalStateException) {
        Log.e(tag, "safeDismiss() of $this threw e1: $e1")
    }
}

/**
 * Call removeView() and catch Exceptions
 */
fun ViewManager.safeRemoveView(view: View, tag: String = TAG) {
    try {
        this.removeView(view)
    } catch (e: Exception) {
        Log.e(tag, "removeView() of $this threw e: $e")
    }
}

/**
 * Retrieve overall information about highest version of an application package that is installed
 * on the system
 */
fun PackageManager.getPackageInfo(packageName: String): PackageInfo? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(
            VersionedPackage(packageName, PackageManager.VERSION_CODE_HIGHEST),
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        getPackageInfo(packageName, 0)
    }
}

@SuppressLint("StartActivityAndCollapseDeprecated")
fun TileService.startActivityAndCollapseCustom(intent: android.content.Intent) {
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
        this.startActivityAndCollapse(
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )
    } else {
        @Suppress("DEPRECATION")
        this.startActivityAndCollapse(intent)
    }
}

/**
 * Set the user language for the app from the userLanguages list in the preferences or use the default android settings
 */
fun Context.setUserLanguage(force: Boolean = false) {
    val userLanguages = App.getInstance().prefManager.userLanguages
    if (userLanguages?.isNotBlank() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.forLanguageTags(userLanguages)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(userLanguages))
        }
    } else if (force) {
        // Use default android settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java)?.applicationLocales =
                LocaleList.getEmptyLocaleList()
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
    }
}
