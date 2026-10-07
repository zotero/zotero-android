package org.zotero.android.speech

import android.speech.tts.Voice
import org.zotero.android.api.pojo.speech.VoicesResponse
import org.zotero.android.speech.data.ReadAloudLanguage
import org.zotero.android.speech.data.RemoteVoice

object VoiceLocaleNames {

    fun variationName(languageCode: String, baseLanguage: String): String {
        val localized = LanguageDetector.localizedIdentifierName(languageCode) ?: return languageCode
        val localizedLanguage = LanguageDetector.localizedLanguageName(baseLanguage) ?: return localized
        if (!localized.startsWith(localizedLanguage)) {
            return localized
        }
        val suffix = localized.removePrefix(localizedLanguage).trim(' ', '(', ')')
        return suffix.ifEmpty { localized }
    }

    fun availableLocalLanguages(voices: List<Voice>): List<ReadAloudLanguage> {
        return makeLanguages(voices.map { it.languageTag })
    }

    fun availableRemoteLanguages(tier: RemoteVoice.Tier, response: VoicesResponse): List<ReadAloudLanguage> {
        val tierData = response.tierData(tier) ?: return emptyList()
        return makeLanguages(tierData.flatMap { it.locales.keys })
    }

    private fun makeLanguages(locales: List<String>): List<ReadAloudLanguage> {
        return locales
            .groupBy { LanguageDetector.baseLanguage(of = it) }
            .mapNotNull { (baseCode, baseLocales) ->
                val name = LanguageDetector.localizedLanguageName(baseCode) ?: return@mapNotNull null
                ReadAloudLanguage(id = baseCode, name = name, locales = baseLocales.distinct().sorted())
            }
            .sortedWith { first, second -> String.CASE_INSENSITIVE_ORDER.compare(first.name, second.name) }
    }
}