package org.zotero.android.speech

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.zotero.android.speech.data.TextRange
import timber.log.Timber

class LocalVoiceProcessor(
    preferredLanguage: String?,
    override var detectedLanguage: String?,
    speechRateModifier: Float,
    private val delegate: VoiceProcessorDelegate,
    private val localVoiceCatalog: LocalVoiceCatalog,
    private val voiceUtility: VoiceUtility,
    private val scope: CoroutineScope,
) : VoiceProcessor, LocalSpeechProgressListener.Handler {

    private val maxUtteranceLength = 3000

    private var engine: TextToSpeech? = null
    private var speakJob: Job? = null
    private var generation = 0
    private var utterances: Map<String, LocalUtterance> = emptyMap()
    private var text: String? = null
    private var lastRangeStart = 0
    private var voice: Voice? = null
    private var isActive = false
    private var isPaused = false

    override var preferredLanguage: String? = preferredLanguage
        private set

    override var speechRateModifier: Float = speechRateModifier
        set(value) {
            field = value
            utteranceChanged()
        }

    override val speechVoice: SpeechVoice?
        get() = voice?.let { SpeechVoice.Local(it) }

    override val canResume: Boolean
        get() = isPaused

    override val segmentAudioProgress: Float
        get() = 0f

    override val segmentAudioElapsedTime: Double
        get() = 0.0

    override fun speak(segments: List<SpeechDocumentParser.Segment>, startPageTextOffset: Int) {
        speak(
            pageText = segments.joinToString(SpeechDocumentParser.segmentSeparator) { it.text },
            startIndex = startPageTextOffset,
        )
    }

    override fun pause() {
        if (!isActive || isPaused) {
            return
        }
        generation += 1
        engine?.stop()
        isPaused = true
        delegate.state.value = SpeechState.Paused
    }

    override fun resume() {
        if (!isPaused) {
            return
        }
        reloadUtterance()
    }

    override fun stop() {
        if (!isActive && delegate.state.value != SpeechState.Initializing) {
            return
        }
        generation += 1
        speakJob?.cancel()
        engine?.stop()
        finishSpeaking()
    }

    override fun invalidateCurrentPlayback() {
    }

    fun set(voice: Voice, preferredLanguage: String?) {
        if (this.voice?.identifier == voice.identifier) {
            return
        }
        this.preferredLanguage = preferredLanguage
        this.voice = voice
        utteranceChanged()
    }

    fun release() {
        generation += 1
        speakJob?.cancel()
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    override fun utteranceStarted(utteranceId: String) {
        if (!isCurrent(utteranceId)) {
            return
        }
        isPaused = false
        delegate.state.value = SpeechState.Speaking
    }

    override fun utteranceRangeStarted(utteranceId: String, range: TextRange) {
        if (!isCurrent(utteranceId)) {
            return
        }
        val utterance = utterances[utteranceId] ?: return
        val adjusted = TextRange(location = utterance.pageOffset + range.location, length = range.length)
        lastRangeStart = adjusted.location
        delegate.speechRangeWillChange(adjusted)
    }

    override fun utteranceFinished(utteranceId: String) {
        if (!isCurrent(utteranceId)) {
            return
        }
        val utterance = utterances[utteranceId] ?: return
        if (!utterance.isLast) {
            return
        }
        if (!delegate.goToNextPageIfAvailable()) {
            finishSpeaking()
        }
    }

    override fun utteranceFailed(utteranceId: String) {
        if (!isCurrent(utteranceId)) {
            return
        }
        generation += 1
        engine?.stop()
        finishSpeaking()
    }

    private fun speak(pageText: String, startIndex: Int) {
        generation += 1
        val currentGeneration = generation
        speakJob?.cancel()
        engine?.stop()

        val clampedStart = startIndex.coerceIn(0, pageText.length)
        text = pageText
        lastRangeStart = clampedStart
        isActive = true
        isPaused = false

        speakJob = scope.launch {
            val engine = prepareEngine() ?: return@launch finishSpeaking()
            if (currentGeneration != generation) {
                return@launch
            }
            val voice = this@LocalVoiceProcessor.voice ?: resolveVoice(engine)
            this@LocalVoiceProcessor.voice = voice
            voice?.let { engine.voice = it }
            engine.setSpeechRate(speechRateModifier)

            val chunks = LocalUtterance.split(
                pageText = pageText,
                startIndex = clampedStart,
                generation = currentGeneration,
                maxLength = minOf(TextToSpeech.getMaxSpeechInputLength(), maxUtteranceLength),
            )
            if (chunks.isEmpty()) {
                return@launch finishSpeaking()
            }
            utterances = chunks.associateBy { it.id }
            chunks.forEachIndexed { index, chunk ->
                val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                engine.speak(chunk.text, mode, null, chunk.id)
            }
        }
    }

    private suspend fun prepareEngine(): TextToSpeech? {
        engine?.let { return it }
        localVoiceCatalog.loadVoices()
        val created = localVoiceCatalog.createEngine() ?: return null
        created.setOnUtteranceProgressListener(LocalSpeechProgressListener(scope, this))
        engine = created
        return created
    }

    private fun resolveVoice(engine: TextToSpeech): Voice? {
        return voiceUtility.findLocalVoice(language) ?: engine.defaultVoice
    }

    private fun utteranceChanged() {
        if (isActive && !isPaused) {
            reloadUtterance()
        }
    }

    private fun reloadUtterance() {
        val text = text ?: return
        val startIndex = delegate.speechRange?.location ?: lastRangeStart
        speak(pageText = text, startIndex = startIndex)
    }

    private fun isCurrent(utteranceId: String): Boolean {
        return LocalUtterance.generation(utteranceId) == generation
    }

    private fun finishSpeaking() {
        Timber.d("LocalVoiceProcessor: finished speaking")
        text = null
        utterances = emptyMap()
        voice = null
        isActive = false
        isPaused = false
        delegate.state.value = SpeechState.Stopped
    }
}