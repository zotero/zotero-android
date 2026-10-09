package org.zotero.android.speech

import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.speech.documentworker.DocumentWorkerWebCallChainExecutor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeechManagerFactory @Inject constructor(
    private val documentWorkerExecutor: DocumentWorkerWebCallChainExecutor,
    private val voiceProcessorFactory: VoiceProcessorFactory,
    private val dispatchers: Dispatchers,
) {
    fun <Index : Any> create(
        delegate: SpeechManagerDelegate<Index>,
        voiceLanguage: String?,
    ): SpeechManager<Index> {
        return SpeechManager(
            delegate = delegate,
            voiceLanguage = voiceLanguage,
            voiceProcessorFactory = voiceProcessorFactory,
            documentWorkerExecutor = documentWorkerExecutor,
            dispatchers = dispatchers,
        )
    }
}