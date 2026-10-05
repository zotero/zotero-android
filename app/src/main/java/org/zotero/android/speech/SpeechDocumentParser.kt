package org.zotero.android.speech

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import org.zotero.android.speech.data.RemoteVoice
import org.zotero.android.speech.data.SDTRect
import org.zotero.android.speech.data.TextRange

private fun JsonElement?.asStringOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.asString

private fun JsonElement?.asBooleanOrNull(): Boolean? =
    (this as? JsonPrimitive)?.takeIf { it.isBoolean }?.asBoolean

private fun JsonElement?.asIntOrNull(): Int? =
    (this as? JsonPrimitive)?.takeIf { it.isNumber }?.asInt

private fun JsonElement?.asDoubleOrNull(): Double? =
    (this as? JsonPrimitive)?.takeIf { it.isNumber }?.asDouble

object SpeechDocumentParser {

    data class Paragraph(
        val text: String,
        val page: Int,
        val pageOffset: Int,
        val rects: List<SDTRect>,
        val charRects: List<SDTRect?>,
    )

    data class Segment(
        val text: String,
        val pageOffset: Int,
        val charRects: List<SDTRect?> = emptyList(),
    )

    data class ParsedDocument(
        val paragraphs: List<Paragraph>,
        val language: String?,
    )

    private val classesTypesToIgnore = setOf("excluded", "auxiliary")

    const val segmentSeparator = "\n\n"

    private const val linkGroupCoverageThreshold = 0.25

    private data class Run(
        val text: String,
        val page: Int,
        val hasRefs: Boolean,
        val sup: Boolean,
        val charRects: List<SDTRect?>,
    )

    private data class ElisionRange(val start: Int, val end: Int)

    private data class RunMapping(
        val absStart: Int,
        val absEnd: Int,
        val hasRefs: Boolean,
        val sup: Boolean,
    )

    fun parse(materialized: JsonObject): ParsedDocument {
        val language = language(materialized)
        val content = materialized.get("content") as? JsonArray
            ?: return ParsedDocument(paragraphs = emptyList(), language = language)

        val paragraphs = mutableListOf<Paragraph>()
        val pageLengths = mutableMapOf<Int, Int>()

        for (blockElement in content) {
            val block = blockElement as? JsonObject ?: continue
            val flowClass = block.get("flowClass").asStringOrNull()
            if (flowClass != null && classesTypesToIgnore.contains(flowClass)) continue
            val fallbackPage = startPage(block) ?: pageLengths.keys.maxOrNull() ?: 0
            val runs = mutableListOf<Run>()
            collectRuns(block, fallbackPage, runs)
            appendSegments(runs, block, paragraphs, pageLengths)
        }

        return ParsedDocument(paragraphs = paragraphs, language = language)
    }

    private fun collectRuns(node: JsonObject, fallbackPage: Int, runs: MutableList<Run>) {
        val text = node.get("text").asStringOrNull()
        if (text != null) {
            if (text.isEmpty()) return
            val page = textMapPage(node) ?: runs.lastOrNull()?.page ?: fallbackPage
            val refs = node.get("refs") as? JsonArray
            val hasRefs = refs != null && refs.size() > 0
            val style = node.get("style") as? JsonObject
            val sup = style?.get("sup").asBooleanOrNull() == true
            runs.add(Run(text = text, page = page, hasRefs = hasRefs, sup = sup, charRects = charRects(node, page)))
            return
        }

        val nodePage = startPage(node) ?: fallbackPage
        val children = node.get("content") as? JsonArray ?: return
        for (childElement in children) {
            val child = childElement as? JsonObject ?: continue
            collectRuns(child, nodePage, runs)
        }
    }

    private fun appendSegments(
        runs: List<Run>,
        block: JsonObject,
        paragraphs: MutableList<Paragraph>,
        pageLengths: MutableMap<Int, Int>,
    ) {
        fun flush(page: Int, pageRuns: List<Run>) {
            val fallbackRect = boundingRect(rects(block, page))
            val (strippedCharacters, strippedRects) = strippingCitations(pageRuns, fallbackRect)

            val first = strippedCharacters.indexOfFirst { !it.isWhitespace() }
            val last = strippedCharacters.indexOfLast { !it.isWhitespace() }
            if (first < 0 || last < 0) return
            val text = strippedCharacters.subList(first, last + 1).joinToString("")
            val charRects = strippedRects.subList(first, last + 1)

            val existingLength = pageLengths[page] ?: 0
            val offset = if (existingLength == 0) 0 else existingLength + segmentSeparator.length
            pageLengths[page] = offset + text.length
            paragraphs.add(Paragraph(text = text, page = page, pageOffset = offset, rects = rects(block, page), charRects = charRects))
        }

        var currentPage: Int? = null
        var pageRuns = mutableListOf<Run>()

        for (run in runs) {
            if (currentPage != null && currentPage != run.page) {
                flush(currentPage, pageRuns)
                pageRuns = mutableListOf()
            }
            currentPage = run.page
            pageRuns.add(run)
        }
        currentPage?.let { flush(it, pageRuns) }
    }

    private fun strippingCitations(runs: List<Run>, fallbackRect: SDTRect?): Pair<List<Char>, List<SDTRect?>> {
        val characters = mutableListOf<Char>()
        val charRects = mutableListOf<SDTRect?>()
        val mappings = mutableListOf<RunMapping>()
        for (run in runs) {
            val runCharacters = run.text.toList()
            val absStart = characters.size
            characters.addAll(runCharacters)
            when {
                run.charRects.any { it != null } -> charRects.addAll(run.charRects)
                fallbackRect != null -> charRects.addAll(runCharacters.map { if (isSDTWhitespace(it)) null else fallbackRect })
                else -> charRects.addAll(run.charRects)
            }
            if (runCharacters.any { !it.isWhitespace() }) {
                mappings.add(RunMapping(absStart = absStart, absEnd = characters.size, hasRefs = run.hasRefs, sup = run.sup))
            }
        }

        val elided = elidedRanges(characters, mappings)
        if (elided.isEmpty()) return characters to charRects

        val keptCharacters = mutableListOf<Char>()
        val keptRects = mutableListOf<SDTRect?>()
        var cursor = 0
        for (range in elided) {
            if (range.start > cursor) {
                keptCharacters.addAll(characters.subList(cursor, range.start))
                keptRects.addAll(charRects.subList(cursor, range.start))
            }
            cursor = maxOf(cursor, range.end)
        }
        if (cursor < characters.size) {
            keptCharacters.addAll(characters.subList(cursor, characters.size))
            keptRects.addAll(charRects.subList(cursor, characters.size))
        }
        return keptCharacters to keptRects
    }

    private fun elidedRanges(text: List<Char>, mappings: List<RunMapping>): List<ElisionRange> {
        val ranges = mutableListOf<ElisionRange>()
        for ((open, close) in listOf('[' to ']', '(' to ')')) {
            val stack = mutableListOf<Int>()
            for (index in text.indices) {
                if (text[index] == open) {
                    stack.add(index)
                } else if (text[index] == close) {
                    val start = stack.removeLastOrNull()
                    if (start != null && isLinkGroup(text, mappings, start, index + 1)) {
                        ranges.add(ElisionRange(start, index + 1))
                    }
                }
            }
        }
        for (mapping in mappings) {
            if (mapping.hasRefs && mapping.sup) {
                ranges.add(ElisionRange(mapping.absStart, mapping.absEnd))
            }
        }
        return mergeRanges(ranges)
    }

    private fun isLinkGroup(text: List<Char>, mappings: List<RunMapping>, start: Int, end: Int): Boolean {
        var linkedCharacters = 0
        for (mapping in mappings) {
            if (!mapping.hasRefs) continue
            val from = maxOf(start, mapping.absStart)
            val to = minOf(end, mapping.absEnd)
            for (index in from until maxOf(from, to)) {
                if (!text[index].isWhitespace()) linkedCharacters++
            }
        }
        if (linkedCharacters <= 0) return false

        var contentCharacters = 0
        for (index in (start + 1) until maxOf(start + 1, end - 1)) {
            if (!text[index].isWhitespace()) contentCharacters++
        }
        return contentCharacters > 0 && linkedCharacters.toDouble() / contentCharacters.toDouble() >= linkGroupCoverageThreshold
    }

    private fun mergeRanges(ranges: List<ElisionRange>): List<ElisionRange> {
        val sorted = ranges
            .filter { it.end > it.start }
            .sortedWith(compareBy({ it.start }, { it.end }))
        val merged = mutableListOf<ElisionRange>()
        for (range in sorted) {
            val last = merged.lastOrNull()
            if (last != null && range.start <= last.end) {
                merged[merged.size - 1] = last.copy(end = maxOf(last.end, range.end))
            } else {
                merged.add(range)
            }
        }
        return merged
    }

    private data class RunDatum(val rect: SDTRect, val pageIndex: Int)

    private data class CharPosition(val start: Double, val end: Double)

    private fun charRects(node: JsonObject, page: Int): List<SDTRect?> {
        val characters = (node.get("text").asStringOrNull() ?: "").toList()
        val anchor = node.get("anchor") as? JsonObject
        val textMap = anchor?.get("textMap").asStringOrNull()
            ?: return List(characters.size) { null }
        val runData = buildRunData(parseTextMap(textMap))
        if (runData.isEmpty()) return List(characters.size) { null }

        val result = mutableListOf<SDTRect?>()
        var runIndex = 0
        for (character in characters) {
            if (isSDTWhitespace(character)) {
                result.add(null)
            } else if (runIndex < runData.size) {
                val datum = runData[runIndex]
                runIndex++
                result.add(if (datum.pageIndex == page) datum.rect else null)
            } else {
                result.add(null)
            }
        }
        return result
    }

    private fun parseTextMap(textMap: String): List<JsonArray> {
        val parsed = try {
            JsonParser.parseString(textMap) as? JsonArray
        } catch (e: Exception) {
            null
        } ?: return emptyList()
        return parsed.mapNotNull { it as? JsonArray }
    }

    private fun buildRunData(runs: List<JsonArray>): List<RunDatum> {
        val data = mutableListOf<RunDatum>()
        for (run in runs) {
            if (run.size() < 6) continue
            val header = run[0].asIntOrNull() ?: continue
            val pageIndex = run[1].asIntOrNull() ?: continue
            val minX = run[2].asDoubleOrNull() ?: continue
            val minY = run[3].asDoubleOrNull() ?: continue
            val maxX = run[4].asDoubleOrNull() ?: continue
            val maxY = run[5].asDoubleOrNull() ?: continue
            val axisDirection = (header shr 1) and 0b11
            val vertical = axisDirection == 1 || axisDirection == 3
            val positions = reconstructCharPositions(run, vertical, minX, minY, maxX, maxY).toMutableList()
            if ((header and 1) != 0 && positions.isNotEmpty()) {
                positions.removeAt(positions.size - 1)
            }
            for (position in positions) {
                if (!position.start.isFinite() || !position.end.isFinite()) continue
                val rect = if (vertical) {
                    SDTRect(x = minX, y = position.start, width = maxX - minX, height = position.end - position.start)
                } else {
                    SDTRect(x = position.start, y = minY, width = position.end - position.start, height = maxY - minY)
                }
                data.add(RunDatum(rect = rect, pageIndex = pageIndex))
            }
        }
        return data
    }

    private fun reconstructCharPositions(
        run: JsonArray,
        vertical: Boolean,
        minX: Double,
        minY: Double,
        maxX: Double,
        maxY: Double,
    ): List<CharPosition> {
        val start = if (vertical) minY else minX
        val end = if (vertical) maxY else maxX
        val widths = if (run.size() > 6) (6 until run.size()).map { run[it] } else emptyList()
        if (widths.isEmpty()) return listOf(CharPosition(start, end))

        val positions = mutableListOf<CharPosition>()
        var cursor = start
        for (width in widths) {
            val pair = width as? JsonArray
            if (pair != null && pair.size() >= 2) {
                val delta = pair[0].asDoubleOrNull()
                val advance = pair[1].asDoubleOrNull()
                if (delta != null && advance != null) {
                    cursor += delta
                    positions.add(CharPosition(cursor, cursor + advance))
                    cursor += advance
                    continue
                }
            }
            val advance = width.asDoubleOrNull()
            if (advance != null) {
                positions.add(CharPosition(cursor, cursor + advance))
                cursor += advance
            }
        }
        return positions
    }

    private fun isSDTWhitespace(character: Char): Boolean =
        character == ' ' || character == '\n' || character == '\t'

    fun pdfLineRects(range: TextRange, segments: List<Segment>): List<SDTRect> {
        if (range.length <= 0) return emptyList()
        val rangeStart = range.location
        val rangeEnd = range.location + range.length
        val rects = mutableListOf<SDTRect>()
        for (segment in segments) {
            val segmentStart = segment.pageOffset
            val segmentEnd = segment.pageOffset + segment.text.length
            val from = maxOf(rangeStart, segmentStart)
            val to = minOf(rangeEnd, segmentEnd)
            if (from >= to) continue
            for (index in (from - segmentStart) until (to - segmentStart)) {
                if (index < segment.charRects.size) {
                    segment.charRects[index]?.let { rects.add(it) }
                }
            }
        }
        return mergeLineRects(rects)
    }

    private fun mergeLineRects(rects: List<SDTRect>): List<SDTRect> {
        val merged = mutableListOf<SDTRect>()
        for (rect in rects) {
            val last = merged.lastOrNull()
            if (last != null && sameLine(last, rect)) {
                merged[merged.size - 1] = unionRect(last, rect)
            } else {
                merged.add(rect)
            }
        }
        return merged
    }

    private fun sameLine(a: SDTRect, b: SDTRect): Boolean {
        val overlap = minOf(a.y + a.height, b.y + b.height) - maxOf(a.y, b.y)
        val minHeight = maxOf(0.001, minOf(a.height, b.height))
        return overlap / minHeight >= 0.6
    }

    fun language(materialized: JsonObject): String? {
        val metadata = materialized.get("metadata") as? JsonObject ?: return null
        val source = metadata.get("source") as? JsonObject ?: return null
        val properties = source.get("properties") as? JsonObject ?: return null
        val language = (properties.get("language") ?: properties.get("Language")).asStringOrNull()
        val trimmed = language?.trim()
        return trimmed?.takeIf { it.isNotEmpty() }
    }

    private fun startPage(node: JsonObject): Int? {
        val anchor = node.get("anchor") as? JsonObject ?: return null
        val pageRects = anchor.get("pageRects") as? JsonArray ?: return null
        val firstRect = pageRects.firstOrNull() as? JsonArray ?: return null
        return firstRect.firstOrNull().asIntOrNull()
    }

    private fun textMapPage(node: JsonObject): Int? {
        val anchor = node.get("anchor") as? JsonObject ?: return null
        val textMap = anchor.get("textMap").asStringOrNull() ?: return null
        val entries = try {
            JsonParser.parseString(textMap) as? JsonArray
        } catch (e: Exception) {
            null
        } ?: return null
        val firstEntry = entries.firstOrNull() as? JsonArray ?: return null
        if (firstEntry.size() <= 1) return null
        return firstEntry[1].asIntOrNull()
    }

    private fun rects(block: JsonObject, page: Int): List<SDTRect> {
        val anchor = block.get("anchor") as? JsonObject ?: return emptyList()
        val pageRects = anchor.get("pageRects") as? JsonArray ?: return emptyList()
        return pageRects.mapNotNull { entry ->
            val rect = entry as? JsonArray ?: return@mapNotNull null
            if (rect.size() < 5) return@mapNotNull null
            val rectPage = rect[0].asIntOrNull() ?: return@mapNotNull null
            if (rectPage != page) return@mapNotNull null
            val x0 = rect[1].asDoubleOrNull() ?: return@mapNotNull null
            val y0 = rect[2].asDoubleOrNull() ?: return@mapNotNull null
            val x1 = rect[3].asDoubleOrNull() ?: return@mapNotNull null
            val y1 = rect[4].asDoubleOrNull() ?: return@mapNotNull null
            SDTRect(x = x0, y = y0, width = x1 - x0, height = y1 - y0)
        }
    }

    private fun boundingRect(rects: List<SDTRect>): SDTRect? {
        if (rects.isEmpty()) return null
        var union = rects.first()
        for (rect in rects.drop(1)) {
            union = unionRect(union, rect)
        }
        return union
    }

    private fun unionRect(a: SDTRect, b: SDTRect): SDTRect {
        val minX = minOf(a.x, b.x)
        val minY = minOf(a.y, b.y)
        val maxX = maxOf(a.x + a.width, b.x + b.width)
        val maxY = maxOf(a.y + a.height, b.y + b.height)
        return SDTRect(x = minX, y = minY, width = maxX - minX, height = maxY - minY)
    }

    fun paragraphRange(index: Int, segments: List<Segment>): TextRange? {
        val (_, segment) = segmentContaining(index, segments) ?: return null
        val end = segment.pageOffset + segment.text.length
        val start = maxOf(index, segment.pageOffset)
        return TextRange(start, end - start)
    }

    fun sentenceRange(index: Int, segments: List<Segment>): TextRange? {
        val (_, segment) = segmentContaining(index, segments) ?: return null
        val intra = maxOf(0, index - segment.pageOffset)
        val sentence = TextTokenizer.findSentence(segment.text, intra) ?: return null
        return TextRange(segment.pageOffset + sentence.range.location, sentence.range.length)
    }

    fun nextSentenceStart(index: Int, segments: List<Segment>): Int? {
        val (segmentIndex, segment) = segmentContaining(index, segments) ?: return null
        val segmentEnd = segment.pageOffset + segment.text.length
        if (index < segmentEnd) {
            val intra = maxOf(0, index - segment.pageOffset)
            val relativeNext = TextTokenizer.nextSentenceStart(segment.text, intra)
            if (relativeNext != null) {
                val candidate = segment.pageOffset + relativeNext
                if (candidate < segmentEnd) {
                    return candidate
                }
            }
        }
        val nextIndex = segmentIndex + 1
        if (nextIndex >= segments.size) return null
        return firstSentenceStart(segments[nextIndex])
    }

    fun previousSentenceStart(index: Int, segments: List<Segment>): Int? {
        val (segmentIndex, segment) = segmentContaining(index, segments) ?: return null
        if (index > segment.pageOffset) {
            val intra = index - segment.pageOffset
            val relativeStart = TextTokenizer.previousSentenceStart(segment.text, intra)
            if (relativeStart != null) {
                return segment.pageOffset + relativeStart
            }
        }
        if (segmentIndex <= 0) return null
        return lastSentenceStart(segments[segmentIndex - 1])
    }

    fun lastSentenceStart(segments: List<Segment>): Int? {
        val last = segments.lastOrNull() ?: return null
        return lastSentenceStart(last)
    }

    private fun firstSentenceStart(segment: Segment): Int {
        return segment.pageOffset + (TextTokenizer.findSentence(segment.text, 0)?.range?.location ?: 0)
    }

    private fun lastSentenceStart(segment: Segment): Int {
        return segment.pageOffset + (TextTokenizer.previousSentenceStart(segment.text, segment.text.length) ?: 0)
    }

    private fun segmentContaining(index: Int, segments: List<Segment>): Pair<Int, Segment>? {
        for ((offset, segment) in segments.withIndex()) {
            if (index < segment.pageOffset + segment.text.length) {
                return offset to segment
            }
        }
        return null
    }

    fun unitRange(index: Int, granularity: RemoteVoice.Granularity, segments: List<Segment>): TextRange? {
        val (_, segment) = segmentContaining(index, segments) ?: return null
        if (granularity == RemoteVoice.Granularity.paragraph) {
            return TextRange(segment.pageOffset, segment.text.length)
        }
        val intra = maxOf(0, index - segment.pageOffset)
        val sentence = TextTokenizer.findSentenceContaining(segment.text, intra) ?: return null
        return TextRange(segment.pageOffset + sentence.range.location, sentence.range.length)
    }

    fun firstUnitRange(granularity: RemoteVoice.Granularity, segments: List<Segment>): TextRange? {
        val first = segments.firstOrNull() ?: return null
        if (granularity == RemoteVoice.Granularity.paragraph) {
            return TextRange(first.pageOffset, first.text.length)
        }
        return sentenceRange(first.pageOffset, segments)
    }

    fun lastUnitRange(granularity: RemoteVoice.Granularity, segments: List<Segment>): TextRange? {
        val last = segments.lastOrNull() ?: return null
        if (granularity == RemoteVoice.Granularity.paragraph) {
            return TextRange(last.pageOffset, last.text.length)
        }
        val start = lastSentenceStart(segments) ?: return null
        return sentenceRange(start, segments)
    }

    fun nextUnitRange(afterEndOf: TextRange, granularity: RemoteVoice.Granularity, segments: List<Segment>): TextRange? {
        val end = afterEndOf.end
        if (granularity == RemoteVoice.Granularity.paragraph) {
            val next = segments.firstOrNull { it.pageOffset >= end } ?: return null
            return TextRange(next.pageOffset, next.text.length)
        }
        val start = nextSentenceStart(end, segments) ?: return null
        return sentenceRange(start, segments)
    }

    fun previousUnitRange(location: Int, granularity: RemoteVoice.Granularity, segments: List<Segment>): TextRange? {
        if (granularity == RemoteVoice.Granularity.paragraph) {
            val previous = segments.lastOrNull { it.pageOffset < location } ?: return null
            return TextRange(previous.pageOffset, previous.text.length)
        }
        val start = previousSentenceStart(location, segments) ?: return null
        return sentenceRange(start, segments)
    }
}