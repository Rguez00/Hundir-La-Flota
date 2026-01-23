package com.mario.hlf.ui

import com.mario.hlf.network.client.GameEventLoopClient
import com.mario.hlf.network.client.GameTcpClient
import com.mario.hlf.network.client.TcpClientConnection
import com.mario.hlf.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.CoroutineContext

class GameController(
    private val scope: CoroutineScope
) {
    private var conn: TcpClientConnection? = null
    private var api: GameTcpClient? = null
    private var loop: GameEventLoopClient? = null

    private var currentGameId: String? = null

    private var bindLatestJob: Job? = null
    private var bindGameOverJob: Job? = null

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Disconnected)
    val uiState: StateFlow<GameUiState> = _uiState

    // ✅ Scope REAL para red/IO (evita bloquear UI)
    private val ioScope = CoroutineScope(scope.coroutineContext + Dispatchers.IO)

    fun connect(host: String, port: Int, name: String) {
        // UI -> Connecting siempre en Main
        scope.launch(Dispatchers.Main) {
            _uiState.value = GameUiState.Connecting(host, port, name)
        }

        ioScope.launch {
            try {
                safeCloseAll()

                val c = TcpClientConnection.connect(host, port)
                conn = c

                val a = GameTcpClient(c)
                api = a

                // ✅ Handshake antes del loop (solo hay 1 lector aquí)
                val gid = a.hello(playerName = name)
                currentGameId = gid

                // ✅ lectura infinita para el loop
                c.setReadTimeout(0)

                // ✅ Loop leyendo SIEMPRE en IO
                val l = GameEventLoopClient(a, ioScope)
                loop = l
                l.start()

                // ✅ Collectors pueden vivir en scope (Main) sin bloquear,
                // porque solo consumen StateFlows, no hacen I/O
                bindStateFlows(l)

                // ✅ Cambiar pantalla SIEMPRE en Main
                withContext(Dispatchers.Main) {
                    _uiState.value = GameUiState.Connected(host, port, name, gid)
                }
            } catch (t: Throwable) {
                safeCloseAll()
                withContext(Dispatchers.Main) {
                    _uiState.value = GameUiState.Error(t.toString())
                }
            }
        }
    }

    fun startGame() {
        api?.sendStartGame(boardSize = 10, allowAdjacency = false)
    }

    fun placeShip(player: PlayerId, row: Int, col: Int, ship: ShipTypeId, orientation: OrientationId) {
        api?.sendPlaceShip(player, row, col, ship, orientation)
    }

    fun shoot(player: PlayerId, row: Int, col: Int) {
        api?.sendShoot(player, row, col)
    }

    private fun bindStateFlows(l: GameEventLoopClient) {
        bindLatestJob?.cancel()
        bindGameOverJob?.cancel()

        bindLatestJob = scope.launch {
            l.latestState.collect { st ->
                if (st != null) {
                    _uiState.update { prev ->
                        val gid = when (prev) {
                            is GameUiState.Connected -> prev.gameId
                            is GameUiState.InGame -> prev.gameId
                            else -> currentGameId ?: "UNKNOWN"
                        }
                        val prevGameOver = (prev as? GameUiState.InGame)?.gameOver
                        GameUiState.InGame(gameId = gid, state = st, gameOver = prevGameOver)
                    }
                }
            }
        }

        bindGameOverJob = scope.launch {
            l.gameOver.collect { go ->
                if (go != null) {
                    _uiState.update { prev ->
                        val cur = prev as? GameUiState.InGame
                        if (cur != null) cur.copy(gameOver = go) else prev
                    }
                }
            }
        }
    }

    fun disconnect() {
        ioScope.launch {
            safeCloseAll()
            withContext(Dispatchers.Main) {
                _uiState.value = GameUiState.Disconnected
            }
        }
    }

    private suspend fun safeCloseAll() {
        bindLatestJob?.cancel()
        bindGameOverJob?.cancel()
        bindLatestJob = null
        bindGameOverJob = null

        currentGameId = null

        val l = loop
        loop = null
        runCatching { l?.stop() }

        val a = api
        api = null
        runCatching { a?.close() }

        val c = conn
        conn = null
        runCatching { c?.close() }
    }
}
