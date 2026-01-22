package com.mario.hlf.domain.usecase

import com.mario.hlf.domain.rules.Game

class StartGameUseCase {
    operator fun invoke(boardSize: Int = 10, allowAdjacency: Boolean = false): Game {
        return Game.start(boardSize = boardSize, allowAdjacency = allowAdjacency)
    }
}
