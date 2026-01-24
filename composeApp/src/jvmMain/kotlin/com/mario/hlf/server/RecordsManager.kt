package com.mario.hlf.server

import com.mario.hlf.protocol.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Gestor de records y estadísticas del servidor
 *
 * Responsabilidades:
 * - Cargar records desde records.json
 * - Guardar records de forma atómica (con backup)
 * - Actualizar estadísticas tras partidas
 * - Proporcionar consultas de estadísticas
 */
class RecordsManager(
    private val filePath: String = "records.json"
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private var records: RecordsDto = RecordsDto()
    private val file = File(filePath)

    init {
        loadFromFile()
    }

    /**
     * Obtiene todos los records actuales
     */
    fun getRecords(): RecordsDto = records

    /**
     * Obtiene estadísticas de un jugador específico
     */
    fun getPlayerStats(playerName: String): PlayerStatsDto {
        return records.players[playerName] ?: PlayerStatsDto()
    }

    /**
     * Registra el resultado de una partida y actualiza estadísticas
     */
    fun recordGameResult(
        player1Name: String,
        player2Name: String?,
        winner: String,
        mode: GameModeId,
        totalShots: Int,
        totalHits: Int,
        turns: Int
    ) {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        // Actualizar stats del jugador 1
        val p1Stats = getPlayerStats(player1Name)
        val updatedP1 = updatePlayerStats(
            stats = p1Stats,
            won = winner == player1Name,
            mode = mode,
            shots = totalShots,
            hits = totalHits,
            turns = if (winner == player1Name) turns else null
        )

        val updatedPlayers = records.players.toMutableMap()
        updatedPlayers[player1Name] = updatedP1.copy(lastUpdated = now)

        // Si es PVP, actualizar también stats del jugador 2
        if (mode == GameModeId.PVP && player2Name != null) {
            val p2Stats = getPlayerStats(player2Name)
            val updatedP2 = updatePlayerStats(
                stats = p2Stats,
                won = winner == player2Name,
                mode = mode,
                shots = totalShots, // En el futuro, trackear por separado
                hits = totalHits,   // En el futuro, trackear por separado
                turns = if (winner == player2Name) turns else null
            )
            updatedPlayers[player2Name] = updatedP2.copy(lastUpdated = now)
        }

        // Actualizar meta
        val updatedMeta = records.meta.copy(
            lastBackup = now
        )

        records = RecordsDto(
            players = updatedPlayers,
            meta = updatedMeta
        )

        saveToFile()
    }

    /**
     * Actualiza las estadísticas de un modo específico
     */
    private fun updatePlayerStats(
        stats: PlayerStatsDto,
        won: Boolean,
        mode: GameModeId,
        shots: Int,
        hits: Int,
        turns: Int?
    ): PlayerStatsDto {
        val modeStats = when (mode) {
            GameModeId.PVP -> stats.pvp
            GameModeId.PVE -> stats.pve
        }

        val newWins = if (won) modeStats.wins + 1 else modeStats.wins
        val newLosses = if (!won) modeStats.losses + 1 else modeStats.losses
        val newTotalGames = modeStats.totalGames + 1

        // Racha actual
        val newCurrentStreak = if (won) modeStats.currentStreak + 1 else 0
        val newBestStreak = maxOf(modeStats.bestStreak, newCurrentStreak)

        // Precisión
        val newTotalShots = modeStats.totalShots + shots
        val newTotalHits = modeStats.totalHits + hits
        val accuracy = if (newTotalShots > 0) newTotalHits.toDouble() / newTotalShots else 0.0
        val newBestAccuracy = maxOf(modeStats.bestAccuracy, accuracy)

        // Promedio de precisión (aproximado)
        val newAvgAccuracy = if (newTotalGames > 0) {
            ((modeStats.avgAccuracy * modeStats.totalGames) + accuracy) / newTotalGames
        } else {
            accuracy
        }

        // Partida más rápida
        val newFastestWinTurns = if (won && turns != null) {
            if (modeStats.fastestWinTurns == null) {
                turns
            } else {
                minOf(modeStats.fastestWinTurns, turns)
            }
        } else {
            modeStats.fastestWinTurns
        }

        val updatedModeStats = ModeStatsDto(
            wins = newWins,
            losses = newLosses,
            draws = modeStats.draws,
            bestStreak = newBestStreak,
            currentStreak = newCurrentStreak,
            bestAccuracy = newBestAccuracy,
            avgAccuracy = newAvgAccuracy,
            fastestWinTurns = newFastestWinTurns,
            totalGames = newTotalGames,
            totalShots = newTotalShots,
            totalHits = newTotalHits
        )

        return when (mode) {
            GameModeId.PVP -> stats.copy(pvp = updatedModeStats)
            GameModeId.PVE -> stats.copy(pve = updatedModeStats)
        }
    }

    /**
     * Carga records desde el archivo JSON
     */
    private fun loadFromFile() {
        if (!file.exists()) {
            log("⚠️  Archivo $filePath no existe, creando uno nuevo con valores por defecto")
            records = createDefaultRecords()
            saveToFile()
            return
        }

        try {
            val content = file.readText()
            records = json.decodeFromString<RecordsDto>(content)
            log("✅ Records cargados exitosamente desde $filePath (${records.players.size} jugadores)")
        } catch (e: Exception) {
            log("❌ Error cargando records: ${e.message}")
            log("⚠️  Usando records por defecto y creando backup del archivo corrupto")

            // Hacer backup del archivo corrupto
            val backupPath = "${filePath}.backup.${System.currentTimeMillis()}"
            Files.copy(file.toPath(), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING)
            log("📦 Backup guardado en: $backupPath")

            records = createDefaultRecords()
            saveToFile()
        }
    }

    /**
     * Guarda records al archivo JSON de forma atómica
     */
    private fun saveToFile() {
        try {
            // Escribir primero a archivo temporal
            val tempFile = File("${filePath}.tmp")
            val content = json.encodeToString(records)
            tempFile.writeText(content)

            // Mover atómicamente al archivo final
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )

            log("💾 Records guardados exitosamente en $filePath")
        } catch (e: Exception) {
            log("❌ Error guardando records: ${e.message}")
        }
    }

    /**
     * Crea estructura de records por defecto
     */
    private fun createDefaultRecords(): RecordsDto {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        return RecordsDto(
            players = emptyMap(),
            meta = MetaDto(
                version = 1,
                lastBackup = now
            )
        )
    }

    /**
     * Obtiene top N jugadores por victorias en un modo
     */
    fun getTopPlayers(mode: GameModeId, limit: Int = 10): List<Pair<String, ModeStatsDto>> {
        return records.players
            .map { (name, stats) ->
                val modeStats = when (mode) {
                    GameModeId.PVP -> stats.pvp
                    GameModeId.PVE -> stats.pve
                }
                name to modeStats
            }
            .sortedByDescending { it.second.wins }
            .take(limit)
    }

    /**
     * Obtiene jugadores con mejor racha
     */
    fun getTopStreaks(mode: GameModeId, limit: Int = 10): List<Pair<String, Int>> {
        return records.players
            .map { (name, stats) ->
                val streak = when (mode) {
                    GameModeId.PVP -> stats.pvp.bestStreak
                    GameModeId.PVE -> stats.pve.bestStreak
                }
                name to streak
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * Obtiene jugadores con mejor precisión
     */
    fun getTopAccuracy(mode: GameModeId, limit: Int = 10): List<Pair<String, Double>> {
        return records.players
            .map { (name, stats) ->
                val accuracy = when (mode) {
                    GameModeId.PVP -> stats.pvp.bestAccuracy
                    GameModeId.PVE -> stats.pve.bestAccuracy
                }
                name to accuracy
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    /**
     * Obtiene partidas más rápidas
     */
    fun getFastestWins(mode: GameModeId, limit: Int = 10): List<Pair<String, Int>> {
        return records.players
            .mapNotNull { (name, stats) ->
                val turns = when (mode) {
                    GameModeId.PVP -> stats.pvp.fastestWinTurns
                    GameModeId.PVE -> stats.pve.fastestWinTurns
                }
                turns?.let { name to it }
            }
            .sortedBy { it.second }
            .take(limit)
    }

    /**
     * Crea un backup manual de los records
     */
    fun createBackup(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupPath = "${filePath}.backup.$timestamp"

        try {
            Files.copy(file.toPath(), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING)
            log("📦 Backup manual creado: $backupPath")
            return backupPath
        } catch (e: Exception) {
            log("❌ Error creando backup: ${e.message}")
            throw e
        }
    }

    private fun log(msg: String) = println("[RECORDS] $msg")
}
