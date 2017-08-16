package com.github.ipcjs.screenshottile

import com.github.ipcjs.screenshottile.TestUtil.p
import org.junit.Test

/**
 * Created by ipcjs on 2017/8/17.
 */
class AllTest {
    @Test
    fun testInt() {
        try {
            p("".toInt())
        } catch(e: NumberFormatException) {
            p(">>>")
            e.printStackTrace()
        }
    }
}