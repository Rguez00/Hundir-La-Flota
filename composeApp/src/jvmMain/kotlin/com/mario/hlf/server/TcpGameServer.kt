package com.mario.hlf.server

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class TcpGameServer(
    private val config: ServerConfig,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val ownsScope: Boolean = true, // si te pasan un scope externo => false
    private val sessions: SessionRegistry = SessionRegistry(),
    private val rooms: RoomRegistry = RoomRegistry(),
    private val connections: ConnectionRegistry = ConnectionRegistry(),
    private val games: GameService = GameService(),
    private val router: MessageRouter = MessageRouter(sessions, rooms, games)
) {
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val clientJobs = ConcurrentHashMap<String, Job>()
    private val clientSockets = ConcurrentHashMap<String, Socket>()

    fun start() {
        check(!running.get()) { "Server already started" }
        running.set(true)

        val ss = ServerSocket()
        ss.bind(InetSocketAddress(config.host, config.port))
        serverSocket = ss

        log("Server listening on ${config.host}:${config.port} (maxClients=${config.maxClients})")

        acceptJob = scope.launch {
            try {
                while (running.get()) {
                    val socket = try {
                        ss.accept()
                    } catch (_: SocketException) {
                        break // normal al cerrar ServerSocket en stop()
                    }

                    if (sessions.count() >= config.maxClients) {
                        log("Rejecting client: SERVER_FULL")
                        try { socket.close() } catch (_: Throwable) {}
                        continue
                    }

                    val session = sessions.create()
                    val clientId = session.clientId
                    clientSockets[clientId.value] = socket

                    log("Client connected: ${socket.inetAddress.hostAddress}:${socket.port} -> clientId=${clientId.value}")

                    val job = scope.launch {
                        try {
                            ClientSession(
                                socket = socket,
                                config = config,
                                clientId = clientId,
                                sessions = sessions,
                                rooms = rooms,
                                connections = connections,
                                router = router
                            ).run()
                        } finally {
                            clientSockets.remove(clientId.value)
                            connections.unregister(clientId)
                            rooms.removeClient(clientId)
                            sessions.remove(clientId)
                            log("Client disconnected: clientId=${clientId.value}")
                        }
                    }

                    clientJobs[clientId.value] = job
                    job.invokeOnCompletion { clientJobs.remove(clientId.value) }
                }
            } catch (t: Throwable) {
                if (running.get()) log("Server accept loop error: ${t.message}")
            } finally {
                try { ss.close() } catch (_: Throwable) {}
                log("Server stopped.")
            }
        }
    }

    /**
     * ✅ Mantiene compatibilidad con TODOS los tests actuales.
     * Internamente ejecuta la parada suspend.
     */
    fun stop() = runBlocking { stopAsync() }

    /**
     * ✅ Para UI/coroutines (no bloquea hilo).
     */
    suspend fun stopAsync() {
        if (!running.getAndSet(false)) return

        // 1) desbloquear accept()
        try { serverSocket?.close() } catch (_: Throwable) {}
        serverSocket = null

        // 2) parar accept loop
        try { acceptJob?.cancelAndJoin() } catch (_: Throwable) {}
        acceptJob = null

        // 3) cerrar sockets clientes => desbloquea readFrame()
        clientSockets.values.toList().forEach { s ->
            try { s.close() } catch (_: Throwable) {}
        }
        clientSockets.clear()

        // 4) cancelar/esperar handlers
        clientJobs.values.toList().forEach { job ->
            try { job.cancelAndJoin() } catch (_: Throwable) {}
        }
        clientJobs.clear()

        // 5) solo cancelamos scope si es nuestro
        if (ownsScope) scope.cancel()
    }

    private fun log(msg: String) = println("[SERVER] $msg")
}
