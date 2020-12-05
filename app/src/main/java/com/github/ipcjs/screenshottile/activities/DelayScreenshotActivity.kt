package com.github.ipcjs.screenshottile.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.github.ipcjs.screenshottile.databinding.ActivityDelayBinding
import com.github.ipcjs.screenshottile.utils.screenshot

/**
 * Created by ipcjs on 2017/8/15.
 * Changes by cuzi@openmail.cc
 */

class DelayScreenshotActivity : Activity() {
    companion object {
        const val EXTRA_DELAY = "delay"

        /**
         * Get intent.
         */
        fun newIntent(ctx: Context, delay: Int): Intent {
            val intent = Intent(ctx, DelayScreenshotActivity::class.java)
            intent.putExtra(EXTRA_DELAY, delay)
            return intent
        }
    }

    private lateinit var binding: ActivityDelayBinding
    private var count: Int = 3

    private val countDownRunnable = object : Runnable {
        override fun run() {
            binding.view.text = count--.toString()
            when {
                count < 0 -> postScreenshotAndFinish()  // 此时界面显示0
                else -> binding.view.postDelayed(this, 1000)
            }
        }
    }

    private fun postScreenshotAndFinish() {
        binding.view.visibility = View.GONE
        binding.view.post {
            screenshotAndFinish()
        }
    }

    private fun screenshotAndFinish() {
        screenshot(this)
        binding.view.removeCallbacks(countDownRunnable)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDelayBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        count = intent.getIntExtra(EXTRA_DELAY, count)
        binding.view.setOnClickListener {
            postScreenshotAndFinish()
        }
        binding.view.post(countDownRunnable)
    }
}
