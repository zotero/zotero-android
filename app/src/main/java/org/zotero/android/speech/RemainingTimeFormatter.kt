package org.zotero.android.speech

import kotlin.math.ceil

object RemainingTimeFormatter {
    const val maxDisplayThresholdSeconds: Double = 90 * 24 * 60 * 60.0
    const val warningThresholdSeconds: Double = 180.0
    private const val secondsPerDay: Long = 24 * 60 * 60

    fun formatted(remainingTimeSeconds: Double): String {
        val roundedUpSeconds = (ceil(remainingTimeSeconds / 60.0) * 60).toLong()
        return when {
            roundedUpSeconds == 0L -> "0m"
            roundedUpSeconds < 60L -> "<1m"
            roundedUpSeconds >= secondsPerDay -> {
                val days = roundedUpSeconds / secondsPerDay
                val hours = (roundedUpSeconds % secondsPerDay) / 3600
                "${days}d ${hours}h"
            }

            else -> {
                val hours = roundedUpSeconds / 3600
                val minutes = (roundedUpSeconds % 3600) / 60
                if (hours == 0L) "${minutes}m" else "${hours}h ${minutes}m"
            }
        }
    }

    fun shouldDisplay(remainingTimeSeconds: Double): Boolean {
        return remainingTimeSeconds < maxDisplayThresholdSeconds
    }

    fun isWarning(remainingTimeSeconds: Double): Boolean {
        return remainingTimeSeconds < warningThresholdSeconds
    }
}