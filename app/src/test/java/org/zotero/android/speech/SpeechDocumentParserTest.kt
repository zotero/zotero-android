package org.zotero.android.speech

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test
import org.zotero.android.speech.SpeechDocumentParser.Segment
import org.zotero.android.speech.data.RemoteVoice
import org.zotero.android.speech.data.SDTRect
import org.zotero.android.speech.data.TextRange

class SpeechDocumentParserTest {

    private val sut = SpeechDocumentParser

    private val text = "One. Two.\n\nThree. Four."
    private val segments = listOf(
        Segment(text = "One. Two.", pageOffset = 0),
        Segment(text = "Three. Four.", pageOffset = 11),
    )

    @Test
    fun `parse reads every block type but skips blocks with an excluded flowClass`() {
        val materialized = document(
            paragraphBlock(page = 0, runs = listOf(makeRun("First sentence. Second.", page = 0))),
            block(type = "heading", page = 0, runs = listOf(makeRun("A Heading", page = 0))),
            block(type = "image", page = 0, runs = listOf(makeRun("caption", page = 0))),
            paragraphBlock(page = 0, flowClass = "excluded", runs = listOf(makeRun("42", page = 0))),
            paragraphBlock(page = 0, flowClass = "auxiliary", runs = listOf(makeRun("Running header", page = 0))),
            paragraphBlock(page = 0, runs = listOf(makeRun("Third.", page = 0))),
        )

        val paragraphs = sut.parse(materialized).paragraphs

        paragraphs.map { it.text } shouldBeEqualTo listOf("First sentence. Second.", "A Heading", "caption", "Third.")
        paragraphs.all { it.page == 0 }.shouldBeTrue()
    }

    @Test
    fun `parse reads list blocks`() {
        val materialized = document(
            block(
                type = "list", page = 0, runs = listOf(
                    contentWrapper(makeRun("Item one. ", page = 0)),
                    contentWrapper(makeRun("Item two.", page = 0)),
                )
            )
        )

        val paragraphs = sut.parse(materialized).paragraphs

        paragraphs.size shouldBeEqualTo 1
        paragraphs.first().text.contains("Item one.").shouldBeTrue()
        paragraphs.first().text.contains("Item two.").shouldBeTrue()
    }

    @Test
    fun `parse skips blocks with excluded flowClass`() {
        val materialized = document(
            paragraphBlock(page = 0, flowClass = "excluded", runs = listOf(makeRun("1", page = 0))),
            paragraphBlock(page = 0, runs = listOf(makeRun("Real content.", page = 0))),
        )

        sut.parse(materialized).paragraphs.map { it.text } shouldBeEqualTo listOf("Real content.")
    }

    @Test
    fun `parse assigns page offsets so paragraphs on a page are separated by a blank line`() {
        val materialized = document(
            paragraphBlock(page = 0, runs = listOf(makeRun("Alpha.", page = 0))),
            paragraphBlock(page = 0, runs = listOf(makeRun("Beta.", page = 0))),
        )

        val paragraphs = sut.parse(materialized).paragraphs

        paragraphs.size shouldBeEqualTo 2
        paragraphs[0].pageOffset shouldBeEqualTo 0
        paragraphs[1].pageOffset shouldBeEqualTo 8
    }

    @Test
    fun `parse assigns paragraphs to their page from the run textMap`() {
        val materialized = document(
            paragraphBlock(page = 3, runs = listOf(makeRun("On page three.", page = 3)))
        )

        val paragraphs = sut.parse(materialized).paragraphs

        paragraphs.size shouldBeEqualTo 1
        paragraphs.first().page shouldBeEqualTo 3
        paragraphs.first().pageOffset shouldBeEqualTo 0
    }

    @Test
    fun `parse splits a paragraph that spans two pages into one paragraph per page`() {
        val materialized = document(
            paragraphBlock(
                page = 0, runs = listOf(
                    makeRun("Starts on page zero. ", page = 0),
                    makeRun("Continues on page one.", page = 1),
                )
            )
        )

        val paragraphs = sut.parse(materialized).paragraphs

        paragraphs.size shouldBeEqualTo 2
        paragraphs[0].page shouldBeEqualTo 0
        paragraphs[0].text shouldBeEqualTo "Starts on page zero."
        paragraphs[0].pageOffset shouldBeEqualTo 0
        paragraphs[1].page shouldBeEqualTo 1
        paragraphs[1].text shouldBeEqualTo "Continues on page one."
        paragraphs[1].pageOffset shouldBeEqualTo 0
    }

    @Test
    fun `parse assigns a run without a textMap to the surrounding page`() {
        val materialized = document(
            paragraphBlock(
                page = 2, runs = listOf(
                    makeRun("Alpha", page = 2),
                    runWithoutTextMap(" "),
                    makeRun("Beta", page = 2),
                )
            )
        )

        val paragraphs = sut.parse(materialized).paragraphs

        paragraphs.size shouldBeEqualTo 1
        paragraphs.first().page shouldBeEqualTo 2
        paragraphs.first().text shouldBeEqualTo "Alpha Beta"
    }

    @Test
    fun `parse returns no paragraphs for empty or missing content`() {
        sut.parse(JsonObject().apply { add("content", JsonArray()) }).paragraphs.shouldBeEmpty()
        sut.parse(JsonObject()).paragraphs.shouldBeEmpty()
    }

    @Test
    fun `citation elision removes a bracketed numeric citation that links to a reference`() {
        val materialized = document(
            paragraphBlock(
                page = 0, runs = listOf(
                    makeRun("This matters ", page = 0),
                    makeRun("[2]", page = 0, hasRefs = true),
                    makeRun(".", page = 0),
                )
            )
        )

        sut.parse(materialized).paragraphs.map { it.text } shouldBeEqualTo listOf("This matters .")
    }

    @Test
    fun `citation elision removes a parenthetical author-year citation that links to a reference`() {
        val materialized = document(
            paragraphBlock(
                page = 0, runs = listOf(
                    makeRun("As shown ", page = 0),
                    makeRun("(Smith 2026)", page = 0, hasRefs = true),
                    makeRun(" it works.", page = 0),
                )
            )
        )

        sut.parse(materialized).paragraphs.map { it.text } shouldBeEqualTo listOf("As shown  it works.")
    }

    @Test
    fun `citation elision keeps a bracketed aside that is not a reference link`() {
        val materialized = document(
            paragraphBlock(page = 0, runs = listOf(makeRun("This is verbatim [sic] text.", page = 0)))
        )

        sut.parse(materialized).paragraphs.map { it.text } shouldBeEqualTo listOf("This is verbatim [sic] text.")
    }

    @Test
    fun `citation elision keeps a parenthetical group whose linked coverage is below the threshold`() {
        val materialized = document(
            paragraphBlock(
                page = 0, runs = listOf(
                    makeRun("Note ", page = 0),
                    makeRun("(see the discussion around ", page = 0),
                    makeRun("2", page = 0, hasRefs = true),
                    makeRun(" for context)", page = 0),
                )
            )
        )

        sut.parse(materialized).paragraphs.map { it.text } shouldBeEqualTo
                listOf("Note (see the discussion around 2 for context)")
    }

    @Test
    fun `citation elision removes a superscript reference marker`() {
        val materialized = document(
            paragraphBlock(
                page = 0, runs = listOf(
                    makeRun("Some claim", page = 0),
                    makeRun("3", page = 0, hasRefs = true, sup = true),
                    makeRun(" continues.", page = 0),
                )
            )
        )

        sut.parse(materialized).paragraphs.map { it.text } shouldBeEqualTo listOf("Some claim continues.")
    }

    @Test
    fun `citation elision keeps a superscript marker that does not link to a reference`() {
        val materialized = document(
            paragraphBlock(
                page = 0, runs = listOf(
                    makeRun("E = mc", page = 0),
                    makeRun("2", page = 0, sup = true),
                )
            )
        )

        sut.parse(materialized).paragraphs.map { it.text } shouldBeEqualTo listOf("E = mc2")
    }

    @Test
    fun `citation elision keeps an inline reference link that is neither bracketed nor superscript`() {
        val materialized = document(
            paragraphBlock(
                page = 0, runs = listOf(
                    makeRun("See ", page = 0),
                    makeRun("Smith and Jones", page = 0, hasRefs = true),
                    makeRun(" for more.", page = 0),
                )
            )
        )

        sut.parse(materialized).paragraphs.map { it.text } shouldBeEqualTo listOf("See Smith and Jones for more.")
    }

    @Test
    fun `charRects aligns a per-character rect with each character of the readable text`() {
        val materialized = document(
            paragraphBlock(page = 0, runs = listOf(glyphRun("Hello", page = 0, x = 0.0, charWidth = 5.0, top = 0.0, height = 10.0)))
        )

        val paragraph = sut.parse(materialized).paragraphs.first()

        paragraph.text shouldBeEqualTo "Hello"
        paragraph.charRects.size shouldBeEqualTo 5
        paragraph.charRects[0] shouldBeEqualTo rect(0.0, 0.0, 5.0, 10.0)
        paragraph.charRects[4] shouldBeEqualTo rect(20.0, 0.0, 5.0, 10.0)
    }

    @Test
    fun `charRects assigns null to whitespace characters`() {
        val materialized = document(
            paragraphBlock(page = 0, runs = listOf(glyphRun("a b", page = 0, x = 0.0, charWidth = 5.0)))
        )

        val paragraph = sut.parse(materialized).paragraphs.first()

        paragraph.text shouldBeEqualTo "a b"
        paragraph.charRects[0] shouldBeEqualTo rect(0.0, 0.0, 5.0, 10.0)
        paragraph.charRects[1].shouldBeNull()
        paragraph.charRects[2] shouldBeEqualTo rect(5.0, 0.0, 5.0, 10.0)
    }

    @Test
    fun `charRects drops a citation's geometry along with its text`() {
        val materialized = document(
            paragraphBlock(
                page = 0, runs = listOf(
                    glyphRun("Ref ", page = 0, x = 0.0, charWidth = 5.0),
                    glyphRun("[2]", page = 0, x = 100.0, charWidth = 5.0, hasRefs = true),
                    glyphRun(" end", page = 0, x = 200.0, charWidth = 5.0),
                )
            )
        )

        val paragraph = sut.parse(materialized).paragraphs.first()

        paragraph.text shouldBeEqualTo "Ref  end"
        paragraph.charRects.size shouldBeEqualTo 8
        paragraph.charRects.mapNotNull { it?.x } shouldBeEqualTo listOf(0.0, 5.0, 10.0, 200.0, 205.0, 210.0)
    }

    private val oneLine = Segment(
        text = "abcd",
        pageOffset = 0,
        charRects = listOf(rect(0.0, 0.0, 5.0, 10.0), rect(5.0, 0.0, 5.0, 10.0), rect(10.0, 0.0, 5.0, 10.0), rect(15.0, 0.0, 5.0, 10.0)),
    )

    @Test
    fun `pdfLineRects merges rects on the same line into one`() {
        sut.pdfLineRects(TextRange(0, 4), listOf(oneLine)) shouldBeEqualTo listOf(rect(0.0, 0.0, 20.0, 10.0))
    }

    @Test
    fun `pdfLineRects covers only the requested sub-range`() {
        sut.pdfLineRects(TextRange(1, 2), listOf(oneLine)) shouldBeEqualTo listOf(rect(5.0, 0.0, 10.0, 10.0))
    }

    @Test
    fun `pdfLineRects splits rects that fall on different lines`() {
        val twoLines = Segment(
            text = "abcd",
            pageOffset = 0,
            charRects = listOf(rect(0.0, 0.0, 5.0, 10.0), rect(5.0, 0.0, 5.0, 10.0), rect(0.0, 20.0, 5.0, 10.0), rect(5.0, 20.0, 5.0, 10.0)),
        )

        sut.pdfLineRects(TextRange(0, 4), listOf(twoLines)) shouldBeEqualTo
                listOf(rect(0.0, 0.0, 10.0, 10.0), rect(0.0, 20.0, 10.0, 10.0))
    }

    @Test
    fun `pdfLineRects spans a range across two segments and skips the separator gap`() {
        val first = Segment("ab", 0, listOf(rect(0.0, 0.0, 5.0, 10.0), rect(5.0, 0.0, 5.0, 10.0)))
        val second = Segment("cd", 4, listOf(rect(0.0, 20.0, 5.0, 10.0), rect(5.0, 20.0, 5.0, 10.0)))

        sut.pdfLineRects(TextRange(0, 6), listOf(first, second)) shouldBeEqualTo
                listOf(rect(0.0, 0.0, 10.0, 10.0), rect(0.0, 20.0, 10.0, 10.0))
    }

    @Test
    fun `pdfLineRects returns no rects for an empty range`() {
        sut.pdfLineRects(TextRange(0, 0), listOf(oneLine)).shouldBeEmpty()
    }

    @Test
    fun `language reads the lowercase language key`() {
        sut.parse(metadata("language", "en-GB")).language shouldBeEqualTo "en-GB"
    }

    @Test
    fun `language reads the capitalized Language key`() {
        sut.language(metadata("Language", "de-DE")) shouldBeEqualTo "de-DE"
    }

    @Test
    fun `language returns null when the language is absent`() {
        sut.language(metadata("title", "Something")).shouldBeNull()
    }

    @Test
    fun `language returns null when metadata is missing`() {
        sut.language(JsonObject().apply { add("content", JsonArray()) }).shouldBeNull()
    }

    @Test
    fun `language returns null for an empty or whitespace language`() {
        sut.language(metadata("language", "")).shouldBeNull()
        sut.language(metadata("language", "   ")).shouldBeNull()
    }

    @Test
    fun `paragraphRange returns from the index to the end of the containing segment`() {
        sut.paragraphRange(0, segments) shouldBeEqualTo TextRange(0, 9)
        sut.paragraphRange(5, segments) shouldBeEqualTo TextRange(5, 4)
    }

    @Test
    fun `paragraphRange returns the next whole segment when the index is in the gap between segments`() {
        sut.paragraphRange(9, segments) shouldBeEqualTo TextRange(11, 12)
    }

    @Test
    fun `paragraphRange returns null past the last segment`() {
        sut.paragraphRange(23, segments).shouldBeNull()
        sut.paragraphRange(100, segments).shouldBeNull()
    }

    @Test
    fun `sentenceRange returns the sentence starting at the index within its segment`() {
        trimmed(text, sut.sentenceRange(0, segments)) shouldBeEqualTo "One."
        trimmed(text, sut.sentenceRange(5, segments)) shouldBeEqualTo "Two."
    }

    @Test
    fun `sentenceRange crosses into the next segment when the index is in the gap`() {
        trimmed(text, sut.sentenceRange(9, segments)) shouldBeEqualTo "Three."
    }

    @Test
    fun `sentenceRange never lets a sentence span a paragraph boundary`() {
        val mergeText = "Alpha beta\n\nGamma delta."
        val mergeSegments = listOf(Segment("Alpha beta", 0), Segment("Gamma delta.", 12))

        trimmed(mergeText, sut.sentenceRange(0, mergeSegments)) shouldBeEqualTo "Alpha beta"
    }

    @Test
    fun `sentenceRange returns null past the last segment`() {
        sut.sentenceRange(100, segments).shouldBeNull()
    }

    @Test
    fun `nextSentenceStart advances to the next sentence within the segment`() {
        val start = sut.nextSentenceStart(4, segments).shouldNotBeNull()
        trimmed(text, sut.sentenceRange(start, segments)) shouldBeEqualTo "Two."
    }

    @Test
    fun `nextSentenceStart advances past the sentence containing a mid-sentence index`() {
        val start = sut.nextSentenceStart(2, segments).shouldNotBeNull()
        trimmed(text, sut.sentenceRange(start, segments)) shouldBeEqualTo "Two."
    }

    @Test
    fun `nextSentenceStart rolls into the first sentence of the next segment`() {
        val start = sut.nextSentenceStart(9, segments).shouldNotBeNull()
        trimmed(text, sut.sentenceRange(start, segments)) shouldBeEqualTo "Three."
    }

    @Test
    fun `nextSentenceStart returns null past the last sentence`() {
        sut.nextSentenceStart(23, segments).shouldBeNull()
    }

    @Test
    fun `previousSentenceStart returns the previous sentence within the segment`() {
        val start = sut.previousSentenceStart(5, segments).shouldNotBeNull()
        trimmed(text, sut.sentenceRange(start, segments)) shouldBeEqualTo "One."
    }

    @Test
    fun `previousSentenceStart rolls back into the previous segment's last sentence`() {
        val start = sut.previousSentenceStart(11, segments).shouldNotBeNull()
        trimmed(text, sut.sentenceRange(start, segments)) shouldBeEqualTo "Two."
    }

    @Test
    fun `previousSentenceStart returns null at the first sentence of the first segment`() {
        sut.previousSentenceStart(0, segments).shouldBeNull()
        sut.previousSentenceStart(2, segments).shouldBeNull()
    }

    @Test
    fun `lastSentenceStart returns the last sentence of the last segment`() {
        val start = sut.lastSentenceStart(segments).shouldNotBeNull()
        trimmed(text, sut.sentenceRange(start, segments)) shouldBeEqualTo "Four."
    }

    @Test
    fun `lastSentenceStart returns null when there are no segments`() {
        sut.lastSentenceStart(emptyList()).shouldBeNull()
    }

    @Test
    fun `unitRange with paragraph granularity covers the whole containing segment`() {
        sut.unitRange(5, RemoteVoice.Granularity.paragraph, segments) shouldBeEqualTo TextRange(0, 9)
    }

    private fun trimmed(text: String, range: TextRange?): String? {
        range ?: return null
        return text.substring(range.location, range.end).trim()
    }

    private fun metadata(key: String, value: String): JsonObject {
        val properties = JsonObject().apply { addProperty(key, value) }
        val source = JsonObject().apply { add("properties", properties) }
        val metadata = JsonObject().apply { add("source", source) }
        return JsonObject().apply { add("metadata", metadata) }
    }

    private fun makeRun(text: String, page: Int, hasRefs: Boolean = false, sup: Boolean = false): JsonObject =
        buildRun(text, "[[0,$page,0,0,0,0]]", hasRefs, sup)

    private fun runWithoutTextMap(text: String): JsonObject = JsonObject().apply { addProperty("text", text) }

    private fun glyphRun(
        text: String,
        page: Int,
        x: Double = 0.0,
        charWidth: Double = 5.0,
        top: Double = 0.0,
        height: Double = 10.0,
        hasRefs: Boolean = false,
        sup: Boolean = false,
    ): JsonObject {
        val glyphCount = text.count { it != ' ' && it != '\n' && it != '\t' }
        val maxX = x + charWidth * glyphCount
        val prefix = "0,$page,$x,$top,$maxX,${top + height}"
        val textMap = if (glyphCount > 0) {
            "[[$prefix,${List(glyphCount) { charWidth }.joinToString(",")}]]"
        } else {
            "[[$prefix]]"
        }
        return buildRun(text, textMap, hasRefs, sup)
    }

    private fun buildRun(text: String, textMap: String, hasRefs: Boolean, sup: Boolean): JsonObject {
        val run = JsonObject()
        run.addProperty("text", text)
        run.add("anchor", JsonObject().apply { addProperty("textMap", textMap) })
        if (hasRefs) {
            run.add("refs", JsonParser.parseString("[[0]]"))
        }
        if (sup) {
            run.add("style", JsonObject().apply { addProperty("sup", true) })
        }
        return run
    }

    private fun contentWrapper(vararg runs: JsonObject): JsonObject {
        val content = JsonArray().apply { runs.forEach { add(it) } }
        return JsonObject().apply { add("content", content) }
    }

    private fun block(type: String, page: Int, flowClass: String? = null, runs: List<JsonObject>): JsonObject {
        val block = JsonObject()
        block.addProperty("type", type)
        block.add("anchor", JsonObject().apply { add("pageRects", JsonParser.parseString("[[$page,0,0,0,0]]")) })
        block.add("content", JsonArray().apply { runs.forEach { add(it) } })
        if (flowClass != null) {
            block.addProperty("flowClass", flowClass)
        }
        return block
    }

    private fun paragraphBlock(page: Int, flowClass: String? = null, runs: List<JsonObject>): JsonObject =
        block(type = "paragraph", page = page, flowClass = flowClass, runs = runs)

    private fun document(vararg blocks: JsonObject): JsonObject {
        val content = JsonArray().apply { blocks.forEach { add(it) } }
        return JsonObject().apply { add("content", content) }
    }

    private fun rect(x: Double, y: Double, width: Double, height: Double) = SDTRect(x, y, width, height)
}