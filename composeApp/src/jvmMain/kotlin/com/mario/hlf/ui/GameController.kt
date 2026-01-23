package com.mario.hlf.ui

import com.mario.hlf.network.client.GameEventLoopClient
import com.mario.hlf.network.client.GameTcpClient
import com.mario.hlf.network.client.TcpClientConnection
import com.mario.hlf.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class GameController(
    private val scope: CoroutineScope
) {
    private var conn: TcpClientConnection? = null
    private var api: GameTcpClient? = null
    private var loop: GameEventLoopClient? = null

    // gameId actual (lo da el server en WELCOME)
    private var currentGameId: String? = null

    // jobs de bindings para no duplicar collectors
    private var bindLatestJob: Job? = null
    private var bindGameOverJob: Job? = null

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Disconnected)
    val uiState: StateFlow<GameUiState> = _uiState

    fun connect(host: String, port: Int, name: String) {
        _uiState.value = GameUiState.Connecting(host, port, name)

        scope.launch(Dispatchers.IO) {
            try {
                // por si había algo previo
                safeCloseAll()

                val c = TcpClientConnection.connect(host, port)
                c.setReadTimeout(0) // lectura infinita (se corta cerrando socket)
                conn = c

                val a = GameTcpClient(c)
                api = a

                a.hello(playerName = name)
                val gid = requireNotNull(a.gameId) { "Server did not provide gameId" }
                currentGameId = gid

                val l = GameEventLoopClient(a, scope)
                loop = l
                l.start()

                // ✅ bind flows justo después de arrancar loop
                bindStateFlows(l)

                _uiState.value = GameUiState.Connected(host, port, name, gid)

            } catch (t: Throwable) {
                safeCloseAll()
                _uiState.value = GameUiState.Error(t.message ?: "Connect error")
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

    /**
     * ✅ Idempotente: cancela collectors anteriores y crea unos nuevos para ESTE loop.
     * Se llama SOLO desde connect() cuando ya existe loop.
     */
    private fun bindStateFlows(l: GameEventLoopClient) {
        bindLatestJob?.cancel()
        bindGameOverJob?.cancel()

        bindLatestJob = scope.launch {
            l.latestState.collect { st ->
                if (st != null) {
                    _uiState.update { prev ->
                        val prevGameId = when (prev) {
                            is GameUiState.Connected -> prev.gameId
                            is GameUiState.InGame -> prev.gameId
                            else -> null
                        }

                        val gid = prevGameId ?: currentGameId ?: "UNKNOWN"
                        val prevGameOver = (prev as? GameUiState.InGame)?.gameOver

                        GameUiState.InGame(
                            gameId = gid,
                            state = st,
                            gameOver = prevGameOver
                        )
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

    /**
     * ✅ No suspend: usable desde UI directamente.
     */
    fun disconnect() {
        scope.launch(Dispatchers.IO) {
            safeCloseAll()
            _uiState.value = GameUiState.Disconnected
        }
    }

    /**
     * ✅ Suspend porque loop.stop() es suspend.
     * Idempotente.
     */
    private suspend fun safeCloseAll() {
        // parar collectors primero
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
