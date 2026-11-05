package org.zotero.android.speech.data

import org.zotero.android.speech.SpeechDocumentParser

sealed class HighlightVoiceInfo {
    data class Remote(
        val granularity: RemoteVoice.Granularity,
        val audioProgress: Float,
        val elapsedTime: Double,
    ) : HighlightVoiceInfo()

    data object Local : HighlightVoiceInfo()
}

data class HighlightResult<PageIndex>(
    val text: String,
    val pageIndex: PageIndex,
)

data class HighlightSession<PageIndex>(
    val unitRanges: List<TextRange>,
    val anchorIndex: Int,
    val granularity: RemoteVoice.Granularity,
    val segments: List<SpeechDocumentParser.Segment>,
    val pageIndex: PageIndex,
) {
    val range: TextRange
        get() {
            val first = unitRanges.firstOrNull() ?: return TextRange(0, 0)
            val last = unitRanges.last()
            return TextRange(first.location, last.end - first.location)
        }

    val pageText: String
        get() = segments.joinToString(SpeechDocumentParser.segmentSeparator) { it.text }
}