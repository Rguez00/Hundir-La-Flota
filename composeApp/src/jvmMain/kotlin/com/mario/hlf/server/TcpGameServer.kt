package com.mario.hlf.server

import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    private val activeClients = AtomicInteger(0)
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

                    // guard maxClients (usa contador real de activos)
                    if (activeClients.get() >= config.maxClients) {
                        log("Rejecting client: SERVER_FULL")
                        try { socket.close() } catch (_: Throwable) {}
                        continue
                    }

                    // Reservamos plaza antes de lanzar la sesión
                    activeClients.incrementAndGet()

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
                        } catch (t: Throwable) {
                            // Si quieres, aquí puedes loguear errores por cliente (sin tumbar el server)
                            log("Client session error (clientId=${session.clientId.value}): ${t.message}")
                        } finally {
                            // Cleanup aquí SOLO del contador.
                            // La limpieza de sessions/rooms/connections vive en ClientSession.finally (versión final).
                            activeClients.decrementAndGet()
                            log("Client disconnected: clientId=${session.clientId.value}")
                        }
                    }
                }
            } catch (e: SocketException) {
                // típico cuando paramos y cerramos el ServerSocket
                if (running.get()) log("Server socket error: ${e.message}")
            } catch (e: IOException) {
                if (running.get()) log("Server IO error: ${e.message}")
            } catch (t: Throwable) {
                if (running.get()) log("Server accept loop error: ${t.message}")
            } finally {
                try { ss.close() } catch (_: Throwable) {}
                log("Server stopped.")
            }
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        try { serverSocket?.close() } catch (_: Throwable) {}
        scope.cancel()
    }

    private fun log(msg: String) = println("[SERVER] $msg")
}
