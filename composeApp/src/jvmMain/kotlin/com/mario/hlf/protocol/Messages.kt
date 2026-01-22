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
    val records: RecordsDto
) : ServerMsg

// --- Errores ---

@Serializable
@SerialName("ERROR")
data class ErrorMsg(
    val code: String,
    val message: String // texto en español
) : ServerMsg

@Serializable
@SerialName("START_GAME")
data class StartGame(
    val boardSize: Int = 10,
    val allowAdjacency: Boolean = false
) : Msg

@Serializable
@SerialName("PLACE_SHIP")
data class PlaceShip(
    val gameId: String,
    val player: PlayerId,
    val row: Int,
    val col: Int,
    val ship: ShipTypeId,
    val orientation: OrientationId
) : Msg

@Serializable
@SerialName("SHOOT")
data class Shoot(
    val gameId: String,
    val player: PlayerId,
    val row: Int,
    val col: Int
) : Msg

@Serializable
@SerialName("GAME_STATE")
data class GameStateEvent(
    val gameId: String,
    val state: GameStateDto
) : Msg

@Serializable
@SerialName("GAME_OVER")
data class GameOverEvent(
    val gameId: String,
    val winner: PlayerId
) : Msg
