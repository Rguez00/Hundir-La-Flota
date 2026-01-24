package com.mario.hlf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mario.hlf.protocol.ServerConfigDto

@Composable
fun SettingsScreen(
    serverConfig: ServerConfigDto?,
    onBack: () -> Unit
) {
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
                    "⚙️ CONFIGURACIÓN DEL SERVIDOR",
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

            HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.3f))

            if (serverConfig != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0D1117).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "CONFIGURACIÓN ACTUAL",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF00),
                            letterSpacing = 1.sp
                        )

                        HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.3f))

                    // Red y Conexiones
                    SectionHeader("🌐 Red y Conexiones")

                    SettingRow(
                        icon = "🖥️",
                        label = "Servidor",
                        value = "${serverConfig.host}:${serverConfig.port}"
                    )

                    SettingRow(
                        icon = "👥",
                        label = "Clientes Máximos",
                        value = serverConfig.maxClients.toString()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Configuración del Juego
                    SectionHeader("🎮 Configuración del Juego")

                    SettingRow(
                        icon = "📏",
                        label = "Tamaño del Tablero",
                        value = "${serverConfig.boardSize}x${serverConfig.boardSize}"
                    )

                    SettingRow(
                        icon = "⏱️",
                        label = "Tiempo por Turno",
                        value = "${serverConfig.turnSeconds} segundos"
                    )

                    SettingRow(
                        icon = "🏆",
                        label = "Formato de Partida",
                        value = "Mejor de ${serverConfig.bestOf}"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inteligencia Artificial
                    SectionHeader("🤖 Inteligencia Artificial")

                    SettingRow(
                        icon = "🎯",
                        label = "Dificultad de la IA",
                        value = when (serverConfig.aiDifficulty.name) {
                            "EASY" -> "Fácil 🟢"
                            "NORMAL" -> "Normal 🟡"
                            "HARD" -> "Difícil 🔴"
                            else -> serverConfig.aiDifficulty.name
                        }
                    )

                        // Descripción de la dificultad
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A3A1A).copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "ℹ️ Sobre la dificultad:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00FF00)
                                )
                                Text(
                                    when (serverConfig.aiDifficulty.name) {
                                        "EASY" -> "La IA dispara completamente al azar, sin estrategia."
                                        "NORMAL" -> "La IA usa modo Hunt & Target: patrón de tablero de ajedrez y seguimiento al impactar."
                                        "HARD" -> "La IA usa estrategia avanzada: evita adyacentes y predice dirección de barcos."
                                        else -> "Configuración personalizada"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF00FF00).copy(alpha = 0.8f)
                                )
                            }
                        }
                }
            }

                Spacer(modifier = Modifier.height(16.dp))

                // Nota informativa
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0A1A2A).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("💡", fontSize = 24.sp)
                        Column {
                            Text(
                                "CONFIGURACIÓN DEL SERVIDOR",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00D9FF)
                            )
                            Text(
                                "Esta configuración se define en el archivo server.properties del servidor. " +
                                        "Para cambiarla, modifica ese archivo y reinicia el servidor.",
                                fontSize = 12.sp,
                                color = Color(0xFF00D9FF).copy(alpha = 0.8f)
                            )
                        }
                    }
                }

            } else {
                // Sin configuración disponible
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4A0A0A).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "⚠️",
                                fontSize = 48.sp
                            )
                            Text(
                                "NO HAY CONFIGURACIÓN DEL SERVIDOR DISPONIBLE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF0000)
                            )
                            Text(
                                "Conéctate al servidor para ver su configuración",
                                fontSize = 14.sp,
                                color = Color(0xFFFF0000).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF00FF00),
        letterSpacing = 1.sp
    )
}

@Composable
private fun SettingRow(
    icon: String,
    label: String,
    value: String
) {
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
                icon,
                fontSize = 18.sp
            )
            Text(
                label,
                fontSize = 14.sp,
                color = Color(0xFF00FF00).copy(alpha = 0.8f)
            )
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF1A3A1A).copy(alpha = 0.6f)
        ) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF00)
            )
        }
    }
}
