package com.github.cvzi.screenshottile.activities

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.ActivityAboutBinding
import com.github.cvzi.screenshottile.utils.formatLocalizedString
import com.github.cvzi.screenshottile.utils.getLocalizedString
import com.github.cvzi.screenshottile.utils.getUpdateUrl
import com.github.cvzi.screenshottile.utils.minPaddingFromInsets
import com.github.cvzi.screenshottile.utils.openEmail
import com.github.cvzi.screenshottile.utils.openUri
import java.security.MessageDigest

/**
 * About activity showing app version, release channel, and links to source code, license, ...
 */
class AboutActivity : BaseAppCompatActivity() {
    companion object {
        /**
         * Start this activity from another activity
         */
        fun start(ctx: Context) = ctx.startActivity(Intent(ctx, AboutActivity::class.java))
    }

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_about)
        binding.setVariable(BR.strings, App.texts)
        binding.scrollView.minPaddingFromInsets()

        title = getLocalizedString(R.string.about_app_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupForkNote()

        binding.textVersionValue.text = formatLocalizedString(
            R.string.about_version_value,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toString(),
            BuildConfig.BUILD_TYPE,
            BuildConfig.APPLICATION_ID
        )
        val releaseChannels = getReleaseChannels().ifEmpty {
            listOf(
                if (BuildConfig.DEBUG) {
                    getLocalizedString(R.string.about_release_channel_debug)
                } else {
                    getLocalizedString(R.string.about_release_channel_unknown)
                }
            )
        }
        binding.textReleaseChannelValue.text = releaseChannels.joinToString(", ")
        getReleaseChannelUrl(releaseChannels)?.let { url ->
            binding.textReleaseChannelValue.apply {
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    openUri(url)
                }
            }
        }

        binding.imageAppIcon.setOnClickListener {
            MainActivity.start(this)
        }

        binding.buttonSourceCode.setOnClickListener {
            openUri(getLocalizedString(R.string.pref_static_field_link_about_app_3))
        }
        binding.buttonContactEmail.setOnClickListener {
            openEmail(getString(R.string.contact_email), getString(R.string.contact_email_subject))
        }
        binding.buttonLicense.setOnClickListener {
            openUri(getLocalizedString(R.string.pref_static_field_link_about_license_1))
        }
        binding.buttonOpenSource.setOnClickListener {
            openUri(getLocalizedString(R.string.pref_static_field_link_about_open_source))
        }
        binding.buttonPrivacy.setOnClickListener {
            openUri(getLocalizedString(R.string.pref_static_field_link_about_privacy))
        }
        binding.buttonUpdateCheck.setOnClickListener {
            openUri(getUpdateUrl(this))
        }
        binding.buttonBackup.setOnClickListener {
            BackupPrefsActivity.start(this)
        }
        binding.buttonDonate.setOnClickListener {
            openUri(getLocalizedString(R.string.pref_static_field_link_about_donate))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun getReleaseChannelUrl(releaseChannels: List<String>): String? {
        return when {
            "GitHub" in releaseChannels -> getString(R.string.release_url_github)
            "F-Droid" in releaseChannels -> getString(R.string.release_url_fdroid)
            else -> null
        }
    }

    private fun setupForkNote() {
        val repoName = getLocalizedString(R.string.about_original_project_repo_name)
        val fullText = formatLocalizedString(R.string.about_fork_note, repoName)
        val start = fullText.indexOf(repoName)
        if (start == -1) {
            binding.textForkNote.text = fullText
            return
        }

        val builder = SpannableStringBuilder(fullText)
        builder.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    openUri(getLocalizedString(R.string.pref_static_field_link_about_app_1))
                }
            },
            start,
            start + repoName.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.textForkNote.apply {
            text = builder
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = android.graphics.Color.TRANSPARENT
        }
    }

    private fun getReleaseChannels(): List<String> {
        val signatures: Array<Signature> = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                ).signingInfo?.apkContentsSigners ?: emptyArray()
            }

            else -> {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
                    ?: emptyArray()
            }
        }

        return signatures.mapNotNull { signature ->
            val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            val hexString = digest.joinToString("") { "%02x".format(it) }
            when (hexString) {
                getString(R.string.cert_github_sha256) -> "GitHub"
                getString(R.string.cert_fdroid_sha256) -> "F-Droid"
                else -> null
            }
        }.distinct()
    }
}
