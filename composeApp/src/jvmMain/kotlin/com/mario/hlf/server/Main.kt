package com.mario.hlf.server

fun main() {
    val config = ServerConfig.load() // files/server.properties
    val server = TcpGameServer(config)

    server.start()

    println("[SERVER] Running. Press ENTER to stop.")
    readlnOrNull()

    server.stop()
}
