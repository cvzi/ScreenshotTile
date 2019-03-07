package com.github.ipcjs.screenshottile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_delay.*

/**
 * Created by ipcjs on 2017/8/15.
 */

class DelayScreenshotActivity : Activity() {
    companion object {
        const val EXTRA_DELAY = "delay"

        /**
         * Get intent
         */
        fun newIntent(ctx: Context, delay: Int): Intent {
            val intent = Intent(ctx, DelayScreenshotActivity::class.java)
            intent.putExtra(EXTRA_DELAY, delay)
            return intent
        }
    }

    private var count: Int = 3

    private val countDownRunnable = object : Runnable {
        override fun run() {
            view.text = count--.toString()
            when {
                count < 0 -> postScreenshotAndFinish()  // 此时界面显示0
                else -> view.postDelayed(this, 1000)
            }
        }
    }

    private fun postScreenshotAndFinish() {
        view.visibility = View.GONE
        view.post {
            screenshotAndFinish()
        }
    }

    private fun screenshotAndFinish() {
        screenshot(this)
        view.removeCallbacks(countDownRunnable)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delay)
        count = intent.getIntExtra(EXTRA_DELAY, count)
        view.setOnClickListener {
            postScreenshotAndFinish()
        }
        view.post(countDownRunnable)
    }
}
