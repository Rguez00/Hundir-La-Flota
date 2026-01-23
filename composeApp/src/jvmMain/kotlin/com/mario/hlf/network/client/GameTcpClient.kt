package com.mario.hlf.network.client

import com.mario.hlf.protocol.*
import java.io.Closeable
import java.util.UUID

class GameTcpClient(private val conn: TcpClientConnection) : Closeable {

    var gameId: String? = null
        private set

    override fun close() = conn.close()

    private fun newReqId(prefix: String): String = "$prefix-${UUID.randomUUID()}"
    private fun requireGameId(): String = requireNotNull(gameId) { "No gameId. Call hello() first." }

    /**
     * ✅ MEJORADO: Handshake con modo de juego
     */
    fun hello(
        clientVersion: String = "1.0",
        playerName: String,
        mode: GameModeId = GameModeId.PVP, // ✅ NUEVO
        handshakeTimeoutMs: Int = 2500
    ): Welcome {
        val reqId = newReqId("hello-$playerName")

        conn.send(
            Envelope(
                v = 1,
                requestId = reqId,
                gameId = null,
                payload = Hello(
                    clientVersion = clientVersion,
                    playerName = playerName,
                    mode = mode // ✅ NUEVO
                )
            )
        )

        val resp = conn.receiveWithTimeoutForHandshake(handshakeTimeoutMs)

        return when (val p = resp.payload) {
            is Welcome -> {
                val gid = requireNotNull(resp.gameId) { "WELCOME received but Envelope.gameId is null" }
                gameId = gid
                p
            }
            is ErrorMsg -> error("HELLO failed: ${p.code} - ${p.message}")
            else -> error("Expected WELCOME, got ${p::class.simpleName}")
        }
    }

    fun receiveEnvelope(): Envelope = conn.receive()

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

    fun sendPlaceShip(player: PlayerId, row: Int, col: Int, ship: ShipTypeId, orientation: OrientationId) {
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