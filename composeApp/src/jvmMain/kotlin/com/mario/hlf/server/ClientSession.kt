package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.ErrorMsg
import com.mario.hlf.protocol.Hello
import com.mario.hlf.protocol.ProtocolJson
import com.mario.hlf.protocol.RecordsDto
import com.mario.hlf.protocol.ServerConfigDto
import com.mario.hlf.protocol.Welcome
import java.net.Socket
import java.util.UUID

class ClientSession(
    private val socket: Socket,
    private val config: ServerConfig,
    private val clientId: ClientId,
    private val sessions: SessionRegistry,
    private val rooms: RoomRegistry
) {

    suspend fun run() {
        socket.use { s ->
            val input = s.getInputStream()
            val output = s.getOutputStream()

            while (true) {
                val frame = Framing.readFrame(input) ?: break
                val json = frame.toString(Charsets.UTF_8)

                val env = ProtocolJson.decodeFromString(Envelope.serializer(), json)
                val responseEnv = handle(env)

                val outJson = ProtocolJson.encodeToString(Envelope.serializer(), responseEnv)
                Framing.writeFrame(output, outJson.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private suspend fun handle(env: Envelope): Envelope {
        return when (val payload = env.payload) {
            is Hello -> {
                // 1) Guardar nombre en sesión
                sessions.update(clientId) {
                    it.copy(playerName = payload.playerName, status = SessionStatus.CONNECTED)
                }

                // 2) Emparejar / crear room
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

                val welcome = Welcome(
                    serverVersion = "1.0",
                    config = ServerConfigDto(
                        host = config.host,
                        port = config.port,
                        maxClients = config.maxClients
                    ),
                    records = RecordsDto()
                )

                Envelope(
                    v = env.v,
                    requestId = env.requestId ?: UUID.randomUUID().toString(),
                    // 👇 Fuente de verdad: lo que tenga la sesión tras el join
                    gameId = updated.gameId?.value,
                    payload = welcome
                )
            }

            else -> {
                val err = ErrorMsg(
                    code = "UNSUPPORTED",
                    message = "En FASE 5.2 solo se soporta HELLO. Requests de juego en 5.3."
                )
                Envelope(
                    v = env.v,
                    requestId = env.requestId ?: UUID.randomUUID().toString(),
                    gameId = env.gameId,
                    payload = err
                )
            }
        }
    }
}
