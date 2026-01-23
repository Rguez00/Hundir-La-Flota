package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LobbyScreen(
    title: String,
    subtitle: String,
    gameId: String? = null,
    showStart: Boolean,
    onStart: (() -> Unit)?,
    onDisconnect: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)

            if (gameId != null) {
                Text("gameId: $gameId", style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showStart && onStart != null) {
                    Button(onClick = onStart) { Text("START_GAME") }
                }
                OutlinedButton(onClick = onDisconnect) { Text("Desconectar") }
            }
        }
    }
}
