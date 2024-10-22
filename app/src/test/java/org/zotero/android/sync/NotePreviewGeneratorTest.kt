package org.zotero.android.sync

import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class NotePreviewGeneratorTest {

    private val sut = NotePreviewGenerator

    @Before
    fun setUp() {
    }

    @Test
    fun `returns null for an empty note`() {
        runBlocking {
            val result = sut.preview("")
            result shouldBeEqualTo null
        }
    }

    @Test
    fun `returns string with sanitized tabs`() {
        runBlocking {
            val result = sut.preview("a string with\t a tab")
            result shouldBeEqualTo "a string with a tab"
        }
    }

    @Test
    fun `returns only first sentence from a paragraph with new lines`() {
        runBlocking {
            val result = sut.preview("First Paragraph\nSecond Paragraph")
            result shouldBeEqualTo "First Paragraph"
        }
    }

    @Test
    fun `returns only first 200 chars from a note`() {
        runBlocking {
            val longNote =
                "Science is a systematic discipline that builds and organises knowledge in the form of testable hypotheses and predictions about the world.[1][2] Modern science is typically divided into two or three major branches:[3] the natural sciences (e.g., physics, chemistry, and biology), which study the physical world; and the behavioural sciences (e.g., economics, psychology, and sociology), which study individuals and societies."
            val first200CharsOfANote = longNote.take(200)

            val result = sut.preview(longNote)
            result shouldBeEqualTo first200CharsOfANote
        }
    }

    @Test
    fun `returns note unchanged if it ends with complete surrogate`() {
        runBlocking {
            val stringWithFullSurrogate =
                "Hey\uD83D\uDE0A"

            val result = sut.preview(stringWithFullSurrogate)
            result shouldBeEqualTo stringWithFullSurrogate
        }
    }

    @Test
    fun `returns string with last character dropped if note ends with incomplete surrogate`() {
        runBlocking {
            val stringWithFullSurrogate =
                "Hey\uD83D"

            val result = sut.preview(stringWithFullSurrogate)
            result shouldBeEqualTo "Hey"
        }
    }

}