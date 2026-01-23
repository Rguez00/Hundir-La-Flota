package com.mario.hlf.network.client

import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.Hello
import com.mario.hlf.protocol.Welcome
import com.mario.hlf.server.ServerConfig
import com.mario.hlf.server.TcpGameServer
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpClientHelloTest {

    @Test
    fun `client can HELLO and receive WELCOME with gameId`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5691, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        try {
            TcpClientConnection.connect("127.0.0.1", 5691).use { client ->
                client.setReadTimeout(1500)

                client.send(
                    Envelope(
                        v = 1,
                        requestId = "hello-test",
                        gameId = null,
                        payload = Hello(clientVersion = "1.0", playerName = "Client")
                    )
                )

                val resp = client.receive()
                assertTrue(resp.payload is Welcome)
                assertNotNull(resp.gameId)
            }
        } finally {
            server.stop()
        }
    }
}