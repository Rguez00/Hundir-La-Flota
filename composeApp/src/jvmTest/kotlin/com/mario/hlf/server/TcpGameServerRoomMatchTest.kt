package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.Hello
import com.mario.hlf.protocol.ProtocolJson
import com.mario.hlf.protocol.Welcome
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpGameServerRoomMatchTest {

    @Test
    fun `two HELLO clients get matched into same gameId`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5680, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        var s1: Socket? = null
        var s2: Socket? = null

        try {
            s1 = Socket("127.0.0.1", 5680)
            s2 = Socket("127.0.0.1", 5680)

            val gameId1 = sendHelloAndGetGameId(s1, "r1", "P1")
            val gameId2 = sendHelloAndGetGameId(s2, "r2", "P2")

            assertEquals(gameId1, gameId2)
        } finally {
            try { s1?.close() } catch (_: Throwable) {}
            try { s2?.close() } catch (_: Throwable) {}
            server.stop()
        }
    }

    private fun sendHelloAndGetGameId(s: Socket, reqId: String, name: String): String {
        val env = Envelope(v = 1, requestId = reqId, gameId = null, payload = Hello("1.0", name))
        val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
        Framing.writeFrame(s.getOutputStream(), json.toByteArray(Charsets.UTF_8))

        val resp = Framing.readFrame(s.getInputStream())!!
        val respEnv = ProtocolJson.decodeFromString(Envelope.serializer(), resp.toString(Charsets.UTF_8))
        assertTrue(respEnv.payload is Welcome)
        val gid = respEnv.gameId
        assertNotNull(gid)
        return gid
    }
}
