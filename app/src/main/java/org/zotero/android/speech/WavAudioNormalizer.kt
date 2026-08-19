package org.zotero.android.speech

import kotlin.math.abs
import kotlin.math.pow

object WavAudioNormalizer {

    fun normalize(data: ByteArray, targetDB: Float = -1.0f): ByteArray {
        if (data.size <= 44 ||
            !data.copyOfRange(0, 4).contentEquals("RIFF".toByteArray(Charsets.US_ASCII)) ||
            !data.copyOfRange(8, 12).contentEquals("WAVE".toByteArray(Charsets.US_ASCII))
        ) {
            return data
        }

        var offset = 12
        var dataChunkOffset = -1
        var dataChunkSize = 0
        while (offset + 8 <= data.size) {
            val chunkId = String(data, offset, 4, Charsets.US_ASCII)
            val chunkSize = (data[offset + 4].toInt() and 0xFF) or
                ((data[offset + 5].toInt() and 0xFF) shl 8) or
                ((data[offset + 6].toInt() and 0xFF) shl 16) or
                ((data[offset + 7].toInt() and 0xFF) shl 24)
            if (chunkId == "data") {
                dataChunkOffset = offset + 8
                dataChunkSize = chunkSize
                break
            }
            offset += 8 + chunkSize
        }

        if (dataChunkOffset < 0 || dataChunkOffset + dataChunkSize > data.size) return data

        val bitsPerSample = (data[34].toInt() and 0xFF) or ((data[35].toInt() and 0xFF) shl 8)
        if (bitsPerSample != 16) return data

        val sampleCount = dataChunkSize / 2
        if (sampleCount <= 0) return data

        var peak = 0f
        for (i in 0 until sampleCount) {
            val sample = readInt16LE(data, dataChunkOffset + i * 2)
            val absolute = abs(sample.toFloat())
            if (absolute > peak) {
                peak = absolute
            }
        }

        if (peak <= 0f) return data

        val targetPeak = 10f.pow(targetDB / 20f) * Short.MAX_VALUE
        val gain = targetPeak / peak

        val normalized = data.copyOf()
        for (i in 0 until sampleCount) {
            val byteOffset = dataChunkOffset + i * 2
            val sample = readInt16LE(normalized, byteOffset)
            val scaled = sample.toFloat() * gain
            val clamped = scaled.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
            writeInt16LE(normalized, byteOffset, clamped.toInt().toShort())
        }

        return normalized
    }

    private fun readInt16LE(data: ByteArray, offset: Int): Short {
        val lo = data[offset].toInt() and 0xFF
        val hi = data[offset + 1].toInt() and 0xFF
        return ((hi shl 8) or lo).toShort()
    }

    private fun writeInt16LE(data: ByteArray, offset: Int, value: Short) {
        data[offset] = (value.toInt() and 0xFF).toByte()
        data[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }
}
