package com.baddysays.saylat.data

import java.security.MessageDigest

/** Применение бинарной дельты (сервер delta_codec.py). */
object DeltaCodec {
    private val DELTA_MAGIC = byteArrayOf('D'.code.toByte(), 'L'.code.toByte())
    private const val DELTA_VERSION = 0x01
    private const val CMD_COPY = 0x01
    private const val CMD_INSERT = 0x02
    private const val HEADER_SIZE = 35
    private const val MAX_RESTORED_BYTES = 20 * 1024 * 1024

    fun applyDelta(oldData: ByteArray, delta: ByteArray): ByteArray {
        require(delta.size >= HEADER_SIZE) { "Delta too short" }
        require(delta[0] == DELTA_MAGIC[0] && delta[1] == DELTA_MAGIC[1]) { "Invalid delta magic" }
        require(delta[2].toInt() and 0xFF == DELTA_VERSION) { "Unsupported delta version" }

        val baseEtagRaw = delta.copyOfRange(3, 19)
        val newEtagRaw = delta.copyOfRange(19, 35)
        if (oldData.isNotEmpty()) {
            val oldEtag = etagToRaw(computeEtag(oldData))
            require(oldEtag.contentEquals(baseEtagRaw)) { "Base ETag mismatch" }
        }

        val result = ArrayList<Byte>()
        var offset = HEADER_SIZE
        while (offset < delta.size) {
            when (delta[offset].toInt() and 0xFF) {
                CMD_COPY -> {
                    offset++
                    val (copyOffset, o1) = readVarint(delta, offset)
                    offset = o1
                    val (length, o2) = readVarint(delta, offset)
                    offset = o2
                    require(copyOffset >= 0 && length >= 0) { "Invalid COPY bounds" }
                    require(copyOffset + length <= oldData.size) { "COPY out of range" }
                    require(result.size + length <= MAX_RESTORED_BYTES) { "Restored payload too large" }
                    result.addAll(oldData.copyOfRange(copyOffset, copyOffset + length).toList())
                }
                CMD_INSERT -> {
                    offset++
                    val (length, o1) = readVarint(delta, offset)
                    offset = o1
                    require(length >= 0 && offset + length <= delta.size) { "INSERT out of range" }
                    require(result.size + length <= MAX_RESTORED_BYTES) { "Restored payload too large" }
                    result.addAll(delta.copyOfRange(offset, offset + length).toList())
                    offset += length
                }
                else -> error("Unknown delta command at $offset")
            }
        }

        val restored = result.toByteArray()
        val restoredEtag = etagToRaw(computeEtag(restored))
        require(restoredEtag.contentEquals(newEtagRaw)) { "Restored ETag mismatch" }
        return restored
    }

    fun computeEtag(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun etagToRaw(hex: String): ByteArray {
        val bytes = ByteArray(16)
        for (i in 0 until 16) {
            bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }

    private fun readVarint(data: ByteArray, start: Int): Pair<Int, Int> {
        var result = 0
        var shift = 0
        var offset = start
        while (offset < data.size) {
            val byte = data[offset++].toInt() and 0xFF
            result = result or (byte and 0x7F shl shift)
            if (byte and 0x80 == 0) return result to offset
            shift += 7
        }
        error("Truncated varint")
    }
}
