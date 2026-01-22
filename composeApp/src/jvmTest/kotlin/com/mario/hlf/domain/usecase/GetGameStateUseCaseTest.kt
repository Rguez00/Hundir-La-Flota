package com.mario.hlf.domain.usecase

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.rules.Game
import com.mario.hlf.domain.usecase.dto.CellView
import kotlin.test.Test
import kotlin.test.assertEquals

class GetGameStateUseCaseTest {

    @Test
    fun `viewer sees own ships but opponent ships are hidden`() {
        val startGame = StartGameUseCase()
        val placeShip = PlaceShipUseCase()
        val getState = GetGameStateUseCase()

        val game = startGame(boardSize = 10, allowAdjacency = true)

        // Colocamos un DESTROYER para P1 y uno para P2 (necesario para este test, no hace falta llegar a BATTLE)
        placeShip(game, Game.Player.P1, Coordinate(0, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P2, Coordinate(5, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        val stateForP1 = getState(game, viewer = Game.Player.P1)

        // En "self" P1 debe ver su SHIP
        assertEquals(CellView.SHIP, stateForP1.self.cells[0][0])

        // En "opponent" P1 NO debe ver el SHIP de P2 (debe ser UNKNOWN)
        assertEquals(CellView.UNKNOWN, stateForP1.opponent.cells[5][0])
    }
}
