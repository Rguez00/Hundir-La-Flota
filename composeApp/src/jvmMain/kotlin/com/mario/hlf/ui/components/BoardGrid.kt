package com.mario.hlf.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mario.hlf.protocol.BoardStateDto
import com.mario.hlf.protocol.CellViewId

@Composable
fun BoardGrid(
    title: String,
    board: BoardStateDto,
    showShips: Boolean,
    onCellClick: ((row: Int, col: Int) -> Unit)? = null
) {
    // Detectar barcos en el tablero para renderizado visual
    val shipInfo = remember(board) {
        if (showShips) detectShips(board) else emptyMap()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ✅ Título con estilo naval
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FF00), // Verde fluorescente
            letterSpacing = 2.sp
        )

        // ✅ Grid con coordenadas
        Row {
            // Columna de números (filas)
            Column {
                Box(modifier = Modifier.size(45.dp)) // Esquina vacía
                for (r in 0 until board.size) {
                    Box(
                        modifier = Modifier.size(70.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${r + 1}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF00)
                        )
                    }
                }
            }

            Column {
                // Fila de letras (columnas)
                Row {
                    for (c in 0 until board.size) {
                        Box(
                            modifier = Modifier.size(70.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${'A' + c}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF00)
                            )
                        }
                    }
                }

                // Grid de celdas con estilo oscuro/verde
                Box {
                    Column(
                        modifier = Modifier
                            .border(3.dp, Color(0xFF00FF00)) // Borde verde fluorescente
                            .background(Color(0xFF0A0F0A)) // Fondo muy oscuro
                    ) {
                        for (r in 0 until board.size) {
                            Row {
                                for (c in 0 until board.size) {
                                    val raw = board.cells[r][c]
                                    val cell = if (!showShips && raw == CellViewId.SHIP) CellViewId.EMPTY else raw
                                    val cellShipInfo = shipInfo[r to c]

                                    Cell(
                                        cell = cell,
                                        shipInfo = cellShipInfo,
                                        onClick = onCellClick?.let { { it(r, c) } }
                                    )
                                }
                            }
                        }
                    }

                    // Renderizar barcos completos por encima
                    shipInfo.values
                        .map { it.ship }
                        .distinct()
                        .forEach { ship ->
                            FullShipOverlay(ship)
                        }
                }
            }
        }
    }
}

@Composable
private fun Cell(cell: CellViewId, shipInfo: ShipCellInfo?, onClick: (() -> Unit)?) {
    // ✅ Estilo oscuro con verde fluorescente (tema Matrix/militar)
    val bgColor = when (cell) {
        CellViewId.UNKNOWN -> Color(0xFF0D1117)
        CellViewId.EMPTY -> Color(0xFF0D1117)
        CellViewId.SHIP -> Color(0xFF1A3A1A) // Verde oscuro para barcos
        CellViewId.HIT -> Color(0xFF4A0A0A) // Rojo oscuro para impactos
        CellViewId.MISS -> Color(0xFF0A1A2A) // Azul oscuro para fallos
    }

    Box(
        modifier = Modifier
            .size(70.dp)
            .border(1.dp, Color(0xFF00FF00).copy(alpha = 0.3f))
            .background(bgColor)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when (cell) {
            CellViewId.SHIP -> {
                // No renderizar nada aquí, el barco completo se renderiza en FullShipOverlay
            }
            CellViewId.HIT -> {
                // Mostrar solo explosión, el barco se renderiza en overlay
                Image(
                    painter = painterResource("drawable/HLF_fx_hit_mark.png"),
                    contentDescription = "Hit",
                    modifier = Modifier.size(70.dp),
                    contentScale = ContentScale.Fit
                )
            }
            CellViewId.MISS -> {
                Image(
                    painter = painterResource("drawable/HLF_ui_radar_miss.png"),
                    contentDescription = "Miss",
                    modifier = Modifier.size(42.dp),
                    contentScale = ContentScale.Fit,
                    alpha = 0.8f
                )
            }
            CellViewId.UNKNOWN -> {
                Text("·", fontSize = 22.sp, color = Color(0xFF00FF00).copy(alpha = 0.3f))
            }
            CellViewId.EMPTY -> {
                // Celda vacía
            }
        }
    }
}

/**
 * Renderiza un barco completo como overlay ocupando todas sus casillas
 */
@Composable
private fun FullShipOverlay(ship: DetectedShip) {
    val imageName = ship.type.imageName

    // Obtener coordenadas del barco (ordenadas)
    val sortedCells = when (ship.orientation) {
        ShipOrientation.HORIZONTAL -> ship.cells.sortedBy { it.second } // por columna
        ShipOrientation.VERTICAL -> ship.cells.sortedBy { it.first } // por fila
    }

    if (sortedCells.isEmpty()) return

    val firstCell = sortedCells.first()
    val row = firstCell.first
    val col = firstCell.second

    // Calcular dimensiones del barco - celdas cuadradas
    val cellSize = 70.dp
    val borderWidth = 1.dp
    val totalCellSize = cellSize + borderWidth * 2

    val width = if (ship.orientation == ShipOrientation.HORIZONTAL) {
        totalCellSize * ship.size
    } else {
        totalCellSize
    }

    val height = if (ship.orientation == ShipOrientation.VERTICAL) {
        totalCellSize * ship.size
    } else {
        totalCellSize
    }

    // Posicionar el barco
    val offsetX = col * totalCellSize.value
    val offsetY = row * totalCellSize.value

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .width(width)
            .height(height)
            .padding(0.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource("drawable/${imageName}.png"),
            contentDescription = ship.type.name,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = if (ship.orientation == ShipOrientation.VERTICAL) 90f else 0f
                    scaleX = 1.0f
                    scaleY = 2.2f // Escalar verticalmente para hacer las naves más "gordas"
                },
            contentScale = ContentScale.Fit,
            alpha = 0.95f
        )
    }
}