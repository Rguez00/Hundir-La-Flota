package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.GameOverEvent
import com.mario.hlf.protocol.GameStateDto
import com.mario.hlf.ui.components.BoardGrid

@Composable
fun BattleScreen(
    state: GameStateDto,
    gameOver: GameOverEvent?,
    onDisconnect: () -> Unit,
    onShoot: (row: Int, col: Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Fase: BATTLE | Turno: ${state.currentTurn}", style = MaterialTheme.typography.titleMedium)

        if (gameOver != null) {
            Text("GAME OVER — Ganador: ${gameOver.winner}", style = MaterialTheme.typography.titleMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            BoardGrid(
                title = "Mi Flota",
                board = state.self,
                showShips = true,
                onCellClick = null
            )

            BoardGrid(
                title = "Radar",
                board = state.opponent,
                showShips = false,
                onCellClick = { r, c -> onShoot(r, c) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDisconnect) { Text("Desconectar") }
        }
    }
}
