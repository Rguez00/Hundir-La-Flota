package com.mario.hlf.server

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.model.ShotResult
import com.mario.hlf.domain.rules.Game
import com.mario.hlf.domain.usecase.GetGameStateUseCase
import com.mario.hlf.domain.usecase.PlaceShipUseCase
import com.mario.hlf.domain.usecase.ShootUseCase
import com.mario.hlf.domain.usecase.StartGameUseCase
import com.mario.hlf.domain.usecase.dto.GameState
import com.mario.hlf.protocol.GameModeId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Fuente de verdad del estado de partidas en el servidor.
 * - Mantiene el mapa gameId -> GameEntry
 * - Garantiza operaciones atómicas por partida con Mutex (por gameId)
 */
class GameService(
    private val startGameUseCase: StartGameUseCase = StartGameUseCase(),
    private val placeShipUseCase: PlaceShipUseCase = PlaceShipUseCase(),
    private val shootUseCase: ShootUseCase = ShootUseCase(),
    private val getStateUseCase: GetGameStateUseCase = GetGameStateUseCase(),
) {
    private data class GameEntry(
        var game: Game? = null,
        val mode: GameModeId = GameModeId.PVP, // ✅ NUEVO
        val mutex: Mutex = Mutex()
    )

    private val games = ConcurrentHashMap<String, GameEntry>()

    private fun entryOf(gameId: GameId, mode: GameModeId = GameModeId.PVP): GameEntry =
        games.computeIfAbsent(gameId.value) { GameEntry(mode = mode) }

    fun exists(gameId: GameId): Boolean = games.containsKey(gameId.value)

    fun remove(gameId: GameId) {
        games.remove(gameId.value)
    }

    /**
     * ✅ MEJORADO: Incluir modo de juego
     */
    suspend fun startGame(
        gameId: GameId,
        mode: GameModeId,
        boardSize: Int = 10,
        allowAdjacency: Boolean = false
    ): Game {
        val entry = entryOf(gameId, mode)
        return entry.mutex.withLock {
            check(entry.game == null) { "Game already started for gameId=${gameId.value}" }
            val g = startGameUseCase.invoke(boardSize = boardSize, allowAdjacency = allowAdjacency)
            entry.game = g
            g
        }
    }

    suspend fun placeShip(
        gameId: GameId,
        player: Game.Player,
        start: Coordinate,
        type: ShipType,
        orientation: Orientation
    ): Game {
        val entry = games[gameId.value] ?: error("Game not found for gameId=${gameId.value}")
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")
            placeShipUseCase.invoke(g, player, start, type, orientation)
            // No reasignar: Game es mutable
            g
        }
    }

    /**
     * Mantener para compatibilidad (puede seguir siendo útil en tests internos).
     * OJO: no devuelve snapshots; solo ejecuta el disparo.
     */
    suspend fun shoot(gameId: GameId, target: Coordinate): ShotResult {
        val entry = games[gameId.value] ?: error("Game not found for gameId=${gameId.value}")
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")
            shootUseCase.invoke(g, target)
        }
    }

    suspend fun getGameState(gameId: GameId, viewer: Game.Player): GameState {
        val entry = games[gameId.value] ?: error("Game not found for gameId=${gameId.value}")
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")
            getStateUseCase.invoke(g, viewer)
        }
    }

    /**
     * Útil para detectar fin de partida
     */
    suspend fun isOver(gameId: GameId): Boolean {
        val entry = games[gameId.value] ?: return false
        return entry.mutex.withLock {
            val g = entry.game ?: return@withLock false
            g.phase == Game.Phase.OVER
        }
    }

    /**
     * ✅ NUEVO: Obtener modo de juego
     */
    suspend fun getMode(gameId: GameId): GameModeId? {
        return games[gameId.value]?.mode
    }

    /**
     * ✅ NUEVO: Obtener número de barcos pendientes por colocar
     */
    suspend fun getRemainingShips(gameId: GameId, player: Game.Player): Int {
        val entry = games[gameId.value] ?: return 0
        return entry.mutex.withLock {
            val g = entry.game ?: return@withLock 0
            g.remainingCount(player)
        }
    }

    data class ShootOutcome(
        val p1State: GameState,
        val p2State: GameState,
        val isOver: Boolean,
        val winner: Game.Player?
    )

    /**
     * Disparo + snapshots consistentes bajo el MISMO lock.
     * Esto evita race conditions (estado mezclado) en el router.
     */
    suspend fun shootAndSnapshot(gameId: GameId, target: Coordinate): ShootOutcome {
        val entry = games[gameId.value] ?: error("Game not found for gameId=${gameId.value}")
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")

            // Ejecuta disparo (Game es mutable)
            shootUseCase.invoke(g, target)

            // Snapshots consistentes bajo lock
            val p1 = getStateUseCase.invoke(g, Game.Player.P1)
            val p2 = getStateUseCase.invoke(g, Game.Player.P2)

            ShootOutcome(
                p1State = p1,
                p2State = p2,
                isOver = (g.phase == Game.Phase.OVER),
                winner = g.winner
            )
        }
    }
}