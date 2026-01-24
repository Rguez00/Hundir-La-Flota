package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.GameModeId

@Composable
fun MainMenuScreen(
    defaultHost: String,
    defaultPort: Int,
    defaultName: String,
    onConnect: (host: String, port: Int, name: String, mode: GameModeId) -> Unit,
    onViewRecords: (() -> Unit)? = null,
    onViewSettings: (() -> Unit)? = null,
    onExit: () -> Unit
) {
    var host by remember { mutableStateOf(defaultHost) }
    var portText by remember { mutableStateOf(defaultPort.toString()) }
    var name by remember { mutableStateOf(defaultName) }
    var mode by remember { mutableStateOf(GameModeId.PVP) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ✅ MEJORADO: Título más grande
        Text(
            "🚢 Hundir la Flota",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Campos de conexión
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Servidor") },
            placeholder = { Text("127.0.0.1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter { ch -> ch.isDigit() }.take(5) },
            label = { Text("Puerto") },
            placeholder = { Text("5678") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(20) }, // ✅ NUEVO: límite de caracteres
            label = { Text("Tu nombre") },
            placeholder = { Text("Jugador") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ MEJORADO: Selector de modo más visual
        Text("Modo de juego:", style = MaterialTheme.typography.titleMedium)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = mode == GameModeId.PVP,
                onClick = { mode = GameModeId.PVP },
                label = { Text("⚔️ PVP") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = mode == GameModeId.PVE,
                onClick = { mode = GameModeId.PVE },
                label = { Text("🤖 PVE") },
                modifier = Modifier.weight(1f)
            )
        }

        // ✅ NUEVO: Descripción del modo
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when (mode) {
                        GameModeId.PVP -> "Jugador vs Jugador"
                        GameModeId.PVE -> "Jugador vs IA"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (mode) {
                        GameModeId.PVP -> "Compite contra otro jugador en línea. El servidor te emparejará con un rival disponible."
                        GameModeId.PVE -> "Practica contra la inteligencia artificial del servidor. Ideal para aprender."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botones de acción principal
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: defaultPort
                    val playerName = name.trim().ifBlank { defaultName }
                    onConnect(host.trim(), port, playerName, mode)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🎮 Conectar")
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.weight(1f)
            ) {
                Text("Salir")
            }
        }

        // Botones secundarios (Records y Settings)
        if (onViewRecords != null || onViewSettings != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (onViewRecords != null) {
                    OutlinedButton(
                        onClick = onViewRecords,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📊 Ver Records")
                    }
                }

                if (onViewSettings != null) {
                    OutlinedButton(
                        onClick = onViewSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚙️ Configuración")
                    }
                }
            }
        }
    }
}