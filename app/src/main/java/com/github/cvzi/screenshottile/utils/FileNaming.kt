package com.github.cvzi.screenshottile.utils

import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.services.ScreenshotAccessibilityService
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.random.Random

/**
 * Get mime type from file extension
 */
fun mimeFromFileExtension(fileExtension: String): String {
    return when (fileExtension.lowercase()) {
        "jpg" -> "image/jpeg"
        else -> "image/${fileExtension.lowercase()}"
    }
}

/**
 * Format the filename
 */
fun formatFileName(fileNamePattern: String, date: Date): String {
    val timeStamp: String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(date)
    val counter = App.getInstance().prefManager.screenshotCount.toString()

    var fileName = fileNamePattern.replace("%timestamp%", timeStamp)
    fileName = fileName.replace("%counter%", counter.padStart(max(5, counter.length), '0'))
    while (fileName.contains("%randint%")) {
        val randInt = Random.nextInt(0, Int.MAX_VALUE).toString()
            .padStart(Int.MAX_VALUE.toString().length, '0')
        fileName = fileName.replaceFirst("%randint%", randInt)
    }
    while (fileName.contains("%random%")) {
        fileName = fileName.replaceFirst("%random%", UUID.randomUUID().toString())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && (fileName.contains("%app%") || fileName.contains(
            "%package%"
        ))
    ) {
        val service = ScreenshotAccessibilityService.instance
        val packageName = service?.getForegroundPackageName() ?: ""
        fileName = fileName.replace("%package%", sanitizeFilename(packageName))
        if (fileName.contains("%app%")) {
            val appLabel = resolveAppLabel(packageName)
            fileName = fileName.replace("%app%", sanitizeFilename(appLabel))
        }
    }
    return fileName
}

private fun resolveAppLabel(packageName: String): String {
    if (packageName.isBlank()) {
        return ""
    }

    val context = App.getInstance()
    val pm = context.packageManager

    // Try launcher activity
    try {
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(
                    launchIntent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(launchIntent, 0)
            }

            resolveInfo?.let {
                val label = it.loadLabel(pm).toString()
                if (label.isNotBlank()) {
                    return label
                }
            }
        }
    } catch (_: Exception) {
        // no-op
    }

    // Try ApplicationInfo
    try {
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        return pm.getApplicationLabel(appInfo).toString()
    } catch (_: Exception) {
        // no-op
    }

    // Fallback
    return shortPackageName(packageName)
}

/**
 * Get last part of package name
 */
private fun shortPackageName(pkg: String): String {
    val parts = pkg.split('.')
    return when {
        parts.size >= 2 -> parts.takeLast(2).joinToString(".")
        else -> pkg
    }
}

/**
 * Remove characters that are not allowed in filenames
 */
private fun sanitizeFilename(name: String): String {
    return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
}
