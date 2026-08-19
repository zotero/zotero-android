package org.zotero.android.speech.data

data class RemoteVoice(
    val id: String,
    val label: String,
    val creditsPerMinute: Int,
    val granularity: Granularity,
    val sentenceDelay: Int,
    val tier: Tier,
) {
    enum class Granularity(val value: String) {
        sentence("sentence"),
        paragraph("paragraph");

        companion object {
            private val map = entries.associateBy(Granularity::value)

            fun from(value: String) = map[value]
        }
    }

    enum class Tier(val value: String) {
        standard("standard"),
        premium("premium");

        companion object {
            private val map = entries.associateBy(Tier::value)

            fun from(value: String) = map[value]
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is RemoteVoice && other.id == id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
