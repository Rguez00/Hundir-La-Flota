package com.mario.hlf.server

import com.mario.hlf.domain.rules.Game
import com.mario.hlf.domain.usecase.dto.GameState
import com.mario.hlf.protocol.*
import com.mario.hlf.protocol.toDomain
import com.mario.hlf.protocol.toDomainCoordinate
import com.mario.hlf.protocol.toProtocol
import java.util.UUID

class MessageRouter(
    private val sessions: SessionRegistry,
    private val rooms: RoomRegistry,
    private val games: GameService
) {

    suspend fun handle(clientId: ClientId, env: Envelope): List<Dispatch> {
        return try {
            val session = sessions.get(clientId)
                ?: return listOf(
                    toSelf(clientId, env, errorFor(req = env, gameId = env.gameId, code = "NO_SESSION", msg = "Sesión no encontrada"))
                )

            val gid = resolveGameId(session, env)
                ?: return listOf(
                    toSelf(clientId, env, errorFor(req = env, gameId = env.gameId, code = "NO_GAME", msg = "No hay gameId asociado"))
                )

            val gameId = GameId(gid)

            val room = rooms.getRoomByGameId(gameId)
                ?: return listOf(
                    toSelf(clientId, env, errorFor(req = env, gameId = gid, code = "NO_ROOM", msg = "No existe room para gameId=$gid"))
                )

            val selfPlayer = session.slot?.let { slotToPlayer(it) }
                ?: return listOf(
                    toSelf(clientId, env, errorFor(req = env, gameId = gid, code = "NO_SLOT", msg = "Sesión sin slot asignado"))
                )

            when (val p = env.payload) {

                is StartGame -> {
                    if (selfPlayer != Game.Player.P1) {
                        return listOf(
                            toSelf(clientId, env, errorFor(req = env, gameId = gid, code = "FORBIDDEN", msg = "Solo P1 puede iniciar la partida"))
                        )
                    }
                    games.startGame(gameId, boardSize = p.boardSize, allowAdjacency = p.allowAdjacency)
                    broadcastState(room, env, gameId)
                }

                is PlaceShip -> {
                    if (p.player.toDomain() != selfPlayer) {
                        return listOf(
                            toSelf(clientId, env, errorFor(req = env, gameId = gid, code = "FORBIDDEN", msg = "Player no coincide con sesión"))
                        )
                    }
                    games.placeShip(
                        gameId = gameId,
                        player = selfPlayer,
                        start = p.toDomainCoordinate(),
                        type = p.ship.toDomain(),
                        orientation = p.orientation.toDomain()
                    )
                    broadcastState(room, env, gameId)
                }

                is Shoot -> {
                    // 1) Anticheat: el player del mensaje debe ser el de la sesión
                    if (p.player.toDomain() != selfPlayer) {
                        return listOf(
                            toSelf(clientId, env, errorFor(req = env, gameId = gid, code = "FORBIDDEN", msg = "Player no coincide con sesión"))
                        )
                    }

                    // 2) Enforce fase + turno usando el estado de dominio (viewer=selfPlayer)
                    val preState = games.getGameState(gameId, selfPlayer)

                    if (preState.phase != Game.Phase.BATTLE) {
                        return listOf(
                            toSelf(
                                clientId,
                                env,
                                errorFor(req = env, gameId = gid, code = "INVALID_PHASE", msg = "No se puede disparar en fase ${preState.phase}")
                            )
                        )
                    }

                    if (preState.currentTurn != selfPlayer) {
                        return listOf(
                            toSelf(clientId, env, errorFor(req = env, gameId = gid, code = "FORBIDDEN", msg = "No es tu turno"))
                        )
                    }

                    // 3) Disparo + snapshots consistentes bajo el mismo lock
                    val outcome = games.shootAndSnapshot(gameId, p.toDomainCoordinate())

                    // 4) Broadcast del estado a ambos usando snapshots
                    val out = mutableListOf<Dispatch>()
                    out += broadcastStateFromSnapshots(
                        room = room,
                        req = env,
                        gameId = gameId,
                        p1State = outcome.p1State,
                        p2State = outcome.p2State
                    )

                    // 5) Si terminó, GAME_OVER
                    if (outcome.isOver && outcome.winner != null) {
                        out += broadcastGameOver(room, env, gameId, outcome.winner)
                    }

                    out
                }

                // Handshake se maneja en ClientSession
                is Hello, is Welcome, is ErrorMsg -> {
                    listOf(
                        toSelf(clientId, env, errorFor(req = env, gameId = gid, code = "UNSUPPORTED", msg = "Handshake fuera del router"))
                    )
                }

                else -> {
                    listOf(
                        toSelf(clientId, env, errorFor(req = env, gameId = gid, code = "UNSUPPORTED", msg = "Mensaje no soportado"))
                    )
                }
            }
        } catch (t: Throwable) {
            listOf(
                toSelf(clientId, env, errorFor(req = env, gameId = env.gameId, code = "EXCEPTION", msg = (t.message ?: "Error interno")))
            )
        }
    }

    private fun resolveGameId(session: SessionInfo, env: Envelope): String? =
        env.gameId ?: session.gameId?.value

    private fun slotToPlayer(slot: Int): Game.Player = when (slot) {
        1 -> Game.Player.P1
        2 -> Game.Player.P2
        else -> error("Invalid slot=$slot")
    }

    /**
     * Error estándar. Pasamos explícitamente gameId para evitar líos cuando env.gameId venga null.
     */
    private fun errorFor(req: Envelope, gameId: String?, code: String, msg: String): Envelope =
        Envelope(
            v = req.v,
            requestId = req.requestId ?: UUID.randomUUID().toString(),
            gameId = gameId ?: req.gameId,
            payload = ErrorMsg(code = code, message = msg)
        )

    private fun toSelf(clientId: ClientId, req: Envelope, resp: Envelope): Dispatch =
        Dispatch(target = clientId, envelope = resp.copy(requestId = req.requestId))

    private suspend fun broadcastState(room: Room, req: Envelope, gameId: GameId): List<Dispatch> {
        val p1State = games.getGameState(gameId, Game.Player.P1).toProtocol()
        val p2State = games.getGameState(gameId, Game.Player.P2).toProtocol()

        val p1Env = Envelope(
            v = req.v,
            requestId = req.requestId,
            gameId = gameId.value,
            payload = GameStateEvent(gameId = gameId.value, state = p1State)
        )
        val p2Env = Envelope(
            v = req.v,
            requestId = req.requestId,
            gameId = gameId.value,
            payload = GameStateEvent(gameId = gameId.value, state = p2State)
        )

        val p1 = room.players.getOrNull(0)
        val p2 = room.players.getOrNull(1)

        val list = mutableListOf<Dispatch>()
        if (p1 != null) list += Dispatch(p1, p1Env)
        if (p2 != null) list += Dispatch(p2, p2Env)
        return list
    }

    private fun broadcastStateFromSnapshots(
        room: Room,
        req: Envelope,
        gameId: GameId,
        p1State: GameState,
        p2State: GameState
    ): List<Dispatch> {
        val p1Dto = p1State.toProtocol()
        val p2Dto = p2State.toProtocol()

        val p1Env = Envelope(
            v = req.v,
            requestId = req.requestId,
            gameId = gameId.value,
            payload = GameStateEvent(gameId = gameId.value, state = p1Dto)
        )
        val p2Env = Envelope(
            v = req.v,
            requestId = req.requestId,
            gameId = gameId.value,
            payload = GameStateEvent(gameId = gameId.value, state = p2Dto)
        )

        val p1 = room.players.getOrNull(0)
        val p2 = room.players.getOrNull(1)

        val list = mutableListOf<Dispatch>()
        if (p1 != null) list += Dispatch(p1, p1Env)
        if (p2 != null) list += Dispatch(p2, p2Env)
        return list
    }

    private fun broadcastGameOver(room: Room, req: Envelope, gameId: GameId, winner: Game.Player): List<Dispatch> {
        val winnerId = when (winner) {
            Game.Player.P1 -> PlayerId.P1
            Game.Player.P2 -> PlayerId.P2
        }

        val env = Envelope(
            v = req.v,
            requestId = req.requestId,
            gameId = gameId.value,
            payload = GameOverEvent(gameId = gameId.value, winner = winnerId)
        )

        return room.players.map { Dispatch(it, env) }
    }
}

data class Dispatch(
    val target: ClientId,
    val envelope: Envelope
)
