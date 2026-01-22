package com.mario.hlf.server

import java.util.Properties

data class ServerConfig(
    val host: String,
    val port: Int,
    val maxClients: Int,
) {
    companion object {
        fun load(resourceName: String = "server.properties"): ServerConfig {
            val props = Properties()
            val stream = Thread.currentThread().contextClassLoader
                .getResourceAsStream("files/server.properties")
                ?: error("No se encontró files/server.properties en composeResources")


            stream.use { props.load(it) }

            val host = props.getProperty("server.host", "0.0.0.0")
            val port = props.getProperty("server.port", "5678").toInt()
            val maxClients = props.getProperty("max.clients", "10").toInt()
            return ServerConfig(host, port, maxClients)
        }
    }
}
