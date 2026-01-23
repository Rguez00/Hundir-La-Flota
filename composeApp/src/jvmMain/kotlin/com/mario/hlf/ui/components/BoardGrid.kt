package com.mario.hlf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.BoardStateDto
import com.mario.hlf.protocol.CellViewId

@Composable
fun BoardGrid(
    title: String,
    board: BoardStateDto,
    showShips: Boolean,
    onCellClick: ((row: Int, col: Int) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // ✅ Grid con coordenadas
        Row {
            // Columna de números (filas)
            Column {
                Box(modifier = Modifier.size(24.dp)) // Esquina vacía
                for (r in 0 until board.size) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${r + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column {
                // Fila de letras (columnas)
                Row {
                    for (c in 0 until board.size) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${'A' + c}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Grid de celdas
                Column(modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.outline)) {
                    for (r in 0 until board.size) {
                        Row {
                            for (c in 0 until board.size) {
                                val raw = board.cells[r][c]
                                val cell = if (!showShips && raw == CellViewId.SHIP) CellViewId.EMPTY else raw

                                Cell(
                                    cell = cell,
                                    onClick = onCellClick?.let { { it(r, c) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cell(cell: CellViewId, onClick: (() -> Unit)?) {
    // ✅ MEJORADO: Símbolos más visuales y colores
    val (symbol, bgColor) = when (cell) {
        CellViewId.UNKNOWN -> "·" to MaterialTheme.colorScheme.surfaceVariant
        CellViewId.EMPTY -> " " to MaterialTheme.colorScheme.surface
        CellViewId.SHIP -> "🚢" to MaterialTheme.colorScheme.primaryContainer
        CellViewId.HIT -> "💥" to MaterialTheme.colorScheme.errorContainer
        CellViewId.MISS -> "💧" to MaterialTheme.colorScheme.secondaryContainer
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(bgColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            style = MaterialTheme.typography.bodyMedium,
            color = when (cell) {
                CellViewId.HIT -> MaterialTheme.colorScheme.onErrorContainer
                CellViewId.MISS -> MaterialTheme.colorScheme.onSecondaryContainer
                CellViewId.SHIP -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}