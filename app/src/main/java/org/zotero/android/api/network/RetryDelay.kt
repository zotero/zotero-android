package org.zotero.android.api.network

import java.lang.Math.pow
import kotlin.math.min

sealed class RetryDelay {
    companion object {
        val maxAttemptsCount: Int = 10
    }

    data class constant(val millis: Long) : RetryDelay()
    data class progressive(
        val initial: Long = 2500L,
        val multiplier: Double = 2.0,
        val maxDelay: Long = 3600 * 1000L
    ) : RetryDelay()

    fun millis(attempt: Int): Long {
        when (this) {
            is constant -> {
                return this.millis
            }

            is progressive -> {
                val delay = if (attempt == 1) {
                    this.initial
                } else {
                    (this.initial * pow(this.multiplier, (attempt - 1).toDouble()))
                }
                return min(this.maxDelay, delay.toLong())
            }
        }

    }
}