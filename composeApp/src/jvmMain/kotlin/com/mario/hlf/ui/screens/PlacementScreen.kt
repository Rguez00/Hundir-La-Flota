package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.GameModeId
import com.mario.hlf.protocol.GameStateDto
import com.mario.hlf.protocol.OrientationId
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.protocol.ShipTypeId
import com.mario.hlf.ui.components.BoardGrid

@Composable
fun PlacementScreen(
    state: GameStateDto,
    me: PlayerId,
    mode: GameModeId,
    onDisconnect: () -> Unit,
    onPlaceShip: (row: Int, col: Int, ship: ShipTypeId, orientation: OrientationId) -> Unit
) {
    var selectedShip by remember { mutableStateOf(ShipTypeId.CARRIER) }
    var orientation by remember { mutableStateOf(OrientationId.HORIZONTAL) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ✅ Header con info de fase
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "⚓ Fase: Colocación de Barcos",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        if (mode == GameModeId.PVE) "Contra IA" else "Multijugador",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // ✅ NUEVO: Indicador de progreso
                PlacementProgress(state, me)
            }
        }

        // ✅ Selectores de barco
        Text("Selecciona el barco:", style = MaterialTheme.typography.titleSmall)
        ShipSelector(
            selected = selectedShip,
            onSelect = { selectedShip = it },
            placedShips = getPlacedShips(state, me) // ✅ NUEVO: mostrar cuáles ya colocaste
        )

        // ✅ Selector de orientación
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = orientation == OrientationId.HORIZONTAL,
                onClick = { orientation = OrientationId.HORIZONTAL },
                label = { Text("➡️ Horizontal") }
            )
            FilterChip(
                selected = orientation == OrientationId.VERTICAL,
                onClick = { orientation = OrientationId.VERTICAL },
                label = { Text("⬇️ Vertical") }
            )
        }

        // ✅ Tableros
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.weight(1f)
        ) {
            BoardGrid(
                title = "🚢 Mi Flota",
                board = state.self,
                showShips = true,
                onCellClick = { r, c -> onPlaceShip(r, c, selectedShip, orientation) }
            )

            // ✅ Tablero rival (solo en PVP, muestra progreso)
            if (mode == GameModeId.PVP) {
                BoardGrid(
                    title = "👁️ Rival",
                    board = state.opponent,
                    showShips = false,
                    onCellClick = null
                )
            }
        }

        // ✅ Botones
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDisconnect) {
                Text("❌ Abandonar")
            }
        }

        // ✅ Ayuda
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                "💡 Click en tu tablero para colocar el barco seleccionado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * ✅ NUEVO: Indicador de progreso de colocación
 */
@Composable
private fun PlacementProgress(state: GameStateDto, me: PlayerId) {
    // Contar barcos colocados (células con SHIP en self)
    val totalShips = 5 // CARRIER, BATTLESHIP, CRUISER, SUBMARINE, DESTROYER
    val placedCount = countPlacedShips(state.self)

    Column(horizontalAlignment = Alignment.End) {
        Text(
            "$placedCount / $totalShips",
            style = MaterialTheme.typography.titleLarge,
            color = if (placedCount == totalShips)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            "barcos",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

/**
 * ✅ NUEVO: Selector visual de barcos
 */
@Composable
private fun ShipSelector(
    selected: ShipTypeId,
    onSelect: (ShipTypeId) -> Unit,
    placedShips: Set<ShipTypeId>
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ShipTypeId.entries.forEach { ship ->
            val isPlaced = ship in placedShips

            FilterChip(
                selected = ship == selected,
                onClick = { if (!isPlaced) onSelect(ship) },
                enabled = !isPlaced,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${ship.name} (${getShipSize(ship)})")
                        if (isPlaced) {
                            Spacer(Modifier.width(4.dp))
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    }
}

// ========== HELPERS ==========

private fun countPlacedShips(board: com.mario.hlf.protocol.BoardStateDto): Int {
    // Aproximación: contar celdas con SHIP
    // Mejor sería que el servidor envíe esta info
    var shipCells = 0
    board.cells.forEach { row ->
        row.forEach { cell ->
            if (cell == com.mario.hlf.protocol.CellViewId.SHIP) shipCells++
        }
    }
    // Total de celdas de barcos: 5+4+3+3+2 = 17
    return when {
        shipCells == 0 -> 0
        shipCells in 1..2 -> 1
        shipCells in 3..4 -> 2
        shipCells in 5..7 -> 3
        shipCells in 8..11 -> 4
        else -> 5
    }
}

private fun getPlacedShips(state: GameStateDto, me: PlayerId): Set<ShipTypeId> {
    // ⚠️ Placeholder: el servidor debería enviar esta info
    // Por ahora, inferimos basándonos en celdas
    return emptySet()
}

private fun getShipSize(ship: ShipTypeId): Int = when (ship) {
    ShipTypeId.CARRIER -> 5
    ShipTypeId.BATTLESHIP -> 4
    ShipTypeId.CRUISER -> 3
    ShipTypeId.SUBMARINE -> 3
    ShipTypeId.DESTROYER -> 2
}