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

    private var currentGameId: String? = null
    private var myPlayerId: PlayerId? = null
    private var currentRoomId: String? = null
    private var currentRoomStatus: RoomStatusId? = null
    private var currentMode: GameModeId = GameModeId.PVP

    private var bindLatestJob: Job? = null
    private var bindGameOverJob: Job? = null
    private var bindRoomUpdateJob: Job? = null
    private var bindDisconnectJob: Job? = null // ✅ NUEVO
    private var bindErrorsJob: Job? = null // ✅ NUEVO

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Disconnected)
    val uiState: StateFlow<GameUiState> = _uiState

    // IO scope para red (no bloquea UI)
    private val ioScope = CoroutineScope(scope.coroutineContext + Dispatchers.IO)

    /**
     * ✅ MEJORADO: Conectar con modo especificado
     */
    fun connect(host: String, port: Int, name: String, mode: GameModeId = GameModeId.PVP) {
        currentMode = mode

        scope.launch(Dispatchers.Main) {
            _uiState.value = GameUiState.Connecting(host, port, name, mode)
        }

        ioScope.launch {
            try {
                safeCloseAll()

                val c = TcpClientConnection.connect(host, port)
                conn = c

                val a = GameTcpClient(c)
                api = a

                // ✅ Handshake con modo
                val welcome = a.hello(playerName = name, mode = mode)

                val gid = requireNotNull(a.gameId) { "Server did not provide gameId in Envelope" }
                currentGameId = gid

                val me = welcome.slot
                val roomId = welcome.roomId.ifBlank { "LOCAL" }
                val roomStatus = welcome.roomStatus
                val confirmedMode = welcome.mode // ✅ NUEVO: servidor confirma modo

                myPlayerId = me
                currentRoomId = roomId
                currentRoomStatus = roomStatus
                currentMode = confirmedMode

                // ✅ lectura infinita para el loop
                c.setReadTimeout(0)

                val l = GameEventLoopClient(a, ioScope)
                loop = l
                l.start()

                bindStateFlows(l)

                withContext(Dispatchers.Main) {
                    _uiState.value = GameUiState.Connected(
                        host = host,
                        port = port,
                        name = name,
                        gameId = gid,
                        roomId = roomId,
                        roomStatus = roomStatus,
                        me = me,
                        mode = confirmedMode
                    )
                }
            } catch (t: Throwable) {
                safeCloseAll()
                withContext(Dispatchers.Main) {
                    _uiState.value = GameUiState.Error(
                        message = t.message ?: "Error de conexión",
                        reconnectable = true
                    )
                }
            }
        }
    }

    /**
     * ✅ MEJORADO: Permitir START_GAME en PVE también
     */
    fun startGame() {
        api?.sendStartGame(boardSize = 10, allowAdjacency = false)
    }

    /**
     * ✅ MEJORADO: Permitir PLACE_SHIP en PVE también
     */
    fun placeShip(row: Int, col: Int, ship: ShipTypeId, orientation: OrientationId) {
        val me = myPlayerId ?: PlayerId.P1
        api?.sendPlaceShip(me, row, col, ship, orientation)
    }

    /**
     * ✅ MEJORADO: Permitir SHOOT en PVE también
     */
    fun shoot(row: Int, col: Int) {
        val me = myPlayerId ?: PlayerId.P1
        api?.sendShoot(me, row, col)
    }

    /**
     * ✅ MEJORADO: Binding completo de todos los eventos
     */
    private fun bindStateFlows(l: GameEventLoopClient) {
        // Cancelar jobs previos
        bindLatestJob?.cancel()
        bindGameOverJob?.cancel()
        bindRoomUpdateJob?.cancel()
        bindDisconnectJob?.cancel()
        bindErrorsJob?.cancel()

        // 1) Estado de juego (PLACEMENT/BATTLE)
        bindLatestJob = scope.launch {
            l.latestState.collect { st ->
                if (st != null) {
                    _uiState.update { prev ->
                        val gid = extractGameId(prev)
                        val me = extractPlayerId(prev)
                        val mode = extractMode(prev)
                        val prevGameOver = (prev as? GameUiState.InGame)?.gameOver
                        val prevDisconnected = (prev as? GameUiState.InGame)?.opponentDisconnected ?: false

                        GameUiState.InGame(
                            gameId = gid,
                            me = me,
                            state = st,
                            gameOver = prevGameOver,
                            mode = mode,
                            opponentDisconnected = prevDisconnected
                        )
                    }
                }
            }
        }

        // 2) Game Over
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

        // 3) ✅ NUEVO: Room Update (PVP)
        bindRoomUpdateJob = scope.launch {
            l.roomUpdate.collect { upd ->
                if (upd != null) {
                    currentRoomId = upd.roomId
                    currentRoomStatus = upd.roomStatus

                    _uiState.update { prev ->
                        if (prev is GameUiState.Connected) {
                            prev.copy(roomId = upd.roomId, roomStatus = upd.roomStatus)
                        } else prev
                    }
                }
            }
        }

        // 4) ✅ NUEVO: Player Disconnected
        bindDisconnectJob = scope.launch {
            l.playerDisconnected.collect { event ->
                if (event != null) {
                    _uiState.update { prev ->
                        val cur = prev as? GameUiState.InGame
                        if (cur != null) {
                            cur.copy(opponentDisconnected = true)
                        } else prev
                    }
                }
            }
        }

        // 5) ✅ NUEVO: Errores del servidor
        bindErrorsJob = scope.launch {
            l.errors.collect { error ->
                // Mostrar error en UI (podrías usar Snackbar, Dialog, etc.)
                println("[GameController] Error from server: ${error.code} - ${error.message}")

                // Si es error crítico, volver a pantalla de error
                if (error.code in listOf("NO_SESSION", "NO_GAME", "CONNECTION_ERROR")) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = GameUiState.Error(
                            message = "${error.code}: ${error.message}",
                            reconnectable = true
                        )
                    }
                }
            }
        }
    }

    /**
     * ✅ NUEVO: Volver a lobby (después de Game Over o desconexión)
     */
    fun backToLobby() {
        scope.launch(Dispatchers.Main) {
            val current = _uiState.value

            when (current) {
                is GameUiState.InGame -> {
                    // Volver a Connected si aún tenemos gameId
                    if (currentGameId != null) {
                        _uiState.value = GameUiState.Connected(
                            host = "unknown", // ⚠️ Idealmente guardar estos datos
                            port = 0,
                            name = "unknown",
                            gameId = currentGameId!!,
                            roomId = currentRoomId ?: "unknown",
                            roomStatus = RoomStatusId.WAITING,
                            me = myPlayerId ?: PlayerId.P1,
                            mode = currentMode
                        )
                    } else {
                        _uiState.value = GameUiState.Disconnected
                    }
                }
                is GameUiState.Error -> {
                    // Volver a disconnected
                    _uiState.value = GameUiState.Disconnected
                }
                else -> {
                    // Ya está en lobby o desconectado
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
        bindRoomUpdateJob?.cancel()
        bindDisconnectJob?.cancel()
        bindErrorsJob?.cancel()

        bindLatestJob = null
        bindGameOverJob = null
        bindRoomUpdateJob = null
        bindDisconnectJob = null
        bindErrorsJob = null

        currentGameId = null
        myPlayerId = null
        currentRoomId = null
        currentRoomStatus = null
        // ⚠️ NO reseteamos currentMode aquí: lo decide connect()

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

    // ========== HELPERS ==========

    private fun extractGameId(state: GameUiState): String = when (state) {
        is GameUiState.Connected -> state.gameId
        is GameUiState.InGame -> state.gameId
        else -> currentGameId ?: "UNKNOWN"
    }

    private fun extractPlayerId(state: GameUiState): PlayerId = when (state) {
        is GameUiState.Connected -> state.me
        is GameUiState.InGame -> state.me
        else -> myPlayerId ?: PlayerId.P1
    }

    private fun extractMode(state: GameUiState): GameModeId = when (state) {
        is GameUiState.Connected -> state.mode
        is GameUiState.InGame -> state.mode
        else -> currentMode
    }
}