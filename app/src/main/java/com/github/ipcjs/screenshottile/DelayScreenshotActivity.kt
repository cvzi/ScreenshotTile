package com.github.ipcjs.screenshottile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import android.view.View
import com.github.ipcjs.screenshottile.Utils.p
import kotlinx.android.synthetic.main.activity_delay.*

/**
 * Created by ipcjs on 2017/8/15.
 */

class DelayScreenshotActivity : Activity() {
    companion object {
        const val EXTRA_DELAY = "delay"
        fun start(ctx: Context, delay: Int) {
            ctx.startActivity(newIntent(ctx, delay))
        }

        fun startAndCollapse(ts: TileService, delay: Int) {
            ts.startActivityAndCollapse(newIntent(ts, delay))
        }

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
        Utils.screenshot()
        view.removeCallbacks(countDownRunnable)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        p("DelayScreenshotActivity.onCreate")
        setContentView(R.layout.activity_delay)
        count = intent.getIntExtra(EXTRA_DELAY, count)
        view.setOnClickListener {
            postScreenshotAndFinish()
        }
        view.post(countDownRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        p("DelayScreenshotActivity.onDestroy")
    }
}
