package com.mario.hlf.ui.components

import com.mario.hlf.protocol.BoardStateDto
import com.mario.hlf.protocol.CellViewId

/**
 * Detecta y analiza barcos en el tablero para renderizado visual
 */

data class DetectedShip(
    val cells: Set<Pair<Int, Int>>,
    val size: Int,
    val orientation: ShipOrientation,
    val type: ShipVisualType
)

enum class ShipOrientation {
    HORIZONTAL,
    VERTICAL
}

enum class ShipVisualType(val size: Int, val imageName: String) {
    CARRIER(5, "HLF_ship_carrier"),
    BATTLESHIP(4, "HLF_ship_battleship"),
    CRUISER(3, "HLF_ship_cruiser"),
    DESTROYER(2, "HLF_ship_destroyer"),
    UNKNOWN(1, "HLF_ship_destroyer") // Fallback
}

/**
 * Información de una celda específica de un barco detectado
 */
data class ShipCellInfo(
    val ship: DetectedShip,
    val position: ShipPartPosition, // HEAD, BODY, TAIL
    val indexInShip: Int // 0 = inicio, size-1 = fin
)

enum class ShipPartPosition {
    HEAD,   // Primera celda
    BODY,   // Celdas intermedias
    TAIL    // Última celda
}

/**
 * Detecta todos los barcos en el tablero analizando patrones de celdas SHIP o HIT
 */
fun detectShips(board: BoardStateDto): Map<Pair<Int, Int>, ShipCellInfo> {
    val visited = mutableSetOf<Pair<Int, Int>>()
    val detectedShips = mutableListOf<DetectedShip>()

    // Encontrar todas las celdas que son parte de un barco
    for (row in 0 until board.size) {
        for (col in 0 until board.size) {
            val coord = row to col
            val cell = board.cells[row][col]

            // Solo procesar celdas SHIP o HIT que no hemos visitado
            if ((cell == CellViewId.SHIP || cell == CellViewId.HIT) && coord !in visited) {
                val ship = exploreShip(board, row, col, visited)
                if (ship != null) {
                    detectedShips.add(ship)
                }
            }
        }
    }

    // Crear mapa de coordenada -> información de celda
    return buildShipCellInfoMap(detectedShips)
}

/**
 * Explora un barco completo desde una celda inicial
 */
private fun exploreShip(
    board: BoardStateDto,
    startRow: Int,
    startCol: Int,
    visited: MutableSet<Pair<Int, Int>>
): DetectedShip? {
    val cells = mutableSetOf<Pair<Int, Int>>()
    val queue = mutableListOf(startRow to startCol)

    while (queue.isNotEmpty()) {
        val (row, col) = queue.removeAt(0)
        val coord = row to col

        if (coord in visited || row !in 0 until board.size || col !in 0 until board.size) {
            continue
        }

        val cell = board.cells[row][col]
        if (cell != CellViewId.SHIP && cell != CellViewId.HIT) {
            continue
        }

        visited.add(coord)
        cells.add(coord)

        // Explorar solo horizontal y vertical (no diagonal)
        queue.add(row - 1 to col) // arriba
        queue.add(row + 1 to col) // abajo
        queue.add(row to col - 1) // izquierda
        queue.add(row to col + 1) // derecha
    }

    if (cells.isEmpty()) return null

    // Determinar orientación y ordenar celdas
    val orientation = detectOrientation(cells)
    val size = cells.size
    val type = when (size) {
        5 -> ShipVisualType.CARRIER
        4 -> ShipVisualType.BATTLESHIP
        3 -> ShipVisualType.CRUISER
        2 -> ShipVisualType.DESTROYER
        else -> ShipVisualType.UNKNOWN
    }

    return DetectedShip(cells, size, orientation, type)
}

/**
 * Detecta si un barco es horizontal o vertical
 */
private fun detectOrientation(cells: Set<Pair<Int, Int>>): ShipOrientation {
    if (cells.size <= 1) return ShipOrientation.HORIZONTAL

    val rows = cells.map { it.first }.toSet()
    val cols = cells.map { it.second }.toSet()

    // Si todas las celdas están en la misma fila -> HORIZONTAL
    // Si todas las celdas están en la misma columna -> VERTICAL
    return if (rows.size == 1) {
        ShipOrientation.HORIZONTAL
    } else {
        ShipOrientation.VERTICAL
    }
}

/**
 * Construye un mapa de cada celda a su información dentro del barco
 */
private fun buildShipCellInfoMap(ships: List<DetectedShip>): Map<Pair<Int, Int>, ShipCellInfo> {
    val map = mutableMapOf<Pair<Int, Int>, ShipCellInfo>()

    for (ship in ships) {
        // Ordenar celdas del barco según orientación
        val sortedCells = when (ship.orientation) {
            ShipOrientation.HORIZONTAL -> ship.cells.sortedBy { it.second } // por columna
            ShipOrientation.VERTICAL -> ship.cells.sortedBy { it.first } // por fila
        }

        sortedCells.forEachIndexed { index, coord ->
            val position = when {
                sortedCells.size == 1 -> ShipPartPosition.HEAD // Barco de 1 celda
                index == 0 -> ShipPartPosition.HEAD
                index == sortedCells.size - 1 -> ShipPartPosition.TAIL
                else -> ShipPartPosition.BODY
            }

            map[coord] = ShipCellInfo(ship, position, index)
        }
    }

    return map
}
