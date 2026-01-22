package com.mario.hlf.domain.usecase.dto

import com.mario.hlf.domain.rules.Game

data class GameState(
    val phase: Game.Phase,
    val currentTurn: Game.Player,
    val winner: Game.Player?,
    val self: BoardState,
    val opponent: BoardState
)

data class BoardState(
    val size: Int,
    val cells: List<List<CellView>>
)

enum class CellView {
    UNKNOWN, // no info (fog of war)
    EMPTY,
    SHIP,
    HIT,
    MISS
}
