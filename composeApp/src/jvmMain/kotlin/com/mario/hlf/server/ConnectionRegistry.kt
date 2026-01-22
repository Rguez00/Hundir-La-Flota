package com.mario.hlf.server

import com.mario.hlf.network.Framing
import com.mario.hlf.protocol.Envelope
import com.mario.hlf.protocol.ProtocolJson
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    suspend fun sendTo(clientId: ClientId, env: Envelope) {
        conns[clientId.value]?.send(env)
    }

    suspend fun sendToMany(clientIds: List<ClientId>, envs: List<Envelope>) {
        // envs y clientIds suelen ir en paralelo; si no, se manda cada env a todos
        if (envs.size == clientIds.size) {
            for (i in clientIds.indices) sendTo(clientIds[i], envs[i])
        } else {
            for (cid in clientIds) for (env in envs) sendTo(cid, env)
        }
    }
}
