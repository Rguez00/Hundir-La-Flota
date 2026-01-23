package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpGameServerStartGameForbiddenTest {

    @Test
    fun `P2 cannot START_GAME (FORBIDDEN) and no broadcast`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5687, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        var s1: Socket? = null
        var s2: Socket? = null

        try {
            s1 = Socket("127.0.0.1", 5687)
            s2 = Socket("127.0.0.1", 5687)

            val gameId1 = hello(s1, "P1")
            val gameId2 = hello(s2, "P2")
            assertNotNull(gameId1)
            assertEquals(gameId1, gameId2)

            // P2 intenta START_GAME
            send(
                s2, Envelope(
                    v = 1,
                    requestId = "start-by-p2",
                    gameId = gameId1,
                    payload = StartGame(boardSize = 10, allowAdjacency = false)
                )
            )

            val resp = read(s2, 1500)
            assertTrue(resp.payload is ErrorMsg)
            val err = resp.payload as ErrorMsg
            assertEquals("FORBIDDEN", err.code)

            // no broadcast a P1
            assertNoMessage(s1, 250)

        } finally {
            try { s1?.close() } catch (_: Throwable) {}
            try { s2?.close() } catch (_: Throwable) {}
            server.stop()
        }
    }

    // helpers
    private fun hello(socket: Socket, name: String): String? {
        send(
            socket,
            Envelope(
                v = 1,
                requestId = "hello-$name",
                gameId = null,
                payload = Hello(clientVersion = "1.0", playerName = name)
            )
        )
        val resp = read(socket)
        assertTrue(resp.payload is Welcome)
        return resp.gameId
    }

    private fun send(socket: Socket, env: Envelope) {
        val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
        Framing.writeFrame(socket.getOutputStream(), json.toByteArray(Charsets.UTF_8))
    }

    private fun read(socket: Socket, timeoutMs: Int = 1500): Envelope {
        val prev = socket.soTimeout
        socket.soTimeout = timeoutMs
        try {
            val frame = Framing.readFrame(socket.getInputStream())
                ?: throw AssertionError("EOF while waiting for frame")
            val json = frame.toString(Charsets.UTF_8)
            return ProtocolJson.decodeFromString(Envelope.serializer(), json)
        } catch (e: java.net.SocketTimeoutException) {
            throw AssertionError("Timeout waiting for server message (${timeoutMs}ms)")
        } finally {
            socket.soTimeout = prev
        }
    }

    private fun assertNoMessage(socket: Socket, timeoutMs: Int) {
        val prev = socket.soTimeout
        socket.soTimeout = timeoutMs
        try {
            val frame = Framing.readFrame(socket.getInputStream())
            if (frame != null) {
                val json = frame.toString(Charsets.UTF_8)
                val env = ProtocolJson.decodeFromString(Envelope.serializer(), json)
                throw AssertionError("Expected no message, but received: ${env.payload::class.simpleName}")
            }
        } catch (_: java.net.SocketTimeoutException) {
            // OK
        } finally {
            socket.soTimeout = prev
        }
    }
}
