package com.mario.hlf.protocol

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.rules.Game

// --- Player / enums ---

fun PlayerId.toDomain(): Game.Player = when (this) {
    PlayerId.P1 -> Game.Player.P1
    PlayerId.P2 -> Game.Player.P2
}

fun OrientationId.toDomain(): Orientation = when (this) {
    OrientationId.HORIZONTAL -> Orientation.HORIZONTAL
    OrientationId.VERTICAL -> Orientation.VERTICAL
}

fun ShipTypeId.toDomain(): ShipType = when (this) {
    ShipTypeId.CARRIER -> ShipType.CARRIER
    ShipTypeId.BATTLESHIP -> ShipType.BATTLESHIP
    ShipTypeId.CRUISER -> ShipType.CRUISER
    ShipTypeId.SUBMARINE -> ShipType.SUBMARINE
    ShipTypeId.DESTROYER -> ShipType.DESTROYER
}

// --- Coordinates ---

fun PlaceShip.toDomainCoordinate(): Coordinate = Coordinate(row, col)
fun Shoot.toDomainCoordinate(): Coordinate = Coordinate(row, col)
