package com.mario.hlf.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.protocol.RoomStatusId

@Composable
fun LobbyScreen(
    title: String,
    subtitle: String,
    gameId: String? = null,
    mode: GameModeId? = null,
    me: PlayerId? = null,
    roomStatus: RoomStatusId? = null,
    showStart: Boolean,
    onStart: () -> Unit,
    onDisconnect: () -> Unit
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

        // ✅ Contenido centrado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ✅ Panel del lobby
            Card(
                modifier = Modifier
                    .width(600.dp)
                    .shadow(16.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A1628).copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // ✅ Título con estilo
                    Text(
                        text = title.uppercase(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00D9FF),
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        color = Color(0xFF88E0F0),
                        textAlign = TextAlign.Center
                    )

                    HorizontalDivider(color = Color(0xFF00D9FF).copy(alpha = 0.3f))

                    // ✅ Info de sala profesional
                    if (gameId != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoRow("ID DE BATALLA", formatGameId(gameId))

                            if (mode != null) {
                                InfoRow(
                                    "MODO",
                                    if (mode == GameModeId.PVP) "⚔️ COMBATE PVP" else "🤖 ENTRENAMIENTO PVE"
                                )
                            }

                            if (me != null) {
                                InfoRow("ROL", formatPlayerRole(me))
                            }
                        }

                        HorizontalDivider(color = Color(0xFF00D9FF).copy(alpha = 0.3f))
                    }

                    // ✅ Indicador de estado
                    when (roomStatus) {
                        RoomStatusId.WAITING -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    color = Color(0xFF00D9FF)
                                )
                                Text(
                                    text = if (mode == GameModeId.PVP) {
                                        "ESPERANDO RIVAL..."
                                    } else {
                                        "INICIANDO SISTEMAS IA..."
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00D9FF),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        RoomStatusId.READY -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF00D9FF).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF00D9FF),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "¡SALA LISTA PARA COMBATE!",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00D9FF),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                        null -> { /* Sin estado */ }
                    }

                    // ✅ Botones con estilo naval
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (showStart) {
                            Button(
                                onClick = onStart,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .shadow(8.dp, RoundedCornerShape(8.dp)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00D9FF),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "⚓ INICIAR COMBATE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF00D9FF)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "❌ SALIR",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ✅ Fila de información con estilo naval
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D9FF).copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * ✅ Formatea el GameId a algo más profesional
 */
private fun formatGameId(gameId: String): String {
    // Toma los primeros 8 caracteres y formatea como ID militar
    val short = gameId.take(8).uppercase()
    return "OP-$short"
}

/**
 * ✅ Formatea el rol del jugador
 */
private fun formatPlayerRole(player: PlayerId): String {
    return when (player) {
        PlayerId.P1 -> "🎖️ COMANDANTE (P1)"
        PlayerId.P2 -> "🎖️ ALMIRANTE (P2)"
        PlayerId.AI -> "🤖 IA ENEMIGA"
    }
}