package org.zotero.android.speech

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.zotero.android.speech.data.RemoteVoice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceProcessorFactory @Inject constructor(
    private val localVoiceCatalog: LocalVoiceCatalog,
    private val voiceUtility: VoiceUtility,
    private val remoteVoicesController: RemoteVoicesController,
    private val context: Context,
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

    fun makeRemoteProcessor(
        language: String?,
        detectedLanguage: String?,
        tier: RemoteVoice.Tier,
        speechRateModifier: Float,
        delegate: VoiceProcessorDelegate,
        scope: CoroutineScope,
    ): RemoteVoiceProcessor {
        return RemoteVoiceProcessor(
            preferredLanguage = language,
            detectedLanguage = detectedLanguage,
            tier = tier,
            speechRateModifier = speechRateModifier,
            delegate = delegate,
            remoteVoicesController = remoteVoicesController,
            voiceResolver = RemoteVoiceResolver(remoteVoicesController, voiceUtility),
            context = context,
            scope = scope,
        )
    }
}