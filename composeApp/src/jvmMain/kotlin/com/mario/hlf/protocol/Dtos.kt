package com.mario.hlf.protocol

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfigDto(
    val host: String,
    val port: Int,
    val maxClients: Int,
    val boardSize: Int = 10,
    val turnSeconds: Int = 60,
    val bestOf: Int = 3,
    val difficulty: String = "NORMAL"
)

@Serializable
data class RecordsDto(
    val players: Map<String, PlayerStatsDto> = emptyMap(),
    val meta: MetaDto = MetaDto()
)

@Serializable
data class PlayerStatsDto(
    val pvp: ModeStatsDto = ModeStatsDto(),
    val pve: ModeStatsDto = ModeStatsDto(),
    val lastUpdated: String? = null
)

@Serializable
data class ModeStatsDto(
    val wins: Int = 0,
    val losses: Int = 0,
    val bestStreak: Int = 0,
    val bestAccuracy: Double = 0.0,
    val fastestWinTurns: Int? = null
)

@Serializable
data class MetaDto(
    val version: Int = 1
)
