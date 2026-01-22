package com.mario.hlf.domain.model

import com.mario.hlf.domain.errors.DomainError

class Board(private val size: Int = 10, private val allowAdjacency: Boolean = false) {

    private val grid: Array<Array<CellState>> =
        Array(size) { Array(size) { CellState.EMPTY } }

    // Guardamos barcos y sus celdas
    private val ships: MutableList<PlacedShip> = mutableListOf()

    data class PlacedShip(
        val type: ShipType,
        val cells: Set<Coordinate>,
        val hits: MutableSet<Coordinate> = mutableSetOf()
    ) {
        fun isSunk(): Boolean = hits.containsAll(cells)
    }

    fun cellAt(c: Coordinate): CellState {
        if (!c.isInside(size)) throw DomainError.OutOfBounds()
        return grid[c.row][c.col]
    }

    fun placeShip(start: Coordinate, type: ShipType, orientation: Orientation) {
        if (!start.isInside(size)) throw DomainError.OutOfBounds()

        val cells = ShipPlacement(start, orientation, type).cells().toList()

        // bounds
        if (cells.any { !it.isInside(size) }) throw DomainError.OutOfBounds()

        // overlap
        if (cells.any { grid[it.row][it.col] != CellState.EMPTY }) throw DomainError.Overlap()

        // adjacency (opcional)
        if (!allowAdjacency) {
            val forbidden = cells.flatMap { neighborsIncludingDiagonal(it) }.toSet()
            val anyTouching = forbidden.any { n ->
                n.isInside(size) && grid[n.row][n.col] == CellState.SHIP
            }
            if (anyTouching) throw DomainError.AdjacentNotAllowed()
        }

        // place
        cells.forEach { grid[it.row][it.col] = CellState.SHIP }
        ships += PlacedShip(type = type, cells = cells.toSet())
    }

    fun shoot(target: Coordinate): ShotResult {
        if (!target.isInside(size)) throw DomainError.OutOfBounds()

        return when (grid[target.row][target.col]) {
            CellState.HIT, CellState.MISS -> ShotResult.AlreadyTried

            CellState.EMPTY -> {
                grid[target.row][target.col] = CellState.MISS
                ShotResult.Miss
            }

            CellState.SHIP -> {
                grid[target.row][target.col] = CellState.HIT
                val ship = ships.firstOrNull { target in it.cells }
                    ?: return ShotResult.Hit // no debería pasar

                ship.hits += target
                if (ship.isSunk()) ShotResult.Sunk(ship.type) else ShotResult.Hit
            }
        }
    }

    fun allShipsSunk(): Boolean = ships.isNotEmpty() && ships.all { it.isSunk() }

    private fun neighborsIncludingDiagonal(c: Coordinate): List<Coordinate> {
        val deltas = listOf(-1, 0, 1)
        return deltas.flatMap { dr ->
            deltas.map { dc -> Coordinate(c.row + dr, c.col + dc) }
        }
    }
}
