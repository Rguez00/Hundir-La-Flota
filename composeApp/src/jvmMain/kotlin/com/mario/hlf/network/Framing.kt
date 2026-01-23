package com.mario.hlf.network

import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/**
 * Framing binario: [len:Int32 big-endian] + [payload bytes]
 * - readFrame() devuelve null si el stream se cierra limpiamente.
 */
object Framing {
    private const val MAX_FRAME_BYTES = 1024 * 1024 // 1MB para evitar OOM

    fun readFrame(input: InputStream): ByteArray? {
        val lenBuf = ByteArray(4)

        val n = input.read(lenBuf)
        if (n == -1) return null

        if (n < 4) {
            try {
                readFully(input, lenBuf, n, 4)
            } catch (_: EOFException) {
                return null
            }
        }

        val len = ((lenBuf[0].toInt() and 0xFF) shl 24) or
                ((lenBuf[1].toInt() and 0xFF) shl 16) or
                ((lenBuf[2].toInt() and 0xFF) shl 8) or
                (lenBuf[3].toInt() and 0xFF)

        require(len in 0..MAX_FRAME_BYTES) { "Invalid frame length: $len" }

        val payload = ByteArray(len)
        try {
            readFully(input, payload, 0, len)
        } catch (_: EOFException) {
            return null
        }
        return payload
    }


    fun writeFrame(output: OutputStream, payload: ByteArray) {
        val len = payload.size
        require(len <= MAX_FRAME_BYTES) { "Frame too large: $len" }

        val lenBuf = byteArrayOf(
            ((len ushr 24) and 0xFF).toByte(),
            ((len ushr 16) and 0xFF).toByte(),
            ((len ushr 8) and 0xFF).toByte(),
            (len and 0xFF).toByte(),
        )

        output.write(lenBuf)
        output.write(payload)
        output.flush()
    }

    private fun readFully(input: InputStream, buf: ByteArray, off: Int, len: Int) {
        var read = 0
        while (read < len) {
            val r = input.read(buf, off + read, len - read)
            if (r == -1) throw EOFException("Unexpected EOF while reading frame")
            read += r
        }
    }
}
