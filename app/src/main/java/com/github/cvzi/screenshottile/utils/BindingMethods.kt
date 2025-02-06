package com.github.cvzi.screenshottile.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import com.github.cvzi.screenshottile.App
import java.util.Locale


@BindingAdapter("android:text")
fun setLocalizedText(view: TextView, @StringRes res: Int) {
    view.text = view.context.getLocalizedString(res)
}

@BindingAdapter("android:contentDescription")
fun setLocalizedText(view: View, @StringRes res: Int) {
    view.contentDescription = view.context.getLocalizedString(res)
}

@BindingAdapter("android:hint")
fun setLocalizedText(view: AutoCompleteTextView, @StringRes res: Int) {
    view.hint = view.context.getLocalizedString(res)
}


fun Context.getLocalizedString(@StringRes res: Int): String {
    val locales = resources.configuration.locales
    val len = locales.size()
    for (i in 0 until len) {
        val localizedResources = getLocalizedResources(this, locales.get(i))
        val str = localizedResources.getString(res)
        if (!str.startsWith("\u200c")) {
            return str
        }
    }
    val localizedResources = getLocalizedResources(this, Locale.ENGLISH)
    return localizedResources.getString(res)
}

fun Context.formatLocalizedString(@StringRes res: Int, vararg formatArgs: Any): String {
    val locales = resources.configuration.locales
    val len = locales.size()
    for (i in 0 until len) {
        val localizedResources = getLocalizedResources(this, locales.get(i))
        val str = localizedResources.getString(res, *formatArgs)
        if (!str.startsWith("\u200c")) {
            return str
        }
    }
    val localizedResources = getLocalizedResources(this, Locale.ENGLISH)
    return localizedResources.getString(res, *formatArgs)
}




fun getLocalizedResources(context: Context, desiredLocale: Locale): Resources {
    val tag = desiredLocale.toLanguageTag()
    val resources = App.resources.get(tag)
    if (resources != null) {
        return resources
    }
    val conf = Configuration(context.resources.configuration)
    conf.setLocale(desiredLocale)
    val localizedContext = context.createConfigurationContext(conf)
    App.resources.put(tag, localizedContext.resources)
    return localizedContext.resources
}
