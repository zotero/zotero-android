package org.zotero.android.screens.reader.readaloud.voiceselection

import android.speech.tts.Voice
import org.zotero.android.api.pojo.speech.VoicesResponse
import org.zotero.android.architecture.ViewState
import org.zotero.android.speech.LanguageDetector
import org.zotero.android.speech.SpeechVoice
import org.zotero.android.speech.data.LocaleLocalVoiceGroup
import org.zotero.android.speech.data.LocaleRemoteVoiceGroup
import org.zotero.android.speech.data.RemoteVoice

enum class ReadAloudVoiceType(val remoteTier: RemoteVoice.Tier?) {
    premium(RemoteVoice.Tier.premium),
    standard(RemoteVoice.Tier.standard),
    local(null),
}

sealed class ReadAloudLanguageChoice {
    data object Auto : ReadAloudLanguageChoice()
    data class Language(val code: String) : ReadAloudLanguageChoice()

    fun baseLanguage(detectedLanguage: String): String = when (this) {
        Auto -> LanguageDetector.baseLanguage(of = detectedLanguage)
        is Language -> code
    }

    fun resolvedLocale(detectedLanguage: String): String = when (this) {
        Auto -> detectedLanguage
        is Language -> LanguageDetector.canonicalVariation(code) ?: code
    }
}

data class ReadAloudRegion(
    val locale: String,
    val displayName: String,
)

data class ReadAloudVoiceSelectionState(
    val type: ReadAloudVoiceType = ReadAloudVoiceType.standard,
    val language: ReadAloudLanguageChoice = ReadAloudLanguageChoice.Auto,
    val selectedVoice: SpeechVoice? = null,
    val selectedRegionLocale: String? = null,
    val localVoices: List<Voice> = emptyList(),
    val groupedLocalVoices: List<LocaleLocalVoiceGroup> = emptyList(),
    val groupedRemoteVoices: List<LocaleRemoteVoiceGroup> = emptyList(),
    val voicesResponse: VoicesResponse? = null,
    val isLoading: Boolean = false,
    val loadError: Boolean = false,
    val standardCreditsRemaining: Int? = null,
    val premiumCreditsRemaining: Int? = null,
    val detectedLanguage: String = "en",
    val savesOnVoiceChange: Boolean = false,
) : ViewState {

    val baseLanguage: String
        get() = language.baseLanguage(detectedLanguage)

    val resolvedLocale: String
        get() = language.resolvedLocale(detectedLanguage)

    val canShowLanguage: Boolean
        get() = type == ReadAloudVoiceType.local || voicesResponse != null

    val currentRegions: List<ReadAloudRegion>
        get() = when (type) {
            ReadAloudVoiceType.local -> groupedLocalVoices.map { ReadAloudRegion(it.locale, it.displayName) }
            ReadAloudVoiceType.premium, ReadAloudVoiceType.standard ->
                groupedRemoteVoices.map { ReadAloudRegion(it.locale, it.displayName) }
        }

    val currentLocalVoices: List<Voice>
        get() = groupedLocalVoices.firstOrNull { it.locale == selectedRegionLocale }?.voices.orEmpty()

    val currentRemoteVoices: List<RemoteVoice>
        get() = groupedRemoteVoices.firstOrNull { it.locale == selectedRegionLocale }?.voices.orEmpty()

    val displayedCreditsRemaining: Int?
        get() = when (type) {
            ReadAloudVoiceType.premium -> premiumCreditsRemaining
            ReadAloudVoiceType.standard -> standardCreditsRemaining
            ReadAloudVoiceType.local -> null
        }

    val preferredLanguageForDismiss: String?
        get() = when (language) {
            ReadAloudLanguageChoice.Auto -> null
            is ReadAloudLanguageChoice.Language -> resolvedLocale
        }
}