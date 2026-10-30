package org.zotero.android.screens.reader.readaloud

import org.zotero.android.speech.RemainingTimeFormatter
import org.zotero.android.speech.SpeechState
import org.zotero.android.speech.SpeechVoice
import org.zotero.android.speech.data.RemoteVoice

data class ReadAloudControlsState(
    val isPlayVisible: Boolean,
    val isPauseVisible: Boolean,
    val isProgressVisible: Boolean,
    val areSkipButtonsEnabled: Boolean,
    val remainingTimeText: String?,
    val downgrade: Downgrade?,
) {
    enum class Downgrade { standard, local }

    companion object {
        fun from(state: SpeechState, remainingTime: Double?, voice: SpeechVoice?): ReadAloudControlsState {
            val remainingTimeText = remainingTime
                ?.takeIf { RemainingTimeFormatter.isWarning(it) }
                ?.let { RemainingTimeFormatter.formatted(it) }
            val downgrade = downgrade(remainingTime, voice)
            return when (state) {
                SpeechState.Initializing -> ReadAloudControlsState(
                    isPlayVisible = false,
                    isPauseVisible = false,
                    isProgressVisible = true,
                    areSkipButtonsEnabled = false,
                    remainingTimeText = remainingTimeText,
                    downgrade = downgrade,
                )

                SpeechState.Loading -> ReadAloudControlsState(
                    isPlayVisible = false,
                    isPauseVisible = false,
                    isProgressVisible = true,
                    areSkipButtonsEnabled = true,
                    remainingTimeText = remainingTimeText,
                    downgrade = downgrade,
                )

                SpeechState.Speaking -> ReadAloudControlsState(
                    isPlayVisible = false,
                    isPauseVisible = true,
                    isProgressVisible = false,
                    areSkipButtonsEnabled = true,
                    remainingTimeText = remainingTimeText,
                    downgrade = downgrade,
                )

                SpeechState.Paused -> ReadAloudControlsState(
                    isPlayVisible = true,
                    isPauseVisible = false,
                    isProgressVisible = false,
                    areSkipButtonsEnabled = true,
                    remainingTimeText = remainingTimeText,
                    downgrade = downgrade,
                )

                SpeechState.Stopped, is SpeechState.OutOfCredits -> ReadAloudControlsState(
                    isPlayVisible = true,
                    isPauseVisible = false,
                    isProgressVisible = false,
                    areSkipButtonsEnabled = false,
                    remainingTimeText = remainingTimeText,
                    downgrade = downgrade,
                )
            }
        }

        private fun downgrade(remainingTime: Double?, voice: SpeechVoice?): Downgrade? {
            if (remainingTime == null || remainingTime > 0) {
                return null
            }
            val remoteVoice = (voice as? SpeechVoice.Remote)?.voice ?: return null
            return when (remoteVoice.tier) {
                RemoteVoice.Tier.premium -> Downgrade.standard
                RemoteVoice.Tier.standard -> Downgrade.local
            }
        }
    }
}