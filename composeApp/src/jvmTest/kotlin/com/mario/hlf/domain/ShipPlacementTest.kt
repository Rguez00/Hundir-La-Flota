package com.mario.hlf.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ShipPlacementTest {

    @Test
    fun `cells horizontal are consecutive in col`() {
        val p = ShipPlacement(
            start = Coordinate(2, 3),
            orientation = Orientation.HORIZONTAL,
            type = ShipType.DESTROYER
        )
        assertEquals(
            setOf(Coordinate(2, 3), Coordinate(2, 4)),
            p.cells()
        )
    }

    @Test
    fun `cells vertical are consecutive in row`() {
        val p = ShipPlacement(
            start = Coordinate(5, 1),
            orientation = Orientation.VERTICAL,
            type = ShipType.CRUISER
        )
        assertEquals(
            setOf(Coordinate(5, 1), Coordinate(6, 1), Coordinate(7, 1)),
            p.cells()
        )
    }
}
