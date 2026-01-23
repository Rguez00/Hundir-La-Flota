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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Fuente de verdad del estado de partidas en el servidor.
 * - Mantiene el mapa gameId -> Game
 * - Garantiza operaciones atómicas por partida con Mutex (por gameId)
 *
 * Nota: el dominio ya gestiona reglas; aquí solo gestionamos lifecycle + concurrencia.
 */
class GameService(
    private val startGameUseCase: StartGameUseCase = StartGameUseCase(),
    private val placeShipUseCase: PlaceShipUseCase = PlaceShipUseCase(),
    private val shootUseCase: ShootUseCase = ShootUseCase(),
    private val getStateUseCase: GetGameStateUseCase = GetGameStateUseCase(),
) {
    private data class Entry(
        var game: Game? = null,
        val mutex: Mutex = Mutex()
    )

    private val games = ConcurrentHashMap<String, Entry>()

    private fun entryOf(gameId: GameId): Entry =
        games.computeIfAbsent(gameId.value) { Entry() }

    fun exists(gameId: GameId): Boolean = games.containsKey(gameId.value)

    fun remove(gameId: GameId) {
        games.remove(gameId.value)
    }

    suspend fun startGame(gameId: GameId, boardSize: Int = 10, allowAdjacency: Boolean = false): Game {
        val entry = entryOf(gameId)
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
        val entry = entryOf(gameId)
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")
            val updated = placeShipUseCase.invoke(g, player, start, type, orientation)
            entry.game = updated
            updated
        }
    }

    /**
     * Mantener para compatibilidad (puede seguir siendo útil en tests internos).
     * OJO: no devuelve snapshots; solo ejecuta el disparo.
     */
    suspend fun shoot(gameId: GameId, target: Coordinate): ShotResult {
        val entry = entryOf(gameId)
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")
            shootUseCase.invoke(g, target)
        }
    }

    suspend fun getGameState(gameId: GameId, viewer: Game.Player): GameState {
        val entry = entryOf(gameId)
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")
            getStateUseCase.invoke(g, viewer)
        }
    }

    /**
     * Útil para 5.3+ cuando queramos emitir GAME_OVER y limpiar memoria.
     */
    suspend fun isOver(gameId: GameId): Boolean {
        val entry = entryOf(gameId)
        return entry.mutex.withLock {
            val g = entry.game ?: return@withLock false
            g.phase == Game.Phase.OVER
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
        val entry = entryOf(gameId)
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
