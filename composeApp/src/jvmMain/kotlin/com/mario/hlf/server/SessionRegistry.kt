package com.mario.hlf.server

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

enum class SessionStatus { CONNECTED, WAITING, IN_GAME }

data class SessionInfo(
    val clientId: ClientId,
    val connectedAt: Instant = Instant.now(),
    val playerName: String? = null,
    val status: SessionStatus = SessionStatus.CONNECTED,
    val roomId: RoomId? = null,
    val gameId: GameId? = null,
    val slot: Int? = null, // 1 o 2
)

class SessionRegistry {
    private val sessions = ConcurrentHashMap<String, SessionInfo>()

    fun create(): SessionInfo {
        val id = newClientId()
        val info = SessionInfo(clientId = id)
        sessions[id.value] = info
        return info
    }

    fun update(clientId: ClientId, transform: (SessionInfo) -> SessionInfo): SessionInfo {
        return sessions.compute(clientId.value) { _, old ->
            val current = old ?: SessionInfo(clientId = clientId)
            transform(current)
        }!!
    }

    fun get(clientId: ClientId): SessionInfo? = sessions[clientId.value]

    fun remove(clientId: ClientId) {
        sessions.remove(clientId.value)
    }

    fun count(): Int = sessions.size
}
