package com.mario.hlf.server

import com.mario.hlf.domain.rules.Game
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
                ?: return listOf(toSelf(clientId, env, errorFor(env, "NO_SESSION", "Sesión no encontrada")))

            val gid = resolveGameId(session, env)
                ?: return listOf(toSelf(clientId, env, errorFor(env, "NO_GAME", "No hay gameId asociado")))

            val gameId = GameId(gid)

            val room = rooms.getRoomByGameId(gameId)
                ?: return listOf(toSelf(clientId, env, errorFor(env, "NO_ROOM", "No existe room para gameId=$gid")))

            val selfPlayer = session.slot?.let { slotToPlayer(it) }
                ?: return listOf(toSelf(clientId, env, errorFor(env, "NO_SLOT", "Sesión sin slot asignado")))

            when (val p = env.payload) {
                is StartGame -> {
                    if (selfPlayer != Game.Player.P1) {
                        return listOf(toSelf(clientId, env, errorFor(env, "FORBIDDEN", "Solo P1 puede iniciar la partida")))
                    }
                    games.startGame(gameId, boardSize = p.boardSize, allowAdjacency = p.allowAdjacency)
                    broadcastState(room, env, gameId)
                }

                is PlaceShip -> {
                    if (p.player.toDomain() != selfPlayer) {
                        return listOf(toSelf(clientId, env, errorFor(env, "FORBIDDEN", "Player no coincide con sesión")))
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
                    if (p.player.toDomain() != selfPlayer) {
                        return listOf(toSelf(clientId, env, errorFor(env, "FORBIDDEN", "Player no coincide con sesión")))
                    }

                    games.shoot(gameId, p.toDomainCoordinate())

                    val out = mutableListOf<Dispatch>()
                    out.addAll(broadcastState(room, env, gameId))

                    if (games.isOver(gameId)) {
                        val stateP1 = games.getGameState(gameId, Game.Player.P1)
                        val winner = stateP1.winner
                        if (winner != null) {
                            out.addAll(broadcastGameOver(room, env, gameId, winner))
                        }
                    }

                    out
                }

                // Handshake se maneja en ClientSession
                is Hello, is Welcome, is ErrorMsg ->
                    listOf(toSelf(clientId, env, errorFor(env, "UNSUPPORTED", "Handshake fuera del router en 5.3.2")))

                else ->
                    listOf(toSelf(clientId, env, errorFor(env, "UNSUPPORTED", "Mensaje no soportado en 5.3.2")))
            }
        } catch (t: Throwable) {
            listOf(toSelf(clientId, env, errorFor(env, "EXCEPTION", t.message ?: "Error interno")))
        }
    }

    private fun resolveGameId(session: SessionInfo, env: Envelope): String? =
        env.gameId ?: session.gameId?.value

    private fun slotToPlayer(slot: Int): Game.Player = when (slot) {
        1 -> Game.Player.P1
        2 -> Game.Player.P2
        else -> error("Invalid slot=$slot")
    }

    private fun errorFor(req: Envelope, code: String, msg: String): Envelope =
        Envelope(
            v = req.v,
            requestId = req.requestId ?: UUID.randomUUID().toString(),
            gameId = req.gameId,
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
