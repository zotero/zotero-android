package org.zotero.android.speech

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.zotero.android.architecture.coroutines.Dispatchers
import org.zotero.android.speech.data.RemoteVoice
import org.zotero.android.speech.data.SDTRect
import org.zotero.android.speech.data.TextRange
import org.zotero.android.speech.documentworker.DocumentWorkerWebCallChainExecutor
import timber.log.Timber
import java.lang.ref.WeakReference

class SpeechManager<Index : Any>(
    delegate: SpeechManagerDelegate<Index>,
    private val documentWorkerExecutor: DocumentWorkerWebCallChainExecutor,
    private val dispatchers: Dispatchers,
) {
    data class SpeechParagraph<Index>(
        val text: String,
        val page: Index,
        val pageOffset: Int,
        val rects: List<SDTRect>,
        val charRects: List<SDTRect?>,
    )

    private val delegateReference = WeakReference(delegate)
    private val delegate: SpeechManagerDelegate<Index>? get() = delegateReference.get()

    private val mutableState = MutableStateFlow<SpeechState>(SpeechState.Stopped)
    val state: StateFlow<SpeechState> = mutableState

    private val mutableExtractionProgress = MutableStateFlow<Double?>(null)
    val extractionProgress: StateFlow<Double?> = mutableExtractionProgress

    private data class Position(
        val paragraphIndex: Int,
        val range: TextRange,
        val highlightRange: TextRange,
        val highlightGranularity: RemoteVoice.Granularity,
    )

    data class ResumePosition<Index>(
        val page: Index,
        val paragraphIndex: Int,
        val offset: Int,
    )

    private enum class NavigationDirection { forward, backward }

    private val navigationMultiTapIntervalMillis = 300L
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    private var paragraphs: List<SpeechParagraph<Index>> = emptyList()
    private var paragraphIndicesByPage: Map<Index, List<Int>> = emptyMap()
    private var pageTextLength: Map<Index, Int> = emptyMap()
    private var documentLoaded = false
    private var documentLanguage: String? = null

    private lateinit var processor: VoiceProcessor
    private var position: Position? = null
        set(value) {
            field = value
            if (value == null || value.paragraphIndex >= paragraphs.size) return
            val paragraph = paragraphs[value.paragraphIndex]
            onSpeakingPositionChanged?.invoke(
                ResumePosition(page = paragraph.page, paragraphIndex = value.paragraphIndex, offset = value.range.location)
            )
        }
    private var currentSpeakingPage: Index? = null
    private var pendingNavigation: Pair<NavigationDirection, Job>? = null
    var onSpeakingPositionChanged: ((ResumePosition<Index>) -> Unit)? = null

    private val speechRange: TextRange?
        get() {
            val position = position ?: return null
            if (position.paragraphIndex >= paragraphs.size) return null
            val paragraph = paragraphs[position.paragraphIndex]
            return TextRange(paragraph.pageOffset + position.range.location, position.range.length)
        }

    private val highlightGranularity: RemoteVoice.Granularity
        get() = when (val voice = processor.speechVoice) {
            is SpeechVoice.Remote -> if (voice.voice.granularity == RemoteVoice.Granularity.paragraph) {
                RemoteVoice.Granularity.paragraph
            } else {
                RemoteVoice.Granularity.sentence
            }
            is SpeechVoice.Local, null -> RemoteVoice.Granularity.sentence
        }

    private fun findHighlightUnit(index: Int, text: String, granularity: RemoteVoice.Granularity): TextTokenizer.Match? {
        return when (granularity) {
            RemoteVoice.Granularity.paragraph -> TextTokenizer.findParagraphContaining(text, index)
            RemoteVoice.Granularity.sentence -> TextTokenizer.findSentenceContaining(text, index)
        }
    }

    val currentReadAloudHighlight: Pair<List<SDTRect>, Index>?
        get() {
            val position = position ?: return null
            if (position.paragraphIndex >= paragraphs.size) return null
            val paragraph = paragraphs[position.paragraphIndex]
            val range = position.highlightRange
            if (range.length <= 0) return null
            val rects = highlightRects(paragraph, range)
            if (rects.isEmpty()) return null
            return rects to paragraph.page
        }

    private fun highlightRects(paragraph: SpeechParagraph<Index>, highlightRange: TextRange): List<SDTRect> {
        val pageTextRange = TextRange(paragraph.pageOffset + highlightRange.location, highlightRange.length)
        return SpeechDocumentParser.pdfLineRects(pageTextRange, segments(paragraph.page))
    }

    private fun TextRange.contains(index: Int): Boolean = index >= location && index < end

    private fun segments(page: Index): List<SpeechDocumentParser.Segment> {
        return (paragraphIndicesByPage[page] ?: emptyList()).map {
            val paragraph = paragraphs[it]
            SpeechDocumentParser.Segment(text = paragraph.text, pageOffset = paragraph.pageOffset, charRects = paragraph.charRects)
        }
    }

    private fun pageText(page: Index): String {
        return (paragraphIndicesByPage[page] ?: emptyList())
            .joinToString(SpeechDocumentParser.segmentSeparator) { paragraphs[it].text }
    }

    private fun resolveParagraph(pageTextOffset: Int, page: Index): Pair<Int, Int>? {
        val indices = paragraphIndicesByPage[page] ?: emptyList()
        var chosen = indices.firstOrNull() ?: return null
        for (index in indices) {
            if (paragraphs[index].pageOffset <= pageTextOffset) {
                chosen = index
            } else {
                break
            }
        }
        val paragraph = paragraphs[chosen]
        return chosen to maxOf(0, minOf(pageTextOffset - paragraph.pageOffset, paragraph.text.length))
    }

    private fun firstReadablePage(atOrAfter: Index): Index? {
        if (paragraphIndicesByPage[atOrAfter]?.isNotEmpty() == true) return atOrAfter
        return nextReadablePage(atOrAfter)
    }

    private fun nextReadablePage(after: Index): Index? {
        val delegate = delegate ?: return null
        var current = after
        while (true) {
            val next = delegate.getNextPageIndex(current) ?: return null
            if (paragraphIndicesByPage[next]?.isNotEmpty() == true) return next
            current = next
        }
    }

    private fun previousReadablePage(before: Index): Index? {
        val delegate = delegate ?: return null
        var current = before
        while (true) {
            val previous = delegate.getPreviousPageIndex(current) ?: return null
            if (paragraphIndicesByPage[previous]?.isNotEmpty() == true) return previous
            current = previous
        }
    }

    fun stop() {
        processor.stop()
    }

    fun navigateForward() {
        coalesceNavigation(NavigationDirection.forward)
    }

    fun navigateBackward() {
        coalesceNavigation(NavigationDirection.backward)
    }

    private fun coalesceNavigation(direction: NavigationDirection) {
        pendingNavigation?.let { (pendingDirection, job) ->
            job.cancel()
            pendingNavigation = null
            if (pendingDirection == direction) {
                navigate(direction, RemoteVoice.Granularity.paragraph)
                return
            }
            navigate(pendingDirection, RemoteVoice.Granularity.sentence)
        }
        val job = scope.launch {
            delay(navigationMultiTapIntervalMillis)
            pendingNavigation = null
            navigate(direction, RemoteVoice.Granularity.sentence)
        }
        pendingNavigation = direction to job
    }

    private fun navigate(direction: NavigationDirection, unit: RemoteVoice.Granularity) {
        when (direction) {
            NavigationDirection.forward -> forward(unit)
            NavigationDirection.backward -> backward(unit)
        }
    }

    fun forward(unit: RemoteVoice.Granularity) {
        val position = position ?: return
        if (position.paragraphIndex >= paragraphs.size) return
        if (unit == RemoteVoice.Granularity.paragraph) {
            move(position.paragraphIndex + 1, 0)
            return
        }
        val paragraph = paragraphs[position.paragraphIndex]
        val currentEnd = position.range.end
        val next = TextTokenizer.nextSentenceStart(paragraph.text, currentEnd)
        if (next != null && next < paragraph.text.length) {
            move(position.paragraphIndex, next)
        } else {
            move(position.paragraphIndex + 1, 0)
        }
    }

    fun backward(unit: RemoteVoice.Granularity) {
        val position = position ?: return
        if (position.paragraphIndex >= paragraphs.size) return
        val paragraph = paragraphs[position.paragraphIndex]
        if (unit == RemoteVoice.Granularity.paragraph) {
            move(if (position.range.location > 0) position.paragraphIndex else position.paragraphIndex - 1, 0)
            return
        }
        val previous = TextTokenizer.previousSentenceStart(paragraph.text, position.range.location)
        if (previous != null) {
            move(position.paragraphIndex, previous)
        } else if (position.paragraphIndex > 0) {
            val previousParagraph = paragraphs[position.paragraphIndex - 1]
            val lastSentence = TextTokenizer.previousSentenceStart(previousParagraph.text, previousParagraph.text.length) ?: 0
            move(position.paragraphIndex - 1, lastSentence)
        } else if (position.range.location != 0) {
            move(position.paragraphIndex, 0)
        } else {
            stop()
        }
    }

    private fun move(paragraphIndex: Int, offset: Int) {
        if (paragraphIndex < 0 || paragraphIndex >= paragraphs.size) {
            stop()
            return
        }
        Timber.i("SpeechManager: move to paragraph $paragraphIndex, offset $offset")
        moveTo(paragraphIndex, offset)
        if (!state.value.isPaused) {
            startSpeaking(paragraphIndex, offset, reportPageChange = false)
        }
        delegate?.focusPage(paragraphs[paragraphIndex].page)
    }

    private fun startSpeaking(paragraphIndex: Int, offset: Int = 0, reportPageChange: Boolean) {
        if (paragraphIndex >= paragraphs.size) {
            stop()
            return
        }
        val paragraph = paragraphs[paragraphIndex]
        val previousPage = currentPage(position?.paragraphIndex)
        if (position?.paragraphIndex != paragraphIndex) {
            position = Position(
                paragraphIndex = paragraphIndex,
                range = TextRange(offset, 0),
                highlightRange = TextRange(0, 0),
                highlightGranularity = highlightGranularity,
            )
        }
        if (reportPageChange && previousPage != null && previousPage != paragraph.page) {
            delegate?.moved(paragraph.page, previousPage)
        }
        currentSpeakingPage = paragraph.page
        processor.speak(segments(paragraph.page), paragraph.pageOffset + offset)
    }

    private fun moveTo(paragraphIndex: Int, offset: Int) {
        if (paragraphIndex >= paragraphs.size) return
        val paragraph = paragraphs[paragraphIndex]
        val sentence = TextTokenizer.findSentence(paragraph.text, offset) ?: return

        val previousPage = currentPage(position?.paragraphIndex)
        val pageDidChange = previousPage != paragraph.page
        val granularity = highlightGranularity

        val currentPosition = position
        val isInCurrentHighlight = currentPosition != null &&
                currentPosition.paragraphIndex == paragraphIndex &&
                currentPosition.highlightRange.length > 0 &&
                currentPosition.highlightGranularity == granularity &&
                currentPosition.highlightRange.contains(offset)

        val newHighlightRange: TextRange
        val highlightText: String?
        if (isInCurrentHighlight && currentPosition != null) {
            newHighlightRange = currentPosition.highlightRange
            highlightText = null
        } else {
            val unit = findHighlightUnit(offset, paragraph.text, granularity)
            newHighlightRange = unit?.range ?: sentence.range
            highlightText = unit?.text ?: sentence.text
        }

        position = Position(paragraphIndex, sentence.range, newHighlightRange, granularity)
        processor.invalidateCurrentPlayback()

        if (highlightText != null) {
            delegate?.readAloudHighlightChanged(
                text = highlightText,
                rects = highlightRects(paragraph, newHighlightRange),
                pageIndex = paragraph.page,
                sourceLocation = paragraph.pageOffset + newHighlightRange.location,
                sourceTextLength = pageTextLength[paragraph.page] ?: paragraph.text.length,
            )
        }

        if (pageDidChange && previousPage != null) {
            delegate?.moved(paragraph.page, previousPage)
        }
    }

    private fun currentPage(paragraphIndex: Int?): Index? {
        if (paragraphIndex == null || paragraphIndex >= paragraphs.size) return null
        return paragraphs[paragraphIndex].page
    }

    private suspend fun loadDocumentIfNeeded(): Boolean {
        if (documentLoaded) return true
        val delegate = delegate
        val file = delegate?.documentFile
        val contentType = delegate?.documentContentType
        if (delegate == null || file == null || contentType == null) {
            Timber.e("SpeechManager: can't get document file")
            return false
        }
        val startTime = System.currentTimeMillis()
        mutableExtractionProgress.value = null
        val pack = try {
            documentWorkerExecutor.extractStructuredDocumentText(file, contentType, delegate.documentPassword)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "SpeechManager: structured document text extraction failed")
            return false
        }
        val parsed = withContext(dispatchers.default) {
            try {
                SpeechDocumentParser.parse(pack.materialize())
            } catch (e: Exception) {
                Timber.e(e, "SpeechManager: could not parse structured document text")
                SpeechDocumentParser.ParsedDocument(paragraphs = emptyList(), language = null)
            }
        }
        store(parsed)
        documentLoaded = true
        Timber.i("SpeechManager: extracted ${paragraphs.size} paragraph(s) in ${System.currentTimeMillis() - startTime}ms")
        return true
    }

    private fun store(parsed: SpeechDocumentParser.ParsedDocument) {
        val delegate = delegate ?: return
        val newParagraphs = mutableListOf<SpeechParagraph<Index>>()
        val indicesByPage = mutableMapOf<Index, MutableList<Int>>()
        val lengths = mutableMapOf<Index, Int>()
        for (paragraph in parsed.paragraphs) {
            val page = delegate.pageIndex(paragraph.page) ?: continue
            val index = newParagraphs.size
            newParagraphs.add(
                SpeechParagraph(
                    text = paragraph.text,
                    page = page,
                    pageOffset = paragraph.pageOffset,
                    rects = paragraph.rects,
                    charRects = paragraph.charRects,
                )
            )
            indicesByPage.getOrPut(page) { mutableListOf() }.add(index)
            lengths[page] = maxOf(lengths[page] ?: 0, paragraph.pageOffset + paragraph.text.length)
        }
        paragraphs = newParagraphs
        paragraphIndicesByPage = indicesByPage
        pageTextLength = lengths
        documentLanguage = parsed.language
    }
}