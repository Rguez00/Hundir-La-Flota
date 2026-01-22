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
    private val rooms: RoomRegistry,
    private val connections: ConnectionRegistry,
    private val router: MessageRouter
) {

    suspend fun run() {
        socket.use { s ->
            val input = s.getInputStream()
            val output = s.getOutputStream()

            connections.register(clientId, output)

            try {
                while (true) {
                    val frame = Framing.readFrame(input) ?: break
                    val json = frame.toString(Charsets.UTF_8)

                    val env = ProtocolJson.decodeFromString(Envelope.serializer(), json)

                    // Handshake se queda aquí (fase 5.2)
                    if (env.payload is Hello) {
                        val responseEnv = handleHello(env)
                        connections.sendTo(clientId, responseEnv)
                        continue
                    }

                    // Resto de mensajes -> router (fase 5.3.2)
                    val dispatches = router.handle(clientId, env)
                    for (d in dispatches) {
                        connections.sendTo(d.target, d.envelope)
                    }
                }
            } finally {
                connections.unregister(clientId)
            }
        }
    }

    private suspend fun handleHello(env: Envelope): Envelope {
        val payload = env.payload as Hello

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

        return Envelope(
            v = env.v,
            requestId = env.requestId ?: UUID.randomUUID().toString(),
            gameId = updated.gameId?.value,
            payload = welcome
        )
    }

    // (Opcional) Si quieres mantenerlo, pero ahora mismo no lo usamos:
    @Suppress("unused")
    private fun unsupported(env: Envelope, msg: String): Envelope =
        Envelope(
            v = env.v,
            requestId = env.requestId ?: UUID.randomUUID().toString(),
            gameId = env.gameId,
            payload = ErrorMsg(code = "UNSUPPORTED", message = msg)
        )
}
