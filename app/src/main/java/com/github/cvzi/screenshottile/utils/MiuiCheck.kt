package com.github.cvzi.screenshottile.utils

import android.os.Build
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

// Source: https://gist.github.com/starry-shivam/901267c26eb030eb3faf1ccd4d2bdd32

object MiuiCheck {

    /**
     * Check if the device is running on MIUI.
     *
     * By default, HyperOS is included from the check.
     * If you want to exclude HyperOS in the check, set excludeHyperOS to true.
     *
     * @param excludeHyperOS Whether to exclude HyperOS
     * @return True if the device is running on MIUI, false otherwise
     */
    fun isMiui(excludeHyperOS: Boolean = false): Boolean {
        // Check if the device is manufactured by Xiaomi, Redmi, or POCO.
        val brand = Build.BRAND.lowercase()
        if (!setOf("xiaomi", "redmi", "poco").contains(brand)) return false
        // This property is present in both MIUI and HyperOS.
        val isMiui = !getProperty("ro.miui.ui.version.name").isNullOrBlank()
        // This property is exclusive to HyperOS only and isn't present in MIUI.
        val isHyperOS = !getProperty("ro.mi.os.version.name").isNullOrBlank()
        return isMiui && (!excludeHyperOS || !isHyperOS)
    }

    // Private function to get the property value from build.prop.
    private fun getProperty(property: String): String? {
        return try {
            Runtime.getRuntime().exec("getprop $property").inputStream.use { input ->
                BufferedReader(InputStreamReader(input), 1024).readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
