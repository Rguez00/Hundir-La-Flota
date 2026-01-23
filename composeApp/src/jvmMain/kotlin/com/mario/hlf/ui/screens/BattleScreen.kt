package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.*
import com.mario.hlf.ui.components.BoardGrid

@Composable
fun BattleScreen(
    state: GameStateDto,
    me: PlayerId,
    mode: GameModeId,
    gameOver: GameOverEvent?,
    opponentDisconnected: Boolean,
    onDisconnect: () -> Unit,
    onBackToLobby: () -> Unit,
    onShoot: (row: Int, col: Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ✅ Header con estado de turno
        if (gameOver == null && !opponentDisconnected) {
            TurnIndicator(state, me)
        }

        // ✅ Game Over Banner
        if (gameOver != null) {
            GameOverBanner(gameOver, me)
        }

        // ✅ Desconexión del rival
        if (opponentDisconnected && gameOver == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚠️ Rival desconectado - Victoria por abandono",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // ✅ Tableros
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.weight(1f)
        ) {
            BoardGrid(
                title = "🛡️ Mi Flota",
                board = state.self,
                showShips = true,
                onCellClick = null
            )

            BoardGrid(
                title = if (mode == GameModeId.PVE) "🤖 IA" else "🎯 Radar Enemigo",
                board = state.opponent,
                showShips = false,
                onCellClick = if (gameOver == null && !opponentDisconnected && state.currentTurn == me) {
                    { r, c -> onShoot(r, c) }
                } else null
            )
        }

        // ✅ Botones
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (gameOver != null || opponentDisconnected) {
                Button(onClick = onBackToLobby) {
                    Text("🏠 Volver al Lobby")
                }
            }

            OutlinedButton(onClick = onDisconnect) {
                Text("❌ Desconectar")
            }
        }
    }
}

/**
 * ✅ NUEVO: Indicador visual de turno
 */
@Composable
private fun TurnIndicator(state: GameStateDto, me: PlayerId) {
    val isMyTurn = state.currentTurn == me

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMyTurn)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (isMyTurn) "🎯 TU TURNO" else "⏳ Turno del Rival",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isMyTurn)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isMyTurn) "Haz click en el radar enemigo para disparar" else "Espera tu turno...",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isMyTurn)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (!isMyTurn) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

/**
 * ✅ NUEVO: Banner de Game Over
 */
@Composable
private fun GameOverBanner(gameOver: GameOverEvent, me: PlayerId) {
    val won = gameOver.winner == me

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (won)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (won) "🎉 ¡VICTORIA!" else "💔 Derrota",
                style = MaterialTheme.typography.headlineMedium,
                color = if (won)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                when (gameOver.reason) {
                    GameOverReason.ALL_SHIPS_SUNK ->
                        if (won) "Has hundido toda la flota enemiga" else "Tu flota ha sido destruida"
                    GameOverReason.OPPONENT_DISCONNECTED ->
                        "Victoria por abandono del rival"
                    GameOverReason.TIMEOUT ->
                        "Tiempo agotado"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (won)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                else
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}