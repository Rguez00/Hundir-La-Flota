package com.mario.hlf.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShipTest {

    @Test
    fun `ship is sunk when all cells are hit`() {
        val ship = Ship(
            type = ShipType.DESTROYER,
            cells = setOf(Coordinate(0, 0), Coordinate(0, 1))
        )

        assertFalse(ship.isSunk())

        val after1 = ship.registerHit(Coordinate(0, 0))
        assertFalse(after1.isSunk())

        val after2 = after1.registerHit(Coordinate(0, 1))
        assertTrue(after2.isSunk())
    }

    @Test
    fun `hitting outside ship does not change state`() {
        val ship = Ship(
            type = ShipType.DESTROYER,
            cells = setOf(Coordinate(0, 0), Coordinate(0, 1))
        )

        val after = ship.registerHit(Coordinate(9, 9))
        assertFalse(after.isSunk())
    }
}
