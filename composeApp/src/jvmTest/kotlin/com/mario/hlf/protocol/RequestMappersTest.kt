package com.mario.hlf.protocol

import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.rules.Game
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestMappersTest {

    @Test
    fun `protocol ids map to domain equivalents`() {
        assertEquals(Game.Player.P1, PlayerId.P1.toDomain())
        assertEquals(Orientation.HORIZONTAL, OrientationId.HORIZONTAL.toDomain())
        assertEquals(ShipType.DESTROYER, ShipTypeId.DESTROYER.toDomain())
    }
}
