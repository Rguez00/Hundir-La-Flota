package com.mario.hlf

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.mario.hlf.protocol.GameModeId
import com.mario.hlf.protocol.PhaseId
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.protocol.RoomStatusId
import com.mario.hlf.ui.GameController
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
                    onConnect = { host, port, name, mode ->
                        controller.connect(host, port, name, mode)
                    },
                    onExit = onExit
                )
            }

            is GameUiState.Connecting -> {
                LobbyScreen(
                    title = "Conectando",
                    subtitle = "Conectando a ${s.host}:${s.port}...",
                    mode = s.mode,
                    showStart = false,
                    onStart = {},
                    onDisconnect = { controller.disconnect() }
                )
            }

            is GameUiState.Connected -> {
                val isReady = (s.roomStatus == RoomStatusId.READY)

                val canStart = when (s.mode) {
                    GameModeId.PVP -> isReady && s.me == PlayerId.P1
                    GameModeId.PVE -> isReady
                }

                val subtitle = when {
                    s.mode == GameModeId.PVE && isReady ->
                        "IA lista. Puedes iniciar la partida."
                    s.mode == GameModeId.PVP && isReady && s.me == PlayerId.P1 ->
                        "Sala completa. Eres el anfitrión, puedes iniciar."
                    s.mode == GameModeId.PVP && isReady && s.me == PlayerId.P2 ->
                        "Sala completa. Esperando a que el anfitrión inicie..."
                    s.mode == GameModeId.PVP && !isReady ->
                        "Buscando rival..."
                    else ->
                        "Esperando..."
                }

                LobbyScreen(
                    title = "Lobby",
                    subtitle = subtitle,
                    gameId = s.gameId,
                    mode = s.mode,
                    me = s.me,
                    roomStatus = s.roomStatus,
                    showStart = canStart,
                    onStart = { controller.startGame() }, // ✅ CORREGIDO: Siempre lambda
                    onDisconnect = { controller.disconnect() }
                )
            }

            is GameUiState.InGame -> {
                val st = s.state

                when (st.phase) {
                    PhaseId.PLACEMENT -> {
                        PlacementScreen(
                            state = st,
                            me = s.me,
                            mode = s.mode,
                            onDisconnect = { controller.disconnect() },
                            onPlaceShip = { row, col, ship, orientation ->
                                controller.placeShip(row, col, ship, orientation)
                            }
                        )
                    }

                    PhaseId.BATTLE -> {
                        BattleScreen(
                            state = st,
                            me = s.me,
                            mode = s.mode,
                            gameOver = s.gameOver,
                            opponentDisconnected = s.opponentDisconnected,
                            onDisconnect = { controller.disconnect() },
                            onBackToLobby = { controller.backToLobby() },
                            onShoot = { row, col ->
                                controller.shoot(row, col)
                            }
                        )
                    }

                    PhaseId.OVER -> {
                        val won = s.gameOver?.winner == s.me

                        LobbyScreen(
                            title = "Game Over",
                            subtitle = if (won) "🎉 ¡Has ganado!" else "💔 Has perdido",
                            gameId = s.gameId,
                            mode = s.mode,
                            showStart = false,
                            onStart = {},
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
                    onStart = {},
                    onDisconnect = { controller.disconnect() }
                )
            }
        }
    }
}