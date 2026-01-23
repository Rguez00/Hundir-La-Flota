package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpGameServerShootOutOfTurnTest {

    @Test
    fun `P2 cannot SHOOT when it is not their turn`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5685, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        var s1: Socket? = null
        var s2: Socket? = null

        try {
            s1 = Socket("127.0.0.1", 5685)
            s2 = Socket("127.0.0.1", 5685)

            val gameId1 = hello(s1, "P1")
            val gameId2 = hello(s2, "P2")
            assertNotNull(gameId1)
            assertEquals(gameId1, gameId2)

            // START_GAME -> broadcast a ambos
            send(
                s1, Envelope(
                    v = 1,
                    requestId = "start",
                    gameId = gameId1,
                    payload = StartGame(boardSize = 10, allowAdjacency = false)
                )
            )
            readState(s1)
            readState(s2)

            // Completar placement para entrar en BATTLE (turno debe ser P1)
            placeAll(sender = s1, other = s2, gameId = gameId1, player = PlayerId.P1, startRow = 0)
            val last = placeAll(sender = s2, other = s1, gameId = gameId1, player = PlayerId.P2, startRow = 1)

            assertEquals(PhaseId.BATTLE, last.senderLast.phase)
            assertEquals(PhaseId.BATTLE, last.otherLast.phase)
            assertEquals(PlayerId.P1, last.senderLast.currentTurn)
            assertEquals(PlayerId.P1, last.otherLast.currentTurn)

            // P2 intenta disparar cuando NO es su turno -> debe recibir ERROR FORBIDDEN
            send(
                s2, Envelope(
                    v = 1,
                    requestId = "shoot-p2-out-of-turn",
                    gameId = gameId1,
                    payload = Shoot(
                        gameId = gameId1,
                        player = PlayerId.P2,
                        row = 0,
                        col = 0
                    )
                )
            )

            val respP2 = read(s2, 1500)
            assertTrue(respP2.payload is ErrorMsg)
            val err = respP2.payload as ErrorMsg
            assertEquals("FORBIDDEN", err.code)

            // Y MUY IMPORTANTE: NO debe haber broadcast a P1
            assertNoMessage(s1, 250)

        } finally {
            try { s1?.close() } catch (_: Throwable) {}
            try { s2?.close() } catch (_: Throwable) {}
            server.stop()
        }
    }

    // ---------- helpers (mismo patrón que PlacementToBattleTest) ----------

    private data class LastStates(
        val senderLast: GameStateDto,
        val otherLast: GameStateDto
    )

    private fun placeAll(
        sender: Socket,
        other: Socket,
        gameId: String,
        player: PlayerId,
        startRow: Int
    ): LastStates {
        val ships = listOf(
            ShipTypeId.CARRIER,
            ShipTypeId.BATTLESHIP,
            ShipTypeId.CRUISER,
            ShipTypeId.SUBMARINE,
            ShipTypeId.DESTROYER
        )

        var row = startRow
        var lastSender: GameStateDto? = null
        var lastOther: GameStateDto? = null

        for (ship in ships) {
            send(
                sender,
                Envelope(
                    v = 1,
                    requestId = "place-$player-$ship",
                    gameId = gameId,
                    payload = PlaceShip(
                        player = player,
                        gameId = gameId,
                        row = row,
                        col = 0,
                        ship = ship,
                        orientation = OrientationId.HORIZONTAL
                    )
                )
            )

            lastSender = readState(sender)
            lastOther = readState(other)

            row += 2
        }

        return LastStates(
            senderLast = requireNotNull(lastSender) { "No state read from sender" },
            otherLast = requireNotNull(lastOther) { "No state read from other" }
        )
    }

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

    private fun readState(socket: Socket): GameStateDto {
        val env = read(socket, 1500)
        return when (val p = env.payload) {
            is GameStateEvent -> p.state
            is ErrorMsg -> throw AssertionError("Server returned ErrorMsg: ${p.code} - ${p.message}")
            else -> throw AssertionError("Unexpected payload: ${p::class.simpleName}")
        }
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
            // OK: no llegó nada
        } finally {
            socket.soTimeout = prev
        }
    }
}
