package org.zotero.android.screens.reader.readaloud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.documentworker.DocumentWorkerController
import org.zotero.android.speech.SpeechManager
import org.zotero.android.speech.SpeechManagerFactory
import org.zotero.android.speech.SpeechState
import timber.log.Timber
import javax.inject.Inject

class ReaderReadAloudHandler @Inject constructor(
    private val speechManagerFactory: SpeechManagerFactory,
    private val documentWorkerController: DocumentWorkerController,
    dispatchers: Dispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private var speechManager: SpeechManager<Int>? = null
    private var speechDelegate: ReaderSpeechDelegate? = null
    private var stateJob: Job? = null

    private val mutableIsActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = mutableIsActive

    private val mutableControls = MutableStateFlow<ReadAloudControlsState?>(null)
    val controls: StateFlow<ReadAloudControlsState?> = mutableControls

    fun play() {
        speechManager?.resume()
    }

    fun pause() {
        speechManager?.pause()
    }

    fun skipForward() {
        speechManager?.navigateForward()
    }

    fun skipBackward() {
        speechManager?.navigateBackward()
    }

    fun toggle(delegate: ReaderSpeechDelegate) {
        if (speechManager != null) {
            stopAndReleaseManager()
        } else {
            startSession(delegate)
        }
    }

    fun release() {
        stopAndReleaseManager()
        scope.cancel()
    }

    private fun startSession(delegate: ReaderSpeechDelegate) {
        Timber.i("ReaderReadAloudHandler: starting read aloud")
        val manager = speechManagerFactory.create(
            delegate = delegate,
            voiceLanguage = null,
            documentWorkerController = documentWorkerController,
        )
        speechDelegate = delegate
        speechManager = manager
        mutableIsActive.value = true
        stateJob = scope.launch {
            combine(manager.state, manager.remainingTime) { state, remainingTime ->
                state to remainingTime
            }.collect { (state, remainingTime) ->
                if (state is SpeechState.Stopped) {
                    stopAndReleaseManager()
                } else {
                    mutableControls.value = ReadAloudControlsState.from(state, remainingTime)
                }
            }
        }
        manager.start(SpeechManager.StartTarget.CurrentPage)
    }

    private fun stopAndReleaseManager() {
        stateJob?.cancel()
        stateJob = null
        speechManager?.release()
        speechManager = null
        speechDelegate = null
        mutableControls.value = null
        mutableIsActive.value = false
    }
}