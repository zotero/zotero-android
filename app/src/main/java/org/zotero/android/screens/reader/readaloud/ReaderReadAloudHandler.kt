package org.zotero.android.screens.reader.readaloud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.zotero.android.architecture.Defaults
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.database.DbWrapperMain
import org.zotero.android.database.requests.ReadLastReadAloudPositionDbRequest
import org.zotero.android.database.requests.ReadSpeechLanguageDbRequest
import org.zotero.android.database.requests.SetSpeechLanguageDbRequest
import org.zotero.android.database.requests.StoreLastReadAloudPositionDbRequest
import org.zotero.android.documentworker.DocumentWorkerController
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceChange
import org.zotero.android.screens.reader.readaloud.voiceselection.ReadAloudVoiceSelectionArgs
import org.zotero.android.speech.SpeechManager
import org.zotero.android.speech.SpeechManagerFactory
import org.zotero.android.speech.SpeechState
import org.zotero.android.speech.SpeechVoice
import org.zotero.android.speech.data.ReadAloudResumePosition
import org.zotero.android.sync.LibraryIdentifier
import timber.log.Timber
import javax.inject.Inject

class ReaderReadAloudHandler @Inject constructor(
    private val speechManagerFactory: SpeechManagerFactory,
    private val documentWorkerController: DocumentWorkerController,
    private val defaults: Defaults,
    private val dbWrapperMain: DbWrapperMain,
    dispatchers: Dispatchers,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private var itemKey: String? = null
    private var libraryId: LibraryIdentifier? = null
    private var voiceLanguage: String? = null
    private var wasPlayingBeforeVoiceChange = false
    private val positionStoreDebounceMillis = 3000L
    private var lastSpeakingPosition: ReadAloudResumePosition? = null
    private var pendingPosition: ReadAloudResumePosition? = null
    private var pendingPositionJob: Job? = null

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

    fun attach(key: String, libraryId: LibraryIdentifier) {
        itemKey = key
        this.libraryId = libraryId
        voiceLanguage = try {
            dbWrapperMain.realmDbStorage
                .perform(request = ReadSpeechLanguageDbRequest(key = key, libraryId = libraryId))
                .takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "ReaderReadAloudHandler: can't load speech language")
            null
        }
        if (lastSpeakingPosition == null && speechManager == null) {
            lastSpeakingPosition = try {
                dbWrapperMain.realmDbStorage
                    .perform(request = ReadLastReadAloudPositionDbRequest(attachmentKey = key, libraryId = libraryId))
                    .position
            } catch (e: Exception) {
                Timber.e(e, "ReaderReadAloudHandler: can't load read aloud position")
                null
            }
        }
    }

    fun storePendingPosition() {
        pendingPositionJob?.cancel()
        pendingPositionJob = null
        val position = pendingPosition ?: return
        pendingPosition = null
        val key = itemKey ?: return
        val libraryId = libraryId ?: return
        try {
            dbWrapperMain.realmDbStorage.perform(
                request = StoreLastReadAloudPositionDbRequest(key = key, libraryId = libraryId, position = position)
            )
        } catch (e: Exception) {
            Timber.e(e, "ReaderReadAloudHandler: can't store read aloud position")
        }
    }

    private fun scheduleStore(position: ReadAloudResumePosition) {
        lastSpeakingPosition = position
        pendingPosition = position
        pendingPositionJob?.cancel()
        pendingPositionJob = scope.launch {
            delay(positionStoreDebounceMillis)
            storePendingPosition()
        }
    }

    fun prepareVoiceChange(): ReadAloudVoiceSelectionArgs? {
        val manager = speechManager ?: return null
        val voice = manager.voice ?: return null
        val state = manager.state.value
        wasPlayingBeforeVoiceChange = state.isSpeakingOrLoading || state.isOutOfCredits
        manager.pause()
        return ReadAloudVoiceSelectionArgs(
            initialVoice = voice,
            language = manager.language,
            detectedLanguage = manager.detectedLanguage,
            savesOnVoiceChange = true,
        )
    }

    fun onVoiceChange(change: ReadAloudVoiceChange) {
        saveSpeechLanguage(change.preferredLanguage)
        val manager = speechManager ?: return
        manager.set(change.voice, change.preferredLanguage)
        if (wasPlayingBeforeVoiceChange) {
            wasPlayingBeforeVoiceChange = false
            manager.resume()
        }
    }

    private fun saveSpeechLanguage(language: String?) {
        voiceLanguage = language
        val key = itemKey ?: return
        val libraryId = libraryId ?: return
        try {
            dbWrapperMain.realmDbStorage.perform(
                request = SetSpeechLanguageDbRequest(key = key, libraryId = libraryId, language = language)
            )
        } catch (e: Exception) {
            Timber.e(e, "ReaderReadAloudHandler: can't save speech language")
        }
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
                language = voiceLanguage,
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
        storePendingPosition()
        scope.cancel()
    }

    private fun startSession(delegate: ReaderSpeechDelegate, voice: SpeechVoice?) {
        Timber.i("ReaderReadAloudHandler: starting read aloud")
        val manager = speechManagerFactory.create(
            delegate = delegate,
            voiceLanguage = voiceLanguage,
            documentWorkerController = documentWorkerController,
        )
        voice?.let { manager.set(it, preferredLanguage = null) }
        manager.onSpeakingPositionChanged = { scheduleStore(it) }
        speechDelegate = delegate
        speechManager = manager
        mutableIsActive.value = true
        stateJob = scope.launch {
            combine(manager.state, manager.remainingTime) { state, remainingTime ->
                state to remainingTime
            }.collect { (state, remainingTime) ->
                if (state is SpeechState.Stopped) {
                    stopAndReleaseManager()
                    storePendingPosition()
                } else {
                    if (state.isOutOfCredits) {
                        storePendingPosition()
                    }
                    mutableControls.value = ReadAloudControlsState.from(state, remainingTime, manager.voice)
                }
            }
        }
        val resumePosition = lastSpeakingPosition
        manager.start(
            if (resumePosition != null) SpeechManager.StartTarget.Resume(resumePosition) else SpeechManager.StartTarget.CurrentPage
        )
    }

    private fun stopAndReleaseManager() {
        speechDelegate?.clearSpeechHighlight()
        stateJob?.cancel()
        stateJob = null
        speechManager?.release()
        speechManager = null
        speechDelegate = null
        mutableControls.value = null
        mutableIsActive.value = false
    }
}