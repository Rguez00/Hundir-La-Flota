package com.mario.hlf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Fondo espacial/oceánico
        Image(
            painter = painterResource("drawable/HLF_ui_bg_grid.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ✅ Overlay oscuro
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // ✅ Contenido principal
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
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
                    containerColor = Color(0xFF4A0A0A).copy(alpha = 0.9f) // Rojo oscuro
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "⚠️",
                        fontSize = 24.sp
                    )
                    Text(
                        "RIVAL DESCONECTADO - VICTORIA POR ABANDONO",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF0000),
                        letterSpacing = 1.sp
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
                Button(
                    onClick = onBackToLobby,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF00),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "🏠 Volver al Lobby",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF00FF00)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "❌ Desconectar",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
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
                Color(0xFF1A3A1A).copy(alpha = 0.9f) // Verde oscuro
            else
                Color(0xFF0D1117).copy(alpha = 0.9f) // Gris muy oscuro
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (isMyTurn) "🎯 TU TURNO" else "⏳ Turno del Rival",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMyTurn) Color(0xFF00FF00) else Color(0xFF00D9FF),
                    letterSpacing = 1.sp
                )
                Text(
                    if (isMyTurn) "Haz click en el radar enemigo para disparar" else "Espera tu turno...",
                    fontSize = 12.sp,
                    color = if (isMyTurn)
                        Color(0xFF00FF00).copy(alpha = 0.7f)
                    else
                        Color(0xFF00D9FF).copy(alpha = 0.7f)
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
                        secondsRemaining > 30 -> Color(0xFF00FF00)
                        secondsRemaining > 10 -> Color(0xFFFFFF00) // Amarillo
                        else -> Color(0xFFFF0000)
                    }

                    Text(
                        "⏱️ ${secondsRemaining}s",
                        fontSize = 24.sp,
                        color = timeColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF00D9FF)
                    )
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
                Color(0xFF1A3A1A).copy(alpha = 0.9f) // Verde oscuro
            else
                Color(0xFF4A0A0A).copy(alpha = 0.9f) // Rojo oscuro
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (won) "🎉 ¡VICTORIA!" else "💔 DERROTA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (won) Color(0xFF00FF00) else Color(0xFFFF0000),
                letterSpacing = 2.sp
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
                fontSize = 14.sp,
                color = if (won)
                    Color(0xFF00FF00).copy(alpha = 0.8f)
                else
                    Color(0xFFFF0000).copy(alpha = 0.8f)
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
            containerColor = Color(0xFF0D1117).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "📜 HISTORIAL",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF00),
                letterSpacing = 1.sp
            )

            HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.3f))

            // Sección: Tus disparos
            Text(
                "TUS DISPAROS:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF00),
                letterSpacing = 1.sp
            )

            if (myShots.isEmpty()) {
                Text(
                    "Aún no has disparado",
                    fontSize = 11.sp,
                    color = Color(0xFF00FF00).copy(alpha = 0.5f)
                )
            } else {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    myShots.takeLast(10).reversed().forEach { move ->
                        MoveItem(move)
                    }
                }
            }

            HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.3f))

            // Sección: Disparos del rival
            Text(
                if (mode == GameModeId.PVE) "DISPAROS IA:" else "DISPAROS RIVAL:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D9FF),
                letterSpacing = 1.sp
            )

            if (opponentShots.isEmpty()) {
                Text(
                    "Rival no ha disparado",
                    fontSize = 11.sp,
                    color = Color(0xFF00D9FF).copy(alpha = 0.5f)
                )
            } else {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
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
            if (move.isHit) "💥" else "💧",
            fontSize = 14.sp
        )
        Text(
            move.coordinate,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (move.isHit)
                Color(0xFFFF0000)
            else
                Color(0xFF00D9FF)
        )
        Text(
            if (move.isHit) "Impacto" else "Agua",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
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