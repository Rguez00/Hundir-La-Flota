package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import com.mario.hlf.server.ai.AIPlayer
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClientSession(
    private val socket: Socket,
    private val config: ServerConfig,
    private val clientId: ClientId,
    private val sessions: SessionRegistry,
    private val rooms: RoomRegistry,
    private val connections: ConnectionRegistry,
    private val router: MessageRouter,
    private val aiPlayers: ConcurrentHashMap<String, AIPlayer> = ConcurrentHashMap() // ✅ NUEVO
) {
    suspend fun run() {
        log("session started clientId=${clientId.value}")

        socket.use { s ->
            val input = s.getInputStream()
            val output = s.getOutputStream()

            // Registrar SIEMPRE desde el principio
            connections.register(clientId, output)

            try {
                while (true) {
                    log("waiting frame... clientId=${clientId.value}")
                    val env = safeReadEnvelopeOrNull(input) ?: break
                    log("received ${env.payload::class.simpleName} reqId=${env.requestId} gameId=${env.gameId}")

                    when (env.payload) {
                        is Hello -> {
                            val welcomeEnv = handleHello(env)
                            writeEnvelope(output, welcomeEnv)
                            log("sent Welcome reqId=${welcomeEnv.requestId} gameId=${welcomeEnv.gameId}")

                            // ✅ Broadcast ROOM_UPDATE si es PVP y hay otro jugador
                            broadcastRoomUpdateIfNeeded(welcomeEnv.gameId)
                        }

                        else -> {
                            val dispatches = router.handle(clientId, env)
                            for (d in dispatches) {
                                connections.sendTo(d.target, d.envelope)
                            }
                        }
                    }
                }
            } finally {
                handleDisconnection()
            }
        }
    }

    /**
     * ✅ Manejar desconexión limpia
     */
    private suspend fun handleDisconnection() {
        log("handling disconnection for clientId=${clientId.value}")

        val session = sessions.get(clientId)
        val gameId = session?.gameId

        // Notificar a otros jugadores en la sala
        if (gameId != null) {
            val dispatches = router.notifyPlayerDisconnected(clientId, gameId)
            for (d in dispatches) {
                connections.sendTo(d.target, d.envelope)
            }

            // ✅ NUEVO: Limpiar IA si existe
            aiPlayers.remove(gameId.value)
            log("cleaned up AI for gameId=${gameId.value}")
        }

        // Limpiar registros
        runCatching { connections.unregister(clientId) }
        runCatching { rooms.removeClient(clientId) }
        runCatching { sessions.remove(clientId) }

        log("session closed clientId=${clientId.value}")
    }

    private fun safeReadEnvelopeOrNull(input: InputStream): Envelope? {
        val frame = try {
            Framing.readFrame(input)
        } catch (_: EOFException) {
            return null
        } catch (_: SocketException) {
            return null
        } catch (t: Throwable) {
            log("readFrame error: ${t.message}")
            return null
        }

        if (frame == null) return null

        val json = frame.toString(Charsets.UTF_8)
        return try {
            ProtocolJson.decodeFromString(Envelope.serializer(), json)
        } catch (t: Throwable) {
            log("decode error: ${t.message} raw=$json")
            null
        }
    }

    private fun writeEnvelope(output: OutputStream, env: Envelope) {
        val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
        Framing.writeFrame(output, json.toByteArray(Charsets.UTF_8))
        runCatching { output.flush() }
    }

    /**
     * ✅ Handshake con soporte PVE y modo
     */
    private suspend fun handleHello(env: Envelope): Envelope {
        val payload = env.payload as Hello
        val requestedMode = payload.mode

        // 1) Guardar nombre en sesión
        sessions.update(clientId) {
            it.copy(playerName = payload.playerName, status = SessionStatus.CONNECTED)
        }

        // 2) Join / create room con modo especificado
        val join = rooms.joinOrCreate(clientId, requestedMode)

        // 3) Actualizar sesión con room/game/slot
        val status = if (join.roomStatus == RoomStatus.READY) SessionStatus.IN_GAME else SessionStatus.WAITING
        val updated = sessions.update(clientId) {
            it.copy(
                status = status,
                roomId = join.roomId,
                gameId = join.gameId,
                slot = join.slot
            )
        }

        // ✅ NUEVO: Crear IA si es modo PVE
        if (join.mode == GameModeId.PVE && join.gameId != null) {
            val ai = aiPlayers.getOrPut(join.gameId.value) {
                AIPlayer(config.aiDifficulty)
            }
            log("created AI for gameId=${join.gameId.value} with difficulty=${config.aiDifficulty}")
        }

        // 4) Preparar respuesta WELCOME
        val me = if (join.slot == 1) PlayerId.P1 else PlayerId.P2
        val roomStatusId = if (join.roomStatus == RoomStatus.READY) RoomStatusId.READY else RoomStatusId.WAITING

        val welcome = Welcome(
            serverVersion = "1.0",
            config = ServerConfigDto(
                host = config.host,
                port = config.port,
                maxClients = config.maxClients
            ),
            records = RecordsDto(), // ✅ TODO: Cargar records reales
            roomId = join.roomId.value,
            slot = me,
            roomStatus = roomStatusId,
            mode = join.mode
        )

        return Envelope(
            v = env.v,
            requestId = env.requestId ?: UUID.randomUUID().toString(),
            gameId = updated.gameId?.value,
            payload = welcome
        )
    }

    /**
     * ✅ Broadcast ROOM_UPDATE cuando se llena sala PVP
     */
    private suspend fun broadcastRoomUpdateIfNeeded(gameIdStr: String?) {
        if (gameIdStr == null) return

        val gameId = GameId(gameIdStr)
        val room = rooms.getRoomByGameId(gameId) ?: return

        // Solo broadcast si es PVP y ahora está READY
        if (room.mode == GameModeId.PVP && room.status == RoomStatus.READY && room.players.size == 2) {
            val event = RoomUpdateEvent(
                roomId = room.roomId.value,
                roomStatus = RoomStatusId.READY,
                players = room.players.size,
                mode = GameModeId.PVP
            )

            val envelope = Envelope(
                v = 1,
                requestId = UUID.randomUUID().toString(),
                gameId = gameIdStr,
                payload = event
            )

            // Enviar a ambos jugadores
            for (playerId in room.players) {
                connections.sendTo(playerId, envelope)
            }

            log("broadcasted ROOM_UPDATE: sala ${room.roomId.value} ahora READY")
        }
    }

    // ✅ NUEVO: Obtener o crear IA para un juego
    fun getOrCreateAI(gameId: GameId): AIPlayer {
        return aiPlayers.getOrPut(gameId.value) {
            AIPlayer(config.aiDifficulty)
        }
    }

    private fun log(msg: String) = println("[SESSION] $msg")
}