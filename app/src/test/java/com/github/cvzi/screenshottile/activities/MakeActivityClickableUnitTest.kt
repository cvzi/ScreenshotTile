package com.github.cvzi.screenshottile.activities

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.makeActivityClickableFromText
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

open class UnitTestClickableActivityLink {
    private val context: Context = ApplicationProvider.getApplicationContext()

    open fun makeClickableActivities_LocalizedString() {
        val locale = context.resources.configuration.locales.get(0)
        println("Locale: $locale")

        val text = context.resources.getString(R.string.main_general_text)

        val clickableStringResult = makeActivityClickableFromText(text, context)

        assertThat(clickableStringResult.builder.length).isLessThan(text.length)

        assertThat(clickableStringResult.activities).isNotEmpty()

        for (activity in clickableStringResult.activities) {
            println("  Class: \"$activity\"")
            val c = try {
                Class.forName(activity)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
            if (c == null) {
                println("  ❌ c is null")
            }
            assertThat(c).isNotNull()
        }

        println("  ✅ Ok.")
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class UnitTestClickableActivityLinkLocalized : UnitTestClickableActivityLink() {

    @Test
    @Config(qualifiers = "ar")
    fun ar() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "be")
    fun be() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "bn")
    fun bn() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "cs")
    fun cs() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "da")
    fun da() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "de")
    fun de() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "el")
    fun el() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "es")
    fun es() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "fa")
    fun fa() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "fi")
    fun fi() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "fr")
    fun fr() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "gu")
    fun gu() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "he")
    fun he() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "hi")
    fun hi() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "hu")
    fun hu() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "hy")
    fun hy() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "in")
    fun in99() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "it")
    fun it() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "iw")
    fun iw() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "ja")
    fun ja() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "kn")
    fun kn() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "mr")
    fun mr() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "ms")
    fun ms() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "nb")
    fun nb() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "nl")
    fun nl() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "no")
    fun no() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "or")
    fun or() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "pa")
    fun pa() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "pl")
    fun pl() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun ptBR() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "pt-rPT")
    fun ptPT() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "ro")
    fun ro() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "ru")
    fun ru() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "sv")
    fun sv() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "ta")
    fun ta() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "te")
    fun te() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "th")
    fun th() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "tl")
    fun tl() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "tr")
    fun tr() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "ug")
    fun ug() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "uk")
    fun uk() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "ur")
    fun ur() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "vi")
    fun vi() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "zh")
    fun zh() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "zh-rHK")
    fun zhHK() {
        makeClickableActivities_LocalizedString()
    }

    @Test
    @Config(qualifiers = "zh-rTW")
    fun zhTW() {
        makeClickableActivities_LocalizedString()
    }

}
