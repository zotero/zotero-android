package org.zotero.android.screens.reader.readaloud

import org.zotero.android.speech.RemainingTimeFormatter
import org.zotero.android.speech.SpeechState

data class ReadAloudControlsState(
    val isPlayVisible: Boolean,
    val isPauseVisible: Boolean,
    val isProgressVisible: Boolean,
    val areSkipButtonsEnabled: Boolean,
    val remainingTimeText: String?,
) {
    companion object {
        fun from(state: SpeechState, remainingTime: Double?): ReadAloudControlsState {
            val remainingTimeText = remainingTime
                ?.takeIf { RemainingTimeFormatter.isWarning(it) }
                ?.let { RemainingTimeFormatter.formatted(it) }
            return when (state) {
                SpeechState.Initializing -> ReadAloudControlsState(
                    isPlayVisible = false,
                    isPauseVisible = false,
                    isProgressVisible = true,
                    areSkipButtonsEnabled = false,
                    remainingTimeText = remainingTimeText,
                )

                SpeechState.Loading -> ReadAloudControlsState(
                    isPlayVisible = false,
                    isPauseVisible = false,
                    isProgressVisible = true,
                    areSkipButtonsEnabled = true,
                    remainingTimeText = remainingTimeText,
                )

                SpeechState.Speaking -> ReadAloudControlsState(
                    isPlayVisible = false,
                    isPauseVisible = true,
                    isProgressVisible = false,
                    areSkipButtonsEnabled = true,
                    remainingTimeText = remainingTimeText,
                )

                SpeechState.Paused -> ReadAloudControlsState(
                    isPlayVisible = true,
                    isPauseVisible = false,
                    isProgressVisible = false,
                    areSkipButtonsEnabled = true,
                    remainingTimeText = remainingTimeText,
                )

                SpeechState.Stopped, is SpeechState.OutOfCredits -> ReadAloudControlsState(
                    isPlayVisible = true,
                    isPauseVisible = false,
                    isProgressVisible = false,
                    areSkipButtonsEnabled = false,
                    remainingTimeText = remainingTimeText,
                )
            }
        }
    }
}