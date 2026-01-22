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
    private val rooms = ConcurrentHashMap<String, Room>()
    private var waitingRoomId: RoomId? = null

    suspend fun joinOrCreate(clientId: ClientId): JoinResult = mutex.withLock {
        val existingWaiting = waitingRoomId?.let { rooms[it.value] }

        val room = if (existingWaiting == null || existingWaiting.status != RoomStatus.WAITING || existingWaiting.players.size >= 2) {
            val newRoom = Room(roomId = newRoomId(), gameId = newGameId())
            rooms[newRoom.roomId.value] = newRoom
            waitingRoomId = newRoom.roomId
            newRoom
        } else {
            existingWaiting
        }

        // slot asignado por orden de entrada
        room.players.add(clientId)
        val slot = room.players.size

        if (room.players.size == 2) {
            room.status = RoomStatus.READY
            // ya no hay waiting room (se creará otra cuando llegue alguien)
            if (waitingRoomId == room.roomId) waitingRoomId = null
        }

        JoinResult(
            roomId = room.roomId,
            gameId = room.gameId,
            slot = slot,
            roomStatus = room.status
        )
    }

    fun removeClient(clientId: ClientId) {
        // Limpieza simple: si estaba en alguna room, lo quitamos.
        // (En 5.3 decidimos si la room se cancela o espera reconexión)
        rooms.values.forEach { room ->
            room.players.remove(clientId)
        }
    }
    fun getRoomByGameId(gameId: GameId): Room? =
        rooms.values.firstOrNull { it.gameId == gameId }

    fun getRoom(roomId: RoomId): Room? = rooms[roomId.value]

}
