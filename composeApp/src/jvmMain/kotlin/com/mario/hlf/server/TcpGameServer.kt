package com.mario.hlf.server

import com.mario.hlf.server.ai.AIPlayer
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
    private val ownsScope: Boolean = true,
    private val sessions: SessionRegistry = SessionRegistry(),
    private val rooms: RoomRegistry = RoomRegistry(),
    private val connections: ConnectionRegistry = ConnectionRegistry(),
    private val games: GameService = GameService(),
    private val records: RecordsManager = RecordsManager("records.json")
) {
    private val router: MessageRouter = MessageRouter(sessions, rooms, games, records = records)
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    private val clientJobs = ConcurrentHashMap<String, Job>()
    private val clientSockets = ConcurrentHashMap<String, Socket>()

    // ✅ Gestión de IAs
    private val aiPlayers = ConcurrentHashMap<String, AIPlayer>()

    fun start() {
        check(!running.get()) { "Server already started" }
        running.set(true)

        val ss = ServerSocket()
        ss.bind(InetSocketAddress(config.host, config.port))
        serverSocket = ss

        log("═══════════════════════════════════════")
        log("⚡ Server listening on ${config.host}:${config.port}")
        log("👥 Max clients: ${config.maxClients}")
        log("🤖 AI difficulty: ${config.aiDifficulty}")
        log("═══════════════════════════════════════")

        acceptJob = scope.launch {
            try {
                while (running.get()) {
                    val socket = try {
                        ss.accept()
                    } catch (_: SocketException) {
                        break
                    }

                    if (sessions.count() >= config.maxClients) {
                        log("⚠️  Rejecting client: SERVER_FULL (${sessions.count()}/${config.maxClients})")
                        try { socket.close() } catch (_: Throwable) {}
                        continue
                    }

                    val session = sessions.create()
                    val clientId = session.clientId
                    clientSockets[clientId.value] = socket

                    log("✅ Client connected: ${socket.inetAddress.hostAddress}:${socket.port} → clientId=${clientId.value}")

                    val job = scope.launch {
                        try {
                            ClientSession(
                                socket = socket,
                                config = config,
                                clientId = clientId,
                                sessions = sessions,
                                rooms = rooms,
                                connections = connections,
                                router = router,
                                aiPlayers = aiPlayers,
                                records = records
                            ).run()
                        } finally {
                            clientSockets.remove(clientId.value)
                            connections.unregister(clientId)

                            // Cleanup de IA
                            session.gameId?.let { gameId ->
                                aiPlayers.remove(gameId.value)
                            }

                            rooms.removeClient(clientId)
                            sessions.remove(clientId)
                            log("❌ Client disconnected: clientId=${clientId.value}")
                        }
                    }

                    clientJobs[clientId.value] = job
                    job.invokeOnCompletion { clientJobs.remove(clientId.value) }
                }
            } catch (t: Throwable) {
                if (running.get()) log("💥 Server accept loop error: ${t.message}")
            } finally {
                try { ss.close() } catch (_: Throwable) {}
                log("🛑 Server stopped.")
            }
        }
    }

    fun stop() = runBlocking { stopAsync() }

    suspend fun stopAsync() {
        if (!running.getAndSet(false)) return

        log("🛑 Iniciando apagado del servidor...")

        try { serverSocket?.close() } catch (_: Throwable) {}
        serverSocket = null

        try { acceptJob?.cancelAndJoin() } catch (_: Throwable) {}
        acceptJob = null

        log("📡 Cerrando ${clientSockets.size} conexiones...")
        clientSockets.values.toList().forEach { s ->
            try { s.close() } catch (_: Throwable) {}
        }
        clientSockets.clear()

        log("⏳ Esperando ${clientJobs.size} handlers...")
        clientJobs.values.toList().forEach { job ->
            try { job.cancelAndJoin() } catch (_: Throwable) {}
        }
        clientJobs.clear()

        log("🤖 Limpiando ${aiPlayers.size} IAs...")
        aiPlayers.clear()

        if (ownsScope) {
            scope.cancel()
            log("🔄 Scope cancelado")
        }

        log("✅ Servidor apagado correctamente")
    }

    // ✅ CORREGIDO: Sin activeGames
    fun getStats(): ServerStats {
        return ServerStats(
            isRunning = running.get(),
            connectedClients = sessions.count(),
            maxClients = config.maxClients,
            activeAIs = aiPlayers.size
        )
    }

    private fun log(msg: String) = println("[SERVER] $msg")
}

// ✅ CORREGIDO: Sin activeGames
data class ServerStats(
    val isRunning: Boolean,
    val connectedClients: Int,
    val maxClients: Int,
    val activeAIs: Int
)