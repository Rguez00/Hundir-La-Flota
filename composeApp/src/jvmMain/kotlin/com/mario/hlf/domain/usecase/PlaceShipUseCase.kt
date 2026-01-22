package com.mario.hlf.domain.usecase

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.rules.Game

class PlaceShipUseCase {
    operator fun invoke(
        game: Game,
        player: Game.Player,
        start: Coordinate,
        type: ShipType,
        orientation: Orientation
    ): Game {
        return game.placeShip(player, start, type, orientation)
    }
}
