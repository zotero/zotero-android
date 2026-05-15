package org.zotero.android.api.network

import java.lang.Math.pow
import kotlin.math.min

sealed class RetryDelay {
    companion object {
        val maxAttemptsCount: Int = 10
        private const val DEFAULT_INITIAL_DELAY_MS = 2500L
        private const val DEFAULT_MULTIPLIER = 2.0
        private const val MAX_DELAY_MS = 5 * 60 * 1000L
    }

    data class constant(val millis: Long) : RetryDelay()
    data class progressive(
        val initial: Long = DEFAULT_INITIAL_DELAY_MS,
        val multiplier: Double = DEFAULT_MULTIPLIER,
        val maxDelay: Long = MAX_DELAY_MS
    ) : RetryDelay()

    fun millis(attempt: Int): Long {
        return when (this) {
            is constant -> this.millis
            is progressive -> calculateProgressiveDelay(attempt)
        }
    }

    private fun progressive.calculateProgressiveDelay(attempt: Int): Long {
        val delay = if (attempt <= 1) {
            initial
        } else {
            (initial * pow(multiplier, (attempt - 1).toDouble())).toLong()
        }
        return min(maxDelay, delay)
    }
}