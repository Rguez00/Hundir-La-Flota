package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpGameServerShootAnticheatTest {

    @Test
    fun `SHOOT with wrong player id returns FORBIDDEN`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5689, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        var s1: Socket? = null
        var s2: Socket? = null

        try {
            s1 = Socket("127.0.0.1", 5689)
            s2 = Socket("127.0.0.1", 5689)

            val gameId1 = hello(s1, "P1")
            val gameId2 = hello(s2, "P2")
            assertNotNull(gameId1)
            assertEquals(gameId1, gameId2)

            // START_GAME
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

            // Completar placement a BATTLE
            placeAll(sender = s1, other = s2, gameId = gameId1, player = PlayerId.P1, startRow = 0)
            placeAll(sender = s2, other = s1, gameId = gameId1, player = PlayerId.P2, startRow = 1)

            // P2 intenta disparar diciendo que es P1 (mal)
            send(
                s2, Envelope(
                    v = 1,
                    requestId = "shoot-wrong-player",
                    gameId = gameId1,
                    payload = Shoot(
                        gameId = gameId1,
                        player = PlayerId.P1, // MAL: sesión P2
                        row = 0,
                        col = 0
                    )
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

    // helpers (con placeAll)
    private data class LastStates(val senderLast: GameStateDto, val otherLast: GameStateDto)

    private fun placeAll(sender: Socket, other: Socket, gameId: String, player: PlayerId, startRow: Int): LastStates {
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
            senderLast = requireNotNull(lastSender),
            otherLast = requireNotNull(lastOther)
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
            // OK
        } finally {
            socket.soTimeout = prev
        }
    }
}
