package com.github.cvzi.screenshottile.utils

import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.net.URLDecoder

/**
 * Try to generate a path that can be understood by humans
 */
fun nicePathFromUri(documentFile: DocumentFile): String {
    return if (documentFile.name.isNullOrEmpty()) {
        nicePathFromUri(documentFile.uri)
    } else {
        documentFile.name.toString()
    }
}

/**
 * Try to generate a folder name that can be understood by humans
 */
fun nicePathFromUri(uri: Uri): String {
    return nicePathFromUri(uri.toString())
}

/**
 * Try to generate a folder name that can be understood by humans
 */
fun nicePathFromUri(str: String?): String {
    if (str == null) {
        return "null"
    }
    var path = URLDecoder.decode(str, "UTF-8")
    path = path.split("/").last()
    if (path.startsWith("primary:")) {
        path = path.substring(8)
    }
    return path
}

/**
 * Try to generate a full path that can be understood by humans
 */
fun niceFullPathFromUri(uri: Uri): String {
    return niceFullPathFromUri(uri.toString())
}

/**
 * Try to generate a full path that can be understood by humans
 */
fun niceFullPathFromUri(str: String?): String {
    if (str == null) {
        return "null"
    }
    var path = URLDecoder.decode(str, "UTF-8")
    var parts = path.split("/").toMutableList()

    if (parts[3] == "tree") {
        // e.g. content://com.android.externalstorage.documents/tree/1B1A-210F:my/test/folder
        parts = parts.slice(4 until parts.size).toMutableList()
    }
    if (parts[0].endsWith(":")) { // e.g."content:"
        parts.removeAt(0)
    }
    var removeUntil = -1
    for (i in 0 until parts.size) {
        if (parts[i].startsWith("com.android.")) {
            removeUntil = i
        }
    }
    if (removeUntil != -1) {
        parts = parts.slice(removeUntil + 1 until parts.size).toMutableList()
    }
    Log.v(UTILSKT, "niceFullPathFromUri() parts: $parts")

    path = parts.joinToString("/")

    if (path.startsWith("primary:")) {
        path = path.substring(8)
    }

    return path
}
