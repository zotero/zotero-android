package org.zotero.android.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.zotero.android.database.objects.AnnotationsConfig
import org.zotero.android.screens.reader.data.ReaderAnnotationTool
import org.zotero.android.speech.data.HighlightResult
import org.zotero.android.speech.data.HighlightSession
import org.zotero.android.speech.data.HighlightVoiceInfo
import org.zotero.android.speech.data.RemoteVoice
import org.zotero.android.speech.data.TextRange
import java.lang.ref.WeakReference

interface SpeechHighlightSessionManagerDelegate<PageIndex> {
    fun highlightSessionSegments(pageIndex: PageIndex): List<SpeechDocumentParser.Segment>
    fun highlightSessionNextReadablePage(pageIndex: PageIndex): PageIndex?
    fun highlightSessionPreviousReadablePage(pageIndex: PageIndex): PageIndex?
}

class SpeechHighlightSessionManager<PageIndex : Any>(
    private val scope: CoroutineScope,
) {
    var session: HighlightSession<PageIndex>? = null
        private set(value) {
            field = value
            if (value != null) {
                startInactivityTimer()
            } else {
                stopInactivityTimer()
            }
        }

    private var inactivityJob: Job? = null
    var onSessionTimedOut: (() -> Unit)? = null

    val hasActiveSession: Boolean
        get() = session != null

    private var delegateReference = WeakReference<SpeechHighlightSessionManagerDelegate<PageIndex>>(null)
    var delegate: SpeechHighlightSessionManagerDelegate<PageIndex>?
        get() = delegateReference.get()
        set(value) {
            delegateReference = WeakReference(value)
        }

    var annotationTool: ReaderAnnotationTool = ReaderAnnotationTool.highlight
    var annotationColor: String = AnnotationsConfig.defaultActiveColor

    fun startSession(
        voiceInfo: HighlightVoiceInfo,
        position: Int,
        segments: List<SpeechDocumentParser.Segment>,
        pageIndex: PageIndex,
    ): HighlightResult<PageIndex>? {
        val clampedPosition = position.coerceIn(0, maxOf(pageTextLength(segments) - 1, 0))

        val granularity: RemoteVoice.Granularity
        val shouldGoBack: Boolean
        when (voiceInfo) {
            is HighlightVoiceInfo.Remote -> {
                granularity = voiceInfo.granularity
                shouldGoBack = voiceInfo.audioProgress < 0.5f && voiceInfo.elapsedTime < 3.0
            }

            HighlightVoiceInfo.Local -> {
                granularity = RemoteVoice.Granularity.sentence
                val sentence = SpeechDocumentParser.unitRange(clampedPosition, granularity, segments)
                shouldGoBack = if (sentence != null && sentence.length > 0) {
                    val positionInSentence = maxOf(0, clampedPosition - sentence.location)
                    positionInSentence.toFloat() / sentence.length.toFloat() < 0.5f
                } else {
                    false
                }
            }
        }

        val currentUnit = SpeechDocumentParser.unitRange(clampedPosition, granularity, segments) ?: return null

        if (shouldGoBack) {
            SpeechDocumentParser.previousUnitRange(currentUnit.location, granularity, segments)?.let { previous ->
                return setSession(listOf(previous), 0, granularity, segments, pageIndex)
            }
            val previousPage = delegate?.highlightSessionPreviousReadablePage(pageIndex)
            if (previousPage != null) {
                val previousSegments = delegate?.highlightSessionSegments(previousPage).orEmpty()
                SpeechDocumentParser.lastUnitRange(granularity, previousSegments)?.let { last ->
                    return setSession(listOf(last), 0, granularity, previousSegments, previousPage)
                }
            }
        }

        return setSession(listOf(currentUnit), 0, granularity, segments, pageIndex)
    }

    fun moveForward(): HighlightResult<PageIndex>? {
        val session = session ?: return null

        if (session.anchorIndex + 1 < session.unitRanges.size) {
            return setSession(
                listOf(session.unitRanges[session.anchorIndex + 1]), 0, session.granularity, session.segments, session.pageIndex
            )
        }

        val anchorRange = session.unitRanges[session.anchorIndex]
        SpeechDocumentParser.nextUnitRange(anchorRange, session.granularity, session.segments)?.let { next ->
            return setSession(listOf(next), 0, session.granularity, session.segments, session.pageIndex)
        }

        val nextPage = delegate?.highlightSessionNextReadablePage(session.pageIndex)
        if (nextPage != null) {
            val nextSegments = delegate?.highlightSessionSegments(nextPage).orEmpty()
            SpeechDocumentParser.firstUnitRange(session.granularity, nextSegments)?.let { first ->
                return setSession(listOf(first), 0, session.granularity, nextSegments, nextPage)
            }
        }

        return null
    }

    fun moveBackward(): HighlightResult<PageIndex>? {
        val session = session ?: return null

        if (session.anchorIndex - 1 >= 0) {
            return setSession(
                listOf(session.unitRanges[session.anchorIndex - 1]), 0, session.granularity, session.segments, session.pageIndex
            )
        }

        val anchorRange = session.unitRanges[session.anchorIndex]
        SpeechDocumentParser.previousUnitRange(anchorRange.location, session.granularity, session.segments)?.let { previous ->
            return setSession(listOf(previous), 0, session.granularity, session.segments, session.pageIndex)
        }

        val previousPage = delegate?.highlightSessionPreviousReadablePage(session.pageIndex)
        if (previousPage != null) {
            val previousSegments = delegate?.highlightSessionSegments(previousPage).orEmpty()
            SpeechDocumentParser.lastUnitRange(session.granularity, previousSegments)?.let { last ->
                return setSession(listOf(last), 0, session.granularity, previousSegments, previousPage)
            }
        }

        return null
    }

    fun extendForward(): HighlightResult<PageIndex>? {
        val session = session ?: return null

        if (session.anchorIndex > 0) {
            return setSession(
                session.unitRanges.drop(1), session.anchorIndex - 1, session.granularity, session.segments, session.pageIndex
            )
        }

        val next = SpeechDocumentParser.nextUnitRange(session.unitRanges.last(), session.granularity, session.segments) ?: return null
        return setSession(
            session.unitRanges + next, session.anchorIndex, session.granularity, session.segments, session.pageIndex
        )
    }

    fun extendBackward(): HighlightResult<PageIndex>? {
        val session = session ?: return null

        if (session.anchorIndex < session.unitRanges.size - 1) {
            return setSession(
                session.unitRanges.dropLast(1), session.anchorIndex, session.granularity, session.segments, session.pageIndex
            )
        }

        val previous = SpeechDocumentParser.previousUnitRange(
            session.unitRanges.first().location, session.granularity, session.segments
        ) ?: return null
        return setSession(
            listOf(previous) + session.unitRanges, session.anchorIndex + 1, session.granularity, session.segments, session.pageIndex
        )
    }

    fun endSession(): HighlightResult<PageIndex>? {
        val session = session
        if (session == null) {
            this.session = null
            return null
        }
        val text = extractText(session.pageText, session.range)
        this.session = null
        return HighlightResult(text, session.pageIndex)
    }

    fun cancelSession() {
        session = null
    }

    fun startInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = scope.launch {
            delay(inactivityTimeoutMillis)
            onSessionTimedOut?.invoke()
        }
    }

    fun stopInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = null
    }

    fun currentText(): String? {
        val session = session ?: return null
        return extractText(session.pageText, session.range)
    }

    private fun setSession(
        unitRanges: List<TextRange>,
        anchorIndex: Int,
        granularity: RemoteVoice.Granularity,
        segments: List<SpeechDocumentParser.Segment>,
        pageIndex: PageIndex,
    ): HighlightResult<PageIndex>? {
        session = HighlightSession(unitRanges, anchorIndex, granularity, segments, pageIndex)
        return currentResult()
    }

    private fun currentResult(): HighlightResult<PageIndex>? {
        val session = session ?: return null
        val text = currentText() ?: return null
        return HighlightResult(text, session.pageIndex)
    }

    private fun extractText(text: String, range: TextRange): String {
        if (range.location < 0 || range.length < 0 || range.end > text.length) {
            return ""
        }
        return text.substring(range.location, range.end).trim()
    }

    private companion object {
        const val inactivityTimeoutMillis = 5000L
    }

    private fun pageTextLength(segments: List<SpeechDocumentParser.Segment>): Int {
        val last = segments.lastOrNull() ?: return 0
        return last.pageOffset + last.text.length
    }
}