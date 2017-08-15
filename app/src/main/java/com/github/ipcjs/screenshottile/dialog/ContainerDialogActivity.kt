package com.github.ipcjs.screenshottile.dialog

import android.app.Activity
import android.app.DialogFragment
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.ipcjs.screenshottile.Utils

/**
 * Created by ipcjs on 2017/8/16.
 */

class ContainerDialogActivity : Activity() {
    companion object {
        const val EXTRA_FNAME = "fname"
        const val EXTRA_ARGS = "args"

        fun start(ctx: Context, fclass: Class<out DialogFragment>, args: Bundle? = null) {
            ctx.startActivity(newIntent(ctx, fclass, args))
        }

        fun newIntent(ctx: Context, fclass: Class<out DialogFragment>, args: Bundle?): Intent {
            val intent = Intent(ctx, ContainerDialogActivity::class.java)
            intent.putExtra(EXTRA_FNAME, fclass.name)
            intent.putExtra(EXTRA_ARGS, args)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            val fname = intent.getStringExtra(EXTRA_FNAME)
            val args = intent.getBundleExtra(EXTRA_ARGS)
            val fragment = Fragment.instantiate(this, fname, args)
            if (fragment is DialogFragment) {
                fragment.show(fragmentManager, fname)
            } else {
                Utils.p("$fname 必须是 ${DialogFragment::class.java.name}的子类")
            }
        }
    }

}
