package com.mario.hlf.domain

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.model.ShotResult

class Game(
    val playerA: String,
    val playerB: String,
    val size: Int = 10
) {
    fun placeShip(player: String, start: Coordinate, type: ShipType, orientation: Orientation) {
        TODO("Implementar en Fase 2")
    }

    fun start() {
        TODO("Implementar en Fase 2")
    }

    fun shoot(player: String, target: Coordinate): ShotResult {
        TODO("Implementar en Fase 2")
    }

    fun currentTurnPlayer(): String = playerA

    fun isFinished(): Boolean = false

    fun winner(): String? = null
}
