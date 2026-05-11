package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.github.cvzi.screenshottile.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Find the cache directory with maximum free space
 */
private const val TAG = "AppDataCleanup"

@Suppress("unused")
fun getCacheMaxFreeSpace(context: Context): File? {
    val cacheDirs = context.externalCacheDirs
    if (cacheDirs.isNullOrEmpty()) {
        return null
    }
    val maxIndex =
        cacheDirs.indices.maxByOrNull { index ->
            if (cacheDirs[index] == null) {
                0
            } else {
                StatFs(cacheDirs[index].path).availableBytes
            }
        } ?: -1
    if (maxIndex == -1) {
        return null
    }
    return cacheDirs[maxIndex]
}

fun cleanUpAppData(context: Context) = cleanUpAppData(context, null, null)
fun cleanUpAppData(context: Context, keepMaxFiles: Int? = null, onDeleted: (() -> Unit)? = null) {
    CoroutineScope(Job() + Dispatchers.IO).launch(Dispatchers.IO) {
        try {
            val keepMax = keepMaxFiles ?: App.getInstance().prefManager.keepAppDataMax
            Log.d(TAG, "cleanUpAppData[keepMaxFiles=$keepMaxFiles, keepMax=$keepMax]")
            if (keepMax < 0) {
                return@launch
            }
            val folder = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val fileList =
                folder?.listFiles()?.map { file ->
                    val lastModified = try {
                        file.lastModified()
                    } catch (_: Exception) {
                        null
                    }
                    Pair(file, lastModified)
                }?.sortedByDescending { it.second }

            if (fileList != null && fileList.size > keepMax) {
                for (i in keepMax until fileList.size) {
                    Log.d(TAG, "cleanUpAppData() delete ${fileList[i].first}")
                    fileList[i].first.delete()
                }
            }
            if (onDeleted != null) {
                withContext(Dispatchers.Main) {
                    onDeleted.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "cleanUpAppData Error", e)
        }
    }
}
