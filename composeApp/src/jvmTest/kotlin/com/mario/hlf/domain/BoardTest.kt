package com.mario.hlf.domain

import com.mario.hlf.domain.errors.DomainError
import com.mario.hlf.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class BoardTest {

    @Test
    fun `placeShip - coloca un barco y bloquea solape`() {
        val board = Board(size = 10, allowAdjacency = true) // adjacency no importa aquí

        board.placeShip(
            start = Coordinate(0, 0),
            type = ShipType.DESTROYER, // size 2
            orientation = Orientation.HORIZONTAL
        )

        // Celdas ocupadas
        assertEquals(CellState.SHIP, board.cellAt(Coordinate(0, 0)))
        assertEquals(CellState.SHIP, board.cellAt(Coordinate(0, 1)))

        // Solape (intenta poner otro encima)
        try {
            board.placeShip(
                start = Coordinate(0, 1),
                type = ShipType.DESTROYER,
                orientation = Orientation.VERTICAL
            )
            fail("Debería lanzar DomainError.Overlap")
        } catch (e: DomainError.Overlap) {
            // ok
        }
    }

    @Test
    fun `shoot - miss hit sunk alreadyTried`() {
        val board = Board(size = 10, allowAdjacency = true)

        board.placeShip(
            start = Coordinate(5, 5),
            type = ShipType.DESTROYER, // 2 celdas
            orientation = Orientation.HORIZONTAL
        )

        // Agua -> Miss
        assertTrue(board.shoot(Coordinate(0, 0)) is ShotResult.Miss)
        assertEquals(CellState.MISS, board.cellAt(Coordinate(0, 0)))

        // Barco -> Hit
        val r1 = board.shoot(Coordinate(5, 5))
        assertTrue(r1 is ShotResult.Hit)
        assertEquals(CellState.HIT, board.cellAt(Coordinate(5, 5)))

        // Repetido -> AlreadyTried
        val rRepeat = board.shoot(Coordinate(5, 5))
        assertTrue(rRepeat is ShotResult.AlreadyTried)

        // Segundo impacto -> Sunk
        val r2 = board.shoot(Coordinate(5, 6))
        assertTrue(r2 is ShotResult.Sunk)
        val sunk = r2 as ShotResult.Sunk
        assertEquals(ShipType.DESTROYER, sunk.shipType)

        // Todo hundido
        assertTrue(board.allShipsSunk())
    }
}
