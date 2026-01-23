package com.mario.hlf

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mario.hlf.protocol.OrientationId
import com.mario.hlf.protocol.PhaseId
import com.mario.hlf.protocol.PlayerId
import com.mario.hlf.protocol.ShipTypeId
import com.mario.hlf.ui.GameController
import com.mario.hlf.ui.GameUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope

@Composable
fun App(onExit: () -> Unit) {
    val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    val controller = remember { GameController(appScope) }
    val uiState by controller.uiState.collectAsState()

    MaterialTheme {
        when (val s = uiState) {
            is GameUiState.Disconnected -> MainMenu(
                onConnect = { host, port, name -> controller.connect(host, port, name) },
                onExit = onExit
            )

            is GameUiState.Connecting -> SimpleCentered(
                text = "Conectando a ${s.host}:${s.port}…"
            )

            is GameUiState.Connected -> Lobby(
                gameId = s.gameId,
                onStart = { controller.startGame() },
                onDisconnect = { controller.disconnect() }
            )

            is GameUiState.InGame -> {
                val st = s.state

                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Fase: ${st.phase} | Turno: ${st.currentTurn}", style = MaterialTheme.typography.titleMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        com.mario.hlf.ui.components.BoardGrid(
                            title = "Mi Flota",
                            board = st.self,
                            onCellClick = null // en placement lo haremos con selector
                        )

                        com.mario.hlf.ui.components.BoardGrid(
                            title = "Radar",
                            board = st.opponent,
                            onCellClick = { r, c ->
                                // Solo disparar en BATTLE (por ahora dejamos que el server valide también)
                                controller.shoot(player = PlayerId.P1, row = r, col = c)
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { controller.startGame() }) { Text("START_GAME") }
                        OutlinedButton(onClick = { controller.disconnect() }) { Text("Desconectar") }
                    }
                }
            }


            is GameUiState.Error -> ErrorScreen(
                message = s.message,
                onBack = { controller.disconnect() }
            )
        }
    }
}

/* -------- UI mínima “placeholder” -------- */

@Composable
private fun MainMenu(onConnect: (String, Int, String) -> Unit, onExit: () -> Unit) {
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("5678") }
    var name by remember { mutableStateOf("Mario") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Hundir la Flota", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(host, { host = it }, label = { Text("Host") })
        OutlinedTextField(port, { port = it }, label = { Text("Port") })
        OutlinedTextField(name, { name = it }, label = { Text("Nombre") })

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onConnect(host, port.toIntOrNull() ?: 5678, name) }) { Text("Conectar") }
            OutlinedButton(onClick = onExit) { Text("Salir") }
        }
    }
}

@Composable
private fun Lobby(gameId: String, onStart: () -> Unit, onDisconnect: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lobby", style = MaterialTheme.typography.headlineSmall)
        Text("gameId: $gameId")
        Text("Cuando P2 haga HELLO, P1 puede START_GAME.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStart) { Text("START_GAME (P1)") }
            OutlinedButton(onClick = onDisconnect) { Text("Desconectar") }
        }
    }
}

@Composable
private fun PlacementScreenStub(stateInfo: String, onPlaceDemo: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Placement", style = MaterialTheme.typography.headlineSmall)
        Text(stateInfo)
        Button(onClick = onPlaceDemo) { Text("PLACE demo") }
        Text("Luego aquí meteremos tu tablero real + selector de barco + orientación.")
    }
}

@Composable
private fun BattleScreenStub(stateInfo: String, onShootDemo: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Battle", style = MaterialTheme.typography.headlineSmall)
        Text(stateInfo)
        Button(onClick = onShootDemo) { Text("SHOOT demo") }
        Text("Luego aquí tu UI: Mi flota / Radar / logs.")
    }
}

@Composable
private fun GameOverScreen(winner: PlayerId?, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GAME OVER", style = MaterialTheme.typography.headlineMedium)
        Text("Ganador: ${winner ?: "?"}")
        Button(onClick = onExit) { Text("Volver al menú") }
    }
}

@Composable
private fun ErrorScreen(message: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Error", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
        Text(message)
        Button(onClick = onBack) { Text("Volver") }
    }
}

@Composable
private fun SimpleCentered(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
