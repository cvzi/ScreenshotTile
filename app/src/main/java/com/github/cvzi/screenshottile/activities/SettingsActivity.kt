package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.github.cvzi.screenshottile.fragments.SettingFragment

/**
 * Created by ipcjs on 2017/8/16.
 * Changes by cuzi (cuzi@openmail.cc)
 */

class SettingsActivity : BaseAppCompatActivity() {
    companion object {
        const val EXTRA_FRAGMENT_NAME = "fragment_name"
        const val EXTRA_ARGS = "args"
        private val defaultFragment = SettingFragment::class.java

        /**
         * Get intent
         */
        fun newIntent(
            ctx: Context,
            fragmentClass: Class<out Fragment>,
            args: Bundle?
        ): Intent {
            val intent = Intent(ctx, SettingsActivity::class.java)
            intent.putExtra(EXTRA_FRAGMENT_NAME, fragmentClass.name)
            intent.putExtra(EXTRA_ARGS, args)
            return intent
        }

        /**
         * Start activity
         */
        fun start(
            ctx: Context,
            fragmentClass: Class<out Fragment> = defaultFragment,
            args: Bundle? = null
        ) {
            ctx.startActivity(
                newIntent(
                    ctx,
                    fragmentClass,
                    args
                )
            )
        }

        /**
         * Start activity from service
         */
        fun startNewTask(
            ctx: Context,
            fragmentClass: Class<out Fragment> = defaultFragment,
            args: Bundle? = null
        ) {
            ctx.startActivity(
                newIntent(
                    ctx,
                    fragmentClass,
                    args
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fragmentClass = intent.getStringExtra(EXTRA_FRAGMENT_NAME) ?: defaultFragment.name
        if (savedInstanceState == null) {
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
