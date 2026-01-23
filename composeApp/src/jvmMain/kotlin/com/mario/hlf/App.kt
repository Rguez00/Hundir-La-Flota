package com.mario.hlf

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.mario.hlf.protocol.PhaseId
import com.mario.hlf.protocol.RoomStatusId
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.ui.GameController
import com.mario.hlf.ui.GameModeUi
import com.mario.hlf.ui.GameUiState
import com.mario.hlf.ui.screens.BattleScreen
import com.mario.hlf.ui.screens.LobbyScreen
import com.mario.hlf.ui.screens.MainMenuScreen
import com.mario.hlf.ui.screens.PlacementScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Composable
fun App(onExit: () -> Unit) {
    val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    val controller = remember { GameController(appScope) }
    val uiState by controller.uiState.collectAsState()

    MaterialTheme {
        when (val s = uiState) {

            is GameUiState.Disconnected -> {
                MainMenuScreen(
                    defaultHost = "127.0.0.1",
                    defaultPort = 5678,
                    defaultName = "Mario",
                    // ✅ ahora pasamos mode (si tu MainMenuScreen aún no lo soporta, déjalo fijo en PVP)
                    onConnect = { host, port, name, mode ->
                        controller.connect(host, port, name, mode)
                    },
                    onExit = onExit
                )
            }

            is GameUiState.Connecting -> {
                LobbyScreen(
                    title = "Conectando…",
                    subtitle = "Conectando a ${s.host}:${s.port}",
                    showStart = false,
                    onStart = null,
                    onDisconnect = { controller.disconnect() }
                )
            }

            is GameUiState.Connected -> {
                val isReady = (s.roomStatus == RoomStatusId.READY)
                val canStart = isReady && (s.me == PlayerId.P1) && (s.mode == GameModeUi.PVP)

                val subtitle = when {
                    s.mode == GameModeUi.PVE -> "Modo PVE (contra IA)"
                    isReady -> "Sala lista. Puedes iniciar la partida."
                    else -> "Esperando a otro jugador…"
                }

                LobbyScreen(
                    title = "Lobby",
                    subtitle = subtitle,
                    gameId = s.gameId,
                    // ✅ START solo si READY y eres P1 (en PVP)
                    showStart = canStart,
                    onStart = if (canStart) ({ controller.startGame() }) else null,
                    onDisconnect = { controller.disconnect() }
                )
            }

            is GameUiState.InGame -> {
                val st = s.state

                when (st.phase) {
                    PhaseId.PLACEMENT -> {
                        PlacementScreen(
                            state = st,
                            onDisconnect = { controller.disconnect() },
                            onStartGame = { controller.startGame() },
                            // ✅ ya NO pasamos player (lo calcula el controller)
                            onPlaceShip = { row, col, ship, orientation ->
                                controller.placeShip(row, col, ship, orientation)
                            }
                        )
                    }

                    PhaseId.BATTLE -> {
                        BattleScreen(
                            state = st,
                            gameOver = s.gameOver,
                            onDisconnect = { controller.disconnect() },
                            // ✅ ya NO pasamos player
                            onShoot = { row, col ->
                                controller.shoot(row, col)
                            }
                        )
                    }

                    PhaseId.OVER -> {
                        LobbyScreen(
                            title = "Game Over",
                            subtitle = "Ganador: ${s.gameOver?.winner ?: "?"}",
                            showStart = false,
                            onStart = null,
                            onDisconnect = { controller.disconnect() }
                        )
                    }
                }
            }

            is GameUiState.Error -> {
                LobbyScreen(
                    title = "Error",
                    subtitle = s.message,
                    showStart = false,
                    onStart = null,
                    onDisconnect = { controller.disconnect() }
                )
            }
        }
    }
}
