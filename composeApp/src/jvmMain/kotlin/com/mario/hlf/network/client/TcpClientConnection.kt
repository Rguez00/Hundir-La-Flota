package com.mario.hlf.network.client

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.ProtocolJson
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
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
        val frame = Framing.readFrame(input) ?: throw IllegalStateException("EOF while waiting for frame")
        val json = frame.toString(Charsets.UTF_8)
        return ProtocolJson.decodeFromString(Envelope.serializer(), json)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return

        // Cerramos streams primero para desbloquear lecturas/escrituras bloqueantes
        try { input.close() } catch (_: Throwable) {}
        try { output.close() } catch (_: Throwable) {}
        try { socket.close() } catch (_: Throwable) {}
    }
}
