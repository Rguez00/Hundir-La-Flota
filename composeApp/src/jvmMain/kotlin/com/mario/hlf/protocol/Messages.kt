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
    val playerName: String
) : ClientMsg

@Serializable
@SerialName("WELCOME")
data class Welcome(
    val serverVersion: String,
    val config: ServerConfigDto,
    val records: RecordsDto,

    // NUEVO (compatibles hacia atrás)
    val roomId: String = "",
    val slot: PlayerId = PlayerId.P1,
    val roomStatus: RoomStatusId = RoomStatusId.WAITING
) : ServerMsg

@Serializable
enum class RoomStatusId { WAITING, READY }


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
    val winner: PlayerId
) : ServerMsg

@Serializable
@SerialName("ROOM_UPDATE")
data class RoomUpdateEvent(
    val roomId: String,
    val roomStatus: RoomStatusId,
    val players: Int
) : ServerMsg
