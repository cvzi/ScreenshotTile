package com.github.cvzi.screenshottile.utils

/*
 * Created by cuzi (cuzi@openmail.cc) on 2020/12/13.
 */

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.ArrayRes
import com.github.cvzi.screenshottile.App

/**
 * Obtains all available and the currently selected shutter
 */
class ShutterCollection(
    context: Context,
    @ArrayRes arrayResourceId: Int,
    @ArrayRes namesArrayResourceId: Int
) {
    /**
     * Represents a drawable shutter and its name
     */
    inner class Shutter(val name: String, val normal: Int, val move: Int)

    private val list: MutableList<Shutter> = mutableListOf()

    var index = 0
        get() {
            field = App.getInstance().prefManager.floatingButtonShutter
            if (field < 0 || field >= list.size) {
                field = 0
            }
            return field
        }
        set(value) {
            App.getInstance().prefManager.floatingButtonShutter = value
            field = value
        }

    init {
        val availableShutters = context.resources.obtainTypedArray(arrayResourceId)
        for (i in 0 until availableShutters.length()) {
            val currentShutters =
                context.resources.obtainTypedArray(availableShutters.getResourceId(i, -1))

            @Suppress("USELESS_CAST")
            val normal = currentShutters.getResourceId(0, -1)

            @SuppressLint("ResourceType")
            val move = currentShutters.getResourceId(1, -1)
            val name = context.resources.getStringArray(namesArrayResourceId)[i]
            list.add(Shutter(name, normal, move))
            currentShutters.recycle()
        }
        availableShutters.recycle()
    }

    fun current(): Shutter {
        return list[index]
    }

    fun getNames(): Array<String> {
        return Array(list.size) { i -> list[i].name }
    }

    fun getNormals(): Array<Int> {
        return Array(list.size) { i -> list[i].normal }
    }
}
