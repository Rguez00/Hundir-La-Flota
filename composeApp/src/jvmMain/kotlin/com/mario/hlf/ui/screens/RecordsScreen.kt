package com.mario.hlf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.GameModeId
import com.mario.hlf.protocol.ModeStatsDto
import com.mario.hlf.protocol.RecordsDto

@Composable
fun RecordsScreen(
    records: RecordsDto,
    onBack: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(GameModeId.PVP) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📊 Records y Estadísticas",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(onClick = onBack) {
                Text("← Volver")
            }
        }

        // Selector de modo
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedMode == GameModeId.PVP,
                onClick = { selectedMode = GameModeId.PVP },
                label = { Text("⚔️ PVP (Jugador vs Jugador)") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedMode == GameModeId.PVE,
                onClick = { selectedMode = GameModeId.PVE },
                label = { Text("🤖 PVE (Jugador vs IA)") },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()

        // Contenido según modo
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top 10 Jugadores
            item {
                Text(
                    "🏆 Top 10 Jugadores (${if (selectedMode == GameModeId.PVP) "PVP" else "PVE"})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val topPlayers = records.players
                .map { (name, stats) ->
                    val modeStats = if (selectedMode == GameModeId.PVP) stats.pvp else stats.pve
                    name to modeStats
                }
                .sortedByDescending { it.second.wins }
                .take(10)

            items(topPlayers.withIndex().toList()) { (index, pair) ->
                val (name, stats) = pair
                PlayerStatsCard(
                    rank = index + 1,
                    playerName = name,
                    stats = stats
                )
            }

            if (topPlayers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay datos de partidas ${if (selectedMode == GameModeId.PVP) "PVP" else "PVE"} aún",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Espacio extra al final
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PlayerStatsCard(
    rank: Int,
    playerName: String,
    stats: ModeStatsDto
) {
    val winRate = if (stats.totalGames > 0) {
        (stats.wins.toDouble() / stats.totalGames * 100).toInt()
    } else 0

    val medalEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$rank"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rank <= 3) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header con nombre y medalla
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        medalEmoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        playerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Win rate
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (winRate >= 70) {
                        MaterialTheme.colorScheme.primary
                    } else if (winRate >= 50) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                ) {
                    Text(
                        "$winRate% WR",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider()

            // Stats en 2 filas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Victorias", stats.wins.toString(), "✅")
                StatItem("Derrotas", stats.losses.toString(), "❌")
                StatItem("Partidas", stats.totalGames.toString(), "🎮")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    "Mejor Racha",
                    stats.bestStreak.toString(),
                    "🔥"
                )
                StatItem(
                    "Precisión",
                    "${(stats.avgAccuracy * 100).toInt()}%",
                    "🎯"
                )
                StatItem(
                    "Victoria Rápida",
                    stats.fastestWinTurns?.let { "$it turnos" } ?: "-",
                    "⚡"
                )
            }
        }
    }
}

@Composable
private fun RowScope.StatItem(
    label: String,
    value: String,
    emoji: String
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            emoji,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
