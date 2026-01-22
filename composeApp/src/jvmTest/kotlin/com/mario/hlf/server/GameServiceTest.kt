package com.mario.hlf.server

import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.rules.Game
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class GameServiceTest {

    @Test
    fun `startGame creates a new game for gameId`() {
        runBlocking {
            val svc = GameService()
            val gid = GameId("g-1")

            val game = svc.startGame(gid, boardSize = 10, allowAdjacency = false)

            assertNotNull(game)
            assertEquals(Game.Phase.PLACEMENT, game.phase)
        }
    }

    @Test
    fun `startGame twice throws`() {
        runBlocking {
            val svc = GameService()
            val gid = GameId("g-2")

            svc.startGame(gid)

            assertFailsWith<IllegalStateException> {
                svc.startGame(gid)
            }
        }
    }

    @Test
    fun `placeShip before start throws`() {
        runBlocking {
            val svc = GameService()
            val gid = GameId("g-3")

            assertFailsWith<IllegalStateException> {
                svc.placeShip(
                    gameId = gid,
                    player = Game.Player.P1,
                    start = Coordinate(0, 0),
                    type = ShipType.DESTROYER,
                    orientation = Orientation.HORIZONTAL
                )
            }
        }
    }
}
