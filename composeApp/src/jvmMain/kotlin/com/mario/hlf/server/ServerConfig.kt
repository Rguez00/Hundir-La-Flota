package com.mario.hlf.server

import com.mario.hlf.protocol.AIDifficulty
import java.util.Properties



/**
 * Configuración del servidor cargada desde server.properties
 */
data class ServerConfig(
    val host: String,
    val port: Int,
    val maxClients: Int,
    val aiDifficulty: AIDifficulty = AIDifficulty.NORMAL,
    val turnTimeout: Long = 60_000, // 60 segundos en milisegundos
    val allowAdjacency: Boolean = false,
    val boardSize: Int = 10,
    val maxRounds: Int = 1 // Para futuro: mejor de 1, 3 o 5
) {
    companion object {
        /**
         * Carga configuración desde archivo en resources
         */
        fun load(resourceName: String = "files/server.properties"): ServerConfig {
            val props = Properties()

            val stream = Thread.currentThread().contextClassLoader
                .getResourceAsStream(resourceName)
                ?: run {
                    println("⚠️  No se encontró $resourceName, usando configuración por defecto")
                    return ServerConfig(
                        host = "0.0.0.0",
                        port = 5678,
                        maxClients = 10
                    )
                }

            stream.use { props.load(it) }

            return try {
                ServerConfig(
                    host = props.getProperty("server.host", "0.0.0.0"),
                    port = props.getProperty("server.port", "5678").toIntOrNull() ?: 5678,
                    maxClients = props.getProperty("max.clients", "10").toIntOrNull() ?: 10,
                    aiDifficulty = parseAIDifficulty(props.getProperty("ai.difficulty", "NORMAL")),
                    turnTimeout = props.getProperty("turn.timeout", "60000").toLongOrNull() ?: 60_000,
                    allowAdjacency = props.getProperty("allow.adjacency", "false").toBoolean(),
                    boardSize = props.getProperty("board.size", "10").toIntOrNull() ?: 10,
                    maxRounds = props.getProperty("max.rounds", "1").toIntOrNull() ?: 1
                )
            } catch (e: Exception) {
                println("❌ Error parseando configuración: ${e.message}")
                println("⚠️  Usando configuración por defecto")
                ServerConfig(
                    host = "0.0.0.0",
                    port = 5678,
                    maxClients = 10
                )
            }
        }

        /**
         * Parsea la dificultad de la IA con manejo de errores
         */
        private fun parseAIDifficulty(value: String): AIDifficulty {
            return try {
                AIDifficulty.valueOf(value.uppercase())
            } catch (e: Exception) {
                println("⚠️  Dificultad IA inválida '$value', usando NORMAL")
                AIDifficulty.NORMAL
            }
        }
    }

    /**
     * Imprime la configuración al iniciar el servidor
     */
    fun print() {
        println("""
            ╔════════════════════════════════════════╗
            ║   CONFIGURACIÓN DEL SERVIDOR           ║
            ╠════════════════════════════════════════╣
            ║ Host:              $host
            ║ Puerto:            $port
            ║ Max Clientes:      $maxClients
            ║ Dificultad IA:     $aiDifficulty
            ║ Timeout Turno:     ${turnTimeout / 1000}s
            ║ Tamaño Tablero:    ${boardSize}x${boardSize}
            ║ Adyacencia:        ${if (allowAdjacency) "Permitida" else "Prohibida"}
            ║ Rondas:            Mejor de $maxRounds
            ╚════════════════════════════════════════╝
        """.trimIndent())
    }

    /**
     * Validaciones de configuración
     */
    init {
        require(port in 1024..65535) { "Puerto debe estar entre 1024 y 65535" }
        require(maxClients > 0) { "maxClients debe ser mayor a 0" }
        require(turnTimeout > 0) { "turnTimeout debe ser mayor a 0" }
        require(boardSize in 5..20) { "boardSize debe estar entre 5 y 20" }
        require(maxRounds in 1..5) { "maxRounds debe estar entre 1 y 5" }
    }
}