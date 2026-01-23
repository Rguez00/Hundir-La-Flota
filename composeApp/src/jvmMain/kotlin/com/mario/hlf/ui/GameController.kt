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
    private var currentMode: GameModeUi = GameModeUi.PVP

    private var bindLatestJob: Job? = null
    private var bindGameOverJob: Job? = null
    private var bindEventsJob: Job? = null

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Disconnected)
    val uiState: StateFlow<GameUiState> = _uiState

    // IO scope para red (no bloquea UI)
    private val ioScope = CoroutineScope(scope.coroutineContext + Dispatchers.IO)

    fun connect(host: String, port: Int, name: String, mode: GameModeUi = GameModeUi.PVP) {
        // ✅ Guardar modo ANTES de safeCloseAll() (porque safeCloseAll resetea todo)
        currentMode = mode

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

                // ✅ Handshake ANTES del event loop
                val welcome = a.hello(playerName = name)

                val gid = requireNotNull(a.gameId) { "Server did not provide gameId in Envelope" }
                currentGameId = gid

                // ✅ Soportar compatibilidad: si vienen vacíos, no crashear
                val me = welcome.slot
                val roomId = welcome.roomId.ifBlank { "LOCAL" }
                val roomStatus = welcome.roomStatus

                myPlayerId = me
                currentRoomId = roomId
                currentRoomStatus = roomStatus

                // ✅ lectura infinita para el loop
                c.setReadTimeout(0)

                val l = GameEventLoopClient(a, ioScope)
                loop = l
                l.start()

                bindStateFlows(l)
                bindEvents(l) // opcional, pero útil para actualizar lobby

                withContext(Dispatchers.Main) {
                    _uiState.value = GameUiState.Connected(
                        host = host,
                        port = port,
                        name = name,
                        gameId = gid,
                        roomId = roomId,
                        roomStatus = roomStatus,
                        me = me,
                        mode = currentMode
                    )
                }
            } catch (t: Throwable) {
                safeCloseAll()
                withContext(Dispatchers.Main) {
                    _uiState.value = GameUiState.Error(t.toString())
                }
            }
        }
    }

    /**
     * PVP: el servidor decide.
     * PVE: de momento lo bloqueamos hasta implementar IA local (luego cambiaremos esto).
     */
    fun startGame() {
        if (currentMode == GameModeUi.PVE) return
        api?.sendStartGame(boardSize = 10, allowAdjacency = false)
    }

    fun placeShip(row: Int, col: Int, ship: ShipTypeId, orientation: OrientationId) {
        if (currentMode == GameModeUi.PVE) return
        val me = myPlayerId ?: PlayerId.P1
        api?.sendPlaceShip(me, row, col, ship, orientation)
    }

    fun shoot(row: Int, col: Int) {
        if (currentMode == GameModeUi.PVE) return
        val me = myPlayerId ?: PlayerId.P1
        api?.sendShoot(me, row, col)
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
                        val me = when (prev) {
                            is GameUiState.Connected -> prev.me
                            is GameUiState.InGame -> prev.me
                            else -> myPlayerId ?: PlayerId.P1
                        }
                        val mode = when (prev) {
                            is GameUiState.Connected -> prev.mode
                            is GameUiState.InGame -> prev.mode
                            else -> currentMode
                        }

                        val prevGameOver = (prev as? GameUiState.InGame)?.gameOver

                        GameUiState.InGame(
                            gameId = gid,
                            me = me,
                            state = st,
                            gameOver = prevGameOver,
                            mode = mode
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
     * ✅ Opcional: si en el futuro emites RoomUpdateEvent desde el server,
     * aquí puedes actualizar el Lobby en caliente.
     *
     * Si NO existe RoomUpdateEvent aún, déjalo tal cual: no rompe nada.
     */
    private fun bindEvents(l: GameEventLoopClient) {
        bindEventsJob?.cancel()

        bindEventsJob = scope.launch {
            l.events.collect { msg ->
                val upd = msg as? RoomUpdateEvent ?: return@collect

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
        bindEventsJob?.cancel()
        bindLatestJob = null
        bindGameOverJob = null
        bindEventsJob = null

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
}
