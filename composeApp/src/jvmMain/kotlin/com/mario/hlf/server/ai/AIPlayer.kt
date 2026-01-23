package com.mario.hlf.server.ai

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.rules.Game
import com.mario.hlf.protocol.AIDifficulty
import kotlin.random.Random

/**
 * 🤖 IA para modo PVE
 *
 * Niveles de dificultad:
 * - EASY: Disparos completamente aleatorios
 * - NORMAL: Estrategia inteligente (hunt & target mode)
 * - HARD: Normal + evita disparar a celdas adyacentes prematuramente
 */
class AIPlayer(
    private val difficulty: AIDifficulty = AIDifficulty.NORMAL,
    private val random: Random = Random.Default
) {
    // Estado interno para modo NORMAL/HARD
    private var mode: AIMode = AIMode.HUNT
    private val hits = mutableListOf<Coordinate>() // Hits no hundidos
    private val targetQueue = mutableListOf<Coordinate>() // Celdas a investigar
    private val tried = mutableSetOf<Coordinate>() // Celdas ya disparadas

    private enum class AIMode {
        HUNT,   // Buscando barcos
        TARGET  // Investigando un hit
    }

    /**
     * ✅ Coloca toda la flota aleatoriamente en el tablero
     */
    fun placeShipsRandomly(game: Game, player: Game.Player, boardSize: Int = 10) {
        val fleet = Game.DEFAULT_FLEET.toMutableList()
        val maxAttempts = 1000 // Prevenir loops infinitos

        for (shipType in fleet) {
            var placed = false
            var attempts = 0

            while (!placed && attempts < maxAttempts) {
                attempts++

                val orientation = if (random.nextBoolean()) Orientation.HORIZONTAL else Orientation.VERTICAL
                val start = randomCoordinate(boardSize, shipType, orientation)

                try {
                    game.placeShip(player, start, shipType, orientation)
                    placed = true
                } catch (_: Exception) {
                    // Overlap o fuera de límites, reintentar
                }
            }

            check(placed) { "Failed to place ship $shipType after $maxAttempts attempts" }
        }
    }

    /**
     * ✅ Decide el próximo disparo según dificultad
     */
    fun chooseShot(boardSize: Int = 10): Coordinate {
        return when (difficulty) {
            AIDifficulty.EASY -> chooseShotEasy(boardSize)
            AIDifficulty.NORMAL -> chooseShotNormal(boardSize)
            AIDifficulty.HARD -> chooseShotHard(boardSize)
        }
    }

    /**
     * ✅ Notificar resultado del disparo (para NORMAL/HARD)
     */
    fun notifyShotResult(target: Coordinate, wasHit: Boolean, wasSunk: Boolean) {
        tried.add(target)

        when (difficulty) {
            AIDifficulty.EASY -> {
                // Easy no aprende
            }
            AIDifficulty.NORMAL, AIDifficulty.HARD -> {
                if (wasHit) {
                    if (wasSunk) {
                        // Barco hundido: limpiar estado y volver a HUNT
                        hits.clear()
                        targetQueue.clear()
                        mode = AIMode.HUNT
                    } else {
                        // Hit pero no hundido: investigar alrededor
                        hits.add(target)
                        mode = AIMode.TARGET
                        enqueueAdjacent(target)
                    }
                } else {
                    // Miss: si estamos en TARGET y la cola se vacía, volver a HUNT
                    if (mode == AIMode.TARGET && targetQueue.isEmpty()) {
                        mode = AIMode.HUNT
                    }
                }
            }
        }
    }

    /**
     * ✅ Reset del estado (para nueva partida)
     */
    fun reset() {
        mode = AIMode.HUNT
        hits.clear()
        targetQueue.clear()
        tried.clear()
    }

    // ========== ESTRATEGIAS DE DISPARO ==========

    /**
     * EASY: Disparos completamente aleatorios
     */
    private fun chooseShotEasy(boardSize: Int): Coordinate {
        val available = mutableListOf<Coordinate>()
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                val coord = Coordinate(r, c)
                if (coord !in tried) {
                    available.add(coord)
                }
            }
        }

        check(available.isNotEmpty()) { "No shots available" }
        return available.random(random)
    }

    /**
     * NORMAL: Hunt & Target
     * - HUNT: Patrón de tablero de ajedrez (optimizado)
     * - TARGET: Investigar alrededor de hits
     */
    private fun chooseShotNormal(boardSize: Int): Coordinate {
        return when (mode) {
            AIMode.HUNT -> {
                // Patrón de tablero de ajedrez para optimizar búsqueda
                val checkerboard = mutableListOf<Coordinate>()
                for (r in 0 until boardSize) {
                    for (c in 0 until boardSize) {
                        if ((r + c) % 2 == 0) { // Solo celdas pares
                            val coord = Coordinate(r, c)
                            if (coord !in tried) {
                                checkerboard.add(coord)
                            }
                        }
                    }
                }

                if (checkerboard.isNotEmpty()) {
                    checkerboard.random(random)
                } else {
                    // Fallback: cualquier celda disponible
                    chooseShotEasy(boardSize)
                }
            }

            AIMode.TARGET -> {
                // Prioridad a celdas en la cola de investigación
                while (targetQueue.isNotEmpty()) {
                    val next = targetQueue.removeAt(0)
                    if (next !in tried && next.isInside(boardSize)) {
                        return next
                    }
                }

                // Cola vacía, volver a HUNT
                mode = AIMode.HUNT
                chooseShotNormal(boardSize)
            }
        }
    }

    /**
     * HARD: Normal + heurísticas avanzadas
     */
    private fun chooseShotHard(boardSize: Int): Coordinate {
        return when (mode) {
            AIMode.HUNT -> {
                // HARD: Evitar celdas adyacentes a celdas ya probadas (optimización)
                val smart = mutableListOf<Coordinate>()
                for (r in 0 until boardSize) {
                    for (c in 0 until boardSize) {
                        if ((r + c) % 2 == 0) {
                            val coord = Coordinate(r, c)
                            if (coord !in tried && !hasTriedNeighbor(coord)) {
                                smart.add(coord)
                            }
                        }
                    }
                }

                if (smart.isNotEmpty()) {
                    smart.random(random)
                } else {
                    // Fallback a NORMAL
                    chooseShotNormal(boardSize)
                }
            }

            AIMode.TARGET -> {
                // HARD: Si hay múltiples hits, intentar formar línea
                if (hits.size >= 2) {
                    val aligned = findAlignedShots(hits)
                    if (aligned != null) {
                        targetQueue.clear()
                        targetQueue.addAll(aligned)
                    }
                }

                // Mismo comportamiento que NORMAL
                while (targetQueue.isNotEmpty()) {
                    val next = targetQueue.removeAt(0)
                    if (next !in tried && next.isInside(boardSize)) {
                        return next
                    }
                }

                mode = AIMode.HUNT
                chooseShotHard(boardSize)
            }
        }
    }

    // ========== HELPERS ==========

    /**
     * Genera coordenada aleatoria válida para un barco
     */
    private fun randomCoordinate(boardSize: Int, shipType: ShipType, orientation: Orientation): Coordinate {
        val maxRow = if (orientation == Orientation.VERTICAL) boardSize - shipType.size else boardSize
        val maxCol = if (orientation == Orientation.HORIZONTAL) boardSize - shipType.size else boardSize

        return Coordinate(
            row = random.nextInt(maxRow),
            col = random.nextInt(maxCol)
        )
    }

    /**
     * Añade celdas adyacentes (ortogonales) a la cola de investigación
     */
    private fun enqueueAdjacent(coord: Coordinate) {
        val adjacent = listOf(
            Coordinate(coord.row - 1, coord.col), // Arriba
            Coordinate(coord.row + 1, coord.col), // Abajo
            Coordinate(coord.row, coord.col - 1), // Izquierda
            Coordinate(coord.row, coord.col + 1)  // Derecha
        )

        for (adj in adjacent) {
            if (adj !in tried && adj !in targetQueue) {
                targetQueue.add(adj)
            }
        }
    }

    /**
     * Verifica si una celda tiene vecinos ya probados (para HARD)
     */
    private fun hasTriedNeighbor(coord: Coordinate): Boolean {
        val neighbors = listOf(
            Coordinate(coord.row - 1, coord.col),
            Coordinate(coord.row + 1, coord.col),
            Coordinate(coord.row, coord.col - 1),
            Coordinate(coord.row, coord.col + 1)
        )
        return neighbors.any { it in tried }
    }

    /**
     * Encuentra disparos alineados para predecir dirección del barco (HARD)
     */
    private fun findAlignedShots(hits: List<Coordinate>): List<Coordinate>? {
        if (hits.size < 2) return null

        val first = hits[0]
        val second = hits[1]

        // Verificar si están alineados horizontalmente
        if (first.row == second.row) {
            val row = first.row
            val minCol = minOf(first.col, second.col)
            val maxCol = maxOf(first.col, second.col)

            return listOf(
                Coordinate(row, minCol - 1), // Extensión izquierda
                Coordinate(row, maxCol + 1)  // Extensión derecha
            ).filter { it !in tried }
        }

        // Verificar si están alineados verticalmente
        if (first.col == second.col) {
            val col = first.col
            val minRow = minOf(first.row, second.row)
            val maxRow = maxOf(first.row, second.row)

            return listOf(
                Coordinate(minRow - 1, col), // Extensión arriba
                Coordinate(maxRow + 1, col)  // Extensión abajo
            ).filter { it !in tried }
        }

        return null
    }
}