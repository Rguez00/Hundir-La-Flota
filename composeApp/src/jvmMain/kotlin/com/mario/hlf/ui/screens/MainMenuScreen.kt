package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mario.hlf.ui.GameModeUi

@Composable
fun MainMenuScreen(
    defaultHost: String,
    defaultPort: Int,
    defaultName: String,
    onConnect: (host: String, port: Int, name: String, mode: GameModeUi) -> Unit,
    onExit: () -> Unit
) {
    var host by remember { mutableStateOf(defaultHost) }
    var portText by remember { mutableStateOf(defaultPort.toString()) }
    var name by remember { mutableStateOf(defaultName) }
    var mode by remember { mutableStateOf(GameModeUi.PVP) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Hundir la Flota", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host") },
            singleLine = true
        )
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it.filter { ch -> ch.isDigit() }.take(5) },
            label = { Text("Puerto") },
            singleLine = true
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = mode == GameModeUi.PVP,
                onClick = { mode = GameModeUi.PVP },
                label = { Text("PVP") }
            )
            FilterChip(
                selected = mode == GameModeUi.PVE,
                onClick = { mode = GameModeUi.PVE },
                label = { Text("PVE") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: defaultPort
                    onConnect(host.trim(), port, name.trim().ifBlank { defaultName }, mode)
                }
            ) { Text("Conectar") }

            OutlinedButton(onClick = onExit) { Text("Salir") }
        }

        if (mode == GameModeUi.PVE) {
            Text(
                "PVE: luego haremos que el servidor cree un rival IA automáticamente.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
