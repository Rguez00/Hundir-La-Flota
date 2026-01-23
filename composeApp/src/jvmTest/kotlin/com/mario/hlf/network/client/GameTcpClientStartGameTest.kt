package com.mario.hlf.network.client

import com.mario.hlf.protocol.PhaseId
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.server.ServerConfig
import com.mario.hlf.server.TcpGameServer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameTcpClientStartGameTest {

    @Test
    fun `client hello then startGame receives GAME_STATE placement`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5692, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        try {
            TcpClientConnection.connect("127.0.0.1", 5692).use { c1 ->
                TcpClientConnection.connect("127.0.0.1", 5692).use { c2 ->
                    c1.setReadTimeout(1500)
                    c2.setReadTimeout(1500)

                    val p1 = GameTcpClient(c1)
                    val p2 = GameTcpClient(c2)

                    p1.hello(playerName = "P1")
                    p2.hello(playerName = "P2")

                    assertNotNull(p1.gameId)
                    assertEquals(p1.gameId, p2.gameId)

                    val s1 = p1.startGame().state
                    val s2 = p2.receiveEnvelope().payload as com.mario.hlf.protocol.GameStateEvent

                    assertEquals(PhaseId.PLACEMENT, s1.phase)
                    assertEquals(PhaseId.PLACEMENT, s2.state.phase)
                    assertEquals(PlayerId.P1, s1.currentTurn)
                }
            }
        } finally {
            server.stop()
        }
    }
}
