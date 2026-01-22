package com.mario.hlf.domain.usecase

import com.mario.hlf.domain.model.CellState
import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.rules.Game
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceShipUseCaseTest {

    @Test
    fun `place ship use case delegates to game`() {
        val startGame = StartGameUseCase()
        val placeShip = PlaceShipUseCase()

        val game = startGame(boardSize = 10, allowAdjacency = true)

        placeShip(
            game = game,
            player = Game.Player.P1,
            start = Coordinate(0, 0),
            type = ShipType.DESTROYER,
            orientation = Orientation.HORIZONTAL
        )

        assertEquals(CellState.SHIP, game.boardOf(Game.Player.P1).cellAt(Coordinate(0, 0)))
        assertEquals(CellState.SHIP, game.boardOf(Game.Player.P1).cellAt(Coordinate(0, 1)))
    }
}
