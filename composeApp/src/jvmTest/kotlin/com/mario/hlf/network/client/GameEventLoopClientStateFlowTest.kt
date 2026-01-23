package com.mario.hlf.network.client

import com.mario.hlf.protocol.PhaseId
import com.mario.hlf.server.ServerConfig
import com.mario.hlf.server.TcpGameServer
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameEventLoopClientStateFlowTest {

    @Test
    fun `event loop updates latestState when GAME_STATE arrives`() = runBlocking {
        val config = ServerConfig(host = "127.0.0.1", port = 5693, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        // ✅ Scope propio (NO es hijo de runBlocking)
        val loopScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        var loop1: GameEventLoopClient? = null
        var loop2: GameEventLoopClient? = null

        try {
            TcpClientConnection.connect("127.0.0.1", 5693).use { c1 ->
                TcpClientConnection.connect("127.0.0.1", 5693).use { c2 ->
                    c1.setReadTimeout(0)
                    c2.setReadTimeout(0)

                    val api1 = GameTcpClient(c1)
                    val api2 = GameTcpClient(c2)

                    api1.hello(playerName = "P1")
                    api2.hello(playerName = "P2")

                    loop1 = GameEventLoopClient(api1, loopScope).also { it.start() }
                    loop2 = GameEventLoopClient(api2, loopScope).also { it.start() }

                    api1.sendStartGame(boardSize = 10, allowAdjacency = false)

                    withTimeout(1500) {
                        while (loop1?.latestState?.value == null || loop2?.latestState?.value == null) {
                            delay(10)
                        }
                    }

                    val s1 = loop1?.latestState?.value
                    val s2 = loop2?.latestState?.value
                    assertNotNull(s1)
                    assertNotNull(s2)

                    assertEquals(PhaseId.PLACEMENT, s1.phase)
                    assertEquals(PhaseId.PLACEMENT, s2.phase)
                }
            }
        } finally {
            // ✅ parar loops primero (cierran socket y salen)
            loop1?.stop()
            loop2?.stop()

            // ✅ cerrar scope propio
            loopScope.cancel()

            // ✅ parar server (ojo: si lo dejaste suspend, aquí estamos en runBlocking, OK)
            server.stop()
        }
    }
}
