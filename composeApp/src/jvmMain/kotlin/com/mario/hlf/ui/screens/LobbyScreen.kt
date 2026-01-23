package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons                      // ✅ AÑADIR ESTA LÍNEA
import androidx.compose.material.icons.filled.Check               // ✅ YA ESTÁ (línea 14)
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.GameModeId
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.protocol.RoomStatusId

@Composable
fun LobbyScreen(
    title: String,
    subtitle: String,
    gameId: String? = null,
    mode: GameModeId? = null,
    me: PlayerId? = null,
    roomStatus: RoomStatusId? = null,
    showStart: Boolean,
    onStart: () -> Unit,
    onDisconnect: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 500.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header con emoji según modo
                val emoji = when (mode) {
                    GameModeId.PVP -> "⚔️"
                    GameModeId.PVE -> "🤖"
                    null -> "🎮"
                }

                Text(
                    text = "$emoji $title",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Info de sala
                if (gameId != null) {
                    HorizontalDivider()

                    Text(
                        text = "Sala: $gameId",
                        style = MaterialTheme.typography.labelLarge
                    )

                    if (mode != null) {
                        Text(
                            text = "Modo: ${if (mode == GameModeId.PVP) "PVP" else "PVE"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (me != null) {
                        Text(
                            text = "Eres: ${me.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Indicador de estado
                when (roomStatus) {
                    RoomStatusId.WAITING -> {
                        HorizontalDivider()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = if (mode == GameModeId.PVP) {
                                    "Esperando rival..."
                                } else {
                                    "Preparando IA..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    RoomStatusId.READY -> {
                        HorizontalDivider()
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,  // ✅ AQUÍ SE USA Icons
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "¡Sala lista!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    null -> { /* Sin estado */ }
                }

                // Botones
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salir")
                    }

                    if (showStart) {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Iniciar")
                        }
                    }
                }
            }
        }
    }
}