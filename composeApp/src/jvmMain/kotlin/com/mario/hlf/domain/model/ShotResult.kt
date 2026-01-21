package com.mario.hlf.domain.model

sealed class ShotResult {
    data object Miss : ShotResult()
    data object Hit : ShotResult()
    data class Sunk(val shipType: ShipType) : ShotResult()
    data object AlreadyTried : ShotResult()
}
