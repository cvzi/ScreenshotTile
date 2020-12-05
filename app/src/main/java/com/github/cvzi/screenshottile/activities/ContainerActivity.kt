package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * Created by ipcjs on 2017/8/17.
 * Changes by cuzi (cuzi@openmail.cc)
 */

class ContainerActivity : TransparentContainerActivity() {
    companion object {
        /**
         * Start activity
         */
        fun start(ctx: Context, fragmentClass: Class<out Fragment>, args: Bundle? = null) {
            ctx.startActivity(newIntent(ctx, ContainerActivity::class.java, fragmentClass, args))
        }

        /**
         * Start activity from service
         */
        fun startNewTask(ctx: Context, fragmentClass: Class<out Fragment>, args: Bundle? = null) {
            ctx.startActivity(
                newIntent(
                    ctx,
                    ContainerActivity::class.java,
                    fragmentClass,
                    args
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
        }
    }
}
