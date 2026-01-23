package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.*
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import java.util.UUID

class ClientSession(
    private val socket: Socket,
    private val config: ServerConfig,
    private val clientId: ClientId,
    private val sessions: SessionRegistry,
    private val rooms: RoomRegistry,
    private val connections: ConnectionRegistry,
    private val router: MessageRouter
) {
    suspend fun run() {
        log("session started clientId=${clientId.value}")

        socket.use { s ->
            val input = s.getInputStream()
            val output = s.getOutputStream()

            // ✅ registrar SIEMPRE desde el principio
            connections.register(clientId, output)

            try {
                while (true) {
                    log("waiting frame... clientId=${clientId.value}")
                    val env = safeReadEnvelopeOrNull(input) ?: break
                    log("received ${env.payload::class.simpleName} reqId=${env.requestId} gameId=${env.gameId}")

                    when (env.payload) {
                        is Hello -> {
                            val responseEnv = handleHello(env)
                            writeEnvelope(output, responseEnv)
                            log("sent Welcome reqId=${responseEnv.requestId} gameId=${responseEnv.gameId}")
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
                runCatching { connections.unregister(clientId) }
                runCatching { rooms.removeClient(clientId) }
                runCatching { sessions.remove(clientId) }
                log("session closed clientId=${clientId.value}")
            }
        }
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

    private suspend fun handleHello(env: Envelope): Envelope {
        val payload = env.payload as Hello

        // 1) Guardar nombre en sesión
        sessions.update(clientId) {
            it.copy(playerName = payload.playerName, status = SessionStatus.CONNECTED)
        }

        // 2) Join / create room
        val join = rooms.joinOrCreate(clientId)

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

        // 4) Respuesta
        val welcome = Welcome(
            serverVersion = "1.0",
            config = ServerConfigDto(
                host = config.host,
                port = config.port,
                maxClients = config.maxClients
            ),
            records = RecordsDto()
        )

        return Envelope(
            v = env.v,
            requestId = env.requestId ?: UUID.randomUUID().toString(),
            gameId = updated.gameId?.value,
            payload = welcome
        )
    }

    private fun log(msg: String) = println("[SERVER] $msg")
}
