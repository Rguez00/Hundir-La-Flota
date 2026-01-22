package com.mario.hlf.domain.usecase

import com.mario.hlf.domain.rules.Game
import kotlin.test.Test
import kotlin.test.assertEquals

class StartGameUseCaseTest {

    @Test
    fun `start game use case creates game in placement`() {
        val useCase = StartGameUseCase()

        val game = useCase(boardSize = 10, allowAdjacency = true)

        assertEquals(Game.Phase.PLACEMENT, game.phase)
        assertEquals(Game.Player.P1, game.currentTurn)
    }
}
