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
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.BoardStateDto
import com.mario.hlf.protocol.CellViewId

@Composable
fun BoardGrid(
    title: String,
    board: BoardStateDto,
    onCellClick: ((row: Int, col: Int) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        Column(
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            for (r in 0 until board.size) {
                Row {
                    for (c in 0 until board.size) {
                        val cell = board.cells[r][c]
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

@Composable
private fun Cell(cell: CellViewId, onClick: (() -> Unit)?) {
    val symbol = when (cell) {
        CellViewId.UNKNOWN -> "·"
        CellViewId.EMPTY -> " "
        CellViewId.SHIP -> "■"
        CellViewId.HIT -> "X"
        CellViewId.MISS -> "o"
    }

    Box(
        modifier = Modifier
            .size(28.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, style = MaterialTheme.typography.bodyMedium)
    }
}
