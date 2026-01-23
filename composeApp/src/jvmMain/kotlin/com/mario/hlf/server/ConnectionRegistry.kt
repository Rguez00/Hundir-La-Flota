package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.ProtocolJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class ConnectionRegistry {

    private class Connection(private val out: OutputStream) : Closeable {
        private val writeMutex = Mutex()

        suspend fun send(env: Envelope) {
            val json = ProtocolJson.encodeToString(Envelope.serializer(), env)
            val bytes = json.toByteArray(Charsets.UTF_8)
            writeMutex.withLock {
                Framing.writeFrame(out, bytes)
            }
        }

        override fun close() {
            try { out.close() } catch (_: Throwable) {}
        }
    }

    private val conns = ConcurrentHashMap<String, Connection>()

    fun register(clientId: ClientId, out: OutputStream) {
        // si ya había una conexión, la cerramos para no filtrar recursos
        conns.put(clientId.value, Connection(out))?.close()
    }

    fun unregister(clientId: ClientId) {
        conns.remove(clientId.value)?.close()
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

    /**
     * Útil para stop() del server si quieres limpiar todo de golpe.
     */
    fun closeAll() {
        conns.keys.toList().forEach { key ->
            conns.remove(key)?.close()
        }
    }
}
