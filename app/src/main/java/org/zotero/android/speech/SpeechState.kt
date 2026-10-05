package org.zotero.android.speech

sealed class SpeechState {
    enum class OutOfCreditsReason {
        dailyLimitExceeded,
        quotaExceeded,
    }

    data object Initializing : SpeechState()
    data object Loading : SpeechState()
    data object Speaking : SpeechState()
    data object Paused : SpeechState()
    data object Stopped : SpeechState()
    data class OutOfCredits(val reason: OutOfCreditsReason) : SpeechState()

    val isStopped: Boolean get() = this is Stopped
    val isPaused: Boolean get() = this is Paused
    val isSpeaking: Boolean get() = this is Speaking
    val isSpeakingOrLoading: Boolean get() = this is Speaking || this is Initializing || this is Loading
    val isOutOfCredits: Boolean get() = this is OutOfCredits
}