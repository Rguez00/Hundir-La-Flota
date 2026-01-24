package com.mario.hlf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mario.hlf.protocol.GameModeId
import com.mario.hlf.protocol.ModeStatsDto
import com.mario.hlf.protocol.RecordsDto

@Composable
fun RecordsScreen(
    records: RecordsDto,
    onBack: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(GameModeId.PVP) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo
        Image(
            painter = painterResource("drawable/HLF_ui_bg_grid.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

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
                    "📊 RECORDS Y ESTADÍSTICAS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF00),
                    letterSpacing = 2.sp
                )

                OutlinedButton(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF00FF00)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "← VOLVER",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Selector de modo con imágenes
            Text(
                "SELECCIONA MODO DE JUEGO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF00),
                letterSpacing = 1.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ModeCard(
                    text = "PVP",
                    subtitle = "Jugador vs Jugador",
                    icon = "⚔️",
                    imageResource = "drawable/HLF_Icon_PVP.png",
                    selected = selectedMode == GameModeId.PVP,
                    onClick = { selectedMode = GameModeId.PVP },
                    modifier = Modifier.weight(1f)
                )

                ModeCard(
                    text = "PVE",
                    subtitle = "Jugador vs IA",
                    icon = "🤖",
                    imageResource = "drawable/HLF_Icon_PVE.png",
                    selected = selectedMode == GameModeId.PVE,
                    onClick = { selectedMode = GameModeId.PVE },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.3f))

            // Contenido según modo
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top 10 Jugadores
                item {
                    Text(
                        "🏆 TOP 10 JUGADORES (${if (selectedMode == GameModeId.PVP) "PVP" else "PVE"})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF00),
                        letterSpacing = 1.sp
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
                                containerColor = Color(0xFF0D1117).copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay datos de partidas ${if (selectedMode == GameModeId.PVP) "PVP" else "PVE"} aún",
                                    fontSize = 14.sp,
                                    color = Color(0xFF00FF00).copy(alpha = 0.7f)
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
                Color(0xFF1A3A1A).copy(alpha = 0.9f) // Verde oscuro para top 3
            } else {
                Color(0xFF0D1117).copy(alpha = 0.9f) // Gris oscuro para el resto
            }
        ),
        shape = RoundedCornerShape(10.dp)
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
                        fontSize = 24.sp
                    )
                    Text(
                        playerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF00)
                    )
                }

                // Win rate
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (winRate >= 70) {
                        Color(0xFF00FF00).copy(alpha = 0.3f)
                    } else if (winRate >= 50) {
                        Color(0xFF00D9FF).copy(alpha = 0.3f)
                    } else {
                        Color(0xFFFFFF00).copy(alpha = 0.3f)
                    }
                ) {
                    Text(
                        "$winRate% WR",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = if (winRate >= 70) {
                            Color(0xFF00FF00)
                        } else if (winRate >= 50) {
                            Color(0xFF00D9FF)
                        } else {
                            Color(0xFFFFFF00)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.3f))

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
            fontSize = 18.sp
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FF00)
        )
        Text(
            label,
            fontSize = 11.sp,
            color = Color(0xFF00FF00).copy(alpha = 0.7f)
        )
    }
}

/**
 * Card de modo con imagen de fondo
 */
@Composable
private fun ModeCard(
    text: String,
    subtitle: String,
    icon: String,
    imageResource: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(140.dp)
            .border(
                width = if (selected) 3.dp else 2.dp,
                color = if (selected) Color(0xFF00FF00) else Color(0xFF00FF00).copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Imagen de fondo
            Image(
                painter = painterResource(imageResource),
                contentDescription = text,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (selected) 1f else 0.5f
            )

            // Overlay con gradiente
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selected) Color(0xFF00FF00) else Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = if (selected) Color(0xFF00FF00).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
