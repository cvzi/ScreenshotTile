package com.github.ipcjs.screenshottile.dialog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * Created by ipcjs on 2017/8/16.
 */

open class TransparentContainerActivity : FragmentActivity() {
    companion object {
        const val EXTRA_FRAGMENT_NAME = "fragment_name"
        const val EXTRA_ARGS = "args"

        /**
         * Get intent
         */
        fun newIntent(
            ctx: Context,
            activityClass: Class<out Activity>,
            fragmentClass: Class<out Fragment>,
            args: Bundle?
        ): Intent {
            val intent = Intent(ctx, activityClass)
            intent.putExtra(EXTRA_FRAGMENT_NAME, fragmentClass.name)
            intent.putExtra(EXTRA_ARGS, args)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val fragmentClass = intent.getStringExtra(EXTRA_FRAGMENT_NAME)
            val args = intent.getBundleExtra(EXTRA_ARGS)
            val fragment = Fragment.instantiate(this, fragmentClass, args)

            if (fragment is DialogFragment) {
                fragment.show(supportFragmentManager, fragmentClass)
            } else {
                supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, fragment, fragmentClass)
                    .commit()
            }
        }
    }

}
