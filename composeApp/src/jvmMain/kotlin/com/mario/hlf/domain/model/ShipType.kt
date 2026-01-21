package com.mario.hlf.domain.model

enum class ShipType(val size: Int) {
    CARRIER(5),
    BATTLESHIP(4),
    CRUISER(3),
    SUBMARINE(3),
    DESTROYER(2)
}
