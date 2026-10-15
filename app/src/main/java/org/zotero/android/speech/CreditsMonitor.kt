package org.zotero.android.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.zotero.android.api.network.CustomResult
import org.zotero.android.speech.data.RemoteVoice
import timber.log.Timber

class CreditsMonitor(
    private val remoteVoicesController: RemoteVoicesController,
    private val scope: CoroutineScope,
    private val pollIntervalMillis: Long = 60_000L,
) {
    private var pollJob: Job? = null

    fun start(onCredits: (RemoteVoicesController.Credits) -> Unit) {
        if (pollJob != null) {
            return
        }
        pollJob = scope.launch {
            while (isActive) {
                loadCredits()?.let(onCredits)
                delay(pollIntervalMillis)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    suspend fun verifyPlaybackAllowed(
        tier: RemoteVoice.Tier,
        onCredits: (RemoteVoicesController.Credits) -> Unit,
    ): SpeechState.OutOfCreditsReason? {
        val credits = loadCredits() ?: return null
        onCredits(credits)
        return if (credits.remaining(tier) == 0) SpeechState.OutOfCreditsReason.quotaExceeded else null
    }

    private suspend fun loadCredits(): RemoteVoicesController.Credits? {
        val result = remoteVoicesController.loadCredits()
        if (result is CustomResult.GeneralError) {
            Timber.e("CreditsMonitor: could not load credits - $result")
            return null
        }
        return (result as CustomResult.GeneralSuccess).value
    }

    companion object {
        fun remainingTime(
            credits: RemoteVoicesController.Credits,
            tier: RemoteVoice.Tier,
            voice: RemoteVoice?,
        ): Double? {
            return when (tier) {
                RemoteVoice.Tier.standard -> null
                RemoteVoice.Tier.premium -> when {
                    credits.premium == 0 -> 0.0
                    voice != null -> credits.premium.toDouble() / voice.creditsPerMinute * 60
                    else -> null
                }
            }
        }

        private fun RemoteVoicesController.Credits.remaining(tier: RemoteVoice.Tier): Int {
            return when (tier) {
                RemoteVoice.Tier.standard -> standard
                RemoteVoice.Tier.premium -> premium
            }
        }
    }
}