package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpGameServerStartGameBroadcastTest {

    @Test
    fun `START_GAME broadcasts GAME_STATE to both players`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5681, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        try {
            val s1 = Socket("127.0.0.1", 5681)
            val s2 = Socket("127.0.0.1", 5681)

            val gameId1 = sendHelloAndReadGameId(s1, "P1")
            val gameId2 = sendHelloAndReadGameId(s2, "P2")
            assertNotNull(gameId1)
            assertNotNull(gameId2)
            assertEquals(gameId1, gameId2)

            // P1 manda START_GAME
            val start = Envelope(
                v = 1,
                requestId = "req-start",
                gameId = gameId1,
                payload = StartGame(boardSize = 10, allowAdjacency = false)
            )
            writeEnv(s1, start)

            // Ambos deben recibir GAME_STATE
            val e1 = readEnv(s1)
            val e2 = readEnv(s2)

            assertTrue(e1.payload is GameStateEvent)
            assertTrue(e2.payload is GameStateEvent)

            s1.close()
            s2.close()
        } finally {
            server.stop()
        }
    }

    private fun sendHelloAndReadGameId(socket: Socket, name: String): String? {
        val env = Envelope(v = 1, requestId = "hello-$name", gameId = null, payload = Hello("1.0", name))
        writeEnv(socket, env)
        val resp = readEnv(socket)
        assertTrue(resp.payload is Welcome)
        return resp.gameId
    }

    private fun writeEnv(socket: Socket, env: Envelope) {
        val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
        Framing.writeFrame(socket.getOutputStream(), json.toByteArray(Charsets.UTF_8))
    }

    private fun readEnv(socket: Socket): Envelope {
        val frame = Framing.readFrame(socket.getInputStream())!!
        val json = frame.toString(Charsets.UTF_8)
        return ProtocolJson.decodeFromString(Envelope.serializer(), json)
    }
}
