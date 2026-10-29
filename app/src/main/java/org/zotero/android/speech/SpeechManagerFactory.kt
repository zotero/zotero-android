package org.zotero.android.speech

import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.documentworker.DocumentWorkerController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechManagerFactory @Inject constructor(
    private val voiceProcessorFactory: VoiceProcessorFactory,
    private val defaults: Defaults,
    private val dispatchers: Dispatchers,
) {
    fun <Index : Any> create(
        delegate: SpeechManagerDelegate<Index>,
        voiceLanguage: String?,
        documentWorkerController: DocumentWorkerController,
    ): SpeechManager<Index> {
        return SpeechManager(
            delegate = delegate,
            voiceLanguage = voiceLanguage,
            remoteVoiceTier = defaults.getRemoteVoiceTier(),
            voiceProcessorFactory = voiceProcessorFactory,
            documentWorkerController = documentWorkerController,
            defaults = defaults,
            dispatchers = dispatchers,
        )
    }
}