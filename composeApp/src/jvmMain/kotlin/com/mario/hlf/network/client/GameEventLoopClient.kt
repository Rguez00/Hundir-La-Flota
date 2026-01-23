package com.mario.hlf.network.client

import com.mario.hlf.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Loop único de lectura por conexión.
 * - Un solo reader por socket.
 * - stop(): cierra la conexión para desbloquear el readFrame().
 */
class GameEventLoopClient private constructor(
    private val api: GameTcpClient,
    private val scope: CoroutineScope,
    private val ownsScope: Boolean
) {
    constructor(api: GameTcpClient, scope: CoroutineScope) : this(api, scope, ownsScope = false)
    constructor(api: GameTcpClient) : this(api, CoroutineScope(SupervisorJob() + Dispatchers.IO), ownsScope = true)

    private var readerJob: Job? = null
    private val stopping = AtomicBoolean(false)

    // Eventos generales
    private val _events = MutableSharedFlow<ServerMsg>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    // Estados específicos
    private val _latestState = MutableStateFlow<GameStateDto?>(null)
    val latestState = _latestState.asStateFlow()

    private val _gameOver = MutableStateFlow<GameOverEvent?>(null)
    val gameOver = _gameOver.asStateFlow()

    private val _roomUpdate = MutableStateFlow<RoomUpdateEvent?>(null)
    val roomUpdate = _roomUpdate.asStateFlow()

    // ✅ NUEVO: Desconexiones de jugadores
    private val _playerDisconnected = MutableStateFlow<PlayerDisconnectedEvent?>(null)
    val playerDisconnected = _playerDisconnected.asStateFlow()

    // ✅ NUEVO: Errores del servidor
    private val _errors = MutableSharedFlow<ErrorMsg>(extraBufferCapacity = 16)
    val errors = _errors.asSharedFlow()

    fun start() {
        check(readerJob == null) { "Reader already started" }
        stopping.set(false)

        readerJob = scope.launch {
            try {
                while (isActive) {
                    val env = api.receiveEnvelope() // bloqueante

                    when (val p = env.payload) {
                        is GameStateEvent -> {
                            _latestState.value = p.state
                            _events.tryEmit(p)
                        }

                        is GameOverEvent -> {
                            _gameOver.value = p
                            _events.tryEmit(p)
                        }

                        is RoomUpdateEvent -> {
                            _roomUpdate.value = p
                            _events.tryEmit(p)
                        }

                        is PlayerDisconnectedEvent -> { // ✅ NUEVO
                            _playerDisconnected.value = p
                            _events.tryEmit(p)
                        }

                        is ErrorMsg -> { // ✅ MEJORADO
                            _errors.tryEmit(p)
                            _events.tryEmit(p)
                        }

                        // Ignorar otros mensajes (Hello, Welcome, etc.)
                        else -> {
                            // No-op o log si quieres debug
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) return@launch
                if (stopping.get()) return@launch

                // ✅ NUEVO: Propagar error como ErrorMsg interno
                _errors.tryEmit(ErrorMsg("CONNECTION_ERROR", t.message ?: "Unknown error"))
                throw t
            }
        }
    }

    suspend fun stop() {
        val job = readerJob ?: return
        readerJob = null

        stopping.set(true)

        try {
            // desbloquea readFrame()
            api.close()
        } finally {
            job.cancelAndJoin()
            if (ownsScope) scope.cancel()
        }
    }

    /**
     * ✅ NUEVO: Reset de estados (útil para reconectar)
     */
    fun resetStates() {
        _latestState.value = null
        _gameOver.value = null
        _roomUpdate.value = null
        _playerDisconnected.value = null
    }
}