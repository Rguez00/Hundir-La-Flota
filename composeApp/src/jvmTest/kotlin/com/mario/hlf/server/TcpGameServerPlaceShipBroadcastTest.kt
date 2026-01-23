package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpGameServerPlaceShipBroadcastTest {

    @Test
    fun `PLACE_SHIP broadcasts GAME_STATE to both players`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5682, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        try {
            val s1 = Socket("127.0.0.1", 5682)
            val s2 = Socket("127.0.0.1", 5682)

            val gameId1 = sendHelloAndGetGameId(s1, "P1")
            val gameId2 = sendHelloAndGetGameId(s2, "P2")
            assertNotNull(gameId1)
            assertEquals(gameId1, gameId2)

            // START_GAME por P1
            writeEnv(
                s1,
                Envelope(
                    v = 1,
                    requestId = "req-start",
                    gameId = gameId1,
                    payload = StartGame(boardSize = 10, allowAdjacency = false)
                )
            )

            // Ambos reciben GAME_STATE (fase PLACEMENT)
            val startE1 = readEnv(s1)
            val startE2 = readEnv(s2)
            assertTrue(startE1.payload is GameStateEvent)
            assertTrue(startE2.payload is GameStateEvent)

            // PLACE_SHIP P1 (row=0, col=0)
            writeEnv(
                s1,
                Envelope(
                    v = 1,
                    requestId = "req-place-p1",
                    gameId = gameId1,
                    payload = PlaceShip(
                        player = PlayerId.P1,
                        gameId = gameId1,
                        row = 0,
                        col = 0,
                        ship = ShipTypeId.DESTROYER,
                        orientation = OrientationId.HORIZONTAL
                    )
                )
            )

            // Tras PLACE_SHIP, ambos reciben GAME_STATE
            val p1E1 = readEnv(s1)
            val p1E2 = readEnv(s2)
            assertTrue(p1E1.payload is GameStateEvent)
            assertTrue(p1E2.payload is GameStateEvent)

            // PLACE_SHIP P2 (row=2, col=0) para no colisionar
            writeEnv(
                s2,
                Envelope(
                    v = 1,
                    requestId = "req-place-p2",
                    gameId = gameId1,
                    payload = PlaceShip(
                        player = PlayerId.P2,
                        gameId = gameId1,
                        row = 2,
                        col = 0,
                        ship = ShipTypeId.DESTROYER,
                        orientation = OrientationId.HORIZONTAL
                    )
                )
            )

            val p2E1 = readEnv(s1)
            val p2E2 = readEnv(s2)
            assertTrue(p2E1.payload is GameStateEvent)
            assertTrue(p2E2.payload is GameStateEvent)

            s1.close()
            s2.close()
        } finally {
            server.stop()
        }
    }

    private fun sendHelloAndGetGameId(socket: Socket, name: String): String? {
        writeEnv(
            socket,
            Envelope(
                v = 1,
                requestId = "hello-$name",
                gameId = null,
                payload = Hello(clientVersion = "1.0", playerName = name)
            )
        )
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
