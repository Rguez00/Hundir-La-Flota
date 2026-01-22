package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.Hello
import com.mario.hlf.protocol.ProtocolJson
import com.mario.hlf.protocol.Welcome
import org.junit.Test
import java.net.Socket
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class TcpGameServerRoomMatchTest {

    @Test
    fun `two HELLO clients get matched into same gameId`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5680, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        try {
            val gameId1 = Socket("127.0.0.1", 5680).use { s ->
                val env = Envelope(v = 1, requestId = "r1", gameId = null, payload = Hello("1.0", "P1"))
                val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
                Framing.writeFrame(s.getOutputStream(), json.toByteArray(Charsets.UTF_8))

                val resp = Framing.readFrame(s.getInputStream())!!
                val respEnv = ProtocolJson.decodeFromString(Envelope.serializer(), resp.toString(Charsets.UTF_8))
                assertTrue(respEnv.payload is Welcome)
                kotlin.test.assertNotNull(respEnv.gameId)
                respEnv.gameId
            }

            val gameId2 = Socket("127.0.0.1", 5680).use { s ->
                val env = Envelope(v = 1, requestId = "r2", gameId = null, payload = Hello("1.0", "P2"))
                val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
                Framing.writeFrame(s.getOutputStream(), json.toByteArray(Charsets.UTF_8))

                val resp = Framing.readFrame(s.getInputStream())!!
                val respEnv = ProtocolJson.decodeFromString(Envelope.serializer(), resp.toString(Charsets.UTF_8))
                assertTrue(respEnv.payload is Welcome)
                kotlin.test.assertNotNull(respEnv.gameId)
                respEnv.gameId
            }

            assertEquals(gameId1, gameId2)
        } finally {
            server.stop()
        }
    }

}
