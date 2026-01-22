package com.mario.hlf.domain

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GameTest {

    @Test
    fun `game - constructor guarda jugadores y size por defecto`() {
        val game = Game(playerA = "A", playerB = "B") // size por defecto = 10

        assertEquals("A", game.playerA)
        assertEquals("B", game.playerB)
        assertEquals(10, game.size)
    }

    @Test
    fun `game - metodos de fase 2 aun no implementados lanzan error`() {
        val game = Game(playerA = "A", playerB = "B", size = 10)

        assertFailsWith<NotImplementedError> {
            game.placeShip(
                player = "A",
                start = Coordinate(0, 0),
                type = ShipType.DESTROYER,
                orientation = Orientation.HORIZONTAL
            )
        }

        assertFailsWith<NotImplementedError> {
            game.start()
        }

        assertFailsWith<NotImplementedError> {
            game.shoot(player = "A", target = Coordinate(9, 9))
        }
    }
}
