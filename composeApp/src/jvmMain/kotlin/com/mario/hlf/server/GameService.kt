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
    private val startGame: StartGameUseCase = StartGameUseCase(),
    private val placeShip: PlaceShipUseCase = PlaceShipUseCase(),
    private val shoot: ShootUseCase = ShootUseCase(),
    private val getState: GetGameStateUseCase = GetGameStateUseCase(),
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
            val g = startGame.invoke(boardSize = boardSize, allowAdjacency = allowAdjacency)
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
            val updated = placeShip.invoke(g, player, start, type, orientation)
            entry.game = updated
            updated
        }
    }

    suspend fun shoot(gameId: GameId, target: Coordinate): ShotResult {
        val entry = entryOf(gameId)
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")
            shoot.invoke(g, target)
        }
    }

    suspend fun getGameState(gameId: GameId, viewer: Game.Player): GameState {
        val entry = entryOf(gameId)
        return entry.mutex.withLock {
            val g = entry.game ?: error("Game not started for gameId=${gameId.value}")
            getState.invoke(g, viewer)
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
}
