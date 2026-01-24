package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.ServerConfigDto

@Composable
fun SettingsScreen(
    serverConfig: ServerConfigDto?,
    onBack: () -> Unit
) {
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
                "⚙️ Configuración del Servidor",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(onClick = onBack) {
                Text("← Volver")
            }
        }

        HorizontalDivider()

        if (serverConfig != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Configuración Actual",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

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
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "ℹ️ Sobre la dificultad:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when (serverConfig.aiDifficulty.name) {
                                    "EASY" -> "La IA dispara completamente al azar, sin estrategia."
                                    "NORMAL" -> "La IA usa modo Hunt & Target: patrón de tablero de ajedrez y seguimiento al impactar."
                                    "HARD" -> "La IA usa estrategia avanzada: evita adyacentes y predice dirección de barcos."
                                    else -> "Configuración personalizada"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
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
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("💡", style = MaterialTheme.typography.headlineSmall)
                    Column {
                        Text(
                            "Configuración del Servidor",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Esta configuración se define en el archivo server.properties del servidor. " +
                                    "Para cambiarla, modifica ese archivo y reinicia el servidor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

        } else {
            // Sin configuración disponible
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
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
                            style = MaterialTheme.typography.displaySmall
                        )
                        Text(
                            "No hay configuración del servidor disponible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Conéctate al servidor para ver su configuración",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
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
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
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
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
