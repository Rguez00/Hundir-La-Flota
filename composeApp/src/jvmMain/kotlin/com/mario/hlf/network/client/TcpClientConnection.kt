package com.mario.hlf.network.client

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.ProtocolJson
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TcpClientConnection private constructor(
    private val socket: Socket,
    private val input: InputStream,
    private val output: OutputStream,
) : Closeable {

    private val writeLock = ReentrantLock()
    private val closed = AtomicBoolean(false)

    // Debug: detecta doble lector
    private val reading = AtomicBoolean(false)

    companion object {
        fun connect(host: String, port: Int, connectTimeoutMs: Int = 1500): TcpClientConnection {
            val s = Socket()
            s.tcpNoDelay = true
            s.keepAlive = true
            s.connect(InetSocketAddress(host, port), connectTimeoutMs)

            return TcpClientConnection(
                socket = s,
                input = s.getInputStream(),
                output = s.getOutputStream()
            )
        }
    }

    fun setReadTimeout(timeoutMs: Int) {
        socket.soTimeout = timeoutMs
    }

    fun send(env: Envelope) {
        val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
        val bytes = json.toByteArray(Charsets.UTF_8)

        writeLock.withLock {
            Framing.writeFrame(output, bytes)
        }
    }

    fun receive(): Envelope {
        check(reading.compareAndSet(false, true)) {
            "Multiple concurrent receive() calls detected. Only one reader per socket is allowed."
        }

        try {
            val frame = Framing.readFrame(input)
                ?: throw IllegalStateException("EOF while waiting for frame")

            val json = frame.toString(Charsets.UTF_8)

            return ProtocolJson.decodeFromString(Envelope.serializer(), json)
        } finally {
            reading.set(false)
        }
    }

    fun receiveWithTimeoutForHandshake(timeoutMs: Int): Envelope {
        val prev = socket.soTimeout
        return try {
            socket.soTimeout = timeoutMs
            receive()
        } catch (t: SocketTimeoutException) {
            throw IllegalStateException("Timeout waiting for server response ($timeoutMs ms)", t)
        } finally {
            socket.soTimeout = prev
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return

        // ✅ shutdown ayuda muchísimo a desbloquear reads en Windows
        runCatching { socket.shutdownInput() }
        runCatching { socket.shutdownOutput() }
        runCatching { socket.close() }

        // Streams por si acaso (pueden ya estar cerrados)
        runCatching { input.close() }
        runCatching { output.close() }
    }
}
