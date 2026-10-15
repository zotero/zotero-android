package org.zotero.android.screens.reader.readaloud.voiceselection

import android.speech.tts.Voice
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.zotero.android.api.network.CustomResult
import org.zotero.android.architecture.BaseViewModel2
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.ScreenArguments
import org.zotero.android.architecture.ViewEffect
import org.zotero.android.speech.LanguageDetector
import org.zotero.android.speech.LocalVoiceCatalog
import org.zotero.android.speech.RemoteVoicesController
import org.zotero.android.speech.SpeechVoice
import org.zotero.android.speech.VoiceLocaleNames
import org.zotero.android.speech.VoiceUtility
import org.zotero.android.speech.data.LocaleLocalVoiceGroup
import org.zotero.android.speech.data.LocaleRemoteVoiceGroup
import org.zotero.android.speech.data.ReadAloudLanguage
import org.zotero.android.speech.data.RemoteVoice
import org.zotero.android.speech.identifier
import org.zotero.android.speech.languageTag
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ReadAloudVoiceSelectionViewModel @Inject constructor(
    private val defaults: Defaults,
    private val voiceUtility: VoiceUtility,
    private val localVoiceCatalog: LocalVoiceCatalog,
    private val remoteVoicesController: RemoteVoicesController,
) : BaseViewModel2<ReadAloudVoiceSelectionState, ReadAloudVoiceSelectionViewEffect>(ReadAloudVoiceSelectionState()) {

    fun init() = initOnce {
        val args = ScreenArguments.readAloudVoiceSelectionArgs
        val initialVoice = args.initialVoice
        updateState {
            copy(
                type = when (initialVoice) {
                    is SpeechVoice.Local -> ReadAloudVoiceType.local
                    is SpeechVoice.Remote -> if (initialVoice.voice.tier == RemoteVoice.Tier.premium) {
                        ReadAloudVoiceType.premium
                    } else {
                        ReadAloudVoiceType.standard
                    }
                    null -> ReadAloudVoiceType.standard
                },
                language = args.language
                    ?.let { ReadAloudLanguageChoice.Language(LanguageDetector.baseLanguage(of = it)) }
                    ?: ReadAloudLanguageChoice.Auto,
                selectedVoice = initialVoice,
                selectedRegionLocale = (initialVoice as? SpeechVoice.Local)?.voice?.languageTag,
                detectedLanguage = args.detectedLanguage,
                savesOnVoiceChange = args.savesOnVoiceChange,
            )
        }
        viewModelScope.launch {
            localVoiceCatalog.loadVoices()
            reloadLocalVoices()
            loadVoices()
        }
    }

    fun onTypeSelected(type: ReadAloudVoiceType) {
        updateState { copy(type = type) }
        when (type) {
            ReadAloudVoiceType.local -> {
                val state = viewState
                val groupVoice = state.groupedLocalVoices
                    .firstOrNull { it.locale == state.selectedRegionLocale }
                    ?.let { preferredLocalVoice(it) }
                val voice = groupVoice ?: voiceUtility.findLocalVoice(state.resolvedLocale, state.localVoices)
                voice?.let { applySelectedVoice(SpeechVoice.Local(it)) }
            }

            ReadAloudVoiceType.premium, ReadAloudVoiceType.standard -> {
                reloadGroupedRemoteVoices()
                autoSelectRemoteVoice()
            }
        }
    }

    fun onVoiceSelected(voice: SpeechVoice) {
        applySelectedVoice(voice)
    }

    fun onRegionSelected(locale: String) {
        updateState { copy(selectedRegionLocale = locale) }
        autoSelectVoiceForRegion(locale)
    }

    fun onLanguageSelected(language: ReadAloudLanguage?) {
        updateState {
            copy(language = language?.let { ReadAloudLanguageChoice.Language(it.id) } ?: ReadAloudLanguageChoice.Auto)
        }
        reloadLocalVoices()
        reloadGroupedRemoteVoices()
        when (viewState.type) {
            ReadAloudVoiceType.local -> {
                val state = viewState
                voiceUtility.findLocalVoice(state.resolvedLocale, state.localVoices)
                    ?.let { applySelectedVoice(SpeechVoice.Local(it)) }
            }

            ReadAloudVoiceType.premium, ReadAloudVoiceType.standard -> autoSelectRemoteVoice()
        }
    }

    fun createLanguages(): List<ReadAloudLanguage> {
        val state = viewState
        val tier = state.type.remoteTier ?: return VoiceLocaleNames.availableLocalLanguages(localVoiceCatalog.voices)
        val response = state.voicesResponse ?: return emptyList()
        return VoiceLocaleNames.availableRemoteLanguages(tier, response)
    }

    fun onDone() {
        val voice = viewState.selectedVoice
        if (voice != null && !viewState.savesOnVoiceChange) {
            saveVoicePreference(voice)
        }
        val change = voice?.let { ReadAloudVoiceChange(it, viewState.preferredLanguageForDismiss) }
        EventBus.getDefault().post(ReadAloudVoiceSelectionResult(change))
        triggerEffect(ReadAloudVoiceSelectionViewEffect.OnBack)
    }

    fun onCancel() {
        EventBus.getDefault().post(ReadAloudVoiceSelectionResult(null))
        triggerEffect(ReadAloudVoiceSelectionViewEffect.OnBack)
    }

    fun loadVoices() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, loadError = false) }
            val result = remoteVoicesController.loadVoices()
            val voicesResult = (result as? CustomResult.GeneralSuccess)?.value
            if (voicesResult == null) {
                Timber.e("ReadAloudVoiceSelectionViewModel: can't load remote voices - $result")
                updateState { copy(isLoading = false, loadError = true) }
                return@launch
            }
            updateState {
                copy(
                    voicesResponse = voicesResult.response,
                    standardCreditsRemaining = voicesResult.credits.standard,
                    premiumCreditsRemaining = voicesResult.credits.premium,
                )
            }
            reloadGroupedRemoteVoices()
            updateState { copy(isLoading = false) }
        }
    }

    private fun applySelectedVoice(voice: SpeechVoice) {
        updateState { copy(selectedVoice = voice) }
        if (viewState.savesOnVoiceChange) {
            saveVoicePreference(voice)
        }
        val voiceLocale = locale(voice)
        if (voiceLocale != null && voiceLocale != viewState.selectedRegionLocale) {
            updateState { copy(selectedRegionLocale = voiceLocale) }
        }
    }

    private fun reloadLocalVoices() {
        val state = viewState
        val localVoices = voiceUtility.localVoicesForBaseLanguage(state.baseLanguage)
        updateState {
            copy(
                localVoices = localVoices,
                groupedLocalVoices = voiceUtility.groupLocalVoices(localVoices, state.baseLanguage),
            )
        }
        revalidateSelectedRegion()
    }

    private fun reloadGroupedRemoteVoices() {
        val state = viewState
        val response = state.voicesResponse
        val tier = state.type.remoteTier
        if (response == null || tier == null) {
            updateState { copy(groupedRemoteVoices = emptyList()) }
            return
        }
        val locales = voiceUtility.remoteLocales(state.baseLanguage, tier, response)
        val groups = voiceUtility.groupRemoteVoices(locales, tier, state.baseLanguage, response)
        updateState { copy(groupedRemoteVoices = groups) }
        revalidateSelectedRegion()
    }

    private fun autoSelectRemoteVoice() {
        val state = viewState
        val response = state.voicesResponse ?: return
        val tier = state.type.remoteTier ?: return
        val groupVoice = state.groupedRemoteVoices
            .firstOrNull { it.locale == state.selectedRegionLocale }
            ?.let { preferredRemoteVoice(it, tier) }
        val voice = groupVoice ?: voiceUtility.findRemoteVoice(state.resolvedLocale, tier, response)
        voice?.let { applySelectedVoice(SpeechVoice.Remote(it)) }
    }

    private fun preferredRemoteVoice(group: LocaleRemoteVoiceGroup, tier: RemoteVoice.Tier): RemoteVoice? {
        val savedVoices = when (tier) {
            RemoteVoice.Tier.premium -> defaults.getDefaultPremiumRemoteVoiceForLanguage()
            RemoteVoice.Tier.standard -> defaults.getDefaultStandardRemoteVoiceForLanguage()
        }
        val savedVoice = savedVoices[group.locale]
        if (savedVoice != null && savedVoice.tier == tier && group.voices.any { it.id == savedVoice.id }) {
            return savedVoice
        }
        return group.voices.firstOrNull()
    }

    private fun preferredLocalVoice(group: LocaleLocalVoiceGroup): Voice? {
        val savedIdentifier = defaults.getDefaultLocalVoiceForLanguage()[group.locale]
        return group.voices.firstOrNull { it.identifier == savedIdentifier } ?: group.voices.firstOrNull()
    }

    private fun revalidateSelectedRegion() {
        val state = viewState
        val regions = state.currentRegions
        if (regions.isEmpty()) {
            updateState { copy(selectedRegionLocale = null) }
            return
        }
        if (regions.any { it.locale == state.selectedRegionLocale }) {
            return
        }
        val voiceLocale = state.selectedVoice?.let { locale(it) }
        val newLocale = when {
            voiceLocale != null && regions.any { it.locale == voiceLocale } -> voiceLocale
            regions.any { it.locale == state.resolvedLocale } -> state.resolvedLocale
            else -> regions.first().locale
        }
        updateState { copy(selectedRegionLocale = newLocale) }
    }

    private fun locale(voice: SpeechVoice): String? {
        return when (voice) {
            is SpeechVoice.Local -> voice.voice.languageTag
            is SpeechVoice.Remote -> {
                val state = viewState
                val selectedGroup = state.groupedRemoteVoices.firstOrNull { it.locale == state.selectedRegionLocale }
                if (selectedGroup != null && selectedGroup.voices.any { it.id == voice.voice.id }) {
                    selectedGroup.locale
                } else {
                    state.groupedRemoteVoices.firstOrNull { group -> group.voices.any { it.id == voice.voice.id } }?.locale
                }
            }
        }
    }

    private fun autoSelectVoiceForRegion(locale: String) {
        val state = viewState
        val currentVoice = state.selectedVoice
        if (currentVoice != null && locale(currentVoice) == locale) {
            return
        }
        val tier = state.type.remoteTier
        if (tier == null) {
            voiceUtility.findLocalVoice(locale, state.localVoices)?.let { applySelectedVoice(SpeechVoice.Local(it)) }
            return
        }
        val response = state.voicesResponse ?: return
        voiceUtility.findRemoteVoice(locale, tier, response)?.let { applySelectedVoice(SpeechVoice.Remote(it)) }
    }

    private fun saveVoicePreference(voice: SpeechVoice) {
        val storageKey = viewState.resolvedLocale
        when (voice) {
            is SpeechVoice.Local -> {
                defaults.setDefaultLocalVoiceForLanguage(
                    defaults.getDefaultLocalVoiceForLanguage() + (storageKey to voice.voice.identifier)
                )
                defaults.setRemoteVoiceTier(null)
            }

            is SpeechVoice.Remote -> {
                val remoteVoice = voice.voice
                when (remoteVoice.tier) {
                    RemoteVoice.Tier.premium -> defaults.setDefaultPremiumRemoteVoiceForLanguage(
                        defaults.getDefaultPremiumRemoteVoiceForLanguage() + (storageKey to remoteVoice)
                    )

                    RemoteVoice.Tier.standard -> defaults.setDefaultStandardRemoteVoiceForLanguage(
                        defaults.getDefaultStandardRemoteVoiceForLanguage() + (storageKey to remoteVoice)
                    )
                }
                defaults.setRemoteVoiceTier(remoteVoice.tier)
            }
        }
    }
}

sealed class ReadAloudVoiceSelectionViewEffect : ViewEffect {
    object OnBack : ReadAloudVoiceSelectionViewEffect()
}