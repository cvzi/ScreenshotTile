package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SENDTO
import android.net.Uri
import android.os.Build
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BuildConfig
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.ui.getLocalizedString

/**
 * Created by cuzi (cuzi@openmail.cc) on 2018/12/29.
 */

/**
 * Returns true if this set contains the specified CharsSequence as a String.
 */
fun HashSet<String>.contains(seq: CharSequence): Boolean = this.contains(seq.toString())


/**
 * Open an email dialog
 */
fun Context.emailDialog(placeholder: String = "Write your message here...", minLength: Int = 30) {
    val editText = EditText(this).apply {
        setLines(4)
        maxLines = 10
        hint = placeholder
    }
    AlertDialog.Builder(this)
        .setTitle(getLocalizedString(R.string.about_contact_email_title))
        .setView(editText)
        .setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        .setPositiveButton(android.R.string.ok) { dialog, _ ->
            val text = editText.text?.toString() ?: ""
            if (text.length >= minLength) {
                val debug = StringBuilder().apply {
                    append("\n\n---\n")
                    append("Package: ${BuildConfig.APPLICATION_ID}\n")
                    append("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
                    append("Device:  ${Build.MANUFACTURER} ${Build.MODEL}\n")
                    append("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
                    append("Locale:  device=${java.util.Locale.getDefault()} app=${App.getInstance().prefManager.userLanguages}\n")
                }.toString()
                openEmail(getString(R.string.contact_email), getString(R.string.contact_email_subject), text + debug)
            }
            dialog.dismiss()
        }
        .show()
}
