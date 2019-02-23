package com.github.ipcjs.screenshottile.dialog

import android.app.Fragment
import android.content.Context
import android.os.Bundle

/**
 * Created by ipcjs on 2017/8/17.
 */

class ContainerActivity : TransparentContainerActivity() {
    companion object {
        /**
         * Start activity
         */
        fun start(ctx: Context, fclass: Class<out Fragment>, args: Bundle? = null) {
            ctx.startActivity(newIntent(ctx, ContainerActivity::class.java, fclass, args))
        }
    }
}
