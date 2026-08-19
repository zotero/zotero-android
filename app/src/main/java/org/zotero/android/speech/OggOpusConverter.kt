package org.zotero.android.speech

import java.io.ByteArrayOutputStream
import kotlin.math.abs

object OggOpusConverter {
    sealed class Error : Exception() {
        object invalidOpusHeader : Error()
    }

    data class OpusInfo(
        val channelCount: Int,
        val sampleRate: Int,
    )

    private val oggMagicBytes = byteArrayOf(0x4F, 0x67, 0x67, 0x53)

    private val opusHeadMagic = byteArrayOf(0x4F, 0x70, 0x75, 0x73, 0x48, 0x65, 0x61, 0x64)

    fun isOggFormat(data: ByteArray): Boolean {
        if (data.size < 4) return false
        return data.copyOfRange(0, 4).contentEquals(oggMagicBytes)
    }

    fun parseOpusInfo(data: ByteArray): OpusInfo? {
        if (data.size < 47) return null

        val searchRange = minOf(100, data.size - 19)
        for (i in 0 until searchRange) {
            val end = minOf(i + 8, data.size)
            val slice = data.copyOfRange(i, end)
            if (slice.contentEquals(opusHeadMagic)) {
                val headerStart = i
                if (data.size < headerStart + 16) return null

                val channelCount = data[headerStart + 9].toInt() and 0xFF
                val sampleRate = (data[headerStart + 12].toInt() and 0xFF) or
                    ((data[headerStart + 13].toInt() and 0xFF) shl 8) or
                    ((data[headerStart + 14].toInt() and 0xFF) shl 16) or
                    ((data[headerStart + 15].toInt() and 0xFF) shl 24)

                return OpusInfo(channelCount = channelCount, sampleRate = sampleRate)
            }
        }
        return null
    }

    private val validOpusSampleRates = intArrayOf(8000, 12000, 16000, 24000, 48000)

    private fun closestValidOpusSampleRate(inputRate: Int): Int {
        return validOpusSampleRates.minByOrNull { abs(it - inputRate) } ?: 48000
    }

    fun convertToPlayableFormat(data: ByteArray): ByteArray {
        if (!isOggFormat(data)) {
            return data
        }
        return convertOggOpusToWav(data)
    }

    private fun convertOggOpusToWav(oggData: ByteArray): ByteArray {
        val opusInfo = parseOpusInfo(oggData) ?: throw Error.invalidOpusHeader

        val pcmFloatData = decodeOpusToPcmFloat(oggData)

        val int16Data = ByteArrayOutputStream(pcmFloatData.size * 2)
        for (value in pcmFloatData) {
            val clamped = value.coerceIn(-1.0f, 1.0f)
            val int16Value = (clamped * Short.MAX_VALUE).toInt().toShort()
            int16Data.writeLittleEndianShort(int16Value.toInt())
        }
        val int16Bytes = int16Data.toByteArray()

        val sampleRate = closestValidOpusSampleRate(opusInfo.sampleRate)
        val numChannels = opusInfo.channelCount
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val blockAlign = numChannels * (bitsPerSample / 8)
        val dataSize = int16Bytes.size
        val fileSize = 36 + dataSize

        val wavData = ByteArrayOutputStream()

        wavData.write("RIFF".toByteArray(Charsets.US_ASCII))
        wavData.writeLittleEndianInt(fileSize)
        wavData.write("WAVE".toByteArray(Charsets.US_ASCII))

        wavData.write("fmt ".toByteArray(Charsets.US_ASCII))
        wavData.writeLittleEndianInt(16)
        wavData.writeLittleEndianShort(1)
        wavData.writeLittleEndianShort(numChannels)
        wavData.writeLittleEndianInt(sampleRate)
        wavData.writeLittleEndianInt(byteRate)
        wavData.writeLittleEndianShort(blockAlign)
        wavData.writeLittleEndianShort(bitsPerSample)

        wavData.write("data".toByteArray(Charsets.US_ASCII))
        wavData.writeLittleEndianInt(dataSize)
        wavData.write(int16Bytes)

        return wavData.toByteArray()
    }

    private fun decodeOpusToPcmFloat(oggData: ByteArray): FloatArray {
        throw UnsupportedOperationException("Opus decoding is not implemented in this port")
    }
}

private fun ByteArrayOutputStream.writeLittleEndianInt(value: Int) {
    write(value and 0xFF)
    write((value ushr 8) and 0xFF)
    write((value ushr 16) and 0xFF)
    write((value ushr 24) and 0xFF)
}

private fun ByteArrayOutputStream.writeLittleEndianShort(value: Int) {
    write(value and 0xFF)
    write((value ushr 8) and 0xFF)
}
