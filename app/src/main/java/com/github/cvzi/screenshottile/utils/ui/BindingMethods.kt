package com.github.cvzi.screenshottile.utils.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

private var supportedLocalesCache: List<Locale>? = null


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
    val supportedLocales = getSupportedLocales()
    val preferredLocales = MutableList(locales.size()) { i -> locales[i] }
    val selected = resolveLocalizedString(preferredLocales, supportedLocales) { candidate ->
        val localizedResources = getLocalizedResources(this, candidate)
        val raw = localizedResources.getString(res)
        raw
    }
    if (selected != null) {
        return selected
    }
    val localizedResources = getLocalizedResources(this, Locale.ENGLISH)
    val fallback = localizedResources.getString(res).removePrefix("\u200c")
    return fallback
}

fun Context.formatLocalizedString(@StringRes res: Int, vararg formatArgs: Any): String {
    val locales = resources.configuration.locales
    val supportedLocales = getSupportedLocales()
    val preferredLocales = mutableListOf<Locale>()
    for (i in 0 until locales.size()) {
        preferredLocales.add(locales.get(i))
    }
    val selected = resolveLocalizedString(preferredLocales, supportedLocales) { candidate ->
        val localizedResources = getLocalizedResources(this, candidate)
        val raw = localizedResources.getString(res, *formatArgs)
        raw
    }
    if (selected != null) {
        return selected
    }
    val localizedResources = getLocalizedResources(this, Locale.ENGLISH)
    val fallback = localizedResources.getString(res, *formatArgs).removePrefix("\u200c")
    return fallback
}

private fun Context.getSupportedLocales(): List<Locale> {
    supportedLocalesCache?.let { return it }

    val parser = resources.getXml(R.xml.locales_config)
    val locales = mutableListOf<Locale>()
    parser.next()
    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
            val localeTag = parser.getAttributeValue(
                "http://schemas.android.com/apk/res/android",
                "name"
            )
            if (localeTag != null) {
                locales.add(Locale.forLanguageTag(localeTag))
            }
        }
        eventType = parser.next()
    }

    val deduped = locales.distinctBy { localeDedupeKey(it) }
    supportedLocalesCache = deduped
    return deduped
}


fun getLocalizedResources(context: Context, desiredLocale: Locale): Resources {
    val tag = desiredLocale.toLanguageTag()
    val resources = App.resources[tag]
    if (resources != null) {
        return resources
    }
    val conf = Configuration(context.resources.configuration)
    conf.setLocale(desiredLocale)
    val localizedContext = context.createConfigurationContext(conf)
    App.resources[tag] = localizedContext.resources
    return localizedContext.resources
}
