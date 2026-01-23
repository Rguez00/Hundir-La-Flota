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
                Framing.writeFrame(out, bytes)
                // opcional: out.flush() si tu Framing no lo hace; normalmente no hace falta
            }
        }
    }

    private val conns = ConcurrentHashMap<String, Connection>()

    fun register(clientId: ClientId, out: OutputStream) {
        conns[clientId.value] = Connection(out)
    }

    fun unregister(clientId: ClientId) {
        conns.remove(clientId.value)
    }

    /**
     * @return true si se envió, false si no había conexión o falló el envío.
     * Si falla el envío, se hace unregister del clientId.
     */
    suspend fun sendTo(clientId: ClientId, env: Envelope): Boolean {
        val conn = conns[clientId.value] ?: return false
        return try {
            conn.send(env)
            true
        } catch (_: IOException) {
            unregister(clientId)
            false
        } catch (_: Throwable) {
            // Por seguridad: no queremos tumbar el server por un fallo de envío inesperado
            unregister(clientId)
            false
        }
    }

    /**
     * Broadcast típico: 1 envelope a muchos clientes.
     * Devuelve cuántos envíos han tenido éxito.
     */
    suspend fun broadcast(clientIds: List<ClientId>, env: Envelope): Int {
        var ok = 0
        for (cid in clientIds) {
            if (sendTo(cid, env)) ok++
        }
        return ok
    }

    /**
     * Envío paralelo estricto: clientIds[i] recibe envs[i].
     */
    suspend fun sendToMany(clientIds: List<ClientId>, envs: List<Envelope>) {
        require(clientIds.size == envs.size) { "clientIds y envs deben tener el mismo tamaño" }
        for (i in clientIds.indices) {
            sendTo(clientIds[i], envs[i])
        }
    }
}
