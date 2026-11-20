package org.zotero.android.speech.documentworker

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import kotlin.math.min

object SDTContent {

    typealias Ref = List<Int>

    data class Point(val ref: Ref?, val offset: Int?)

    data class LeafBlock(val block: JsonObject, val ref: Ref, val startPoint: Point, val endPoint: Point)

    data class Part(val ref: Ref, val block: JsonObject)

    fun compareRefs(lhs: Ref, rhs: Ref): Int {
        for (index in 0 until min(lhs.size, rhs.size)) {
            if (lhs[index] != rhs[index]) {
                return lhs[index] - rhs[index]
            }
        }
        return lhs.size - rhs.size
    }

    fun sameRef(lhs: Ref?, rhs: Ref?): Boolean {
        if (lhs == null || rhs == null) {
            return false
        }
        return lhs == rhs
    }

    fun isTextNode(node: JsonElement?): Boolean {
        val obj = node as? JsonObject ?: return false
        val text = obj.get("text")
        return text != null && text.isJsonPrimitive && text.asJsonPrimitive.isString
    }

    fun isLeafBlock(node: JsonObject): Boolean {
        if (isTextNode(node)) {
            return false
        }
        val content = node.get("content") as? JsonArray
        if (content == null || content.size() == 0) {
            return true
        }
        return content.none { isBlockChild(it) }
    }

    fun text(node: JsonObject?): String? {
        val textElement = node?.get("text") ?: return null
        if (!textElement.isJsonPrimitive || !textElement.asJsonPrimitive.isString) {
            return null
        }
        return textElement.asString
    }

    fun node(ref: Ref, structure: JsonObject): JsonObject? {
        var content = structure.get("content") as? JsonArray
        var node: JsonObject? = null
        for (index in ref) {
            val children = content ?: return null
            if (index < 0 || index >= children.size()) {
                return null
            }
            val child = children[index] as? JsonObject ?: return null
            node = child
            content = child.get("content") as? JsonArray
        }
        return node
    }

    private fun isBlockChild(child: JsonElement): Boolean {
        val childObject = child as? JsonObject ?: return false
        return !isTextNode(childObject)
    }

    private fun firstTextNode(node: JsonObject?): JsonObject? {
        if (node == null) {
            return null
        }
        if (isTextNode(node)) {
            return node
        }
        val content = node.get("content") as? JsonArray ?: return null
        for (child in content) {
            val textNode = firstTextNode(child as? JsonObject)
            if (textNode != null) {
                return textNode
            }
        }
        return null
    }

    private fun lastTextNode(node: JsonObject?): JsonObject? {
        if (node == null) {
            return null
        }
        if (isTextNode(node)) {
            return node
        }
        val content = node.get("content") as? JsonArray ?: return null
        for (child in content.reversed()) {
            val textNode = lastTextNode(child as? JsonObject)
            if (textNode != null) {
                return textNode
            }
        }
        return null
    }

    fun contentBoundary(value: JsonElement?): Ref? {
        val values = value as? JsonArray ?: return null
        if (values.size() == 0) {
            return null
        }
        val boundary = mutableListOf<Int>()
        for (element in values) {
            val index = integer(element)
            if (index == null || index < 0) {
                return null
            }
            boundary.add(index)
        }
        return boundary
    }

    fun contentRange(value: JsonElement?): Pair<Ref, Ref>? {
        val values = value as? JsonArray ?: return null
        if (values.size() != 2) {
            return null
        }
        val start = contentBoundary(values[0]) ?: return null
        val end = contentBoundary(values[1]) ?: return null
        return start to end
    }

    fun splitContentPoint(point: Ref, content: JsonArray): Point {
        val ref = mutableListOf<Int>()
        var node: JsonElement? = null
        for ((index, value) in point.withIndex()) {
            if (isTextNode(node) && index == point.size - 1) {
                return Point(ref.toList(), value)
            }
            ref.add(value)
            node = if (ref.size == 1) {
                if (value in 0 until content.size()) content[value] else null
            } else {
                val children = (node as? JsonObject)?.get("content") as? JsonArray
                if (children != null && value in 0 until children.size()) children[value] else null
            }
        }
        return Point(ref.toList(), null)
    }

    fun walkContentRangeLeafBlocks(content: JsonArray, rangeValue: JsonElement?, callback: (LeafBlock) -> Unit) {
        val range = contentRange(rangeValue) ?: return
        walkContentRangeLeafBlocks(content, range, callback)
    }

    fun walkContentRangeLeafBlocks(content: JsonArray, range: Pair<Ref, Ref>, callback: (LeafBlock) -> Unit) {
        val (start, end) = range
        if (start.isEmpty() || end.isEmpty()) {
            return
        }
        val span = contentRangeBlockSpan(range, content.size()) ?: return
        if (span.first >= span.second) {
            return
        }
        val startPoint = splitContentPoint(start, content)
        val endPoint = splitContentPoint(end, content)

        for (index in span.first until span.second) {
            val block = content[index] as? JsonObject ?: continue
            if (isTextNode(block)) {
                continue
            }
            walkLeafBlocks(block, listOf(index), range, startPoint, endPoint, callback)
        }
    }

    private fun walkLeafBlocks(
        block: JsonObject,
        ref: Ref,
        range: Pair<Ref, Ref>,
        startPoint: Point,
        endPoint: Point,
        callback: (LeafBlock) -> Unit
    ) {
        if (isLeafBlock(block)) {
            val leafEnd = ref.toMutableList()
            leafEnd[leafEnd.size - 1] = leafEnd[leafEnd.size - 1] + 1
            if (compareRefs(leafEnd, range.first) <= 0 || compareRefs(ref, range.second) >= 0) {
                return
            }
            callback(LeafBlock(block, ref, startPoint, endPoint))
            return
        }
        val children = block.get("content") as? JsonArray ?: return
        for ((index, child) in children.withIndex()) {
            val childObject = child as? JsonObject ?: continue
            if (isTextNode(childObject)) {
                continue
            }
            walkLeafBlocks(childObject, ref + index, range, startPoint, endPoint, callback)
        }
    }

    private fun contentRangeBlockSpan(range: Pair<Ref, Ref>, topLevelBlockCount: Int): Pair<Int, Int>? {
        val (start, end) = range
        val startIndex = start[0]
        if (startIndex > topLevelBlockCount) {
            return null
        }
        if (start == end) {
            return startIndex to startIndex
        }
        val endIndex = end[0]
        if (endIndex > topLevelBlockCount) {
            return null
        }
        val endExclusive = if (endIndex == topLevelBlockCount) topLevelBlockCount else (if (end.size == 1) endIndex else endIndex + 1)
        return startIndex to maxOf(startIndex, endExclusive)
    }

    fun partChain(structure: JsonObject, ref: Ref, include: ((Ref, JsonObject) -> Boolean)? = null): List<Part> {
        val root = partRoot(structure, ref) ?: return emptyList()
        val chain = mutableListOf<Part>()
        val seen = mutableSetOf<Ref>()
        var currentRef: Ref? = root
        while (currentRef != null) {
            val r = currentRef
            if (!seen.add(r)) {
                break
            }
            val block = node(r, structure) ?: break
            if (include != null && !include(r, block)) {
                break
            }
            chain.add(Part(r, block))
            currentRef = contentBoundaryFromPartLink(block.get("nextPart"))
        }
        return chain
    }

    private fun partRoot(structure: JsonObject, ref: Ref): Ref? {
        var currentRef = ref
        val seen = mutableSetOf<Ref>()
        while (true) {
            if (!seen.add(currentRef)) {
                return currentRef
            }
            val link = node(currentRef, structure)?.get("previousPart")
            if (link !is JsonArray) {
                return currentRef
            }
            currentRef = contentBoundaryFromPartLink(link) ?: return null
        }
    }

    fun shouldDropHardHyphenAtPartBoundary(previousBlock: JsonObject, nextBlock: JsonObject): Boolean {
        val lastText = text(lastTextNode(previousBlock))
        if (lastText.isNullOrEmpty() || lastText.last() != hyphen) {
            return false
        }
        val firstChar = text(firstTextNode(nextBlock))?.firstOrNull() ?: return false
        return firstChar in 'a'..'z'
    }

    fun partBoundarySeparator(previousBlock: JsonObject, nextBlock: JsonObject): String {
        val lastNode = lastTextNode(previousBlock) ?: return ""
        val firstNode = firstTextNode(nextBlock) ?: return ""
        val lastText = text(lastNode) ?: return ""
        val firstText = text(firstNode) ?: return ""
        if (lastText.lastOrNull()?.let { isWhitespace(it) } == true || firstText.firstOrNull()?.let { isWhitespace(it) } == true) {
            return ""
        }
        if (textNodeEndsWithSoftHyphen(lastNode)) {
            return ""
        }
        if (isLetterOrNumberLast(lastText) && isLetterOrNumberFirst(firstText)) {
            return space.toString()
        }
        return ""
    }

    private fun contentBoundaryFromPartLink(value: JsonElement?): Ref? {
        if (value !is JsonArray) {
            return null
        }
        val ref = mutableListOf<Int>()
        for (element in value) {
            ref.add(integer(element) ?: return null)
        }
        return ref
    }

    private fun textNodeEndsWithSoftHyphen(node: JsonObject): Boolean {
        val textMapPrimitive = (node.get("anchor") as? JsonObject)?.get("textMap") as? JsonPrimitive
        val textMap = textMapPrimitive?.takeIf { it.isString }?.asString ?: return false
        val runs = try {
            JsonParser.parseString(textMap)
        } catch (e: Exception) {
            return false
        }
        if (runs !is JsonArray || runs.size() == 0) {
            return false
        }
        val lastRun = runs[runs.size() - 1] as? JsonArray ?: return false
        val header = lastRun.firstOrNull() as? JsonPrimitive ?: return false
        if (!header.isNumber) {
            return false
        }
        return (header.asLong and 1L) != 0L
    }

    const val space: Char = ' '
    const val hyphen: Char = '-'

    fun isWhitespace(unit: Char): Boolean {
        val code = unit.code
        return when {
            code in 0x09..0x0D -> true
            code == 0x20 -> true
            code == 0xA0 -> true
            code == 0x1680 -> true
            code in 0x2000..0x200A -> true
            code == 0x2028 -> true
            code == 0x2029 -> true
            code == 0x202F -> true
            code == 0x205F -> true
            code == 0x3000 -> true
            code == 0xFEFF -> true
            else -> false
        }
    }

    fun containsNonWhitespace(text: CharSequence): Boolean {
        return text.any { !isWhitespace(it) }
    }

    private fun isLetterOrNumberLast(text: String): Boolean {
        if (text.isEmpty()) {
            return false
        }
        return isLetterOrNumber(text.codePointBefore(text.length))
    }

    private fun isLetterOrNumberFirst(text: String): Boolean {
        if (text.isEmpty()) {
            return false
        }
        return isLetterOrNumber(text.codePointAt(0))
    }

    private fun isLetterOrNumber(codePoint: Int): Boolean {
        val type = Character.getType(codePoint)
        return type == Character.UPPERCASE_LETTER.toInt() ||
            type == Character.LOWERCASE_LETTER.toInt() ||
            type == Character.TITLECASE_LETTER.toInt() ||
            type == Character.MODIFIER_LETTER.toInt() ||
            type == Character.OTHER_LETTER.toInt() ||
            type == Character.DECIMAL_DIGIT_NUMBER.toInt() ||
            type == Character.LETTER_NUMBER.toInt() ||
            type == Character.OTHER_NUMBER.toInt()
    }

    fun integer(value: JsonElement?): Int? {
        if (value == null || value !is JsonPrimitive || !value.isNumber) {
            return null
        }
        val double = value.asDouble
        if (!double.isFinite() || double != Math.floor(double)) {
            return null
        }
        return value.asInt
    }

    fun isTruthy(value: JsonElement?): Boolean {
        if (value == null || value.isJsonNull) {
            return false
        }
        if (value is JsonPrimitive) {
            return when {
                value.isBoolean -> value.asBoolean
                value.isNumber -> {
                    val double = value.asDouble
                    double != 0.0 && !double.isNaN()
                }
                value.isString -> value.asString.isNotEmpty()
                else -> true
            }
        }
        return true
    }
}