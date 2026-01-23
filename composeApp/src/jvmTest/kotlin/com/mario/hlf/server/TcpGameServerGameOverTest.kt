package com.mario.hlf.server

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import org.junit.Test
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TcpGameServerGameOverTest {

    @Test
    fun `when P1 sinks all P2 ships server broadcasts GAME_OVER to both`() {
        val config = ServerConfig(host = "127.0.0.1", port = 5690, maxClients = 10)
        val server = TcpGameServer(config)
        server.start()

        var s1: Socket? = null
        var s2: Socket? = null

        try {
            s1 = Socket("127.0.0.1", 5690)
            s2 = Socket("127.0.0.1", 5690)

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

            // P1 coloca toda la flota (broadcasts)
            placeAll(sender = s1, other = s2, gameId = gameId1, player = PlayerId.P1, startRow = 0)

            // P2 coloca toda la flota (última coloca -> transición a BATTLE, turno P1)
            val last = placeAll(sender = s2, other = s1, gameId = gameId1, player = PlayerId.P2, startRow = 1)
            assertEquals(PhaseId.BATTLE, last.senderLast.phase)
            assertEquals(PhaseId.BATTLE, last.otherLast.phase)
            assertEquals(PlayerId.P1, last.senderLast.currentTurn)
            assertEquals(PlayerId.P1, last.otherLast.currentTurn)

            // Disparos deterministas: todas las celdas de la flota de P2 según nuestro helper placeAll (col=0, horizontal)
            val targets = listOf(
                // CARRIER len=5 en row=1 col 0..4
                Coordinate(1, 0), Coordinate(1, 1), Coordinate(1, 2), Coordinate(1, 3), Coordinate(1, 4),
                // BATTLESHIP len=4 en row=3 col 0..3
                Coordinate(3, 0), Coordinate(3, 1), Coordinate(3, 2), Coordinate(3, 3),
                // CRUISER len=3 en row=5 col 0..2
                Coordinate(5, 0), Coordinate(5, 1), Coordinate(5, 2),
                // SUBMARINE len=3 en row=7 col 0..2
                Coordinate(7, 0), Coordinate(7, 1), Coordinate(7, 2),
                // DESTROYER len=2 en row=9 col 0..1
                Coordinate(9, 0), Coordinate(9, 1),
            )

            // Para que P1 pueda disparar muchas veces seguidas (ya que vuestro Game cambia turno siempre),
            // hacemos que P2 dispare un MISS "dummy" después de cada disparo de P1 para devolver el turno a P1.
            // (Si cambiasteis reglas a "si aciertas repites turno", este loop sigue funcionando igualmente.)
            for ((i, t) in targets.withIndex()) {
                // P1 dispara
                send(
                    s1, Envelope(
                        v = 1,
                        requestId = "p1-shot-$i",
                        gameId = gameId1,
                        payload = Shoot(gameId = gameId1, player = PlayerId.P1, row = t.row, col = t.col)
                    )
                )

                val p1After = readState(s1)
                val p2After = readState(s2)

                // Cuando sea el último disparo (hundes toda la flota), debe llegar GAME_OVER a ambos.
                if (i == targets.lastIndex) {
                    val e1 = read(s1, 1500)
                    val e2 = read(s2, 1500)

                    assertTrue(e1.payload is GameOverEvent, "Expected GAME_OVER for P1")
                    assertTrue(e2.payload is GameOverEvent, "Expected GAME_OVER for P2")

                    val go1 = e1.payload as GameOverEvent
                    val go2 = e2.payload as GameOverEvent

                    assertEquals(PlayerId.P1, go1.winner)
                    assertEquals(PlayerId.P1, go2.winner)
                    return
                }

                // Si aún no es el final, debe tocarle a P2 (por vuestra regla actual).
                // Y hacemos un miss seguro (0,9) que no pisa nuestros barcos.
                assertEquals(PlayerId.P2, p1After.currentTurn)
                assertEquals(PlayerId.P2, p2After.currentTurn)

                send(
                    s2, Envelope(
                        v = 1,
                        requestId = "p2-dummy-miss-$i",
                        gameId = gameId1,
                        payload = Shoot(gameId = gameId1, player = PlayerId.P2, row = 0, col = 9)
                    )
                )

                // Broadcast del dummy shot
                readState(s1)
                val p2DummyState = readState(s2)

                // Debe volver el turno a P1
                assertEquals(PlayerId.P1, p2DummyState.currentTurn)
            }

            throw AssertionError("Expected to receive GAME_OVER but loop finished")

        } finally {
            try { s1?.close() } catch (_: Throwable) {}
            try { s2?.close() } catch (_: Throwable) {}
            server.stop()
        }
    }

    // -------- helpers --------

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
}
