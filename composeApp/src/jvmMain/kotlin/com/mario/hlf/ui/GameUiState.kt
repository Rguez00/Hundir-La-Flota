package com.mario.hlf.ui

import com.mario.hlf.protocol.GameModeId
import com.mario.hlf.protocol.GameOverEvent
import com.mario.hlf.protocol.GameStateDto
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.protocol.RoomStatusId

/**
 * ✅ SIMPLIFICADO: Usar GameModeId del protocolo directamente
 */
sealed interface GameUiState {
    data object Disconnected : GameUiState

    data class Connecting(
        val host: String,
        val port: Int,
        val name: String,
        val mode: GameModeId // ✅ NUEVO
    ) : GameUiState

    /**
     * Conectado + handshake OK.
     * - PVP: puede estar WAITING (esperando rival) o READY (sala completa)
     * - PVE: siempre READY (IA disponible inmediatamente)
     */
    data class Connected(
        val host: String,
        val port: Int,
        val name: String,
        val gameId: String,
        val roomId: String,
        val roomStatus: RoomStatusId,
        val me: PlayerId,
        val mode: GameModeId
    ) : GameUiState

    /**
     * En partida (PLACEMENT/BATTLE/OVER según state.phase).
     */
    data class InGame(
        val gameId: String,
        val me: PlayerId,
        val state: GameStateDto,
        val gameOver: GameOverEvent? = null,
        val mode: GameModeId,
        val opponentDisconnected: Boolean = false // ✅ NUEVO: rival se desconectó
    ) : GameUiState

    /**
     * ✅ MEJORADO: Error con más contexto
     */
    data class Error(
        val message: String,
        val reconnectable: Boolean = false, // ✅ NUEVO: puede reintentar conexión
        val previousState: GameUiState? = null // ✅ NUEVO: estado previo (para volver)
    ) : GameUiState
}