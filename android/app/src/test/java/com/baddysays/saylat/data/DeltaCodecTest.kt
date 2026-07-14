package com.baddysays.saylat.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class DeltaCodecTest {

    @Test
    fun applyDelta_copyAndInsert_roundtrip() {
        val old = "Hello World".toByteArray(Charsets.UTF_8)
        val new = "Hello World!".toByteArray(Charsets.UTF_8)

        val delta = buildDelta(
            oldData = old,
            newData = new,
            commands = {
                write(0x01) // COPY
                writeVarint(0)
                writeVarint(old.size)
                write(0x02) // INSERT
                writeVarint(1)
                write('!'.code)
            },
        )

        val restored = DeltaCodec.applyDelta(old, delta)
        assertArrayEquals(new, restored)
        assertEquals(DeltaCodec.computeEtag(new), DeltaCodec.computeEtag(restored))
    }

    @Test
    fun applyDelta_insertOnlyFromEmptyBase() {
        val new = "abc".toByteArray(Charsets.UTF_8)
        val delta = buildDelta(
            oldData = ByteArray(0),
            newData = new,
            commands = {
                write(0x02) // INSERT
                writeVarint(3)
                write('a'.code)
                write('b'.code)
                write('c'.code)
            },
        )

        val restored = DeltaCodec.applyDelta(ByteArray(0), delta)
        assertArrayEquals(new, restored)
    }

    @Test
    fun computeEtag_isMd5Hex() {
        val data = "hi".toByteArray(Charsets.UTF_8)
        assertEquals(32, DeltaCodec.computeEtag(data).length)
        assertEquals(DeltaCodec.computeEtag(data), DeltaCodec.computeEtag(data))
    }

    private fun buildDelta(
        oldData: ByteArray,
        newData: ByteArray,
        commands: ByteArrayOutputStream.() -> Unit,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write('D'.code)
        out.write('L'.code)
        out.write(0x01)
        out.write(etagRaw(DeltaCodec.computeEtag(oldData)))
        out.write(etagRaw(DeltaCodec.computeEtag(newData)))
        out.commands()
        return out.toByteArray()
    }

    private fun etagRaw(hex: String): ByteArray {
        val bytes = ByteArray(16)
        for (i in 0 until 16) {
            bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return bytes
    }

    private fun ByteArrayOutputStream.writeVarint(value: Int) {
        var v = value
        while (v > 0x7F) {
            write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        write(v and 0x7F)
    }
}
