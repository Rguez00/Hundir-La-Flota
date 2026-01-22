package com.mario.hlf.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoordinateTest {

    @Test
    fun `coordinate inside board returns true`() {
        assertTrue(Coordinate(0, 0).isInside(10))
        assertTrue(Coordinate(9, 9).isInside(10))
    }

    @Test
    fun `coordinate outside board returns false`() {
        assertFalse(Coordinate(-1, 0).isInside(10))
        assertFalse(Coordinate(10, 0).isInside(10))
        assertFalse(Coordinate(0, 10).isInside(10))
    }
}
