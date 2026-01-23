package com.mario.hlf.network.client

import com.mario.hlf.protocol.*
import java.io.Closeable
import java.util.UUID

class GameTcpClient(private val conn: TcpClientConnection) : Closeable {

    var gameId: String? = null
        private set

    override fun close() = conn.close()

    // ---------------------------------
    // Util
    // ---------------------------------
    private fun newReqId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

    private fun requireGameId(): String =
        requireNotNull(gameId) { "No gameId. Call hello() first." }

    /**
     * ⚠️ Regla FINAL:
     * - Si usas GameEventLoopClient, NO llames a métodos que hagan receive() aquí.
     * - Con EventLoop: usa SOLO sendX() + el loop consume receiveEnvelope().
     */

    // ---------------------------------
    // 1) Handshake (sync)
    // ---------------------------------
    fun hello(clientVersion: String = "1.0", playerName: String): Welcome {
        conn.send(
            Envelope(
                v = 1,
                requestId = newReqId("hello-$playerName"),
                gameId = null,
                payload = Hello(clientVersion = clientVersion, playerName = playerName)
            )
        )

        val resp = conn.receive()
        val payload = resp.payload
        require(payload is Welcome) { "Expected WELCOME, got ${payload::class.simpleName}" }
        gameId = resp.gameId
        return payload
    }

    // ---------------------------------
    // 2) API síncrona (send + receive)
    //    ✅ Útil en tests rápidos SIN EventLoop
    // ---------------------------------
    fun startGame(boardSize: Int = 10, allowAdjacency: Boolean = false): GameStateEvent {
        sendStartGame(boardSize, allowAdjacency)
        return expectGameState()
    }

    fun placeShip(
        player: PlayerId,
        row: Int,
        col: Int,
        ship: ShipTypeId,
        orientation: OrientationId
    ): GameStateEvent {
        sendPlaceShip(player, row, col, ship, orientation)
        return expectGameState()
    }

    /**
     * ⚠️ SÍNCRONO: No usar con EventLoop.
     * Tras shoot pueden venir 1..2 mensajes, así que aquí solo devolvemos el primero.
     */
    fun shootSync(player: PlayerId, row: Int, col: Int): Envelope {
        sendShoot(player, row, col)
        return conn.receive()
    }

    /**
     * Lectura bloqueante del siguiente Envelope.
     * ✅ Usar SOLO si tu EventLoop es el único lector.
     */
    fun receiveEnvelope(): Envelope = conn.receive()

    private fun expectGameState(): GameStateEvent {
        val env = conn.receive()
        return when (val p = env.payload) {
            is GameStateEvent -> p
            is ErrorMsg -> throw AssertionError("Server returned ErrorMsg: ${p.code} - ${p.message}")
            else -> throw AssertionError("Expected GAME_STATE, got ${p::class.simpleName}")
        }
    }

    // ---------------------------------
    // 3) API async (send-only)
    //    ✅ Usar con EventLoop
    // ---------------------------------
    fun sendStartGame(boardSize: Int = 10, allowAdjacency: Boolean = false) {
        val gid = requireGameId()
        conn.send(
            Envelope(
                v = 1,
                requestId = newReqId("start"),
                gameId = gid,
                payload = StartGame(boardSize = boardSize, allowAdjacency = allowAdjacency)
            )
        )
    }

    fun sendPlaceShip(
        player: PlayerId,
        row: Int,
        col: Int,
        ship: ShipTypeId,
        orientation: OrientationId
    ) {
        val gid = requireGameId()
        conn.send(
            Envelope(
                v = 1,
                requestId = newReqId("place-$player-$ship"),
                gameId = gid,
                payload = PlaceShip(
                    gameId = gid,
                    player = player,
                    row = row,
                    col = col,
                    ship = ship,
                    orientation = orientation
                )
            )
        )
    }

    fun sendShoot(player: PlayerId, row: Int, col: Int) {
        val gid = requireGameId()
        conn.send(
            Envelope(
                v = 1,
                requestId = newReqId("shoot-$player-$row-$col"),
                gameId = gid,
                payload = Shoot(
                    gameId = gid,
                    player = player,
                    row = row,
                    col = col
                )
            )
        )
    }
}
