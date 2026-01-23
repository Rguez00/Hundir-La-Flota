package com.mario.hlf.server

import com.mario.hlf.protocol.GameModeId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

enum class RoomStatus { WAITING, READY }

data class Room(
    val roomId: RoomId,
    val gameId: GameId,
    val players: MutableList<ClientId> = mutableListOf(),
    var status: RoomStatus = RoomStatus.WAITING,
    val mode: GameModeId = GameModeId.PVP, // ✅ NUEVO: modo de la sala
    val isAIPlayer: Boolean = false // ✅ NUEVO: indica si P2 es IA
)

data class JoinResult(
    val roomId: RoomId,
    val gameId: GameId,
    val slot: Int,           // 1 o 2
    val roomStatus: RoomStatus,
    val mode: GameModeId     // ✅ NUEVO: modo de la sala
)

class RoomRegistry {
    private val mutex = Mutex()

    // roomId -> Room
    private val rooms = ConcurrentHashMap<String, Room>()

    // gameId -> roomId
    private val roomIdByGameId = ConcurrentHashMap<String, String>()

    // clientId -> roomId (para borrar en O(1))
    private val roomIdByClientId = ConcurrentHashMap<String, String>()

    // roomId de la sala PVP en espera
    private var waitingPvpRoomId: RoomId? = null

    /**
     * ✅ NUEVO: Join con modo especificado
     * - PVP: matchmaking normal (2 humanos)
     * - PVE: sala individual con IA como P2
     */
    suspend fun joinOrCreate(clientId: ClientId, mode: GameModeId): JoinResult = mutex.withLock {
        // Limpiar cliente de salas anteriores (reconnect)
        cleanupClientFromRooms(clientId)

        // Lógica según modo
        val room = when (mode) {
            GameModeId.PVP -> findOrCreatePvpRoom()
            GameModeId.PVE -> createPveRoom()
        }

        // Añadir cliente a la sala
        if (clientId !in room.players) {
            room.players.add(clientId)
            roomIdByClientId[clientId.value] = room.roomId.value
        }

        // Calcular slot y actualizar estado
        val slot = room.players.indexOf(clientId) + 1

        when (mode) {
            GameModeId.PVP -> {
                room.status = if (room.players.size >= 2) RoomStatus.READY else RoomStatus.WAITING
                if (room.status == RoomStatus.READY && waitingPvpRoomId == room.roomId) {
                    waitingPvpRoomId = null
                }
            }
            GameModeId.PVE -> {
                // PVE siempre READY (IA disponible instantáneamente)
                room.status = RoomStatus.READY
            }
        }

        JoinResult(room.roomId, room.gameId, slot, room.status, mode)
    }

    /**
     * ✅ NUEVO: Encontrar o crear sala PVP
     */
    private fun findOrCreatePvpRoom(): Room {
        val waiting = waitingPvpRoomId?.let { rooms[it.value] }

        return if (waiting == null || waiting.status != RoomStatus.WAITING || waiting.players.size >= 2) {
            // Crear nueva sala PVP
            val newRoom = Room(
                roomId = newRoomId(),
                gameId = newGameId(),
                mode = GameModeId.PVP,
                isAIPlayer = false
            )
            rooms[newRoom.roomId.value] = newRoom
            roomIdByGameId[newRoom.gameId.value] = newRoom.roomId.value
            waitingPvpRoomId = newRoom.roomId
            newRoom
        } else {
            waiting
        }
    }

    /**
     * ✅ NUEVO: Crear sala PVE (individual con IA)
     */
    private fun createPveRoom(): Room {
        val room = Room(
            roomId = newRoomId(),
            gameId = newGameId(),
            mode = GameModeId.PVE,
            isAIPlayer = true,
            status = RoomStatus.READY // PVE siempre listo
        )
        rooms[room.roomId.value] = room
        roomIdByGameId[room.gameId.value] = room.roomId.value
        return room
    }

    /**
     * ✅ MEJORADO: Limpiar cliente de salas previas
     */
    private fun cleanupClientFromRooms(clientId: ClientId) {
        val existingRoomId = roomIdByClientId[clientId.value] ?: return
        val room = rooms[existingRoomId] ?: return

        room.players.remove(clientId)
        roomIdByClientId.remove(clientId.value)

        when (room.mode) {
            GameModeId.PVP -> {
                room.status = if (room.players.size >= 2) RoomStatus.READY else RoomStatus.WAITING

                if (room.players.isEmpty()) {
                    rooms.remove(room.roomId.value)
                    roomIdByGameId.remove(room.gameId.value)
                    if (waitingPvpRoomId == room.roomId) waitingPvpRoomId = null
                } else if (room.status == RoomStatus.WAITING) {
                    // Reactivar como sala de espera si quedó 1 jugador
                    if (waitingPvpRoomId == null || rooms[waitingPvpRoomId?.value] == null) {
                        waitingPvpRoomId = room.roomId
                    }
                }
            }
            GameModeId.PVE -> {
                // PVE: siempre eliminar sala si el jugador se va
                rooms.remove(room.roomId.value)
                roomIdByGameId.remove(room.gameId.value)
            }
        }
    }

    /**
     * ✅ MEJORADO: Remover cliente y notificar a otros en la sala
     */
    suspend fun removeClient(clientId: ClientId): List<ClientId> = mutex.withLock {
        val roomId = roomIdByClientId.remove(clientId.value) ?: return@withLock emptyList()
        val room = rooms[roomId] ?: return@withLock emptyList()

        room.players.remove(clientId)

        val remainingPlayers = room.players.toList() // ✅ NUEVO: para notificar

        when (room.mode) {
            GameModeId.PVP -> {
                room.status = if (room.players.size >= 2) RoomStatus.READY else RoomStatus.WAITING

                if (room.players.isEmpty()) {
                    rooms.remove(room.roomId.value)
                    roomIdByGameId.remove(room.gameId.value)
                    if (waitingPvpRoomId == room.roomId) waitingPvpRoomId = null
                } else {
                    // Sala con 1 jugador vuelve a WAITING
                    if (room.status == RoomStatus.WAITING) {
                        if (waitingPvpRoomId == null || rooms[waitingPvpRoomId?.value] == null) {
                            waitingPvpRoomId = room.roomId
                        }
                    }
                }
            }
            GameModeId.PVE -> {
                // PVE: eliminar sala
                rooms.remove(room.roomId.value)
                roomIdByGameId.remove(room.gameId.value)
            }
        }

        // Limpiar referencias huérfanas
        cleanupOrphanedWaitingRoom()

        remainingPlayers // ✅ RETORNAR jugadores que deben ser notificados
    }

    private fun cleanupOrphanedWaitingRoom() {
        val waiting = waitingPvpRoomId
        if (waiting != null && rooms[waiting.value] == null) {
            waitingPvpRoomId = null
        }
    }

    suspend fun getRoomByGameId(gameId: GameId): Room? = mutex.withLock {
        val roomId = roomIdByGameId[gameId.value] ?: return@withLock null
        rooms[roomId]
    }

    suspend fun getRoom(roomId: RoomId): Room? = mutex.withLock {
        rooms[roomId.value]
    }

    /**
     * ✅ NUEVO: Obtener sala de un cliente
     */
    suspend fun getRoomByClientId(clientId: ClientId): Room? = mutex.withLock {
        val roomId = roomIdByClientId[clientId.value] ?: return@withLock null
        rooms[roomId]
    }
}