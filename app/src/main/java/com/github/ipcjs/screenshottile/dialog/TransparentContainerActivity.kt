package com.github.ipcjs.screenshottile.dialog

import android.app.Activity
import android.app.DialogFragment
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService

/**
 * Created by ipcjs on 2017/8/16.
 */

open class TransparentContainerActivity : Activity() {
    companion object {
        const val EXTRA_FNAME = "fname"
        const val EXTRA_ARGS = "args"

        fun start(ctx: Context, fclass: Class<out DialogFragment>, args: Bundle? = null) {
            ctx.startActivity(newIntent(ctx, TransparentContainerActivity::class.java, fclass, args))
        }

        fun startAndCollapse(ts: TileService, fclass: Class<out DialogFragment>, args: Bundle? = null) {
            ts.startActivityAndCollapse(newIntent(ts, TransparentContainerActivity::class.java, fclass, args))
        }

        fun newIntent(ctx: Context, activityClass: Class<out Activity>, fclass: Class<out Fragment>, args: Bundle?): Intent {
            val intent = Intent(ctx, activityClass)
            intent.putExtra(EXTRA_FNAME, fclass.name)
            intent.putExtra(EXTRA_ARGS, args)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val fname = intent.getStringExtra(EXTRA_FNAME)
            val args = intent.getBundleExtra(EXTRA_ARGS)
            val fragment = Fragment.instantiate(this, fname, args)

            if (fragment is DialogFragment) {
                fragment.show(fragmentManager, fname)
            } else {
                fragmentManager.beginTransaction()
                        .add(android.R.id.content, fragment, fname)
                        .commit()
            }
        }
    }

}
