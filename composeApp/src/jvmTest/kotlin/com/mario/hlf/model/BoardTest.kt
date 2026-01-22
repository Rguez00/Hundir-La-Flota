package com.mario.hlf.domain.model

import com.mario.hlf.domain.errors.DomainError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BoardTest {

    @Test
    fun `placeShip throws OutOfBounds when any cell exceeds board`() {
        val board = Board(size = 10)

        // DESTROYER horizontal desde col 9 ocupa col 9 y 10 (fuera)
        assertFailsWith<DomainError.OutOfBounds> {
            board.placeShip(Coordinate(0, 9), ShipType.DESTROYER, Orientation.HORIZONTAL)
        }
    }

    @Test
    fun `placeShip at the edge is valid if it fits exactly`() {
        val board = Board(size = 10)

        // DESTROYER horizontal desde col 8 ocupa 8 y 9 (ok)
        board.placeShip(Coordinate(0, 8), ShipType.DESTROYER, Orientation.HORIZONTAL)

        assertEquals(CellState.SHIP, board.cellAt(Coordinate(0, 8)))
        assertEquals(CellState.SHIP, board.cellAt(Coordinate(0, 9)))
    }

    @Test
    fun `allowAdjacency true allows touching diagonally`() {
        val board = Board(size = 10, allowAdjacency = true)

        board.placeShip(Coordinate(0, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        // diagonal touching would be forbidden if allowAdjacency=false
        board.placeShip(Coordinate(1, 1), ShipType.DESTROYER, Orientation.HORIZONTAL)

        assertEquals(CellState.SHIP, board.cellAt(Coordinate(1, 1)))
        assertEquals(CellState.SHIP, board.cellAt(Coordinate(1, 2)))
    }

    @Test
    fun `shoot returns AlreadyTried when shooting HIT again`() {
        val board = Board(size = 10)

        board.placeShip(Coordinate(0, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        val r1 = board.shoot(Coordinate(0, 0))
        val r2 = board.shoot(Coordinate(0, 0))

        assertEquals(ShotResult.Hit, r1)
        assertEquals(ShotResult.AlreadyTried, r2)
    }

    @Test
    fun `allShipsSunk is false if at least one ship remains`() {
        val board = Board(size = 10)

        board.placeShip(Coordinate(0, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)
        board.placeShip(Coordinate(5, 5), ShipType.DESTROYER, Orientation.HORIZONTAL)

        // hundimos el primero
        board.shoot(Coordinate(0, 0))
        board.shoot(Coordinate(0, 1))

        // debe quedar otro barco vivo
        assertEquals(false, board.allShipsSunk())
    }
}
