package org.zotero.android.speech

import org.zotero.android.speech.data.TextRange
import java.text.BreakIterator
import java.util.Locale

object TextTokenizer {

    const val maxSentenceLength = 350

    data class Match(val text: String, val range: TextRange)

    private class NormalizedText(val text: String, val removals: List<Removal>) {
        data class Removal(val origStart: Int, val length: Int)

        fun normalizedIndex(originalIndex: Int): Int {
            var offset = 0
            for (removal in removals) {
                if (removal.origStart + removal.length <= originalIndex) {
                    offset += removal.length
                } else if (removal.origStart <= originalIndex) {
                    return removal.origStart - offset
                } else {
                    break
                }
            }
            return originalIndex - offset
        }

        fun originalIndex(normalizedIndex: Int): Int {
            var cumulativeOffset = 0
            for (removal in removals) {
                val normPosition = removal.origStart - cumulativeOffset
                if (normalizedIndex < normPosition) {
                    break
                }
                cumulativeOffset += removal.length
            }
            return normalizedIndex + cumulativeOffset
        }

        fun originalRange(normalizedRange: TextRange): TextRange {
            val origStart = originalIndex(normalizedRange.location)
            val origEnd = originalIndex(normalizedRange.end)
            return TextRange(origStart, origEnd - origStart)
        }
    }

    private fun normalizeText(text: String): NormalizedText {
        if (text.length < 3) {
            return NormalizedText(text, emptyList())
        }

        val result = StringBuilder(text.length)
        val removals = mutableListOf<NormalizedText.Removal>()

        var i = 0
        while (i < text.length) {
            val char = text[i]
            if ((char == '.' || char == '!' || char == '?') &&
                i > 0 &&
                i + 1 < text.length &&
                isLetterOrQuoteOrBracket(text[i - 1]) &&
                text[i + 1].isDigit()
            ) {
                result.append(char)
                i++
                val digitStart = i
                while (i < text.length && text[i].isDigit()) {
                    i++
                }
                removals.add(NormalizedText.Removal(digitStart, i - digitStart))
            } else {
                result.append(char)
                i++
            }
        }

        return NormalizedText(result.toString(), removals)
    }

    private fun isLetterOrQuoteOrBracket(char: Char): Boolean {
        return char.isLetter() || char == '"' || char == '\'' || char == '”' || char == '’' || char == ']' || char == ')'
    }

    private fun extractOriginalText(text: String, range: TextRange): String {
        return text.substring(range.location, range.end).trim()
    }

    private fun enforceMaxLength(text: String): Pair<String, Int>? {
        if (text.length <= maxSentenceLength) {
            return null
        }

        val truncated = text.substring(0, maxSentenceLength)
        val lastSpaceIndex = truncated.lastIndexOf(' ')
        if (lastSpaceIndex >= 0) {
            val splitText = truncated.substring(0, lastSpaceIndex).trim()
            if (splitText.isNotEmpty()) {
                return splitText to lastSpaceIndex
            }
        }

        return truncated.trim() to maxSentenceLength
    }

    private fun tokenRangeContaining(
        breakIterator: BreakIterator,
        text: String,
        index: Int
    ): TextRange {
        val safeIndex = index.coerceIn(0, text.length)
        val end = breakIterator.following(safeIndex)
            .let { if (it == BreakIterator.DONE) text.length else it }
        val start = breakIterator.preceding(end).let { if (it == BreakIterator.DONE) 0 else it }
        return TextRange(start, end - start)
    }

    private fun isParagraphSeparator(char: Char): Boolean =
        char == '\n' || char == '\r' || char == '\u2029'

    fun findSentence(text: String, startIndex: Int): Match? {
        if (startIndex >= text.length) {
            return null
        }

        val normalized = normalizeText(text)
        val normStartIndex = normalized.normalizedIndex(startIndex)

        val remainingNormText = normalized.text.substring(normStartIndex)
        val trimmedRemaining = remainingNormText.trim()
        if (trimmedRemaining.isEmpty()) {
            return null
        }

        val breakIterator = BreakIterator.getSentenceInstance(Locale.getDefault())
        breakIterator.setText(remainingNormText)
        val tokenRange = tokenRangeContaining(breakIterator, remainingNormText, 0)
        val extractedNormText =
            remainingNormText.substring(tokenRange.location, tokenRange.end).trim()

        val originalRange: TextRange
        val resultText: String
        if (extractedNormText.isEmpty()) {
            val trimmedStart = remainingNormText.indexOf(trimmedRemaining)
            val normLocation = normStartIndex + trimmedStart
            originalRange =
                normalized.originalRange(TextRange(normLocation, trimmedRemaining.length))
            resultText = extractOriginalText(text, originalRange)
        } else {
            val normLocation = normStartIndex + tokenRange.location
            originalRange = normalized.originalRange(TextRange(normLocation, tokenRange.length))
            resultText = extractOriginalText(text, originalRange)
        }

        val enforced = enforceMaxLength(resultText)
        if (enforced != null) {
            return Match(enforced.first, TextRange(originalRange.location, enforced.second))
        }
        return Match(resultText, originalRange)
    }

    fun findSentenceContaining(text: String, index: Int): Match? {
        if (index < 0 || index >= text.length) {
            return null
        }

        val normalized = normalizeText(text)
        val normIndex = normalized.normalizedIndex(index)

        val breakIterator = BreakIterator.getSentenceInstance(Locale.getDefault())
        breakIterator.setText(normalized.text)
        val tokenRange = tokenRangeContaining(breakIterator, normalized.text, normIndex)

        val originalRange = normalized.originalRange(tokenRange)
        val resultText = extractOriginalText(text, originalRange)
        if (resultText.isEmpty()) {
            return null
        }

        return Match(resultText, originalRange)
    }

    fun findParagraphContaining(text: String, index: Int): Match? {
        if (index >= text.length) {
            return null
        }

        var start = index
        while (start > 0 && !isParagraphSeparator(text[start - 1])) {
            start--
        }

        var end = index
        while (end < text.length && !isParagraphSeparator(text[end])) {
            end++
        }
        if (end < text.length) {
            end++
            if (text[end - 1] == '\r' && end < text.length && text[end] == '\n') {
                end++
            }
        }

        val extractedText = text.substring(start, end).trim()
        if (extractedText.isEmpty()) {
            return null
        }

        return Match(extractedText, TextRange(start, end - start))
    }

    fun nextSentenceStart(text: String, afterIndex: Int): Int? {
        if (afterIndex >= text.length) {
            return null
        }

        val normalized = normalizeText(text)
        val normStartIndex = normalized.normalizedIndex(afterIndex)
        val normResult = nextSentenceTokenStart(normStartIndex, normalized.text) ?: return null
        return normalized.originalIndex(normResult)
    }

    private fun nextSentenceTokenStart(startIndex: Int, text: String): Int? {
        val breakIterator = BreakIterator.getSentenceInstance(Locale.getDefault())
        breakIterator.setText(text)

        var lowerBound = breakIterator.first()
        var upperBound = breakIterator.next()
        while (upperBound != BreakIterator.DONE) {
            val tokenText = text.substring(lowerBound, upperBound)
            if (tokenText.trim().isNotEmpty()) {
                if (startIndex <= lowerBound) {
                    return lowerBound
                }
                if (startIndex < upperBound) {
                    return upperBound
                }
            }
            lowerBound = upperBound
            upperBound = breakIterator.next()
        }
        return null
    }

    fun previousSentenceStart(text: String, beforeIndex: Int): Int? {
        if (beforeIndex <= 0 || beforeIndex > text.length) {
            return null
        }

        val normalized = normalizeText(text)
        val normIndex = normalized.normalizedIndex(beforeIndex)

        val breakIterator = BreakIterator.getSentenceInstance(Locale.getDefault())
        breakIterator.setText(normalized.text)

        val tokens = mutableListOf<TextRange>()
        var lowerBound = breakIterator.first()
        var upperBound = breakIterator.next()
        while (upperBound != BreakIterator.DONE) {
            val tokenText = normalized.text.substring(lowerBound, upperBound)
            if (tokenText.trim().isNotEmpty()) {
                tokens.add(TextRange(lowerBound, upperBound - lowerBound))
            }
            lowerBound = upperBound
            upperBound = breakIterator.next()
        }

        if (tokens.isEmpty()) {
            return null
        }

        var targetTokenIndex: Int? = null
        for ((i, tokenRange) in tokens.withIndex()) {
            if (normIndex <= tokenRange.location) {
                targetTokenIndex = i - 1
                break
            } else if (normIndex < tokenRange.end) {
                targetTokenIndex = i - 1
                break
            } else if (normIndex == tokenRange.end) {
                targetTokenIndex = i
                break
            }
        }

        if (targetTokenIndex == null) {
            targetTokenIndex = tokens.size - 1
        }
        if (targetTokenIndex < 0) {
            return null
        }

        val normLowerBound = tokens[targetTokenIndex].location
        return normalized.originalIndex(normLowerBound)
    }
}