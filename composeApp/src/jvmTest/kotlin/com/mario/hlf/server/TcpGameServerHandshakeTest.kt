package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.Hello
import com.mario.hlf.protocol.ProtocolJson
import com.mario.hlf.protocol.Welcome
import org.junit.Test
import java.net.Socket
import kotlin.test.assertTrue

class TcpGameServerHandshakeTest {

    @Test
    fun `HELLO returns WELCOME`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5679, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        try {
            Socket("127.0.0.1", 5679).use { socket ->
                val out = socket.getOutputStream()
                val input = socket.getInputStream()

                val hello = Hello(clientVersion = "1.0", playerName = "Test")
                val env = Envelope(v = 1, requestId = "r1", gameId = null, payload = hello)
                val json = ProtocolJson.encodeToString(Envelope.serializer(), env)

                Framing.writeFrame(out, json.toByteArray(Charsets.UTF_8))

                val respFrame = Framing.readFrame(input)!!
                val respJson = respFrame.toString(Charsets.UTF_8)
                val respEnv = ProtocolJson.decodeFromString(Envelope.serializer(), respJson)

                assertTrue(respEnv.payload is Welcome)
            }
        } finally {
            server.stop()
        }
    }
}
