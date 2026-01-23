package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.ProtocolJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class ConnectionRegistry {

    private class Connection(private val out: OutputStream) {
        private val writeMutex = Mutex()

        suspend fun send(env: Envelope) {
            val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
            val bytes = json.toByteArray(Charsets.UTF_8)

            writeMutex.withLock {
                // Framing.writeFrame ya hace flush()
                Framing.writeFrame(out, bytes)
            }
        }
    }

    private val conns = ConcurrentHashMap<String, Connection>()

    fun register(clientId: ClientId, out: OutputStream) {
        // 🔥 NO cerramos OutputStream aquí: lo cierra ClientSession/socket.use
        conns[clientId.value] = Connection(out)
    }

    fun unregister(clientId: ClientId) {
        // 🔥 Solo quitamos del mapa (no cerramos out)
        conns.remove(clientId.value)
    }

    suspend fun sendTo(clientId: ClientId, env: Envelope): Boolean {
        val conn = conns[clientId.value] ?: return false
        return try {
            conn.send(env)
            true
        } catch (_: IOException) {
            unregister(clientId)
            false
        } catch (_: Throwable) {
            unregister(clientId)
            false
        }
    }

    suspend fun broadcast(clientIds: List<ClientId>, env: Envelope): Int {
        var ok = 0
        for (cid in clientIds) {
            if (sendTo(cid, env)) ok++
        }
        return ok
    }

    suspend fun sendToMany(clientIds: List<ClientId>, envs: List<Envelope>) {
        require(clientIds.size == envs.size) { "clientIds y envs deben tener el mismo tamaño" }
        for (i in clientIds.indices) {
            sendTo(clientIds[i], envs[i])
        }
    }

    fun closeAll() {
        // 🔥 Igual: no cerramos streams aquí. Solo limpiamos el registro.
        conns.clear()
    }
}
