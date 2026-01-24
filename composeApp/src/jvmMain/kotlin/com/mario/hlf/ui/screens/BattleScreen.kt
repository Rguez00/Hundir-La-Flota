package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.*
import com.mario.hlf.ui.components.BoardGrid
import kotlinx.coroutines.delay

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

        // ✅ Tableros + Historial
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Tableros
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

            // ✅ Historial de movimientos
            MoveHistory(state, me, mode)
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
 * ✅ MEJORADO: Indicador visual de turno con temporizador
 */
@Composable
private fun TurnIndicator(state: GameStateDto, me: PlayerId) {
    val isMyTurn = state.currentTurn == me
    var secondsRemaining by remember { mutableStateOf(60) }

    // ✅ Temporizador de cuenta regresiva
    LaunchedEffect(state.currentTurn) {
        secondsRemaining = 60 // Reiniciar cuando cambia el turno
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
    }

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

            // ✅ Temporizador visual
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMyTurn) {
                    // Mostrar tiempo restante cuando es tu turno
                    val timeColor = when {
                        secondsRemaining > 30 -> MaterialTheme.colorScheme.onPrimaryContainer
                        secondsRemaining > 10 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    }

                    Text(
                        "⏱️ ${secondsRemaining}s",
                        style = MaterialTheme.typography.headlineSmall,
                        color = timeColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
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

/**
 * ✅ NUEVO: Historial de movimientos (últimos disparos)
 */
@Composable
private fun MoveHistory(state: GameStateDto, me: PlayerId, mode: GameModeId) {
    // Extraer disparos del tablero (celdas HIT y MISS)
    val myShots = remember(state.opponent) {
        buildMoveList(state.opponent, isMyBoard = false)
    }
    val opponentShots = remember(state.self) {
        buildMoveList(state.self, isMyBoard = true)
    }

    Card(
        modifier = Modifier.width(220.dp).fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "📜 Historial",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Sección: Tus disparos
            Text(
                "Tus Disparos:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (myShots.isEmpty()) {
                Text(
                    "Aún no has disparado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    myShots.takeLast(10).reversed().forEach { move ->
                        MoveItem(move)
                    }
                }
            }

            HorizontalDivider()

            // Sección: Disparos del rival
            Text(
                if (mode == GameModeId.PVE) "Disparos IA:" else "Disparos Rival:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )

            if (opponentShots.isEmpty()) {
                Text(
                    "Rival no ha disparado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    opponentShots.takeLast(10).reversed().forEach { move ->
                        MoveItem(move)
                    }
                }
            }
        }
    }
}

/**
 * ✅ Representa un movimiento individual
 */
@Composable
private fun MoveItem(move: Move) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (move.isHit) "💥" else "💨",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            move.coordinate,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (move.isHit)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            if (move.isHit) "Impacto" else "Agua",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * ✅ Modelo de datos para movimiento
 */
private data class Move(
    val coordinate: String,
    val isHit: Boolean
)

/**
 * ✅ Construye lista de movimientos desde el tablero
 */
private fun buildMoveList(board: BoardStateDto, isMyBoard: Boolean): List<Move> {
    val moves = mutableListOf<Move>()

    board.cells.forEachIndexed { row, rowCells ->
        rowCells.forEachIndexed { col, cell ->
            when (cell) {
                CellViewId.HIT -> {
                    moves.add(Move(
                        coordinate = "${('A' + row)}${col + 1}",
                        isHit = true
                    ))
                }
                CellViewId.MISS -> {
                    moves.add(Move(
                        coordinate = "${('A' + row)}${col + 1}",
                        isHit = false
                    ))
                }
                else -> {} // Ignorar UNKNOWN, EMPTY, SHIP
            }
        }
    }

    return moves
}