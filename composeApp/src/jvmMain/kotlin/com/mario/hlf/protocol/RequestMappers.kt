package com.mario.hlf.protocol

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.rules.Game

// ========== CONVERSIÓN: PROTOCOL → DOMAIN ==========

/**
 * Convierte PlayerId del protocolo a Game.Player del dominio
 */
fun PlayerId.toDomain(): Game.Player = when (this) {
    PlayerId.P1 -> Game.Player.P1
    PlayerId.P2 -> Game.Player.P2
    PlayerId.AI -> Game.Player.P2 // ✅ IA siempre es P2
}

/**
 * Convierte OrientationId del protocolo a Orientation del dominio
 */
fun OrientationId.toDomain(): Orientation = when (this) {
    OrientationId.HORIZONTAL -> Orientation.HORIZONTAL
    OrientationId.VERTICAL -> Orientation.VERTICAL
}

/**
 * Convierte ShipTypeId del protocolo a ShipType del dominio
 */
fun ShipTypeId.toDomain(): ShipType = when (this) {
    ShipTypeId.CARRIER -> ShipType.CARRIER
    ShipTypeId.BATTLESHIP -> ShipType.BATTLESHIP
    ShipTypeId.CRUISER -> ShipType.CRUISER
    ShipTypeId.SUBMARINE -> ShipType.SUBMARINE
    ShipTypeId.DESTROYER -> ShipType.DESTROYER
}

// ========== CONVERSIÓN: DOMAIN → PROTOCOL ==========

/**
 * ✅ Convierte Game.Player del dominio a PlayerId del protocolo
 * IMPORTANTE: Esta es la versión pública para usar en MessageRouter
 */
fun Game.Player.toProtocol(): PlayerId = when (this) {
    Game.Player.P1 -> PlayerId.P1
    Game.Player.P2 -> PlayerId.P2
}

// ========== EXTRACCIÓN DE COORDENADAS ==========

/**
 * Extrae coordenada del mensaje PlaceShip
 */
fun PlaceShip.toDomainCoordinate(): Coordinate = Coordinate(row, col)

/**
 * Extrae coordenada del mensaje Shoot
 */
fun Shoot.toDomainCoordinate(): Coordinate = Coordinate(row, col)