package com.github.ipcjs.screenshottile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import com.github.ipcjs.screenshottile.Utils.p

/**
 * Created by ipcjs on 2017/8/15.
 */

class DelayScreenshotActivity : Activity() {
    companion object {
        const val EXTRA_DELAY = "delay"
        fun start(ctx: Context, delay: Int) {
            ctx.startActivity(newIntent(ctx, delay))
        }

        fun newIntent(ctx: Context, delay: Int): Intent {
            val intent: Intent
            if (delay > 0) {
                intent = Intent(ctx, DelayScreenshotActivity::class.java)
                intent.putExtra(EXTRA_DELAY, delay)
            } else {
                intent = Intent(ctx, ScreenshotActivity::class.java)
            }
            return intent
        }
    }

    private var count: Int = 3

    private val runnable = object : Runnable {
        override fun run() {
            view.text = count--.toString()
            if (count < 0) {
                screenshotAndFinish()
            } else {
                view.postDelayed(this, 1000)
            }
        }
    }

    private fun screenshotAndFinish() {
        Utils.screenshot()
        view.removeCallbacks(runnable)
        finish()
    }

    private lateinit var view: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p("DelayScreenshotActivity.onCreate")
        count = intent.getIntExtra(EXTRA_DELAY, count)
        view = TextView(this)
        view.textSize = 80f

        view.post(runnable)
        view.gravity = Gravity.CENTER
        view.setOnClickListener {
            screenshotAndFinish()
        }
        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setContentView(view, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        p("DelayScreenshotActivity.onDestroy")
    }
}
