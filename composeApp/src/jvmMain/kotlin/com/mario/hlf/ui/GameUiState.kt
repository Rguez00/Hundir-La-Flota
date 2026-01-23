package com.mario.hlf.ui

import com.mario.hlf.protocol.GameOverEvent
import com.mario.hlf.protocol.GameStateDto
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.protocol.RoomStatusId

enum class GameModeUi { PVP, PVE }

sealed interface GameUiState {
    data object Disconnected : GameUiState

    data class Connecting(
        val host: String,
        val port: Int,
        val name: String
    ) : GameUiState

    /**
     * Conectado + handshake OK.
     * Aún puede estar WAITING si no hay rival (PVP) o READY si ya está la sala completa.
     */
    data class Connected(
        val host: String,
        val port: Int,
        val name: String,
        val gameId: String,
        val roomId: String,
        val roomStatus: RoomStatusId,
        val me: PlayerId,
        val mode: GameModeUi = GameModeUi.PVP
    ) : GameUiState

    /**
     * En partida (PLACEMENT/BATTLE/OVER según state.phase).
     */
    data class InGame(
        val gameId: String,
        val me: PlayerId,
        val state: GameStateDto,
        val gameOver: GameOverEvent? = null,
        val mode: GameModeUi = GameModeUi.PVP
    ) : GameUiState

    data class Error(val message: String) : GameUiState
}
