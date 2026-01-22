package com.mario.hlf.domain.usecase

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.model.ShotResult
import com.mario.hlf.domain.rules.Game
import kotlin.test.Test
import kotlin.test.assertEquals

class ShootUseCaseTest {

    @Test
    fun `shoot use case delegates to game`() {
        val startGame = StartGameUseCase()
        val placeShip = PlaceShipUseCase()
        val shoot = ShootUseCase()

        val game = startGame(boardSize = 10, allowAdjacency = true)

        // P1 coloca flota
        placeShip(game, Game.Player.P1, Coordinate(0, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P1, Coordinate(1, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P1, Coordinate(2, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P1, Coordinate(3, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P1, Coordinate(4, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        // P2 coloca flota
        placeShip(game, Game.Player.P2, Coordinate(6, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P2, Coordinate(7, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P2, Coordinate(8, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P2, Coordinate(9, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        placeShip(game, Game.Player.P2, Coordinate(5, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        // Ya en BATTLE (turno P1). Disparamos a una celda con barco de P2.
        val result = shoot(game, Coordinate(6, 0))

        assertEquals(ShotResult.Hit, result)
    }
}
