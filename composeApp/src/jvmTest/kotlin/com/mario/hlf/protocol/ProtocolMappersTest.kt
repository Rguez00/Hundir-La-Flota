package com.mario.hlf.protocol

import com.mario.hlf.domain.rules.Game
import com.mario.hlf.domain.usecase.dto.BoardState
import com.mario.hlf.domain.usecase.dto.CellView
import com.mario.hlf.domain.usecase.dto.GameState
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtocolMappersTest {

    @Test
    fun `domain game state maps to protocol game state dto`() {
        val domain = GameState(
            phase = Game.Phase.BATTLE,
            currentTurn = Game.Player.P2,
            winner = null,
            self = BoardState(
                size = 2,
                cells = listOf(
                    listOf(CellView.SHIP, CellView.EMPTY),
                    listOf(CellView.HIT, CellView.MISS)
                )
            ),
            opponent = BoardState(
                size = 2,
                cells = listOf(
                    listOf(CellView.UNKNOWN, CellView.UNKNOWN),
                    listOf(CellView.HIT, CellView.MISS)
                )
            )
        )

        val protocol = domain.toProtocol()

        assertEquals(PhaseId.BATTLE, protocol.phase)
        assertEquals(PlayerId.P2, protocol.currentTurn)
        assertEquals(null, protocol.winner)
        assertEquals(CellViewId.SHIP, protocol.self.cells[0][0])
        assertEquals(CellViewId.UNKNOWN, protocol.opponent.cells[0][0])
    }
}
