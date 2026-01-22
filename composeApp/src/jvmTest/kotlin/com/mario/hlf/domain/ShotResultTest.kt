package com.mario.hlf.domain.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ShotResultTest {

    @Test
    fun `shotresult tiene hit miss sunk`() {
        assertTrue(ShotResult.Hit is ShotResult)
        assertTrue(ShotResult.Miss is ShotResult)

        val sunk = ShotResult.Sunk(ShipType.DESTROYER)
        assertTrue(sunk is ShotResult)
    }
}
