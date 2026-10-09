package org.zotero.android.speech

import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceProcessorFactory @Inject constructor(
    private val localVoiceCatalog: LocalVoiceCatalog,
    private val voiceUtility: VoiceUtility,
) {
    fun makeLocalProcessor(
        language: String?,
        detectedLanguage: String?,
        speechRateModifier: Float,
        delegate: VoiceProcessorDelegate,
        scope: CoroutineScope,
    ): LocalVoiceProcessor {
        return LocalVoiceProcessor(
            preferredLanguage = language,
            detectedLanguage = detectedLanguage,
            speechRateModifier = speechRateModifier,
            delegate = delegate,
            localVoiceCatalog = localVoiceCatalog,
            voiceUtility = voiceUtility,
            scope = scope,
        )
    }
}