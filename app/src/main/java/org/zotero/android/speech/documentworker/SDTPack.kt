package org.zotero.android.speech.documentworker

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

class SDTPack(private val data: ByteArray) {

    sealed class Error : Exception() {
        object invalidMagic : Error()
        data class unsupportedPackVersion(val version: Int) : Error()
        object invalidHeader : Error()
        object invalidIndex : Error()
        object invalidLayout : Error()
        object invalidRange : Error()
        object invalidContentChunk : Error()
        object inflateFailed : Error()
        object invalidJSON : Error()
    }

    private data class Header(
        val packVersion: Int,
        val schemaVersion: String,
        val indexLength: Int,
    )

    private data class Index(
        val metadataLength: Int,
        val catalogLength: Int,
        val chunkByteOffsets: List<Int>,
        val chunkBlockStarts: List<Int>,
    )

    companion object {
        private val magic = byteArrayOf(
            0x89.toByte(),
            'S'.code.toByte(),
            'D'.code.toByte(),
            'T'.code.toByte(),
            0x0d,
            0x0a,
            0x1a,
            0x0a
        )
        private const val headerSize = 16
        private const val indexFixedSize = 8
        private const val u32Size = 4
        private const val supportedPackVersion = 1

        private fun readIndex(data: ByteArray, header: Header): Index {
            val indexStart = headerSize
            val metadataLength = readU32LE(data, indexStart)
            val catalogLength = readU32LE(data, indexStart + u32Size)
            val entryCount = (header.indexLength - indexFixedSize) / (u32Size * 2)
            val chunkByteOffsets = mutableListOf<Int>()
            val chunkBlockStarts = mutableListOf<Int>()

            for (i in 0 until entryCount) {
                chunkByteOffsets.add(readU32LE(data, indexStart + indexFixedSize + i * u32Size))
            }
            for (i in 0 until entryCount) {
                chunkBlockStarts.add(
                    readU32LE(
                        data,
                        indexStart + indexFixedSize + entryCount * u32Size + i * u32Size
                    )
                )
            }

            if (metadataLength <= 0 ||
                catalogLength <= 0 ||
                chunkByteOffsets.isEmpty() ||
                chunkByteOffsets.size != chunkBlockStarts.size ||
                chunkByteOffsets.first() != 0 ||
                chunkBlockStarts.first() != 0
            ) {
                throw Error.invalidIndex
            }
            if (chunkByteOffsets.size > 1) {
                assertStrictlyIncreasing(chunkByteOffsets)
                assertStrictlyIncreasing(chunkBlockStarts)
            }
            return Index(metadataLength, catalogLength, chunkByteOffsets, chunkBlockStarts)
        }

        private fun assertStrictlyIncreasing(values: List<Int>) {
            for (i in 1 until values.size) {
                if (values[i] <= values[i - 1]) throw Error.invalidIndex
            }
        }

        private fun validateLayout(data: ByteArray, header: Header, index: Index) {
            val contentLength = index.chunkByteOffsets.lastOrNull() ?: throw Error.invalidIndex
            val contentEnd =
                headerSize + header.indexLength + index.metadataLength + index.catalogLength + contentLength
            if (contentEnd != data.size) throw Error.invalidLayout
        }

        private fun readU32LE(data: ByteArray, offset: Int): Int {
            if (offset < 0 || offset + u32Size > data.size) throw Error.invalidRange
            return (data[offset].toInt() and 0xFF) or
                    ((data[offset + 1].toInt() and 0xFF) shl 8) or
                    ((data[offset + 2].toInt() and 0xFF) shl 16) or
                    ((data[offset + 3].toInt() and 0xFF) shl 24)
        }

        private fun slice(data: ByteArray, start: Int, end: Int): ByteArray {
            if (start < 0 || end > data.size || start > end) throw Error.invalidRange
            return data.copyOfRange(start, end)
        }

        private fun inflate(data: ByteArray): ByteArray {
            val inflater = Inflater(true)
            inflater.setInput(data)
            val output = ByteArrayOutputStream(maxOf(data.size * 4, 64 * 1024))
            val buffer = ByteArray(64 * 1024)
            try {
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                        throw Error.inflateFailed
                    }
                    output.write(buffer, 0, count)
                }
            } catch (e: DataFormatException) {
                throw Error.inflateFailed
            } finally {
                inflater.end()
            }
            return output.toByteArray()
        }
    }

    private val header: Header
    private val index: Index

    init {
        if (data.size < headerSize) throw Error.invalidHeader
        if (!data.copyOfRange(0, magic.size).contentEquals(magic)) throw Error.invalidMagic

        val packVersion = data[8].toInt() and 0xFF
        if (packVersion != supportedPackVersion) throw Error.unsupportedPackVersion(packVersion)

        val indexLength = readU32LE(data, 12)
        if (indexLength < indexFixedSize + u32Size * 2 ||
            (indexLength - indexFixedSize) % (u32Size * 2) != 0 ||
            headerSize + indexLength > data.size
        ) {
            throw Error.invalidHeader
        }

        val header = Header(
            packVersion = packVersion,
            schemaVersion = "${data[9].toInt() and 0xFF}.${data[10].toInt() and 0xFF}.${data[11].toInt() and 0xFF}",
            indexLength = indexLength,
        )
        val index = readIndex(data, header)
        validateLayout(data, header, index)

        this.header = header
        this.index = index
    }

    private val metadataResult: Result<JsonObject> by lazy {
        val start = headerSize + header.indexLength
        runCatching { readDictionaryJSON(start, start + index.metadataLength) }
    }

    private val catalogResult: Result<JsonObject> by lazy {
        val start = headerSize + header.indexLength + index.metadataLength
        runCatching { readDictionaryJSON(start, start + index.catalogLength) }
    }

    fun materialize(): JsonObject {
        val metadata = getMetadata()
        val catalog = getCatalog()
        val content = JsonArray()

        for (chunkIndex in 0 until index.chunkByteOffsets.size - 1) {
            val chunk = inflatedContentChunk(chunkIndex)
            val blockCount =
                index.chunkBlockStarts[chunkIndex + 1] - index.chunkBlockStarts[chunkIndex]
            for (localBlockIndex in 0 until blockCount) {
                content.add(createBlock(blockData(chunk, blockCount, localBlockIndex)))
            }
        }

        val result = JsonObject()
        result.addProperty("schemaVersion", header.schemaVersion)
        result.add("metadata", metadata)
        result.add("catalog", catalog)
        result.add("content", content)
        return result
    }

    fun getMetadata(): JsonObject = metadataResult.getOrThrow()

    fun getCatalog(): JsonObject = catalogResult.getOrThrow()

    fun getTopLevelBlockCount(): Int = index.chunkBlockStarts.lastOrNull() ?: 0

    fun getBlock(ref: List<Int>): JsonObject? {
        val blockIndex = ref.firstOrNull() ?: return null
        var node: JsonElement = topLevelBlock(blockIndex) ?: return null

        for (idx in ref.drop(1)) {
            if (idx < 0) return null
            val obj = node as? JsonObject ?: return null
            val content = obj.get("content") as? JsonArray ?: return null
            if (idx >= content.size()) return null
            node = content[idx]
            if (node !is JsonObject) return null
        }

        return node as? JsonObject
    }

    fun getBlocks(startBlock: Int, endBlock: Int): List<JsonObject> {
        if (startBlock > endBlock) return emptyList()
        val totalTopLevelBlocks = getTopLevelBlockCount()
        val start = maxOf(0, startBlock)
        val end = minOf(totalTopLevelBlocks - 1, endBlock)
        if (start > end) return emptyList()
        val chunkStart = chunkIndex(start) ?: return emptyList()
        val chunkEnd = chunkIndex(end) ?: return emptyList()

        val blocks = mutableListOf<JsonObject>()
        for (ci in chunkStart..chunkEnd) {
            val chunk = inflatedContentChunk(ci)
            val firstBlock = index.chunkBlockStarts[ci]
            val blockCount = index.chunkBlockStarts[ci + 1] - firstBlock
            val localStart = maxOf(start - firstBlock, 0)
            val localEnd = minOf(end - firstBlock, blockCount - 1)
            if (localStart > localEnd) continue
            for (localBlockIndex in localStart..localEnd) {
                blocks.add(createBlock(blockData(chunk, blockCount, localBlockIndex)))
            }
        }
        return blocks
    }

    fun getPageBlocks(pageIndex: Int): List<JsonObject> {
        if (pageIndex < 0) return emptyList()
        val pages = getCatalog().get("pages") as? JsonArray ?: return emptyList()
        if (pageIndex >= pages.size()) return emptyList()
        val page = pages[pageIndex] as? JsonObject ?: return emptyList()
        val span = contentRangeBlockSpan(page.get("contentRange")) ?: return emptyList()
        if (span.first >= span.second) return emptyList()
        return getBlocks(span.first, span.second - 1)
    }

    private fun topLevelBlock(blockIndex: Int): JsonObject? {
        val resolvedChunkIndex = chunkIndex(blockIndex) ?: return null
        val chunk = inflatedContentChunk(resolvedChunkIndex)
        val firstBlock = index.chunkBlockStarts[resolvedChunkIndex]
        val blockCount = index.chunkBlockStarts[resolvedChunkIndex + 1] - firstBlock
        val localBlockIndex = blockIndex - firstBlock
        return createBlock(blockData(chunk, blockCount, localBlockIndex))
    }

    private fun contentRangeBlockSpan(value: JsonElement?): Pair<Int, Int>? {
        val range = value as? JsonArray ?: return null
        if (range.size() != 2) return null
        val start = contentBoundary(range[0]) ?: return null
        val end = contentBoundary(range[1]) ?: return null
        val startIndex = boundaryTopLevelIndex(start) ?: return null
        if (start == end) return startIndex to startIndex
        val endIndexExclusive = boundaryEndIndexExclusive(end) ?: return null
        return startIndex to maxOf(startIndex, endIndexExclusive)
    }

    private fun contentBoundary(value: JsonElement?): List<Int>? {
        val values = value as? JsonArray ?: return null
        if (values.size() == 0) return null
        val boundary = mutableListOf<Int>()
        for (v in values) {
            if (!v.isJsonPrimitive || !v.asJsonPrimitive.isNumber) return null
            val intValue = v.asInt
            if (intValue < 0) return null
            boundary.add(intValue)
        }
        return boundary
    }

    private fun boundaryTopLevelIndex(boundary: List<Int>): Int? {
        val idx = boundary.firstOrNull() ?: return null
        if (idx > getTopLevelBlockCount()) return null
        return idx
    }

    private fun boundaryEndIndexExclusive(boundary: List<Int>): Int? {
        val idx = boundaryTopLevelIndex(boundary) ?: return null
        val topLevelBlockCount = getTopLevelBlockCount()
        if (idx == topLevelBlockCount) return topLevelBlockCount
        return if (boundary.size == 1) idx else idx + 1
    }

    private fun readDictionaryJSON(start: Int, end: Int): JsonObject {
        val inflated = inflate(dataSlice(start, end))
        val json = try {
            JsonParser.parseString(String(inflated, Charsets.UTF_8))
        } catch (e: Exception) {
            throw Error.invalidJSON
        }
        return json as? JsonObject ?: throw Error.invalidJSON
    }

    private fun inflatedContentChunk(chunkIndex: Int): ByteArray {
        val contentStart =
            headerSize + header.indexLength + index.metadataLength + index.catalogLength
        val start = contentStart + index.chunkByteOffsets[chunkIndex]
        val end = contentStart + index.chunkByteOffsets[chunkIndex + 1]
        return inflate(dataSlice(start, end))
    }

    private fun createBlock(bytes: ByteArray): JsonObject {
        val json = try {
            JsonParser.parseString(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            throw Error.invalidJSON
        }
        return json as? JsonObject ?: throw Error.invalidJSON
    }

    private fun chunkIndex(blockIndex: Int): Int? {
        if (blockIndex < 0) return null
        for (ci in 0 until index.chunkBlockStarts.size - 1) {
            if (blockIndex >= index.chunkBlockStarts[ci] && blockIndex < index.chunkBlockStarts[ci + 1]) {
                return ci
            }
        }
        return null
    }

    private fun blockData(chunk: ByteArray, blockCount: Int, localBlockIndex: Int): ByteArray {
        if (blockCount < 0 || localBlockIndex < 0 || localBlockIndex >= blockCount) {
            throw Error.invalidContentChunk
        }
        val headerByteLength = blockCount * u32Size
        if (chunk.size < headerByteLength) throw Error.invalidContentChunk

        val start = readU32LE(chunk, localBlockIndex * u32Size)
        val end = if (localBlockIndex + 1 < blockCount) {
            readU32LE(chunk, (localBlockIndex + 1) * u32Size)
        } else {
            chunk.size - headerByteLength
        }
        if (start > end || headerByteLength + end > chunk.size) throw Error.invalidContentChunk
        return slice(chunk, headerByteLength + start, headerByteLength + end)
    }

    private fun dataSlice(start: Int, end: Int): ByteArray = slice(data, start, end)
}