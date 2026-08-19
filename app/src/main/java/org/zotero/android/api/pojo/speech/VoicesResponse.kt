package org.zotero.android.api.pojo.speech

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
        )
    }
}
