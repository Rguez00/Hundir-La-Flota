package com.mario.hlf.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        // ✅ Contenido principal con distribución profesional
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ✅ HEADER COMPLETO (más compacto)
            PlacementHeader(state, me, mode)

            // ✅ ÁREA PRINCIPAL: Selector + Tablero Central + Controles
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ✅ Panel Izquierdo (Selector de Barcos) - Compacto
                ShipSelectorPanel(
                    selected = selectedShip,
                    onSelect = { selectedShip = it },
                    placedShips = getPlacedShips(state, me),
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                )

                // ✅ Tablero Central (más grande y prominente)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    BoardGrid(
                        title = "🚢 MI FLOTA",
                        board = state.self,
                        showShips = true,
                        onCellClick = { r, c -> onPlaceShip(r, c, selectedShip, orientation) }
                    )
                }

                // ✅ Panel Derecho (Controles)
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Orientación
                    OrientationPanel(
                        orientation = orientation,
                        onOrientationChange = { orientation = it }
                    )

                    // Info de nave seleccionada
                    ShipInfoCard(selectedShip, orientation)

                    Spacer(modifier = Modifier.weight(1f))

                    // Botón abandonar
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00FF00)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "❌ ABANDONAR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // ✅ Ocultar tablero rival en PVP (ya no se muestra en placement)
            if (false && mode == GameModeId.PVP) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (mode == GameModeId.PVP) {
                            BoardGrid(
                                title = "👁️ RIVAL",
                                board = state.opponent,
                                showShips = false,
                                onCellClick = null
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ✅ Header completo de la pantalla de colocación (más compacto)
 */
@Composable
private fun PlacementHeader(state: GameStateDto, me: PlayerId, mode: GameModeId) {
    val totalShips = 5
    val placedCount = countPlacedShips(state.self)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A1628).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Título y modo
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "⚓ DESPLIEGUE DE FLOTA",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00FF00), // Verde fluorescente
                    letterSpacing = 2.sp
                )
                Text(
                    if (mode == GameModeId.PVE) "🤖 ENTRENAMIENTO" else "⚔️ COMBATE PVP",
                    fontSize = 11.sp,
                    color = Color(0xFF88E0F0),
                    letterSpacing = 1.sp
                )
            }

            // Progreso
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "PROGRESO:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF00).copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
                Text(
                    "$placedCount / $totalShips",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (placedCount == totalShips)
                        Color(0xFF00FF00)
                    else
                        Color.White
                )
                Text(
                    "NAVES",
                    fontSize = 10.sp,
                    color = Color(0xFF88E0F0),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * ✅ Panel completo del selector de barcos (con scroll para evitar cortes)
 */
@Composable
private fun ShipSelectorPanel(
    selected: ShipTypeId,
    onSelect: (ShipTypeId) -> Unit,
    placedShips: Set<ShipTypeId>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A1628).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "SELECCIONAR NAVE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FF00), // Verde fluorescente
                letterSpacing = 1.sp
            )

            HorizontalDivider(color = Color(0xFF00FF00).copy(alpha = 0.3f))

            // Lista de barcos con scroll
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                ShipTypeId.entries.forEach { ship ->
                    val isPlaced = ship in placedShips
                    ShipCard(
                        ship = ship,
                        selected = ship == selected,
                        placed = isPlaced,
                        onClick = { if (!isPlaced) onSelect(ship) }
                    )
                }
            }
        }
    }
}

/**
 * ✅ Card individual de barco con imagen (mejorado - más grande + indicador de color)
 */
@Composable
private fun ShipCard(
    ship: ShipTypeId,
    selected: Boolean,
    placed: Boolean,
    onClick: () -> Unit
) {
    val imageResource = when (ship) {
        ShipTypeId.CARRIER -> "drawable/HLF_ship_carrier.png"
        ShipTypeId.BATTLESHIP -> "drawable/HLF_ship_battleship.png"
        ShipTypeId.CRUISER -> "drawable/HLF_ship_cruiser.png"
        ShipTypeId.DESTROYER -> "drawable/HLF_ship_destroyer.png"
        ShipTypeId.SUBMARINE -> "drawable/HLF_ship_cruiser.png"
    }

    // ✅ Borde de color según estado: Verde=disponible, Rojo=colocada, Cyan=seleccionada
    val borderColor = when {
        placed -> Color(0xFFFF0000) // Rojo para colocada
        selected && !placed -> Color(0xFF00D9FF) // Cyan para seleccionada (solo si no está colocada)
        else -> Color(0xFF00FF00) // Verde para disponible
    }

    val borderWidth = when {
        placed -> 3.dp // Rojo más grueso para indicar colocado
        selected && !placed -> 4.dp
        else -> 2.dp
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp) // Más alta para que se vea mejor la imagen
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Transparente para ver la imagen de fondo
        ),
        shape = RoundedCornerShape(8.dp),
        enabled = !placed
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ✅ Imagen de fondo ocupando todo el espacio
            Image(
                painter = painterResource(imageResource),
                contentDescription = getShipName(ship),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (placed) 0.3f else 0.6f
            )

            // ✅ Overlay oscuro
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        when {
                            selected && !placed -> Color(0xFF00D9FF).copy(alpha = 0.3f)
                            placed -> Color.Black.copy(alpha = 0.7f)
                            else -> Color.Black.copy(alpha = 0.5f)
                        }
                    )
            )

            // ✅ Contenido sobre la imagen
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✅ Info del barco
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        getShipName(ship),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (placed) Color(0xFF88E0F0).copy(alpha = 0.5f) else Color(0xFF00FF00)
                    )
                    Text(
                        "${getShipSize(ship)} casillas",
                        fontSize = 12.sp,
                        color = Color(0xFF88E0F0).copy(alpha = if (placed) 0.4f else 0.8f),
                        letterSpacing = 0.5.sp
                    )
                }

                // ✅ Indicador de colocado o seleccionado
                when {
                    placed -> {
                        Text(
                            "✓",
                            fontSize = 36.sp,
                            color = Color(0xFFFF0000), // Rojo
                            fontWeight = FontWeight.Bold
                        )
                    }
                    selected -> {
                        Text(
                            "◉",
                            fontSize = 32.sp,
                            color = Color(0xFF00D9FF), // Cyan
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {
                        Text(
                            "○",
                            fontSize = 24.sp,
                            color = Color(0xFF00FF00), // Verde
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * ✅ Panel completo de orientación
 */
@Composable
private fun OrientationPanel(
    orientation: OrientationId,
    onOrientationChange: (OrientationId) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A1628).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "ORIENTACIÓN",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D9FF),
                letterSpacing = 1.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OrientationButton(
                    text = "HORIZONTAL",
                    icon = "➡️",
                    selected = orientation == OrientationId.HORIZONTAL,
                    onClick = { onOrientationChange(OrientationId.HORIZONTAL) },
                    modifier = Modifier.weight(1f)
                )
                OrientationButton(
                    text = "VERTICAL",
                    icon = "⬇️",
                    selected = orientation == OrientationId.VERTICAL,
                    onClick = { onOrientationChange(OrientationId.VERTICAL) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * ✅ Botón de orientación mejorado con estilo visual naval
 */
@Composable
private fun OrientationButton(
    text: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected)
                Color(0xFF1A3A1A).copy(alpha = 0.9f)
            else
                Color(0xFF0D1117).copy(alpha = 0.8f),
            contentColor = if (selected) Color(0xFF00FF00) else Color(0xFF00FF00).copy(alpha = 0.6f)
        ),
        border = BorderStroke(
            width = if (selected) 3.dp else 2.dp,
            color = if (selected) Color(0xFF00FF00) else Color(0xFF00FF00).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                icon,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * ✅ Card con información de la nave seleccionada
 */
@Composable
private fun ShipInfoCard(ship: ShipTypeId, orientation: OrientationId) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00D9FF).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "◉",
                    fontSize = 20.sp,
                    color = Color(0xFF00D9FF)
                )
                Text(
                    getShipName(ship),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Text(
                "Tamaño: ${getShipSize(ship)} casillas",
                fontSize = 11.sp,
                color = Color(0xFF88E0F0)
            )

            Text(
                "Orientación: ${if (orientation == OrientationId.HORIZONTAL) "Horizontal ➡️" else "Vertical ⬇️"}",
                fontSize = 11.sp,
                color = Color(0xFF88E0F0)
            )

            HorizontalDivider(
                color = Color(0xFF00D9FF).copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                "💡 Click en el tablero para colocar",
                fontSize = 10.sp,
                color = Color(0xFF88E0F0).copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun getShipName(ship: ShipTypeId): String = when (ship) {
    ShipTypeId.CARRIER -> "PORTAAVIONES"
    ShipTypeId.BATTLESHIP -> "ACORAZADO"
    ShipTypeId.CRUISER -> "CRUCERO"
    ShipTypeId.SUBMARINE -> "SUBMARINO"
    ShipTypeId.DESTROYER -> "DESTRUCTOR"
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
    val board = state.self  // Siempre usar el tablero del jugador actual
    val shipInfo = com.mario.hlf.ui.components.detectShips(board)

    val placedTypes = mutableSetOf<ShipTypeId>()

    // Contar cuántos barcos necesitamos de cada tipo
    val size3ShipsNeeded = 2 // CRUISER + SUBMARINE
    var size3ShipsPlaced = 0

    // Contar barcos por tamaño
    val shipsBySize = shipInfo.values.map { it.ship }.distinct().groupBy { it.size }

    // Mapear tamaños detectados a tipos de barco
    shipsBySize.forEach { (size, ships) ->
        when (size) {
            5 -> if (ships.isNotEmpty()) placedTypes.add(ShipTypeId.CARRIER)
            4 -> if (ships.isNotEmpty()) placedTypes.add(ShipTypeId.BATTLESHIP)
            3 -> {
                // Hay 2 barcos de 3 casillas: CRUISER y SUBMARINE
                // Solo marcamos como colocados si tenemos exactamente 2 barcos de tamaño 3
                size3ShipsPlaced = ships.size

                // Si solo hay 1 barco de tamaño 3, NO marcamos ninguno como colocado
                // para permitir que el usuario pueda seleccionar y colocar el segundo
                // Solo cuando hay 2, marcamos ambos como colocados
                if (size3ShipsPlaced >= 2) {
                    placedTypes.add(ShipTypeId.CRUISER)
                    placedTypes.add(ShipTypeId.SUBMARINE)
                }
            }
            2 -> if (ships.isNotEmpty()) placedTypes.add(ShipTypeId.DESTROYER)
        }
    }

    return placedTypes
}

private fun getShipSize(ship: ShipTypeId): Int = when (ship) {
    ShipTypeId.CARRIER -> 5
    ShipTypeId.BATTLESHIP -> 4
    ShipTypeId.CRUISER -> 3
    ShipTypeId.SUBMARINE -> 3
    ShipTypeId.DESTROYER -> 2
}