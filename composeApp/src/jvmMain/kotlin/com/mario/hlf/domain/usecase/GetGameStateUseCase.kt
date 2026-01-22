package com.mario.hlf.domain.usecase

import com.mario.hlf.domain.model.CellState
import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.rules.Game
import com.mario.hlf.domain.usecase.dto.BoardState
import com.mario.hlf.domain.usecase.dto.CellView
import com.mario.hlf.domain.usecase.dto.GameState

class GetGameStateUseCase {

    operator fun invoke(game: Game, viewer: Game.Player): GameState {
        val opponent = if (viewer == Game.Player.P1) Game.Player.P2 else Game.Player.P1

        val selfBoard = game.boardOf(viewer)
        val oppBoard = game.boardOf(opponent)

        val size = extractBoardSize(selfBoard)

        val selfState = BoardState(
            size = size,
            cells = buildCells(size) { c ->
                mapSelfCell(selfBoard.cellAt(c))
            }
        )

        val oppState = BoardState(
            size = size,
            cells = buildCells(size) { c ->
                mapOpponentCell(oppBoard.cellAt(c))
            }
        )

        return GameState(
            phase = game.phase,
            currentTurn = game.currentTurn,
            winner = game.winner,
            self = selfState,
            opponent = oppState
        )
    }

    private fun mapSelfCell(state: CellState): CellView = when (state) {
        CellState.EMPTY -> CellView.EMPTY
        CellState.SHIP -> CellView.SHIP
        CellState.HIT -> CellView.HIT
        CellState.MISS -> CellView.MISS
    }

    private fun mapOpponentCell(state: CellState): CellView = when (state) {
        CellState.HIT -> CellView.HIT
        CellState.MISS -> CellView.MISS
        CellState.EMPTY -> CellView.UNKNOWN
        CellState.SHIP -> CellView.UNKNOWN
    }

    private fun buildCells(size: Int, mapper: (Coordinate) -> CellView): List<List<CellView>> =
        List(size) { r ->
            List(size) { c ->
                mapper(Coordinate(r, c))
            }
        }

    // No tenemos un getter de size en Board; lo inferimos de manera segura leyendo (0,0) y creciendo.
    // Como tu Board es cuadrado y usa isInside, esto funciona.
    private fun extractBoardSize(board: com.mario.hlf.domain.model.Board): Int {
        var s = 1
        while (true) {
            // probamos la esquina (s,0); cuando falle ya nos pasamos
            try {
                board.cellAt(Coordinate(s, 0))
                s++
            } catch (_: Exception) {
                return s
            }
        }
    }
}
