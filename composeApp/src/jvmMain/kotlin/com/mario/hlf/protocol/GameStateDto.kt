package com.mario.hlf.protocol

import kotlinx.serialization.Serializable

@Serializable
data class GameStateDto(
    val phase: PhaseId,
    val currentTurn: PlayerId,
    val winner: PlayerId? = null,
    val self: BoardStateDto,
    val opponent: BoardStateDto
)

@Serializable
data class BoardStateDto(
    val size: Int,
    val cells: List<List<CellViewId>>
)

@Serializable
enum class CellViewId {
    UNKNOWN,
    EMPTY,
    SHIP,
    HIT,
    MISS
}

@Serializable
enum class PhaseId {
    PLACEMENT,
    BATTLE,
    OVER
}
