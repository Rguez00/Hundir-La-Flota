package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpGameServerPlacementToBattleTest {

    @Test
    fun `when both players place full fleet game transitions to BATTLE`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5683, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        try {
            val s1 = Socket("127.0.0.1", 5683)
            val s2 = Socket("127.0.0.1", 5683)

            val gameId1 = hello(s1, "P1")
            val gameId2 = hello(s2, "P2")
            assertNotNull(gameId1)
            assertEquals(gameId1, gameId2)

            // START_GAME -> broadcast a ambos
            send(
                s1,
                Envelope(
                    v = 1,
                    requestId = "start",
                    gameId = gameId1,
                    payload = StartGame(boardSize = 10, allowAdjacency = false)
                )
            )
            readState(s1)
            readState(s2)

            // P1 coloca toda la flota (cada place -> broadcast a ambos)
            placeAll(sender = s1, other = s2, gameId = gameId1, player = PlayerId.P1, startRow = 0)

            // P2 coloca toda la flota (cada place -> broadcast a ambos)
            // OJO: aquí se produce la transición a BATTLE al final de la última colocación.
            val last = placeAll(sender = s2, other = s1, gameId = gameId1, player = PlayerId.P2, startRow = 1)

            val lastS2 = last.senderLast
            val lastS1 = last.otherLast

            assertEquals(PhaseId.BATTLE, lastS1.phase)
            assertEquals(PhaseId.BATTLE, lastS2.phase)
            assertEquals(PlayerId.P1, lastS1.currentTurn)
            assertEquals(PlayerId.P1, lastS2.currentTurn)

            s1.close()
            s2.close()
        } finally {
            server.stop()
        }
    }

    // ---------- helpers ----------

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

            // broadcast a ambos -> leemos estado en ambos sockets
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
}
