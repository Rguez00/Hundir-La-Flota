package com.mario.hlf.domain.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.mario.hlf.domain.model.CellState
import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import kotlin.test.assertEquals
import com.mario.hlf.domain.errors.DomainError
import com.mario.hlf.domain.model.ShotResult
import kotlin.test.assertFailsWith





class GameTest {

    @Test
    fun `start creates game in PLACEMENT with P1 turn and two boards`() {
        val game = Game.start(boardSize = 10, allowAdjacency = false)

        assertEquals(Game.Phase.PLACEMENT, game.phase)
        assertEquals(Game.Player.P1, game.currentTurn)
        assertNull(game.winner)

        assertNotNull(game.boardOf(Game.Player.P1))
        assertNotNull(game.boardOf(Game.Player.P2))
    }
    @Test
    fun `placeShip places on P1 board and not on P2 board`() {
        val game = Game.start(boardSize = 10, allowAdjacency = false)

        game.placeShip(Game.Player.P1, Coordinate(0, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        assertEquals(CellState.SHIP, game.boardOf(Game.Player.P1).cellAt(Coordinate(0, 0)))
        assertEquals(CellState.EMPTY, game.boardOf(Game.Player.P2).cellAt(Coordinate(0, 0)))
    }
    @Test
    fun `placeShip throws InvalidPhase when not in PLACEMENT`() {
        val game = Game.forTest(
            phase = Game.Phase.BATTLE,
            currentTurn = Game.Player.P1
        )

        assertFailsWith<DomainError.InvalidPhase> {
            game.placeShip(
                Game.Player.P1,
                Coordinate(0, 0),
                ShipType.DESTROYER,
                Orientation.HORIZONTAL
            )
        }
    }
    @Test
    fun `default fleet is classic 5 ships`() {
        val fleet = Game.DEFAULT_FLEET
        assertEquals(5, fleet.size)
        assertEquals(listOf(
            ShipType.CARRIER,
            ShipType.BATTLESHIP,
            ShipType.CRUISER,
            ShipType.SUBMARINE,
            ShipType.DESTROYER
        ), fleet)
    }
    @Test
    fun `placing same ship type twice throws ShipNotAvailable`() {
        val game = Game.start(boardSize = 10, allowAdjacency = false)

        game.placeShip(Game.Player.P1, Coordinate(0, 0), ShipType.CARRIER, Orientation.HORIZONTAL)

        assertFailsWith<DomainError.ShipNotAvailable> {
            game.placeShip(Game.Player.P1, Coordinate(2, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        }
    }
    @Test
    fun `fleet is not consumed if placement fails`() {
        val game = Game.start(boardSize = 10, allowAdjacency = false)

        // Colocamos un DESTROYER válido
        game.placeShip(Game.Player.P1, Coordinate(0, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        // Intentamos colocar un CARRIER que solapa (fallará por Overlap)
        assertFailsWith<DomainError.Overlap> {
            game.placeShip(Game.Player.P1, Coordinate(0, 0), ShipType.CARRIER, Orientation.VERTICAL)
        }

        // Si NO se consumió el CARRIER, ahora debe poder colocarse en otra posición válida
        game.placeShip(Game.Player.P1, Coordinate(2, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
    }
    @Test
    fun `placement is complete only when both players placed all ships`() {
        val game = Game.start(boardSize = 10, allowAdjacency = true)

        // al inicio nadie ha colocado todo
        assertEquals(false, game.isPlacementComplete())

        // colocamos toda la flota de P1 (en posiciones seguras)
        game.placeShip(Game.Player.P1, Coordinate(0, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(1, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(2, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(3, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(4, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        // aún falta P2
        assertEquals(false, game.isPlacementComplete())

        // colocamos toda la flota de P2 (en otra zona)
        game.placeShip(Game.Player.P2, Coordinate(6, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(7, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(8, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(9, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(5, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        assertEquals(true, game.isPlacementComplete())
    }
    @Test
    fun `shoot in battle targets opponent board`() {
        val game = Game.start(boardSize = 10, allowAdjacency = true)

        // colocamos flota mínima para llegar a BATTLE (usar solo 1 barco por cada uno sería más cómodo,
        // pero tu Game exige terminar flota para cambiar a BATTLE, así que usamos la flota completa)
        game.placeShip(Game.Player.P1, Coordinate(0, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(1, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(2, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(3, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(4, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        game.placeShip(Game.Player.P2, Coordinate(6, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(7, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(8, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(9, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(5, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        // ya estamos en BATTLE y currentTurn=P1
        val result = game.shoot(Coordinate(6, 0)) // dispara al board de P2 donde hay SHIP

        assertEquals(com.mario.hlf.domain.model.ShotResult.Hit, result)
    }
    @Test
    fun `turn always changes after each shot`() {
        val game = Game.start(boardSize = 10, allowAdjacency = true)

        // Llegamos a BATTLE
        game.placeShip(Game.Player.P1, Coordinate(0, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(1, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(2, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(3, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(4, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        game.placeShip(Game.Player.P2, Coordinate(6, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(7, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(8, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(9, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(5, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        game.shoot(Coordinate(6, 0))
        assertEquals(Game.Player.P2, game.currentTurn)

        game.shoot(Coordinate(0, 0))
        assertEquals(Game.Player.P1, game.currentTurn)
    }
    @Test
    fun `game ends when opponent has all ships sunk`() {
        val game = Game.start(boardSize = 10, allowAdjacency = true)

        // Colocación completa para pasar a BATTLE
        game.placeShip(Game.Player.P1, Coordinate(0, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(1, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(2, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(3, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P1, Coordinate(4, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        game.placeShip(Game.Player.P2, Coordinate(6, 0), ShipType.CARRIER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(7, 0), ShipType.BATTLESHIP, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(8, 0), ShipType.CRUISER, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(9, 0), ShipType.SUBMARINE, Orientation.HORIZONTAL)
        game.placeShip(Game.Player.P2, Coordinate(5, 0), ShipType.DESTROYER, Orientation.HORIZONTAL)

        // Dummy seguro: P2 dispara a una celda que sabemos que P1 NO ocupa (P1 solo usa filas 0..4)
        val dummyTargetOnP1 = Coordinate(9, 9)

        fun ensureP1Turn() {
            if (game.currentTurn == Game.Player.P2) {
                game.shoot(dummyTargetOnP1) // consume el turno de P2 y vuelve a P1
            }
        }

        // Disparos para hundir toda la flota de P2 (coordenadas según colocación de arriba)
        val targetsToSinkP2 = listOf(
            // destroyer row5 col0-1
            Coordinate(5, 0), Coordinate(5, 1),

            // battleship row7 col0-3
            Coordinate(7, 0), Coordinate(7, 1), Coordinate(7, 2), Coordinate(7, 3),

            // cruiser row8 col0-2
            Coordinate(8, 0), Coordinate(8, 1), Coordinate(8, 2),

            // submarine row9 col0-2
            Coordinate(9, 0), Coordinate(9, 1), Coordinate(9, 2),

            // carrier row6 col0-4
            Coordinate(6, 0), Coordinate(6, 1), Coordinate(6, 2), Coordinate(6, 3), Coordinate(6, 4)
        )

        for (t in targetsToSinkP2.dropLast(1)) {
            ensureP1Turn()
            game.shoot(t)
        }

        // último disparo (P1 garantizado) que debe terminar la partida
        ensureP1Turn()
        game.shoot(targetsToSinkP2.last())

        assertEquals(Game.Phase.OVER, game.phase)
        assertEquals(Game.Player.P1, game.winner)
    }





}

