package com.github.cvzi.screenshottile.activities

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.cvzi.screenshottile.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

open class UnitTestFileNameFormatString {
    private val context: Context = ApplicationProvider.getApplicationContext()

    open fun checkLocalizedString() {
        val locale = context.resources.configuration.locales.get(0)
        println("Locale: $locale")

        val text = context.resources.getString(R.string.setting_file_name_placeholders)

        assertThat(text.trim()).isNotEmpty()
        assertThat(text).contains("%timestamp%")
        assertThat(text).contains("%counter%")
        assertThat(text).contains("%random%")
        assertThat(text).contains("%randint%")

        println("  ✅ Ok.")
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class UnitTestFileNameFormatStringLocalized : UnitTestFileNameFormatString() {
    @Test
    @Config(qualifiers = "ar")
    fun ar() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "be")
    fun be() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "bn")
    fun bn() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "cs")
    fun cs() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "da")
    fun da() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "de")
    fun de() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "el")
    fun el() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "es")
    fun es() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "fa")
    fun fa() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "fi")
    fun fi() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "fr")
    fun fr() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "gu")
    fun gu() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "he")
    fun he() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "hi")
    fun hi() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "hu")
    fun hu() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "hy")
    fun hy() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "in")
    fun in99() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "it")
    fun it() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "iw")
    fun iw() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "ja")
    fun ja() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "kn")
    fun kn() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "mr")
    fun mr() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "ms")
    fun ms() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "nb")
    fun nb() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "nl")
    fun nl() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "no")
    fun no() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "or")
    fun or() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "pa")
    fun pa() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "pl")
    fun pl() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "pt-rBR")
    fun ptBR() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "pt-rPT")
    fun ptPT() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "ro")
    fun ro() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "ru")
    fun ru() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "sv")
    fun sv() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "ta")
    fun ta() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "te")
    fun te() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "th")
    fun th() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "tl")
    fun tl() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "tr")
    fun tr() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "ug")
    fun ug() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "uk")
    fun uk() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "ur")
    fun ur() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "vi")
    fun vi() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "zh-rCN")
    fun zhCN() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "zh-rHK")
    fun zhHK() {
        checkLocalizedString()
    }

    @Test
    @Config(qualifiers = "zh-rTW")
    fun zhTW() {
        checkLocalizedString()
    }
}
