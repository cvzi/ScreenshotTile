package com.github.cvzi.screenshottile.functions

import android.net.Uri
import android.util.Log
import java.util.concurrent.ConcurrentHashMap


object AppFunctionResultStore {
    private val map = ConcurrentHashMap<String, Record>()
    private var lastId: String? = null
    private var lastTime: Long? = null
    fun setLastReady(uri: Uri, w: Int, h: Int) {
        lastId?.let { id ->
            val time = lastTime
            // Let it expire
            if (time != null && System.currentTimeMillis() - time < 60 * 1000) {
                setReady(id, uri, w, h)
            } else {
                Log.v("AppFunctionResultStore", "expired: $id")
            }
        }
    }

    fun setLastFailed(msg: String?) {
        lastId?.let { id ->
            val time = lastTime
            if (time != null && System.currentTimeMillis() - time < 60 * 1000) {
                setFailed(id, msg ?: "")
            } else {
                Log.v("AppFunctionResultStore", "expired: $id")
            }
        }
    }

    fun prepare(id: String) {
        lastId = id
        lastTime = System.currentTimeMillis()
        map[id] = Record(Uri.EMPTY, 0, 0, null, true)
    }

    fun setReady(id: String, uri: Uri, w: Int, h: Int) {
        map[id] = Record(uri, w, h, null, false)
    }

    fun setFailed(id: String, msg: String) {
        map[id] = Record(Uri.EMPTY, 0, 0, msg, false)
    }

    fun peek(id: String): Record? = map[id]
    data class Record(
        val uri: Uri,
        val width: Int,
        val height: Int,
        val failedMessage: String?,
        val pending: Boolean
    )
}