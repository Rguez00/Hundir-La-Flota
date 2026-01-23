package com.mario.hlf.server

import com.mario.hlf.domain.model.ShotResult
import com.mario.hlf.domain.rules.Game
import com.mario.hlf.domain.usecase.dto.GameState
import com.mario.hlf.protocol.*
import com.mario.hlf.server.ai.AIPlayer
import kotlinx.coroutines.delay
import java.util.UUID

class MessageRouter(
    private val sessions: SessionRegistry,
    private val rooms: RoomRegistry,
    private val games: GameService,
    private val aiPlayers: MutableMap<String, AIPlayer> = mutableMapOf() // ✅ NUEVO: Gestión de IAs
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
                is StartGame -> handleStartGame(clientId, env, room, gameId, selfPlayer, p)
                is PlaceShip -> handlePlaceShip(clientId, env, room, gameId, selfPlayer, p)
                is Shoot -> handleShoot(clientId, env, room, gameId, selfPlayer, p)
                is Hello, is Welcome, is ErrorMsg ->
                    listOf(toSelf(clientId, env, errorFor(env, "UNSUPPORTED", "Handshake fuera del router")))
                else ->
                    listOf(toSelf(clientId, env, errorFor(env, "UNSUPPORTED", "Mensaje no soportado")))
            }
        } catch (t: Throwable) {
            listOf(toSelf(clientId, env, errorFor(env, "EXCEPTION", t.message ?: "Error interno")))
        }
    }

    /**
     * ✅ MEJORADO: START_GAME con colocación automática de IA en PVE
     */
    private suspend fun handleStartGame(
        clientId: ClientId,
        env: Envelope,
        room: Room,
        gameId: GameId,
        selfPlayer: Game.Player,
        cmd: StartGame
    ): List<Dispatch> {
        // Validación: solo P1 puede iniciar (en PVP) o P1 en PVE
        if (room.mode == GameModeId.PVP && selfPlayer != Game.Player.P1) {
            return listOf(toSelf(clientId, env, errorFor(env, "FORBIDDEN", "Solo P1 puede iniciar la partida")))
        }

        // Iniciar partida
        val game = games.startGame(gameId, mode = room.mode, boardSize = cmd.boardSize, allowAdjacency = cmd.allowAdjacency)

        // ✅ NUEVO: Si es PVE, crear IA y colocar su flota automáticamente
        if (room.mode == GameModeId.PVE) {
            val ai = getOrCreateAI(gameId, AIDifficulty.NORMAL)
            ai.placeShipsRandomly(game, Game.Player.P2, boardSize = cmd.boardSize)
        }

        // Broadcast estado inicial
        return broadcastState(room, env, gameId)
    }

    /**
     * ✅ MEJORADO: PLACE_SHIP con trigger de IA en PVE si es último barco
     */
    private suspend fun handlePlaceShip(
        clientId: ClientId,
        env: Envelope,
        room: Room,
        gameId: GameId,
        selfPlayer: Game.Player,
        cmd: PlaceShip
    ): List<Dispatch> {
        // Anticheat: player del mensaje debe coincidir con sesión
        if (cmd.player.toDomain() != selfPlayer) {
            return listOf(toSelf(clientId, env, errorFor(env, "FORBIDDEN", "Player no coincide con sesión")))
        }

        // Colocar barco
        games.placeShip(
            gameId = gameId,
            player = selfPlayer,
            start = cmd.toDomainCoordinate(),
            type = cmd.ship.toDomain(),
            orientation = cmd.orientation.toDomain()
        )

        // Broadcast estado actualizado
        val dispatches = broadcastState(room, env, gameId).toMutableList()

        // ✅ NUEVO: Si es PVE y P1 terminó placement, transición automática a BATTLE
        if (room.mode == GameModeId.PVE) {
            val remaining = games.getRemainingShips(gameId, Game.Player.P1)
            if (remaining == 0) {
                // P1 terminó, hacer broadcast del estado BATTLE
                delay(300) // Pequeño delay para UX
                dispatches += broadcastState(room, env, gameId)
            }
        }

        return dispatches
    }

    /**
     * ✅ MEJORADO: SHOOT con turno automático de IA en PVE
     */
    private suspend fun handleShoot(
        clientId: ClientId,
        env: Envelope,
        room: Room,
        gameId: GameId,
        selfPlayer: Game.Player,
        cmd: Shoot
    ): List<Dispatch> {
        // 1) Anticheat: player del mensaje debe coincidir con sesión
        if (cmd.player.toDomain() != selfPlayer) {
            return listOf(toSelf(clientId, env, errorFor(env, "FORBIDDEN", "Player no coincide con sesión")))
        }

        // 2) Validar fase y turno
        val preState = games.getGameState(gameId, selfPlayer)

        if (preState.phase != Game.Phase.BATTLE) {
            return listOf(toSelf(clientId, env, errorFor(env, "INVALID_PHASE", "No se puede disparar en fase ${preState.phase}")))
        }

        if (preState.currentTurn != selfPlayer) {
            return listOf(toSelf(clientId, env, errorFor(env, "FORBIDDEN", "No es tu turno")))
        }

        // 3) Ejecutar disparo de P1
        val outcome = games.shootAndSnapshot(gameId, cmd.toDomainCoordinate())

        // 4) Broadcast estado después del disparo de P1
        val dispatches = mutableListOf<Dispatch>()
        dispatches += broadcastStateFromSnapshots(room, env, gameId, outcome.p1State, outcome.p2State)

        // 5) Si terminó, GAME_OVER
        if (outcome.isOver && outcome.winner != null) {
            dispatches += broadcastGameOver(room, env, gameId, outcome.winner, GameOverReason.ALL_SHIPS_SUNK)
            cleanupAI(gameId)
            return dispatches
        }

        // ✅ NUEVO: Si es PVE y ahora es turno de IA, ejecutar automáticamente
        if (room.mode == GameModeId.PVE && outcome.p1State.currentTurn == Game.Player.P2) {
            delay(800) // Simular "pensamiento" de IA

            val aiDispatches = executeAITurn(gameId, room, env)
            dispatches += aiDispatches
        }

        return dispatches
    }

    /**
     * ✅ NUEVO: Ejecutar turno de IA
     */
    private suspend fun executeAITurn(gameId: GameId, room: Room, originalEnv: Envelope): List<Dispatch> {
        val ai = getOrCreateAI(gameId, AIDifficulty.NORMAL)

        // Verificar que sea turno de P2
        val state = games.getGameState(gameId, Game.Player.P2)
        if (state.phase != Game.Phase.BATTLE || state.currentTurn != Game.Player.P2) {
            return emptyList()
        }

        // IA decide disparo
        val target = ai.chooseShot(boardSize = state.self.size)

        // Ejecutar disparo de IA
        val outcome = games.shootAndSnapshot(gameId, target)

        // Notificar resultado a la IA para aprendizaje
        val shotResult = determineShotResult(outcome.p1State, target)
        ai.notifyShotResult(target, shotResult.wasHit, shotResult.wasSunk)

        // Broadcast del estado después del disparo de IA
        val dispatches = mutableListOf<Dispatch>()
        dispatches += broadcastStateFromSnapshots(room, originalEnv, gameId, outcome.p1State, outcome.p2State)

        // Si la IA ganó, GAME_OVER
        if (outcome.isOver && outcome.winner != null) {
            dispatches += broadcastGameOver(room, originalEnv, gameId, outcome.winner, GameOverReason.ALL_SHIPS_SUNK)
            cleanupAI(gameId)
        }

        return dispatches
    }

    /**
     * ✅ NUEVO: Notificar desconexión de jugador
     */
    suspend fun notifyPlayerDisconnected(disconnectedClientId: ClientId, gameId: GameId): List<Dispatch> {
        val room = rooms.getRoomByGameId(gameId) ?: return emptyList()
        val disconnectedSession = sessions.get(disconnectedClientId)

        val disconnectedPlayer = disconnectedSession?.slot?.let { slotToPlayer(it) } ?: return emptyList()
        val disconnectedPlayerId = disconnectedPlayer.toProtocol()

        // Limpiar IA si existe
        cleanupAI(gameId)

        // Crear evento de desconexión
        val event = PlayerDisconnectedEvent(
            gameId = gameId.value,
            player = disconnectedPlayerId,
            reason = "Connection lost"
        )

        val envelope = Envelope(
            v = 1,
            requestId = UUID.randomUUID().toString(),
            gameId = gameId.value,
            payload = event
        )

        // Enviar a todos los jugadores EXCEPTO el desconectado
        return room.players
            .filter { it != disconnectedClientId }
            .map { Dispatch(it, envelope) }
    }

    // ========== GESTIÓN DE IA ==========

    /**
     * ✅ NUEVO: Obtener o crear instancia de IA para un juego
     */
    private fun getOrCreateAI(gameId: GameId, difficulty: AIDifficulty): AIPlayer {
        return aiPlayers.getOrPut(gameId.value) {
            AIPlayer(difficulty)
        }
    }

    /**
     * ✅ NUEVO: Limpiar IA cuando termina partida
     */
    private fun cleanupAI(gameId: GameId) {
        aiPlayers.remove(gameId.value)
    }

    /**
     * ✅ NUEVO: Determinar resultado de disparo para feedback a IA
     */
    private fun determineShotResult(p1State: GameState, target: com.mario.hlf.domain.model.Coordinate): ShotResultInfo {
        val cellView = p1State.opponent.cells[target.row][target.col]

        return when (cellView) {
            com.mario.hlf.domain.usecase.dto.CellView.HIT -> ShotResultInfo(wasHit = true, wasSunk = false)
            com.mario.hlf.domain.usecase.dto.CellView.MISS -> ShotResultInfo(wasHit = false, wasSunk = false)
            else -> ShotResultInfo(wasHit = false, wasSunk = false)
        }
    }

    private data class ShotResultInfo(val wasHit: Boolean, val wasSunk: Boolean)

    // ========== HELPERS ==========

    private fun resolveGameId(session: SessionInfo, env: Envelope): String? =
        env.gameId ?: session.gameId?.value

    private fun slotToPlayer(slot: Int): Game.Player = when (slot) {
        1 -> Game.Player.P1
        2 -> Game.Player.P2
        else -> error("Invalid slot=$slot")
    }

    private fun errorFor(env: Envelope, code: String, msg: String): Envelope =
        Envelope(
            v = env.v,
            requestId = env.requestId ?: UUID.randomUUID().toString(),
            gameId = env.gameId,
            payload = ErrorMsg(code = code, message = msg)
        )

    private fun toSelf(clientId: ClientId, req: Envelope, resp: Envelope): Dispatch =
        Dispatch(target = clientId, envelope = resp.copy(requestId = req.requestId))

    private suspend fun broadcastState(room: Room, req: Envelope, gameId: GameId): List<Dispatch> {
        val p1State = games.getGameState(gameId, Game.Player.P1).toProtocol()
        val p2State = games.getGameState(gameId, Game.Player.P2).toProtocol()

        return broadcastStateFromSnapshots(room, req, gameId, p1State, p2State)
    }

    private fun broadcastStateFromSnapshots(
        room: Room,
        req: Envelope,
        gameId: GameId,
        p1State: GameStateDto,
        p2State: GameStateDto
    ): List<Dispatch> {
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

        val list = mutableListOf<Dispatch>()
        room.players.getOrNull(0)?.let { list += Dispatch(it, p1Env) }
        room.players.getOrNull(1)?.let { list += Dispatch(it, p2Env) }
        return list
    }

    private fun broadcastStateFromSnapshots(
        room: Room,
        req: Envelope,
        gameId: GameId,
        p1State: GameState,
        p2State: GameState
    ): List<Dispatch> {
        return broadcastStateFromSnapshots(room, req, gameId, p1State.toProtocol(), p2State.toProtocol())
    }

    private fun broadcastGameOver(
        room: Room,
        req: Envelope,
        gameId: GameId,
        winner: Game.Player,
        reason: GameOverReason
    ): List<Dispatch> {
        val winnerId = winner.toProtocol()

        val env = Envelope(
            v = req.v,
            requestId = req.requestId,
            gameId = gameId.value,
            payload = GameOverEvent(gameId = gameId.value, winner = winnerId, reason = reason)
        )

        return room.players.map { Dispatch(it, env) }
    }
}

data class Dispatch(
    val target: ClientId,
    val envelope: Envelope
)