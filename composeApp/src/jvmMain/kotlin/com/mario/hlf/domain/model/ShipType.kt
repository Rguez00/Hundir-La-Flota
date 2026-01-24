package com.mario.hlf.domain.model

/**
 * Flota clásica de Hundir la Flota:
 * - 1 Portaaviones (5 casillas)
 * - 1 Acorazado (4 casillas)
 * - 1 Crucero (3 casillas)
 * - 1 Submarino (3 casillas)
 * - 1 Destructor (2 casillas)
 * Total: 5 barcos, 17 casillas
 */
enum class ShipType(val size: Int) {
    CARRIER(5),
    BATTLESHIP(4),
    CRUISER(3),
    SUBMARINE(3),
    DESTROYER(2)
}
