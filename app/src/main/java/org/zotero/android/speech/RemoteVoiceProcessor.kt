package org.zotero.android.speech

import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.zotero.android.speech.data.RemoteAudioSegment
import org.zotero.android.speech.data.RemoteVoice
import org.zotero.android.speech.data.TextRange
import timber.log.Timber

@UnstableApi
class RemoteVoiceProcessor(
    preferredLanguage: String?,
    override var detectedLanguage: String?,
    private var tier: RemoteVoice.Tier,
    speechRateModifier: Float,
    private val delegate: VoiceProcessorDelegate,
    private val remoteVoicesController: RemoteVoicesController,
    private val voiceResolver: RemoteVoiceResolver,
    context: Context,
    private val scope: CoroutineScope,
) : VoiceProcessor, RemoteAudioPlayer.Listener {

    private object EndOfPage : Exception()

    private val debounceMillis = 500L

    private val preloader = RemoteAudioPreloader(remoteVoicesController, scope)
    private val creditsMonitor = CreditsMonitor(remoteVoicesController, scope)
    private val player = RemoteAudioPlayer(context)

    private var segments: List<SpeechDocumentParser.Segment> = emptyList()
    private var voice: RemoteVoice? = null
    private var speakJob: Job? = null
    private var transitionJob: Job? = null
    private var pendingPlaybackRange: TextRange? = null
    private var shouldReloadOnResume = false

    override var preferredLanguage: String? = preferredLanguage
        private set

    override var speechRateModifier: Float = speechRateModifier
        set(value) {
            field = value
            player.speed = value
        }

    override val speechVoice: SpeechVoice?
        get() = voice?.let { SpeechVoice.Remote(it) }

    override val canResume: Boolean
        get() = player.canResume

    override val segmentAudioProgress: Float
        get() = player.progress

    override val segmentAudioElapsedTime: Double
        get() = player.elapsedTime

    init {
        player.listener = this
        player.speed = speechRateModifier
        preloader.onPendingSegmentReady = { range, data ->
            if (range == pendingPlaybackRange) {
                pendingPlaybackRange = null
                handleSpeechSuccess(data)
            }
        }
        preloader.onPendingSegmentFailed = { error ->
            if (pendingPlaybackRange != null) {
                pendingPlaybackRange = null
                handleSpeechFailure(error)
            }
        }
    }

    fun set(voice: RemoteVoice, preferredLanguage: String?) {
        val voiceChanged = this.voice?.id != voice.id || tier != voice.tier
        tier = voice.tier
        this.preferredLanguage = preferredLanguage
        this.voice = voice

        if (voiceChanged) {
            stopPreloadingAndClearCache()
            delegate.remainingTime.value = null
            scope.launch { loadCredits() }
            if (player.canResume) {
                if (player.isPlaying) {
                    player.pause()
                }
                shouldReloadOnResume = true
            }
        }
    }

    suspend fun standardVoice(language: String): RemoteVoice? {
        return voiceResolver.standardVoice(language)
    }

    override fun speak(segments: List<SpeechDocumentParser.Segment>, startPageTextOffset: Int) {
        if (this.segments.map { it.text } != segments.map { it.text } || startPageTextOffset != 0) {
            stopPreloadingAndClearCache()
        }
        this.segments = segments
        player.stop()
        speakJob?.cancel()
        transitionJob?.cancel()
        pendingPlaybackRange = null

        if (delegate.state.value != SpeechState.Initializing) {
            delegate.state.value = SpeechState.Loading
        }

        val needsDebounce = voice != null
        speakJob = scope.launch {
            if (needsDebounce) {
                delay(debounceMillis)
            }
            val resolved = try {
                voice ?: loadVoice()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleSpeechFailure(e)
                return@launch
            }
            startSpeaking(startPageTextOffset, resolved)
        }
    }

    override fun pause() {
        speakJob?.cancel()
        speakJob = null
        if (!player.isPlaying) {
            return
        }
        player.pause()
        creditsMonitor.stop()
        delegate.state.value = SpeechState.Paused
    }

    override fun resume() {
        val state = delegate.state.value
        if (!player.canResume || !(state.isPaused || state.isOutOfCredits)) {
            return
        }
        if (shouldReloadOnResume) {
            shouldReloadOnResume = false
            reloadCurrentSegment()
        } else {
            player.resume()
            delegate.state.value = SpeechState.Speaking
            startCreditPoll()
        }
    }

    override fun stop() {
        finishSpeaking()
    }

    override fun invalidateCurrentPlayback() {
        shouldReloadOnResume = true
    }

    fun release() {
        speakJob?.cancel()
        transitionJob?.cancel()
        preloader.clear()
        creditsMonitor.stop()
        player.release()
    }

    override fun verifyPlaybackAllowed(completion: (SpeechState.OutOfCreditsReason?) -> Unit) {
        scope.launch {
            val reason = creditsMonitor.verifyPlaybackAllowed(tier) { updateRemainingTimeDisplay(it) }
            if (reason != null && voice == null) {
                try {
                    loadVoice()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "RemoteVoiceProcessor: could not load credits before playback")
                    completion(null)
                    return@launch
                }
            }
            completion(reason)
        }
    }

    override fun onPlaybackFinished() {
        val voice = voice
        val speechRange = delegate.speechRange
        if (segments.isEmpty() || voice == null || speechRange == null) {
            finishSpeaking()
            return
        }
        val nextStartIndex = speechRange.end
        val delayMillis = if (voice.sentenceDelay > 0) (voice.sentenceDelay / speechRateModifier).toLong() else 0L
        if (delayMillis == 0L) {
            startSpeaking(nextStartIndex, voice)
            return
        }
        transitionJob?.cancel()
        transitionJob = scope.launch {
            delay(delayMillis)
            val state = delegate.state.value
            if (state.isSpeaking || state == SpeechState.Loading) {
                startSpeaking(nextStartIndex, voice)
            }
        }
    }

    override fun onPlaybackError(error: PlaybackException) {
        Timber.e(error, "RemoteVoiceProcessor: can't play audio")
        finishSpeaking()
    }

    private fun reloadCurrentSegment() {
        val voice = voice
        val speechRange = delegate.speechRange
        if (segments.isEmpty() || voice == null || speechRange == null) {
            return
        }
        preloader.clear()
        player.stop()
        startSpeaking(speechRange.location, voice)
    }

    private fun stopPreloadingAndClearCache() {
        preloader.clear()
        pendingPlaybackRange = null
    }

    private suspend fun loadVoice(): RemoteVoice {
        val resolved = voiceResolver.resolveVoice(language = language, tier = tier)
        voice = resolved
        return resolved
    }

    private fun startSpeaking(startIndex: Int, voice: RemoteVoice) {
        val range = findNextRange(startIndex, voice)
        if (range == null) {
            handleSpeechFailure(EndOfPage)
            return
        }

        delegate.speechRangeWillChange(range)

        preloader.takeCached(range)?.let { data ->
            handleSpeechSuccess(data)
            ensureSegmentsPreloaded(range, voice)
            return
        }

        if (preloader.isLoading(range)) {
            pendingPlaybackRange = range
            preloader.waitForPlayback(range)
            delegate.state.value = SpeechState.Loading
            return
        }

        val segment = segmentFor(range)
        if (segment == null) {
            handleSpeechFailure(EndOfPage)
            return
        }

        delegate.state.value = SpeechState.Loading
        startCreditPoll()
        preloader.loadForPlayback(
            segment = segment,
            voiceId = voice.id,
            onSuccess = { data ->
                handleSpeechSuccess(data)
                ensureSegmentsPreloaded(range, voice)
            },
            onFailure = { error -> handleSpeechFailure(error) },
        )
        ensureSegmentsPreloaded(range, voice)
    }

    private fun ensureSegmentsPreloaded(after: TextRange, voice: RemoteVoice) {
        preloader.ensurePreloaded(after = after, voiceId = voice.id) { index ->
            findNextRange(index, voice)?.let { range -> segmentFor(range) }
        }
    }

    private fun segmentFor(range: TextRange): RemoteAudioSegment? {
        for (segment in segments) {
            val start = segment.pageOffset
            if (range.location >= start && range.location < start + segment.text.length) {
                val intraStart = range.location - start
                val intraEnd = minOf(intraStart + range.length, segment.text.length)
                return RemoteAudioSegment(range = range, text = segment.text.substring(intraStart, intraEnd))
            }
        }
        return null
    }

    private fun findNextRange(index: Int, voice: RemoteVoice): TextRange? {
        return if (voice.granularity == RemoteVoice.Granularity.sentence) {
            SpeechDocumentParser.sentenceRange(index, segments)
        } else {
            SpeechDocumentParser.paragraphRange(index, segments)
        }
    }

    private fun handleSpeechSuccess(data: ByteArray) {
        player.play(data)
        delegate.state.value = SpeechState.Speaking
    }

    private fun handleSpeechFailure(error: Throwable) {
        when {
            error === EndOfPage -> {
                if (!delegate.goToNextPageIfAvailable()) {
                    finishSpeaking()
                }
            }

            error is RemoteVoicesController.Error.outOfCredits -> {
                updateRemainingTimeDisplay(RemoteVoicesController.Credits(standard = 0, premium = 0))
                delegate.state.value = SpeechState.OutOfCredits(error.reason)
            }

            else -> {
                Timber.e(error, "RemoteVoiceProcessor: can't download sound")
                delegate.state.value = SpeechState.Stopped
            }
        }
    }

    private fun startCreditPoll() {
        creditsMonitor.start { updateRemainingTimeDisplay(it) }
    }

    private suspend fun loadCredits() {
        val credits = creditsMonitor.loadCredits() ?: return
        updateRemainingTimeDisplay(credits)
    }

    private fun updateRemainingTimeDisplay(credits: RemoteVoicesController.Credits) {
        if (tier == RemoteVoice.Tier.premium && credits.premium != 0 && voice == null) {
            return
        }
        delegate.remainingTime.value = CreditsMonitor.remainingTime(credits, tier, voice)
    }

    private fun finishSpeaking() {
        speakJob?.cancel()
        speakJob = null
        transitionJob?.cancel()
        transitionJob = null
        segments = emptyList()
        player.stop()
        preloader.clear()
        pendingPlaybackRange = null
        shouldReloadOnResume = false
        creditsMonitor.stop()
        delegate.state.value = SpeechState.Stopped
    }
}