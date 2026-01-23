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
 *
 * Regla:
 * - Si el scope viene de fuera, no lo cancelamos.
 * - Si lo creamos aquí, sí lo cancelamos.
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

    // ✅ Mejor: emitimos ServerMsg (no Msg genérico)
    private val _events = MutableSharedFlow<ServerMsg>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private val _latestState = MutableStateFlow<GameStateDto?>(null)
    val latestState = _latestState.asStateFlow()

    private val _gameOver = MutableStateFlow<GameOverEvent?>(null)
    val gameOver = _gameOver.asStateFlow()

    // ✅ NUEVO: estado de lobby
    private val _roomUpdate = MutableStateFlow<RoomUpdateEvent?>(null)
    val roomUpdate = _roomUpdate.asStateFlow()

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

                        // ✅ si algún día envías ErrorMsg desde router
                        is ErrorMsg -> {
                            _events.tryEmit(p)
                        }

                        // Si llega algo que NO es ServerMsg, lo ignoramos (o log si quieres)
                        else -> {
                            // No-op
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) return@launch
                if (stopping.get()) return@launch
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
}
