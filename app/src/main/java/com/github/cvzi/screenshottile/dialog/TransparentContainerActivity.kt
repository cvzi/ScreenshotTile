package com.github.cvzi.screenshottile.dialog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory

/**
 * Created by ipcjs on 2017/8/16.
 * Changes by cuzi (cuzi@openmail.cc)
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
        val fragmentClass = intent.getStringExtra(EXTRA_FRAGMENT_NAME)
        if (savedInstanceState == null && fragmentClass != null) {
            val args = intent.getBundleExtra(EXTRA_ARGS)
            val fragment: Fragment? = try {
                val fragment = FragmentFactory.loadFragmentClass(
                    classLoader, fragmentClass
                ).getConstructor().newInstance()
                args?.run {
                    classLoader = fragment.javaClass.classLoader
                    fragment.arguments = this
                }
                fragment as Fragment
            } catch (e: Throwable) {
                null
            }

            if (fragment is DialogFragment) {
                fragment.show(supportFragmentManager, fragmentClass)
            } else if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, fragment, fragmentClass)
                    .commit()
            }
        }
    }

}
