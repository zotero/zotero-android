package org.zotero.android.speech

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test

class TextTokenizerTest {

    private val sut = TextTokenizer

    @Test
    fun `findSentence finds first sentence from start`() {
        val text = "First sentence. Second sentence."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "First sentence."
    }

    @Test
    fun `findSentence finds remaining string from current sentence`() {
        val text = "First sentence. Second sentence."
        val result = sut.findSentence(text, 3).shouldNotBeNull()
        result.text shouldBeEqualTo "st sentence."
    }

    @Test
    fun `findSentence finds second sentence after first`() {
        val text = "First sentence. Second sentence."
        val result = sut.findSentence(text, 16).shouldNotBeNull()
        result.text shouldBeEqualTo "Second sentence."
    }

    @Test
    fun `findSentence handles multiple sentences in a paragraph`() {
        val text = "One. Two. Three."

        val first = sut.findSentence(text, 0).shouldNotBeNull()
        first.text shouldBeEqualTo "One."

        val second = sut.findSentence(text, first.range.end).shouldNotBeNull()
        second.text shouldBeEqualTo "Two."

        val third = sut.findSentence(text, second.range.end).shouldNotBeNull()
        third.text shouldBeEqualTo "Three."
    }

    @Test
    fun `findSentence returns null when starting past end of text`() {
        val text = "First sentence."
        sut.findSentence(text, 100).shouldBeNull()
    }

    @Test
    fun `findSentence returns null for empty text`() {
        sut.findSentence("", 0).shouldBeNull()
    }

    @Test
    fun `findSentence returns null for whitespace-only text`() {
        sut.findSentence("   \n\n   ", 0).shouldBeNull()
    }

    @Test
    fun `findSentence handles text with leading whitespace`() {
        val text = "   \n\nFirst sentence."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "First sentence."
    }

    @Test
    fun `findSentence handles text without ending punctuation`() {
        val text = "Some text without punctuation"
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "Some text without punctuation"
    }

    @Test
    fun `findSentence splits sentence with footnote number after period`() {
        val text = "This is a sentence.5 Another sentence here."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "This is a sentence.5"
        result.range.length shouldBeEqualTo 21
    }

    @Test
    fun `findSentence does not split abbreviations like U-S`() {
        val text = "The U.S. government announced new policies."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo text
    }

    @Test
    fun `findSentence does not split short sentences`() {
        val text = "Short sentence."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "Short sentence."
    }

    @Test
    fun `findSentence splits very long sentence at natural break point`() {
        val longText = "word ".repeat(60) + "end.1 More text here."
        val result = sut.findSentence(longText, 0).shouldNotBeNull()
        result.text.endsWith("end.1").shouldBeTrue()
        result.text.length shouldBeLessOrEqualTo TextTokenizer.maxSentenceLength
    }

    @Test
    fun `findSentence splits at last space when no natural break point exists`() {
        val longText = "word ".repeat(100) + "ending"
        val result = sut.findSentence(longText, 0).shouldNotBeNull()
        result.text.length shouldBeLessOrEqualTo TextTokenizer.maxSentenceLength
        result.text.endsWith("word").shouldBeTrue()
    }

    @Test
    fun `findSentence iterates through text with footnotes correctly`() {
        val text = "First sentence.1 Second sentence.2 Third sentence."

        val first = sut.findSentence(text, 0).shouldNotBeNull()
        first.text shouldBeEqualTo "First sentence.1"

        val second = sut.findSentence(text, first.range.end).shouldNotBeNull()
        second.text shouldBeEqualTo "Second sentence.2"

        val third = sut.findSentence(text, second.range.end).shouldNotBeNull()
        third.text shouldBeEqualTo "Third sentence."
    }

    @Test
    fun `findSentence handles exclamation mark followed by digit`() {
        val text = "Amazing discovery!5 The research continues."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "Amazing discovery!5"
    }

    @Test
    fun `findSentence handles question mark followed by digit`() {
        val text = "Is this true?5 Yes it is."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "Is this true?5"
    }

    @Test
    fun `nextSentenceStart finds start of next sentence from end of current`() {
        val text = "First sentence. Second sentence."
        val result = sut.nextSentenceStart(text, 16).shouldNotBeNull()
        result shouldBeEqualTo 16
    }

    @Test
    fun `nextSentenceStart returns end of current sentence when starting in the middle`() {
        val text = "First sentence. Second sentence. Third."
        val result = sut.nextSentenceStart(text, 5).shouldNotBeNull()
        result shouldBeEqualTo 16
    }

    @Test
    fun `nextSentenceStart returns startIndex when at start of a sentence`() {
        val text = "First. Second. Third."
        val result = sut.nextSentenceStart(text, 7).shouldNotBeNull()
        result shouldBeEqualTo 7
    }

    @Test
    fun `nextSentenceStart returns null when starting past end of text`() {
        val text = "First sentence."
        sut.nextSentenceStart(text, 100).shouldBeNull()
    }

    @Test
    fun `nextSentenceStart returns null for empty text`() {
        sut.nextSentenceStart("", 0).shouldBeNull()
    }

    @Test
    fun `nextSentenceStart returns null for whitespace-only text`() {
        sut.nextSentenceStart("   \n\n   ", 0).shouldBeNull()
    }

    @Test
    fun `findSentenceContaining finds sentence containing index with footnotes`() {
        val text = "First sentence.5 Second sentence.2 Third sentence."
        val result = sut.findSentenceContaining(text, 17).shouldNotBeNull()
        result.text shouldContain "Second sentence"
    }

    @Test
    fun `findSentenceContaining finds first sentence containing index at start`() {
        val text = "First sentence.5 Second sentence."
        val result = sut.findSentenceContaining(text, 3).shouldNotBeNull()
        result.text shouldContain "First sentence"
    }

    @Test
    fun `nextSentenceStart finds next sentence index past footnote`() {
        val text = "First sentence.5 Second sentence."
        val result = sut.nextSentenceStart(text, 17).shouldNotBeNull()
        result shouldBeEqualTo 17
    }

    @Test
    fun `previousSentenceStart finds previous sentence start past footnote`() {
        val text = "First sentence.5 Second sentence.2 Third sentence."
        sut.previousSentenceStart(text, text.length).shouldNotBeNull()
    }

    @Test
    fun `previousSentenceStart finds previous sentence when footnote is followed by newline`() {
        val text = "Test sentence.5\nNext sentence"
        val result = sut.previousSentenceStart(text, 18).shouldNotBeNull()
        result shouldBeEqualTo 0
    }

    @Test
    fun `findSentence returns sentence text including footnote digit before newline`() {
        val text = "Test sentence.5\nNext sentence."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "Test sentence.5"
    }

    @Test
    fun `findSentence does not break decimal numbers`() {
        val text = "The value is 3.5 and it matters."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo text
    }

    @Test
    fun `findSentence handles multi-digit footnotes`() {
        val text = "Important claim.12 Next sentence."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "Important claim.12"
    }

    @Test
    fun `findSentence handles closing quote before footnote`() {
        // Unlike NLTokenizer, BreakIterator doesn't split sentences on period+closing-quote, so this stays whole.
        val text = "He said \"hello.\"5 Next sentence."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo text
    }

    @Test
    fun `findSentence handles closing parenthesis before footnote`() {
        val text = "Some claim (see above).5 More text here."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "Some claim (see above).5"
    }

    @Test
    fun `findSentence handles closing bracket before footnote`() {
        val text = "Reference [1].5 Another sentence."
        val result = sut.findSentence(text, 0).shouldNotBeNull()
        result.text shouldBeEqualTo "Reference [1].5"
    }

    @Test
    fun `previousSentenceStart finds previous sentence when in middle of current`() {
        val text = "First. Second. Third."
        val result = sut.previousSentenceStart(text, 17).shouldNotBeNull()
        result shouldBeEqualTo 7
    }

    @Test
    fun `previousSentenceStart finds previous sentence when at start of current`() {
        val text = "First. Second. Third."
        val result = sut.previousSentenceStart(text, 15).shouldNotBeNull()
        result shouldBeEqualTo 7
    }

    @Test
    fun `previousSentenceStart returns null when in first sentence`() {
        val text = "First. Second."
        sut.previousSentenceStart(text, 3).shouldBeNull()
    }
}