package com.mario.hlf.domain.rules

import com.mario.hlf.domain.errors.DomainError
import com.mario.hlf.domain.model.Board
import com.mario.hlf.domain.model.Coordinate
import com.mario.hlf.domain.model.Orientation
import com.mario.hlf.domain.model.ShipType
import com.mario.hlf.domain.model.ShotResult

class Game private constructor(
    var phase: Phase,
    var currentTurn: Player,
    private val boards: Map<Player, Board>,
    var winner: Player?,
    private val remainingFleet: MutableMap<Player, MutableList<ShipType>>
) {

    enum class Player { P1, P2 }
    enum class Phase { PLACEMENT, BATTLE, OVER }

    fun boardOf(player: Player): Board = boards.getValue(player)

    fun remainingCount(player: Player): Int = remainingFleet.getValue(player).size

    fun isPlacementComplete(): Boolean =
        remainingFleet.getValue(Player.P1).isEmpty() && remainingFleet.getValue(Player.P2).isEmpty()

    /**
     * Coloca un barco en el tablero del jugador indicado.
     * Reglas:
     * - Solo permitido en fase PLACEMENT.
     * - Valida que el tipo de barco siga disponible para ese jugador.
     * - Si Board falla (OutOfBounds/Overlap/Adjacency...), NO se consume flota.
     * - Cuando ambos jugadores terminan, transiciona a BATTLE y el turno pasa a P1.
     */
    fun placeShip(player: Player, start: Coordinate, type: ShipType, orientation: Orientation): Game {
        if (phase != Phase.PLACEMENT) throw DomainError.InvalidPhase()

        val list = remainingFleet.getValue(player)

        if (type !in list) throw DomainError.ShipNotAvailable()

        // intentar colocar (si falla, se lanza excepción y NO consumimos)
        boardOf(player).placeShip(start, type, orientation)

        // consumimos el tipo solo si la colocación fue exitosa
        list.remove(type)

        // transición automática a batalla cuando ambos terminaron
        if (isPlacementComplete()) {
            phase = Phase.BATTLE
            currentTurn = Player.P1
        }

        return this
    }

    fun shoot(target: Coordinate): ShotResult {
        if (phase != Phase.BATTLE) throw DomainError.InvalidPhase()

        val shooter = currentTurn
        val opponent = if (shooter == Player.P1) Player.P2 else Player.P1

        val result = boardOf(opponent).shoot(target)

        // si el oponente ya no tiene barcos, termina la partida
        if (boardOf(opponent).allShipsSunk()) {
            phase = Phase.OVER
            winner = shooter
            return result
        }

        // regla: siempre cambia turno si la partida no terminó
        currentTurn = opponent
        return result
    }




    companion object {
        val DEFAULT_FLEET: List<ShipType> = listOf(
            ShipType.CARRIER,     // 5
            ShipType.BATTLESHIP,  // 4
            ShipType.CRUISER,     // 3
            ShipType.SUBMARINE,   // 3
            ShipType.DESTROYER    // 2
        )

        fun start(boardSize: Int = 10, allowAdjacency: Boolean = false): Game {
            val p1Board = Board(size = boardSize, allowAdjacency = allowAdjacency)
            val p2Board = Board(size = boardSize, allowAdjacency = allowAdjacency)

            val remaining = mutableMapOf(
                Player.P1 to DEFAULT_FLEET.toMutableList(),
                Player.P2 to DEFAULT_FLEET.toMutableList()
            )

            return Game(
                phase = Phase.PLACEMENT,
                currentTurn = Player.P1,
                boards = mapOf(Player.P1 to p1Board, Player.P2 to p2Board),
                winner = null,
                remainingFleet = remaining
            )
        }

        internal fun forTest(
            phase: Phase,
            currentTurn: Player,
            boardSize: Int = 10,
            allowAdjacency: Boolean = false,
            winner: Player? = null,
            remainingFleet: MutableMap<Player, MutableList<ShipType>> = mutableMapOf(
                Player.P1 to DEFAULT_FLEET.toMutableList(),
                Player.P2 to DEFAULT_FLEET.toMutableList()
            )
        ): Game {
            val p1Board = Board(size = boardSize, allowAdjacency = allowAdjacency)
            val p2Board = Board(size = boardSize, allowAdjacency = allowAdjacency)

            return Game(
                phase = phase,
                currentTurn = currentTurn,
                boards = mapOf(Player.P1 to p1Board, Player.P2 to p2Board),
                winner = winner,
                remainingFleet = remainingFleet
            )
        }
    }
}
