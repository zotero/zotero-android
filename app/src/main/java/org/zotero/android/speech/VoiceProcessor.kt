package org.zotero.android.speech

import kotlinx.coroutines.flow.MutableStateFlow
import org.zotero.android.speech.data.TextRange

interface VoiceProcessor {
    val speechVoice: SpeechVoice?
    val preferredLanguage: String?
    var detectedLanguage: String?
    var speechRateModifier: Float
    val canResume: Boolean
    val segmentAudioProgress: Float
    val segmentAudioElapsedTime: Double

    val language: String
        get() = preferredLanguage ?: detectedLanguage ?: "en"

    fun verifyPlaybackAllowed(completion: (SpeechState.OutOfCreditsReason?) -> Unit) {
        completion(null)
    }

    fun speak(segments: List<SpeechDocumentParser.Segment>, startPageTextOffset: Int)
    fun pause()
    fun resume()
    fun stop()
    fun invalidateCurrentPlayback()
}

interface VoiceProcessorDelegate {
    val state: MutableStateFlow<SpeechState>
    val remainingTime: MutableStateFlow<Double?>
    val speechRange: TextRange?

    fun goToNextPageIfAvailable(): Boolean
    fun speechRangeWillChange(range: TextRange)
}