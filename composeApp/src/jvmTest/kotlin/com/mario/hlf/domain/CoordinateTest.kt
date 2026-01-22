package com.mario.hlf.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinateTest {

    @Test
    fun `coordinate guarda fila y columna`() {
        val c = Coordinate(row = 3, col = 7)
        assertEquals(3, c.row)
        assertEquals(7, c.col)
    }
}
