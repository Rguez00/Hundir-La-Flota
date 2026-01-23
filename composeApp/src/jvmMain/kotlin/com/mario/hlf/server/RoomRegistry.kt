package com.mario.hlf.server

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

enum class RoomStatus { WAITING, READY }

data class Room(
    val roomId: RoomId,
    val gameId: GameId,
    val players: MutableList<ClientId> = mutableListOf(),
    var status: RoomStatus = RoomStatus.WAITING
)

data class JoinResult(
    val roomId: RoomId,
    val gameId: GameId,
    val slot: Int,           // 1 o 2
    val roomStatus: RoomStatus
)

class RoomRegistry {
    private val mutex = Mutex()

    // roomId -> Room
    private val rooms = ConcurrentHashMap<String, Room>()

    // gameId -> roomId
    private val roomIdByGameId = ConcurrentHashMap<String, String>()

    // clientId -> roomId (para borrar en O(1))
    private val roomIdByClientId = ConcurrentHashMap<String, String>()

    // roomId de la sala en espera
    private var waitingRoomId: RoomId? = null

    suspend fun joinOrCreate(clientId: ClientId): JoinResult = mutex.withLock {
        // Si el cliente ya estaba en una sala (reconnect raro), lo limpiamos primero
        roomIdByClientId[clientId.value]?.let { existingRoomId ->
            val r = rooms[existingRoomId]
            if (r != null) {
                r.players.remove(clientId)
                r.status = if (r.players.size >= 2) RoomStatus.READY else RoomStatus.WAITING

                // si se quedó vacía, bórrala
                if (r.players.isEmpty()) {
                    rooms.remove(r.roomId.value)
                    roomIdByGameId.remove(r.gameId.value)
                    if (waitingRoomId == r.roomId) waitingRoomId = null
                } else {
                    // si queda alguien solo, puede pasar a ser waiting
                    if (r.status == RoomStatus.WAITING) {
                        val w = waitingRoomId
                        if (w == null || rooms[w.value] == null) waitingRoomId = r.roomId
                    }
                }
            }
            roomIdByClientId.remove(clientId.value)
        }

        // --- tu lógica actual tal cual ---
        val waiting = waitingRoomId?.let { rooms[it.value] }
        val room = if (waiting == null || waiting.status != RoomStatus.WAITING || waiting.players.size >= 2) {
            val newRoom = Room(roomId = newRoomId(), gameId = newGameId())
            rooms[newRoom.roomId.value] = newRoom
            roomIdByGameId[newRoom.gameId.value] = newRoom.roomId.value
            waitingRoomId = newRoom.roomId
            newRoom
        } else {
            waiting
        }

        if (clientId !in room.players) {
            room.players.add(clientId)
            roomIdByClientId[clientId.value] = room.roomId.value
        }

        val slot = room.players.indexOf(clientId) + 1
        room.status = if (room.players.size >= 2) RoomStatus.READY else RoomStatus.WAITING

        if (room.status == RoomStatus.READY && waitingRoomId == room.roomId) {
            waitingRoomId = null
        }

        JoinResult(room.roomId, room.gameId, slot, room.status)
    }


    suspend fun removeClient(clientId: ClientId) = mutex.withLock {
        val rid = roomIdByClientId.remove(clientId.value) ?: return@withLock
        val room = rooms[rid] ?: return@withLock

        room.players.remove(clientId)
        room.status = if (room.players.size >= 2) RoomStatus.READY else RoomStatus.WAITING

        if (room.players.isEmpty()) {
            rooms.remove(room.roomId.value)
            roomIdByGameId.remove(room.gameId.value)
            if (waitingRoomId == room.roomId) waitingRoomId = null
        } else {
            // si queda alguien solo, esa sala pasa a ser la waiting si no había otra
            if (room.status == RoomStatus.WAITING) {
                val w = waitingRoomId
                if (w == null || rooms[w.value] == null) waitingRoomId = room.roomId
            }
        }

        // limpieza extra por si waiting apuntaba a sala borrada
        val w = waitingRoomId
        if (w != null && rooms[w.value] == null) waitingRoomId = null
    }

    suspend fun getRoomByGameId(gameId: GameId): Room? = mutex.withLock {
        val roomId = roomIdByGameId[gameId.value] ?: return@withLock null
        rooms[roomId]
    }

    suspend fun getRoom(roomId: RoomId): Room? = mutex.withLock {
        rooms[roomId.value]
    }
}
