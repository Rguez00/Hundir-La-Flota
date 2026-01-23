package com.mario.hlf.ui

import com.mario.hlf.protocol.GameOverEvent
import com.mario.hlf.protocol.GameStateDto

sealed interface GameUiState {
    data object Disconnected : GameUiState
    data class Connecting(val host: String, val port: Int, val name: String) : GameUiState
    data class Connected(
        val host: String,
        val port: Int,
        val name: String,
        val gameId: String
    ) : GameUiState

    data class InGame(
        val gameId: String,
        val state: GameStateDto,
        val gameOver: GameOverEvent? = null
    ) : GameUiState

    data class Error(val message: String) : GameUiState
}
