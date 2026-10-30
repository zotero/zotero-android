package org.zotero.android.screens.reader.readaloud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.documentworker.DocumentWorkerController
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceChange
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceSelectionArgs
import org.zotero.android.speech.SpeechManager
import org.zotero.android.speech.SpeechManagerFactory
import org.zotero.android.speech.SpeechState
import org.zotero.android.speech.SpeechVoice
import timber.log.Timber
import javax.inject.Inject

class ReaderReadAloudHandler @Inject constructor(
    private val speechManagerFactory: SpeechManagerFactory,
    private val documentWorkerController: DocumentWorkerController,
    private val defaults: Defaults,
    dispatchers: Dispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private var pendingOnboardingDelegate: ReaderSpeechDelegate? = null
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

    fun downgradeVoice() {
        speechManager?.downgradeVoiceTierAndContinue()
    }

    fun toggle(delegate: ReaderSpeechDelegate, showOnboarding: (ReadAloudVoiceSelectionArgs) -> Unit) {
        if (speechManager != null) {
            stopAndReleaseManager()
            return
        }
        if (defaults.didShowReadAloudOnboarding()) {
            startSession(delegate, voice = null)
            return
        }
        pendingOnboardingDelegate = delegate
        showOnboarding(
            ReadAloudVoiceSelectionArgs(
                initialVoice = null,
                language = null,
                detectedLanguage = "en",
                savesOnVoiceChange = false,
            )
        )
    }

    fun onOnboardingResult(change: ReadAloudVoiceChange?) {
        val delegate = pendingOnboardingDelegate ?: return
        pendingOnboardingDelegate = null
        if (change == null) {
            return
        }
        defaults.setDidShowReadAloudOnboarding(true)
        startSession(delegate, change.voice)
    }

    fun release() {
        stopAndReleaseManager()
        scope.cancel()
    }

    private fun startSession(delegate: ReaderSpeechDelegate, voice: SpeechVoice?) {
        Timber.i("ReaderReadAloudHandler: starting read aloud")
        val manager = speechManagerFactory.create(
            delegate = delegate,
            voiceLanguage = null,
            documentWorkerController = documentWorkerController,
        )
        voice?.let { manager.set(it, preferredLanguage = null) }
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
                    mutableControls.value = ReadAloudControlsState.from(state, remainingTime, manager.voice)
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