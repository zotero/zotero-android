package org.zotero.android.api.pojo.speech

import org.zotero.android.speech.data.RemoteVoice

data class VoicesResponse(
    val standard: List<Data>?,
    val premium: List<Data>?,
) {
    data class Data(
        val creditsPerMinute: Int,
        val sentenceDelay: Int,
        val segmentGranularity: String,
        val voices: Map<String, Voice>,
        val locales: Map<String, Locale>,
    ) {
        data class Voice(
            val label: String,
        )

        data class Locale(
            val default: List<String>?,
            val other: List<String>?,
        ) {
            val allVoiceIds: List<String>
                get() = default.orEmpty() + other.orEmpty()

            val firstVoiceId: String?
                get() = default?.firstOrNull() ?: other?.firstOrNull()
        }

        val sentenceGranularity: RemoteVoice.Granularity
            get() = RemoteVoice.Granularity.from(segmentGranularity) ?: RemoteVoice.Granularity.sentence

        fun makeVoice(id: String, tier: RemoteVoice.Tier): RemoteVoice {
            return RemoteVoice(
                id = id,
                label = voices[id]?.label ?: "",
                creditsPerMinute = creditsPerMinute,
                granularity = sentenceGranularity,
                sentenceDelay = sentenceDelay,
                tier = tier,
            )
        }

        fun firstVoice(tier: RemoteVoice.Tier): RemoteVoice? {
            val voiceId = locales.values.firstOrNull()?.firstVoiceId ?: return null
            return makeVoice(id = voiceId, tier = tier)
        }
    }

    fun tierData(tier: RemoteVoice.Tier): List<Data>? {
        return when (tier) {
            RemoteVoice.Tier.standard -> standard
            RemoteVoice.Tier.premium -> premium
        }
    }

    fun firstVoice(tier: RemoteVoice.Tier): RemoteVoice? {
        val data = tierData(tier)?.takeIf { it.isNotEmpty() } ?: return null
        return data.firstNotNullOfOrNull { it.firstVoice(tier) }
    }
}