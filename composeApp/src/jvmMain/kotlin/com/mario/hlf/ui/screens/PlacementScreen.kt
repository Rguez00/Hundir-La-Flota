package com.mario.hlf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.GameStateDto
import com.mario.hlf.protocol.OrientationId
import com.mario.hlf.protocol.ShipTypeId
import com.mario.hlf.ui.components.BoardGrid

@Composable
fun PlacementScreen(
    state: GameStateDto,
    onDisconnect: () -> Unit,
    onStartGame: () -> Unit,
    onPlaceShip: (row: Int, col: Int, ship: ShipTypeId, orientation: OrientationId) -> Unit
) {
    var ship by remember { mutableStateOf(ShipTypeId.DESTROYER) }
    var orientation by remember { mutableStateOf(OrientationId.HORIZONTAL) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Fase: PLACEMENT | Turno: ${state.currentTurn}", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShipSelector(
                selected = ship,
                onSelect = { ship = it }
            )
            OrientationSelector(
                selected = orientation,
                onSelect = { orientation = it }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            // ✅ En placement: tu tablero sí debe enseñar barcos
            BoardGrid(
                title = "Mi Flota",
                board = state.self,
                showShips = true,
                onCellClick = { r, c -> onPlaceShip(r, c, ship, orientation) }
            )

            BoardGrid(
                title = "Radar",
                board = state.opponent,
                showShips = false,
                onCellClick = null
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStartGame) { Text("START_GAME") }
            OutlinedButton(onClick = onDisconnect) { Text("Desconectar") }
        }

        Text(
            "Tip: selecciona barco + orientación y haz click en tu tablero para colocarlo.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ShipSelector(selected: ShipTypeId, onSelect: (ShipTypeId) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShipTypeId.entries.forEach { st ->
            FilterChip(
                selected = st == selected,
                onClick = { onSelect(st) },
                label = { Text(st.name) }
            )
        }
    }
}

@Composable
private fun OrientationSelector(selected: OrientationId, onSelect: (OrientationId) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == OrientationId.HORIZONTAL,
            onClick = { onSelect(OrientationId.HORIZONTAL) },
            label = { Text("HORIZONTAL") }
        )
        FilterChip(
            selected = selected == OrientationId.VERTICAL,
            onClick = { onSelect(OrientationId.VERTICAL) },
            label = { Text("VERTICAL") }
        )
    }
}
