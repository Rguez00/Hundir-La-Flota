package com.mario.hlf.domain.usecase

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.ShotResult
import com.mario.hlf.domain.rules.Game

class ShootUseCase {
    operator fun invoke(game: Game, target: Coordinate): ShotResult {
        return game.shoot(target)
    }
}
