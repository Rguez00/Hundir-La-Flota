package com.mario.hlf.protocol

import com.mario.hlf.domain.rules.Game
import com.mario.hlf.domain.usecase.dto.BoardState
import com.mario.hlf.domain.usecase.dto.CellView
import com.mario.hlf.domain.usecase.dto.GameState

fun GameState.toProtocol(): GameStateDto =
    GameStateDto(
        phase = phase.toProtocol(),
        currentTurn = currentTurn.toProtocol(),
        winner = winner?.toProtocol(),
        self = self.toProtocol(),
        opponent = opponent.toProtocol()
    )

private fun Game.Phase.toProtocol(): PhaseId = when (this) {
    Game.Phase.PLACEMENT -> PhaseId.PLACEMENT
    Game.Phase.BATTLE -> PhaseId.BATTLE
    Game.Phase.OVER -> PhaseId.OVER
}

private fun Game.Player.toProtocol(): PlayerId = when (this) {
    Game.Player.P1 -> PlayerId.P1
    Game.Player.P2 -> PlayerId.P2
}

private fun BoardState.toProtocol(): BoardStateDto =
    BoardStateDto(
        size = size,
        cells = cells.map { row -> row.map { it.toProtocol() } }
    )

private fun CellView.toProtocol(): CellViewId = when (this) {
    CellView.UNKNOWN -> CellViewId.UNKNOWN
    CellView.EMPTY -> CellViewId.EMPTY
    CellView.SHIP -> CellViewId.SHIP
    CellView.HIT -> CellViewId.HIT
    CellView.MISS -> CellViewId.MISS
}
