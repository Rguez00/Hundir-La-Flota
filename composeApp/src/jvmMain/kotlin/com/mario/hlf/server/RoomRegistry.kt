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

    // gameId -> roomId (índice)
    private val roomIdByGameId = ConcurrentHashMap<String, String>()

    // roomId de la sala en espera (0 o 1 jugador)
    private var waitingRoomId: RoomId? = null

    suspend fun joinOrCreate(clientId: ClientId): JoinResult = mutex.withLock {
        val existingWaiting = waitingRoomId?.let { rooms[it.value] }

        val room = if (
            existingWaiting == null ||
            existingWaiting.status != RoomStatus.WAITING ||
            existingWaiting.players.size >= 2
        ) {
            val newRoom = Room(roomId = newRoomId(), gameId = newGameId())
            rooms[newRoom.roomId.value] = newRoom
            roomIdByGameId[newRoom.gameId.value] = newRoom.roomId.value
            waitingRoomId = newRoom.roomId
            newRoom
        } else {
            existingWaiting
        }

        // evitar duplicados (por seguridad)
        if (clientId !in room.players) {
            room.players.add(clientId)
        }

        // slot asignado por orden de entrada (1 o 2)
        val slot = room.players.indexOf(clientId) + 1

        // actualizar estado de la sala
        room.status = if (room.players.size >= 2) RoomStatus.READY else RoomStatus.WAITING

        // si ya está READY, ya no hay waiting room
        if (room.status == RoomStatus.READY && waitingRoomId == room.roomId) {
            waitingRoomId = null
        }

        JoinResult(
            roomId = room.roomId,
            gameId = room.gameId,
            slot = slot,
            roomStatus = room.status
        )
    }

    suspend fun removeClient(clientId: ClientId) = mutex.withLock {
        // snapshot para evitar iterar mientras se muta el map
        val snapshot = rooms.values.toList()

        for (room in snapshot) {
            if (room.players.remove(clientId)) {
                room.status = if (room.players.size >= 2) RoomStatus.READY else RoomStatus.WAITING

                if (room.players.isEmpty()) {
                    rooms.remove(room.roomId.value)
                    roomIdByGameId.remove(room.gameId.value)
                    if (waitingRoomId == room.roomId) waitingRoomId = null
                } else {
                    if (room.status == RoomStatus.WAITING && waitingRoomId == null) {
                        waitingRoomId = room.roomId
                    }
                }
            }
        }

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
