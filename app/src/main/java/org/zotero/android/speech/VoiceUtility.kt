package org.zotero.android.speech

import android.speech.tts.Voice
import org.zotero.android.api.pojo.speech.VoicesResponse
import org.zotero.android.architecture.Defaults
import org.zotero.android.speech.data.LocaleLocalVoiceGroup
import org.zotero.android.speech.data.LocaleRemoteVoiceGroup
import org.zotero.android.speech.data.RemoteVoice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceUtility @Inject constructor(
    private val defaults: Defaults,
    private val localVoiceCatalog: LocalVoiceCatalog,
) {

    fun findLocalVoice(language: String, voices: List<Voice> = localVoiceCatalog.voices): Voice? {
        return findVoice(
            language = language,
            storedDefault = { language ->
                val voiceId = defaults.getDefaultLocalVoiceForLanguage()[language] ?: return@findVoice null
                voices.firstOrNull { it.identifier == voiceId }
            },
            voiceForLocale = { locale -> voices.firstOrNull { it.languageTag == locale } },
        )
    }

    fun localVoices(language: String): List<Voice> {
        return filterLocalVoices { it.languageTag == language }
    }

    fun localVoicesForBaseLanguage(baseLanguage: String): List<Voice> {
        return filterLocalVoices { it.baseLanguage == baseLanguage }
    }

    fun findRemoteVoice(language: String, tier: RemoteVoice.Tier, response: VoicesResponse): RemoteVoice? {
        val tierData = response.tierData(tier)?.takeIf { it.isNotEmpty() } ?: return null
        return findVoice(
            language = language,
            storedDefault = { language ->
                val savedVoices = when (tier) {
                    RemoteVoice.Tier.premium -> defaults.getDefaultPremiumRemoteVoiceForLanguage()
                    RemoteVoice.Tier.standard -> defaults.getDefaultStandardRemoteVoiceForLanguage()
                }
                val savedVoice = savedVoices[language] ?: return@findVoice null
                if (savedVoice.tier == tier && tierData.any { it.voices.containsKey(savedVoice.id) }) savedVoice else null
            },
            voiceForLocale = { locale ->
                tierData.firstNotNullOfOrNull { data ->
                    val voiceId = data.locales[locale]?.firstVoiceId ?: return@firstNotNullOfOrNull null
                    data.makeVoice(id = voiceId, tier = tier)
                }
            },
        )
    }

    fun remoteVoices(language: String, tier: RemoteVoice.Tier, response: VoicesResponse): List<RemoteVoice> {
        val tierData = response.tierData(tier) ?: return emptyList()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<RemoteVoice>()
        for (data in tierData) {
            val localeData = data.locales[language] ?: continue
            for (voiceId in localeData.allVoiceIds) {
                if (seen.add(voiceId)) {
                    result.add(data.makeVoice(id = voiceId, tier = tier))
                }
            }
        }
        return result
    }

    fun groupRemoteVoices(
        locales: List<String>,
        tier: RemoteVoice.Tier,
        baseLanguage: String,
        response: VoicesResponse,
    ): List<LocaleRemoteVoiceGroup> {
        return locales.mapNotNull { locale ->
            val voices = remoteVoices(language = locale, tier = tier, response = response)
            if (voices.isEmpty()) {
                return@mapNotNull null
            }
            LocaleRemoteVoiceGroup(
                locale = locale,
                displayName = VoiceLocaleNames.variationName(locale, baseLanguage),
                voices = voices,
            )
        }
    }

    fun groupLocalVoices(voices: List<Voice>, baseLanguage: String): List<LocaleLocalVoiceGroup> {
        return voices.groupBy { it.languageTag }.toSortedMap().map { (locale, localeVoices) ->
            LocaleLocalVoiceGroup(
                locale = locale,
                displayName = VoiceLocaleNames.variationName(locale, baseLanguage),
                voices = localeVoices,
            )
        }
    }

    fun remoteLocales(forBaseLanguage: String, tier: RemoteVoice.Tier, response: VoicesResponse): List<String> {
        val tierData = response.tierData(tier) ?: return emptyList()
        return tierData
            .flatMap { it.locales.keys }
            .filter { LanguageDetector.baseLanguage(of = it) == forBaseLanguage }
            .distinct()
            .sorted()
    }

    private fun filterLocalVoices(predicate: (Voice) -> Boolean): List<Voice> {
        return localVoiceCatalog.voices
            .filter(predicate)
            .sortedWith(
                compareBy<Voice> { it.qualitySortOrder }
                    .thenComparator { first, second -> String.CASE_INSENSITIVE_ORDER.compare(first.name, second.name) }
            )
    }

    private fun <V> findVoice(
        language: String,
        storedDefault: (String) -> V?,
        voiceForLocale: (String) -> V?,
    ): V? {
        storedDefault(language)?.let { return it }

        val base = LanguageDetector.baseLanguage(of = language)

        voiceForLocale(language)?.let { return it }

        val canonical = LanguageDetector.canonicalVariation(base)
        if (canonical != null && canonical != language) {
            voiceForLocale(canonical)?.let { return it }
        }

        val deviceLocale = LanguageDetector.deviceLocale
        if (deviceLocale != language) {
            voiceForLocale(deviceLocale)?.let { return it }
        }

        val deviceBase = LanguageDetector.deviceBaseLanguage
        if (deviceBase != base) {
            val canonicalDevice = LanguageDetector.canonicalVariation(deviceBase)
            if (canonicalDevice != null && canonicalDevice != deviceLocale) {
                voiceForLocale(canonicalDevice)?.let { return it }
            }
        }

        return voiceForLocale("en-US")
    }
}