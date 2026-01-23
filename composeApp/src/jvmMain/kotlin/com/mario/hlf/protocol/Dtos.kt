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
    val aiDifficulty: AIDifficulty = AIDifficulty.NORMAL // ✅ CAMBIO: más específico
)

@Serializable
enum class AIDifficulty {
    EASY,    // Disparos completamente aleatorios
    NORMAL,  // Disparos inteligentes después de primer hit
    HARD     // Estrategia avanzada (futuro)
}

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
    val draws: Int = 0, // ✅ NUEVO: empates por timeout/desconexión mutua
    val bestStreak: Int = 0,
    val currentStreak: Int = 0, // ✅ NUEVO: racha actual
    val bestAccuracy: Double = 0.0,
    val avgAccuracy: Double = 0.0, // ✅ NUEVO: promedio de precisión
    val fastestWinTurns: Int? = null,
    val totalGames: Int = 0, // ✅ NUEVO: total de partidas jugadas
    val totalShots: Int = 0, // ✅ NUEVO: total de disparos realizados
    val totalHits: Int = 0   // ✅ NUEVO: total de aciertos
)

@Serializable
data class MetaDto(
    val version: Int = 1,
    val lastBackup: String? = null // ✅ NUEVO: timestamp del último backup
)

@Serializable
enum class PlayerId {
    P1,
    P2,
    AI // ✅ NUEVO: identificar jugador IA (opcional, podría seguir siendo P2)
}

@Serializable
enum class OrientationId { HORIZONTAL, VERTICAL }

@Serializable
enum class ShipTypeId {
    CARRIER,      // 5 celdas
    BATTLESHIP,   // 4 celdas
    CRUISER,      // 3 celdas
    SUBMARINE,    // 3 celdas
    DESTROYER     // 2 celdas
}