package com.mario.hlf.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Msg

@Serializable
sealed interface ClientMsg : Msg

@Serializable
sealed interface ServerMsg : Msg

// --- Handshake ---

@Serializable
@SerialName("HELLO")
data class Hello(
    val clientVersion: String,
    val playerName: String,
    val mode: GameModeId = GameModeId.PVP // ✅ NUEVO: cliente especifica modo deseado
) : ClientMsg

@Serializable
@SerialName("WELCOME")
data class Welcome(
    val serverVersion: String,
    val config: ServerConfigDto,
    val records: RecordsDto,

    // Info de la sala
    val roomId: String = "",
    val slot: PlayerId = PlayerId.P1,
    val roomStatus: RoomStatusId = RoomStatusId.WAITING,
    val mode: GameModeId = GameModeId.PVP // ✅ NUEVO: servidor confirma modo de juego
) : ServerMsg

@Serializable
enum class RoomStatusId {
    WAITING,  // Esperando segundo jugador (PVP) o listo para empezar (PVE)
    READY     // Sala completa, puede iniciar partida
}

@Serializable
enum class GameModeId {
    PVP,  // Player vs Player
    PVE   // Player vs Environment (IA)
}

// --- Errores ---

@Serializable
@SerialName("ERROR")
data class ErrorMsg(
    val code: String,
    val message: String
) : ServerMsg

// --- Commands (cliente -> servidor) ---

@Serializable
@SerialName("START_GAME")
data class StartGame(
    val boardSize: Int = 10,
    val allowAdjacency: Boolean = false
) : ClientMsg

@Serializable
@SerialName("PLACE_SHIP")
data class PlaceShip(
    val gameId: String,
    val player: PlayerId,
    val row: Int,
    val col: Int,
    val ship: ShipTypeId,
    val orientation: OrientationId
) : ClientMsg

@Serializable
@SerialName("SHOOT")
data class Shoot(
    val gameId: String,
    val player: PlayerId,
    val row: Int,
    val col: Int
) : ClientMsg

// --- Events (servidor -> cliente) ---

@Serializable
@SerialName("GAME_STATE")
data class GameStateEvent(
    val gameId: String,
    val state: GameStateDto
) : ServerMsg

@Serializable
@SerialName("GAME_OVER")
data class GameOverEvent(
    val gameId: String,
    val winner: PlayerId,
    val reason: GameOverReason = GameOverReason.ALL_SHIPS_SUNK // ✅ NUEVO: razón del fin
) : ServerMsg

@Serializable
enum class GameOverReason {
    ALL_SHIPS_SUNK,      // Victoria normal
    OPPONENT_DISCONNECTED, // Rival abandonó
    TIMEOUT               // Tiempo agotado (futuro)
}

@Serializable
@SerialName("ROOM_UPDATE")
data class RoomUpdateEvent(
    val roomId: String,
    val roomStatus: RoomStatusId,
    val players: Int, // Número de jugadores humanos (1 en PVE, 2 en PVP)
    val mode: GameModeId = GameModeId.PVP // ✅ NUEVO: modo de la sala
) : ServerMsg

// ✅ NUEVO: Notificación de desconexión de jugador
@Serializable
@SerialName("PLAYER_DISCONNECTED")
data class PlayerDisconnectedEvent(
    val gameId: String,
    val player: PlayerId,
    val reason: String = "Connection lost"
) : ServerMsg