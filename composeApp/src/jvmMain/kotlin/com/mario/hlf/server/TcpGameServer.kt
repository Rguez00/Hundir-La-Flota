package com.mario.hlf.server

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean

class TcpGameServer(
    private val config: ServerConfig,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val sessions: SessionRegistry = SessionRegistry(),
    private val rooms: RoomRegistry = RoomRegistry(),
    private val connections: ConnectionRegistry = ConnectionRegistry(),
    private val games: GameService = GameService(),
    private val router: MessageRouter = MessageRouter(sessions, rooms, games)
) {
    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null

    fun start() {
        check(!running.get()) { "Server already started" }
        running.set(true)

        val ss = ServerSocket()
        ss.bind(InetSocketAddress(config.host, config.port))
        serverSocket = ss

        log("Server listening on ${config.host}:${config.port} (maxClients=${config.maxClients})")

        scope.launch {
            try {
                while (running.get()) {
                    val socket = ss.accept()

                    // guard maxClients
                    if (sessions.count() >= config.maxClients) {
                        log("Rejecting client: SERVER_FULL")
                        socket.close()
                        continue
                    }

                    val session = sessions.create()
                    log("Client connected: ${socket.inetAddress.hostAddress}:${socket.port} -> clientId=${session.clientId.value}")

                    launch {
                        try {
                            ClientSession(
                                socket = socket,
                                config = config,
                                clientId = session.clientId,
                                sessions = sessions,
                                rooms = rooms,
                                connections = connections,
                                router = router
                            ).run()
                        } finally {
                            sessions.remove(session.clientId)
                            rooms.removeClient(session.clientId)
                            log("Client disconnected: clientId=${session.clientId.value}")
                        }
                    }
                }
            } catch (t: Throwable) {
                if (running.get()) log("Server accept loop error: ${t.message}")
            } finally {
                // si el loop cae, cerramos el socket por seguridad
                try { ss.close() } catch (_: Throwable) {}
                log("Server stopped.")
            }
        }
    }

    fun stop() {
        running.set(false)
        serverSocket?.close()
        scope.cancel()
    }

    private fun log(msg: String) = println("[SERVER] $msg")
}
