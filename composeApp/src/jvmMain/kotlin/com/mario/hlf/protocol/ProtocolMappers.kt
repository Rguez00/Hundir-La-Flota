package com.mario.hlf.protocol

import com.mario.hlf.domain.rules.Game
import com.mario.hlf.domain.usecase.dto.BoardState
import com.mario.hlf.domain.usecase.dto.CellView
import com.mario.hlf.domain.usecase.dto.GameState

/**
 * Convierte un GameState del dominio a GameStateDto del protocolo
 */
fun GameState.toProtocol(): GameStateDto =
    GameStateDto(
        phase = phase.toProtocol(),
        currentTurn = currentTurn.toPlayerIdInternal(), // ✅ CAMBIO AQUÍ
        winner = winner?.toPlayerIdInternal(),          // ✅ CAMBIO AQUÍ
        self = self.toProtocol(),
        opponent = opponent.toProtocol()
    )

/**
 * Convierte Game.Phase del dominio a PhaseId del protocolo
 */
private fun Game.Phase.toProtocol(): PhaseId = when (this) {
    Game.Phase.PLACEMENT -> PhaseId.PLACEMENT
    Game.Phase.BATTLE -> PhaseId.BATTLE
    Game.Phase.OVER -> PhaseId.OVER
}

/**
 * ✅ RENOMBRADO: Función interna para conversión Player → PlayerId
 * Usada solo dentro de este archivo para mapear GameState
 */
private fun Game.Player.toPlayerIdInternal(): PlayerId = when (this) {
    Game.Player.P1 -> PlayerId.P1
    Game.Player.P2 -> PlayerId.P2
}

/**
 * Convierte BoardState del dominio a BoardStateDto del protocolo
 */
private fun BoardState.toProtocol(): BoardStateDto =
    BoardStateDto(
        size = size,
        cells = cells.map { row -> row.map { it.toProtocol() } }
    )

/**
 * Convierte CellView del dominio a CellViewId del protocolo
 */
private fun CellView.toProtocol(): CellViewId = when (this) {
    CellView.UNKNOWN -> CellViewId.UNKNOWN
    CellView.EMPTY -> CellViewId.EMPTY
    CellView.SHIP -> CellViewId.SHIP
    CellView.HIT -> CellViewId.HIT
    CellView.MISS -> CellViewId.MISS
}