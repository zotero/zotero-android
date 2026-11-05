package org.zotero.android.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.Before
import org.junit.Test
import org.zotero.android.speech.data.HighlightVoiceInfo
import org.zotero.android.speech.data.RemoteVoice

class SpeechHighlightSessionManagerTest {

    private class MockPageProvider : SpeechHighlightSessionManagerDelegate<Int> {
        var pages = mapOf<Int, String>()
        var nextPageMap = mapOf<Int, Int>()
        var previousPageMap = mapOf<Int, Int>()

        override fun highlightSessionSegments(pageIndex: Int): List<SpeechDocumentParser.Segment> {
            val text = pages[pageIndex] ?: return emptyList()
            return segments(text)
        }

        override fun highlightSessionNextReadablePage(pageIndex: Int): Int? {
            val next = nextPageMap[pageIndex] ?: return null
            return next.takeIf { pages.containsKey(it) }
        }

        override fun highlightSessionPreviousReadablePage(pageIndex: Int): Int? {
            val previous = previousPageMap[pageIndex] ?: return null
            return previous.takeIf { pages.containsKey(it) }
        }
    }

    private val noGoBack = HighlightVoiceInfo.Remote(RemoteVoice.Granularity.sentence, audioProgress = 0.9f, elapsedTime = 10.0)
    private val goBack = HighlightVoiceInfo.Remote(RemoteVoice.Granularity.sentence, audioProgress = 0.1f, elapsedTime = 1.0)

    private lateinit var delegate: MockPageProvider
    private lateinit var sut: SpeechHighlightSessionManager<Int>

    @Before
    fun setUp() {
        delegate = MockPageProvider()
        sut = SpeechHighlightSessionManager(CoroutineScope(SupervisorJob()))
        sut.delegate = delegate
    }

    @Test
    fun `startSession starts a session at the current sentence`() {
        val result = start(noGoBack, 16, "First sentence. Second sentence. Third sentence.").shouldNotBeNull()

        result.text shouldBeEqualTo "Second sentence."
        result.pageIndex shouldBeEqualTo 0
        sut.hasActiveSession.shouldBeTrue()
        sut.session?.unitRanges?.size shouldBeEqualTo 1
        sut.session?.pageIndex shouldBeEqualTo 0
    }

    @Test
    fun `startSession goes back to previous sentence with low audio progress`() {
        start(goBack, 16, "First sentence. Second sentence. Third sentence.")?.text shouldBeEqualTo "First sentence."
    }

    @Test
    fun `startSession stays on current sentence when go back but no previous exists`() {
        start(goBack, 0, "First sentence. Second sentence.")?.text shouldBeEqualTo "First sentence."
    }

    @Test
    fun `startSession goes back to previous page when at start of current page`() {
        delegate.pages = mapOf(0 to "Last sentence on page 1.")
        delegate.previousPageMap = mapOf(1 to 0)

        val result = start(goBack, 0, "First sentence on page 2.", pageIndex = 1).shouldNotBeNull()

        result.text shouldBeEqualTo "Last sentence on page 1."
        result.pageIndex shouldBeEqualTo 0
        sut.session?.pageIndex shouldBeEqualTo 0
    }

    @Test
    fun `startSession starts a session with paragraph granularity`() {
        val paragraphNoGoBack = HighlightVoiceInfo.Remote(RemoteVoice.Granularity.paragraph, 0.9f, 10.0)

        start(paragraphNoGoBack, 20, "First paragraph.\n\nSecond paragraph.\n\nThird paragraph.")?.text shouldBeEqualTo "Second paragraph."
    }

    @Test
    fun `startSession returns null for empty text`() {
        start(noGoBack, 0, "").shouldBeNull()
    }

    @Test
    fun `startSession with local voice goes back when position is at start of sentence`() {
        start(HighlightVoiceInfo.Local, 16, "First sentence. Second sentence. Third sentence.")?.text shouldBeEqualTo "First sentence."
    }

    @Test
    fun `startSession with local voice does not go back when position is past midpoint of sentence`() {
        start(HighlightVoiceInfo.Local, 26, "First sentence. Second sentence. Third sentence.")?.text shouldBeEqualTo "Second sentence."
    }

    @Test
    fun `moveForward returns null when no session is active`() {
        sut.moveForward().shouldBeNull()
    }

    @Test
    fun `moveForward moves to next sentence in text`() {
        start(noGoBack, 0, "First sentence. Second sentence. Third sentence.")

        val result = sut.moveForward().shouldNotBeNull()

        result.text shouldBeEqualTo "Second sentence."
        result.pageIndex shouldBeEqualTo 0
        sut.session?.unitRanges?.size shouldBeEqualTo 1
    }

    @Test
    fun `moveForward uses anchor plus one from expanded selection`() {
        start(noGoBack, 8, "S1. S2. S3. S4. S5.")
        sut.extendForward()
        sut.extendForward()
        sut.session?.unitRanges?.size shouldBeEqualTo 3

        sut.moveForward()?.text shouldBeEqualTo "S4."
        sut.session?.unitRanges?.size shouldBeEqualTo 1
    }

    @Test
    fun `moveForward crosses to next page when at end of current page`() {
        delegate.pages = mapOf(1 to "Next page sentence.")
        delegate.nextPageMap = mapOf(0 to 1)
        start(noGoBack, 0, "Only sentence.")

        val result = sut.moveForward().shouldNotBeNull()

        result.text shouldBeEqualTo "Next page sentence."
        result.pageIndex shouldBeEqualTo 1
        sut.session?.pageIndex shouldBeEqualTo 1
    }

    @Test
    fun `moveForward returns null at end of last page`() {
        start(noGoBack, 0, "Only sentence.")

        sut.moveForward().shouldBeNull()
    }

    @Test
    fun `moveBackward returns null when no session is active`() {
        sut.moveBackward().shouldBeNull()
    }

    @Test
    fun `moveBackward moves to previous sentence in text`() {
        start(noGoBack, 16, "First sentence. Second sentence. Third sentence.")

        val result = sut.moveBackward().shouldNotBeNull()

        result.text shouldBeEqualTo "First sentence."
        result.pageIndex shouldBeEqualTo 0
    }

    @Test
    fun `moveBackward uses anchor minus one from expanded selection`() {
        start(noGoBack, 8, "S1. S2. S3. S4. S5.")
        sut.extendBackward()
        sut.extendBackward()
        sut.session?.anchorIndex shouldBeEqualTo 2

        sut.moveBackward()?.text shouldBeEqualTo "S2."
        sut.session?.unitRanges?.size shouldBeEqualTo 1
    }

    @Test
    fun `moveBackward crosses to previous page when at start of current page`() {
        delegate.pages = mapOf(0 to "Previous page sentence.")
        delegate.previousPageMap = mapOf(1 to 0)
        start(noGoBack, 0, "Only sentence.", pageIndex = 1)

        val result = sut.moveBackward().shouldNotBeNull()

        result.text shouldBeEqualTo "Previous page sentence."
        result.pageIndex shouldBeEqualTo 0
        sut.session?.pageIndex shouldBeEqualTo 0
    }

    @Test
    fun `moveBackward returns null at start of first page`() {
        start(noGoBack, 0, "Only sentence.")

        sut.moveBackward().shouldBeNull()
    }

    @Test
    fun `extendForward appends next unit when not expanded backward`() {
        start(noGoBack, 0, "S1. S2. S3.")

        sut.extendForward()?.text shouldBeEqualTo "S1. S2."
        sut.session?.unitRanges?.size shouldBeEqualTo 2
    }

    @Test
    fun `extendForward shrinks from start when expanded backward`() {
        start(noGoBack, 4, "S1. S2. S3.")
        sut.extendBackward()

        sut.extendForward()?.text shouldBeEqualTo "S2."
        sut.session?.unitRanges?.size shouldBeEqualTo 1
    }

    @Test
    fun `extendForward does not cross to next page`() {
        delegate.pages = mapOf(1 to "Next page sentence.")
        delegate.nextPageMap = mapOf(0 to 1)
        start(noGoBack, 0, "Only sentence.")

        sut.extendForward().shouldBeNull()
    }

    @Test
    fun `extendBackward prepends previous unit when not expanded forward`() {
        start(noGoBack, 8, "S1. S2. S3.")

        sut.extendBackward()?.text shouldBeEqualTo "S2. S3."
        sut.session?.unitRanges?.size shouldBeEqualTo 2
    }

    @Test
    fun `extendBackward shrinks from end when expanded forward`() {
        start(noGoBack, 4, "S1. S2. S3.")
        sut.extendForward()

        sut.extendBackward()?.text shouldBeEqualTo "S2."
        sut.session?.unitRanges?.size shouldBeEqualTo 1
    }

    @Test
    fun `extendBackward does not cross to previous page`() {
        delegate.pages = mapOf(0 to "Previous page sentence.")
        delegate.previousPageMap = mapOf(1 to 0)
        start(noGoBack, 0, "Only sentence.", pageIndex = 1)

        sut.extendBackward().shouldBeNull()
    }

    @Test
    fun `endSession returns combined text with page index and clears session`() {
        start(noGoBack, 0, "S1. S2. S3.", pageIndex = 5)
        sut.extendForward()

        val result = sut.endSession().shouldNotBeNull()

        result.text shouldBeEqualTo "S1. S2."
        result.pageIndex shouldBeEqualTo 5
        sut.hasActiveSession.shouldBeFalse()
    }

    @Test
    fun `endSession returns null when no session is active`() {
        sut.endSession().shouldBeNull()
    }

    @Test
    fun `cancelSession clears session without returning text`() {
        start(noGoBack, 0, "S1. S2.")

        sut.cancelSession()

        sut.hasActiveSession.shouldBeFalse()
    }

    @Test
    fun `full S1 to S5 scenario`() {
        start(noGoBack, 8, "S1. S2. S3. S4. S5.")
        sut.currentText() shouldBeEqualTo "S3."

        sut.extendForward()
        sut.currentText() shouldBeEqualTo "S3. S4."

        sut.extendForward()
        sut.currentText() shouldBeEqualTo "S3. S4. S5."

        sut.moveForward()
        sut.currentText() shouldBeEqualTo "S4."
        sut.session?.unitRanges?.size shouldBeEqualTo 1
    }

    @Test
    fun `moveBackward after extending forward lands on the unit before the anchor`() {
        start(noGoBack, 8, "S1. S2. S3. S4. S5.")
        sut.extendForward()
        sut.extendForward()

        sut.moveBackward()

        sut.currentText() shouldBeEqualTo "S2."
    }

    @Test
    fun `extend forward then backward returns to original selection`() {
        start(noGoBack, 4, "S1. S2. S3.")

        sut.extendForward()
        sut.currentText() shouldBeEqualTo "S2. S3."

        sut.extendBackward()
        sut.currentText() shouldBeEqualTo "S2."

        sut.extendBackward()
        sut.currentText() shouldBeEqualTo "S1. S2."
    }

    @Test
    fun `navigates across multiple pages with moveForward`() {
        delegate.pages = mapOf(0 to "Page 1 sentence.", 1 to "Page 2 sentence.", 2 to "Page 3 sentence.")
        delegate.nextPageMap = mapOf(0 to 1, 1 to 2)
        start(noGoBack, 0, "Page 1 sentence.")

        val second = sut.moveForward().shouldNotBeNull()
        second.text shouldBeEqualTo "Page 2 sentence."
        second.pageIndex shouldBeEqualTo 1

        val third = sut.moveForward().shouldNotBeNull()
        third.text shouldBeEqualTo "Page 3 sentence."
        third.pageIndex shouldBeEqualTo 2

        sut.moveForward().shouldBeNull()
    }

    @Test
    fun `navigates across multiple pages with moveBackward`() {
        delegate.pages = mapOf(0 to "Page 1 sentence.", 1 to "Page 2 sentence.")
        delegate.previousPageMap = mapOf(2 to 1, 1 to 0)
        start(noGoBack, 0, "Page 3 sentence.", pageIndex = 2)

        val second = sut.moveBackward().shouldNotBeNull()
        second.text shouldBeEqualTo "Page 2 sentence."
        second.pageIndex shouldBeEqualTo 1

        val first = sut.moveBackward().shouldNotBeNull()
        first.text shouldBeEqualTo "Page 1 sentence."
        first.pageIndex shouldBeEqualTo 0

        sut.moveBackward().shouldBeNull()
    }

    private fun start(voiceInfo: HighlightVoiceInfo, position: Int, pageText: String, pageIndex: Int = 0) =
        sut.startSession(voiceInfo, position, segments(pageText), pageIndex)

    private companion object {
        fun segments(pageText: String): List<SpeechDocumentParser.Segment> {
            if (pageText.isEmpty()) return emptyList()
            var offset = 0
            return pageText.split(SpeechDocumentParser.segmentSeparator).map { part ->
                SpeechDocumentParser.Segment(text = part, pageOffset = offset).also {
                    offset += part.length + SpeechDocumentParser.segmentSeparator.length
                }
            }
        }
    }
}