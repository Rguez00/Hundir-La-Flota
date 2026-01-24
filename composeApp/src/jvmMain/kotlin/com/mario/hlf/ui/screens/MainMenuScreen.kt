package com.mario.hlf.ui.screens

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mario.hlf.protocol.GameModeId

@Composable
fun MainMenuScreen(
    defaultHost: String,
    defaultPort: Int,
    defaultName: String,
    onConnect: (host: String, port: Int, name: String, mode: GameModeId) -> Unit,
    onViewRecords: (() -> Unit)? = null,
    onViewSettings: (() -> Unit)? = null,
    onExit: () -> Unit
) {
    var host by remember { mutableStateOf(defaultHost) }
    var portText by remember { mutableStateOf(defaultPort.toString()) }
    var name by remember { mutableStateOf(defaultName) }
    var mode by remember { mutableStateOf(GameModeId.PVP) }

    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Fondo espacial/oceánico
        Image(
            painter = painterResource("drawable/HLF_ui_bg_grid.png"),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ✅ Overlay oscuro para mejorar legibilidad
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        // ✅ Contenido principal centrado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ✅ TÍTULO ÉPICO
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text(
                    "HUNDIR LA FLOTA",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00D9FF), // Cyan brillante
                    letterSpacing = 4.sp,
                    style = MaterialTheme.typography.displayLarge.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFF0088AA),
                            offset = androidx.compose.ui.geometry.Offset(4f, 4f),
                            blurRadius = 8f
                        )
                    )
                )
                Text(
                    "NAVAL WARFARE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF88E0F0),
                    letterSpacing = 6.sp
                )
            }

            // ✅ Panel de conexión con diseño de videojuego
            Card(
                modifier = Modifier
                    .width(700.dp)
                    .heightIn(max = 700.dp)
                    .shadow(16.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A1628).copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(40.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Campos de conexión con estilo mejorado
                    NavalTextField(
                        value = name,
                        onValueChange = { name = it.take(20) },
                        label = "NOMBRE DEL CAPITÁN",
                        placeholder = "Almirante"
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NavalTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = "SERVIDOR",
                            placeholder = "127.0.0.1",
                            modifier = Modifier.weight(1.5f)
                        )

                        NavalTextField(
                            value = portText,
                            onValueChange = { portText = it.filter { ch -> ch.isDigit() }.take(5) },
                            label = "PUERTO",
                            placeholder = "5678",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF00D9FF).copy(alpha = 0.3f))

                    // Selector de modo con estilo naval
                    Text(
                        "MODO DE BATALLA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D9FF),
                        letterSpacing = 2.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavalModeButton(
                            text = "PVP",
                            subtitle = "Jugador vs Jugador",
                            icon = "⚔️",
                            selected = mode == GameModeId.PVP,
                            onClick = { mode = GameModeId.PVP },
                            modifier = Modifier.weight(1f)
                        )

                        NavalModeButton(
                            text = "PVE",
                            subtitle = "Jugador vs IA",
                            icon = "🤖",
                            selected = mode == GameModeId.PVE,
                            onClick = { mode = GameModeId.PVE },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF00D9FF).copy(alpha = 0.3f))

                    // Botones principales con estilo naval
                    NavalButton(
                        text = "INICIAR BATALLA",
                        icon = "⚓",
                        primary = true,
                        onClick = {
                            val port = portText.toIntOrNull() ?: defaultPort
                            val playerName = name.trim().ifBlank { defaultName }
                            onConnect(host.trim(), port, playerName, mode)
                        }
                    )

                    // Botones secundarios
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (onViewRecords != null) {
                            NavalButton(
                                text = "RECORDS",
                                icon = "🏆",
                                primary = false,
                                onClick = onViewRecords,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (onViewSettings != null) {
                            NavalButton(
                                text = "CONFIG",
                                icon = "⚙️",
                                primary = false,
                                onClick = onViewSettings,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        NavalButton(
                            text = "SALIR",
                            icon = "❌",
                            primary = false,
                            onClick = onExit,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer con versión
            Text(
                "v1.0 • Proyecto Académico 2026",
                fontSize = 12.sp,
                color = Color(0xFF88E0F0).copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * ✅ TextField con estilo naval
 */
@Composable
private fun NavalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D9FF),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFF88E0F0).copy(alpha = 0.4f)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFE0E0E0),
                focusedBorderColor = Color(0xFF00D9FF),
                unfocusedBorderColor = Color(0xFF00D9FF).copy(alpha = 0.5f),
                cursorColor = Color(0xFF00D9FF)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

/**
 * ✅ Botón de modo con estilo naval (imagen completa)
 */
@Composable
private fun NavalModeButton(
    text: String,
    subtitle: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconResource = when (text) {
        "PVP" -> "drawable/HLF_Icon_PVP.png"
        "PVE" -> "drawable/HLF_Icon_PVE.png"
        else -> "drawable/HLF_Icon_PVP.png"
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .height(120.dp)
            .border(
                width = if (selected) 3.dp else 2.dp,
                color = if (selected) Color(0xFF00D9FF) else Color(0xFF00D9FF).copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Imagen de fondo que ocupa todo
            Image(
                painter = painterResource(iconResource),
                contentDescription = text,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = if (selected) 1f else 0.6f
            )

            // Overlay con texto
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (selected) Color(0xFF00D9FF) else Color.White
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF88E0F0)
                )
            }
        }
    }
}

/**
 * ✅ Botón principal con estilo naval
 */
@Composable
private fun NavalButton(
    text: String,
    icon: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary)
                Color(0xFF00D9FF)
            else
                Color(0xFF0A1628).copy(alpha = 0.8f),
            contentColor = if (primary) Color.Black else Color(0xFF00D9FF)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                icon,
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}