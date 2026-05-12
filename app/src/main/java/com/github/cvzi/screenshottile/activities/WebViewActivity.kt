package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.ActivityWebViewBinding
import com.github.cvzi.screenshottile.utils.ui.minPaddingFromInsets

/**
 * Shows html files from assets in a WebView.
 * External links are opened in the default browser to not
 * require internet permission.
 */
class WebViewActivity : BaseAppCompatActivity() {

    companion object {
        private const val EXTRA_ASSET_PATH = "extra_asset_path"
        private const val EXTRA_TITLE = "extra_title"

        /**
         * Start this activity from another activity
         */
        fun start(context: Context, assetPath: String, title: String) {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_ASSET_PATH, assetPath)
                putExtra(EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivityWebViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_web_view)
        binding.setVariable(BR.strings, App.texts)
        binding.linearLayout.minPaddingFromInsets()

        val assetPath = intent.getStringExtra(EXTRA_ASSET_PATH) ?: return finish()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""

        setTitle(title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupWebView(assetPath)

        binding.linearLayoutHeader.setOnClickListener {
            AboutActivity.start(this)
        }
    }

    private fun setupWebView(assetPath: String) {
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return false
                return if (url.scheme == "http" || url.scheme == "https") {
                    val intent = Intent(Intent.ACTION_VIEW, url)
                    view?.context?.startActivity(intent)
                    true
                } else {
                    false
                }
            }
        }

        binding.webView.loadUrl("file:///android_asset/$assetPath")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
