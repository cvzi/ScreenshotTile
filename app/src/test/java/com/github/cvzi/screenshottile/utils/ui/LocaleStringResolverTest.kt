package com.github.cvzi.screenshottile.utils.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.cvzi.screenshottile.R
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class LocaleStringResolverTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun normalizeRequestedLocalesPrefersSpecificVariants() {
        val supported = listOf(
            locale("de"),
            locale("en"),
            locale("en-US"),
            locale("en-GB")
        )

        val result = normalizeRequestedLocales(listOf(locale("en")), supported)

        assertThat(result).containsExactly(
            locale("en-US"),
            locale("en-GB"),
            locale("en")
        ).inOrder()
    }

    @Test
    fun normalizeRequestedLocalesFallsBackToGeneric() {
        val supported = listOf(locale("en"), locale("de"))

        val result = normalizeRequestedLocales(listOf(locale("en-US")), supported)

        assertThat(result).containsExactly(locale("en")).inOrder()
    }


    @Test
    fun resolveLocalizedStringFallsBackToNextPreferredLanguage() {
        val preferred = listOf(locale("en"), locale("de"))
        val supported = listOf(locale("en"), locale("de"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "en" -> "English"
                "de" -> "Deutsch"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("English")
    }

    @Test
    fun resolveLocalizedStringPrefersEnglishEvenWhenMarkedAsFallback() {
        val preferred = listOf(locale("en"), locale("de"), locale("en-US"))
        val supported = listOf(locale("en"), locale("de"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "en" -> "Single Tap"
                "de" -> "Einfach-Tipp"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("Single Tap")
    }

    @Test
    fun resolveLocalizedStringXmlValuesPreferenceSingleTap() {
        val preferred = listOf(locale("en"), locale("de"), locale("en-US"))
        val supported = listOf(locale("en"), locale("de"))
        val expected = englishString(R.string.single_tap)

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            getLocalizedResources(context, candidate).getString(R.string.single_tap)
        }

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun resolveLocalizedStringXmlValuesPreferenceMainFloatingButtonTitle() {
        val preferred = listOf(locale("en"), locale("de"), locale("en-US"))
        val supported = listOf(locale("en"), locale("de"))
        val expected = englishString(R.string.main_floating_button_title)

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            getLocalizedResources(context, candidate).getString(R.string.main_floating_button_title)
        }

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun resolveLocalizedStringWithActualXmlValuesPrefersGerman() {
        val preferred = listOf(locale("de"), locale("es"), locale("en-US"))
        val supported = listOf(locale("de"), locale("en"))
        val expected = germanString(R.string.main_floating_button_title)

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            getLocalizedResources(context, candidate).getString(R.string.main_floating_button_title)
        }

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun resolveLocalizedStringXmlValuesFallbackOrder() {
        val preferred = listOf(locale("zh"), locale("de"), locale("en-US"))
        val supported = listOf(locale("de"), locale("en"))
        val expected = germanString(R.string.main_floating_button_title)

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            getLocalizedResources(context, candidate).getString(R.string.main_floating_button_title)
        }

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun resolveLocalizedStringUnsupportedFallbackOrder() {
        val preferred = listOf(locale("en"), locale("de"), locale("en-US"))
        val supported = listOf(locale("en"), locale("de"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "en" -> "English"
                "en-US" -> "US English"
                "de" -> "Deutsch"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("English")
    }

    @Test
    fun resolveLocalizedStringSupportedMoreSpecific() {
        val preferred = listOf(locale("es"), locale("de"), locale("en"))
        val supported = listOf(locale("es-ES"), locale("de"), locale("en"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "es-ES" -> "Español"
                "de" -> "Deutsch"
                "en" -> FALLBACK_MARKER + "English"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("Español")
    }

    @Test
    fun resolveLocalizedStringSupportedLessSpecific() {
        val preferred = listOf(locale("es-ES"), locale("de"), locale("en"))
        val supported = listOf(locale("es"), locale("de"), locale("en"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "es-ES" -> "Español-ES"
                "es" -> "Español"
                "de" -> "Deutsch"
                "en" -> FALLBACK_MARKER + "English"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("Español")
    }

    @Test
    fun resolveLocalizedStringPrefersSupportedOverFallbackEnglish() {
        val preferred = listOf(locale("de"), locale("es"), locale("en-US"))
        val supported = listOf(locale("de"), locale("es"), locale("en"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "de" -> "Deutsch"
                "es" -> "Español"
                "en" -> FALLBACK_MARKER + "English"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("Deutsch")
    }

    @Test
    fun resolveLocalizedStringFallbackLessSupported() {
        val preferred = listOf(locale("es"), locale("de"), locale("en"))
        val supported = listOf(locale("de"), locale("en"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "es" -> "Español"
                "de" -> "Deutsch"
                "en" -> FALLBACK_MARKER + "English"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("Deutsch")
    }

    @Test
    fun resolveLocalizedStringNonAndroidLocaleMissing() {
        val preferred = listOf(locale("bci"), locale("de"), locale("en"))
        val supported = listOf(locale("de"), locale("en"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "de" -> "Deutsch"
                "en" -> FALLBACK_MARKER + "English"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("Deutsch")
    }

    @Test
    fun resolveLocalizedStringNonAndroidLocaleAvailable() {
        val preferred = listOf(locale("bci"), locale("de"), locale("en"))
        val supported = listOf(locale("bci"), locale("en"))

        val result = resolveLocalizedString(preferred, supported) { candidate ->
            when (candidate.toLanguageTag()) {
                "bci" -> "Waole"
                "en" -> FALLBACK_MARKER + "English"
                else -> error("Unexpected locale ${candidate.toLanguageTag()}")
            }
        }

        assertThat(result).isEqualTo("Waole")
    }

    private fun locale(tag: String): Locale = Locale.forLanguageTag(tag)

    private fun englishString(resId: Int): String {
        return getLocalizedResources(context, locale("en")).getString(resId).removePrefix(FALLBACK_MARKER)
    }
    private fun germanString(resId: Int): String {
        return getLocalizedResources(context, locale("de")).getString(resId)
    }


}
