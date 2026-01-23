package com.mario.hlf.network.client

import com.mario.hlf.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cliente con loop de lectura continuo.
 * - Un solo reader loop por conexión.
 * - Para parar correctamente, CERRAMOS la conexión (desbloquea readFrame()).
 *
 * Regla final:
 * - Si el scope lo pasas desde fuera (tests/UI), NO lo cancelamos aquí.
 * - Si no te pasan scope, usamos uno propio y sí lo cancelamos.
 */
class GameEventLoopClient private constructor(
    private val api: GameTcpClient,
    private val scope: CoroutineScope,
    private val ownsScope: Boolean
) {
    constructor(
        api: GameTcpClient,
        scope: CoroutineScope
    ) : this(api, scope, ownsScope = false)

    constructor(
        api: GameTcpClient
    ) : this(api, CoroutineScope(SupervisorJob() + Dispatchers.IO), ownsScope = true)

    private var readerJob: Job? = null
    private val stopping = AtomicBoolean(false)

    private val _events = MutableSharedFlow<Msg>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private val _latestState = MutableStateFlow<GameStateDto?>(null)
    val latestState = _latestState.asStateFlow()

    private val _gameOver = MutableStateFlow<GameOverEvent?>(null)
    val gameOver = _gameOver.asStateFlow()

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
                        else -> _events.tryEmit(p)
                    }
                }
            } catch (t: Throwable) {
                // Cancel normal
                if (t is CancellationException) return@launch

                // Si estamos parando, ignoramos errores por socket cerrado
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
            // Desbloquea readFrame() cerrando la conexión
            api.close()
        } finally {
            // Espera real a que muera el loop
            job.cancelAndJoin()

            // Solo cancelamos si el scope es nuestro
            if (ownsScope) scope.cancel()
        }
    }

}
