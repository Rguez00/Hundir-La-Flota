package com.mario.hlf.server

fun main() {
    // Banner de inicio
    println("""
        ╔════════════════════════════════════════╗
        ║   HUNDIR LA FLOTA - SERVIDOR TCP       ║
        ║   Batalla Naval Multijugador           ║
        ║   Versión 1.0                          ║
        ╚════════════════════════════════════════╝
    """.trimIndent())

    println("\n📁 Cargando configuración desde server.properties...")

    // Cargar configuración desde resources
    val config = try {
        ServerConfig.load("files/server.properties")
    } catch (e: Exception) {
        println("❌ Error fatal cargando configuración: ${e.message}")
        return
    }

    // Mostrar configuración cargada
    config.print()

    println("\n🚀 Iniciando servidor...\n")

    // Crear servidor
    val server = TcpGameServer(config)

    // Hook para cerrar limpiamente con Ctrl+C
    Runtime.getRuntime().addShutdownHook(Thread {
        println("\n\n🛑 Señal de apagado recibida (Ctrl+C)...")
        server.stop()
        println("✅ Servidor cerrado correctamente")
    })

    // Iniciar servidor
    try {
        server.start()

        // Esperar input del usuario
        println("\n═══════════════════════════════════════")
        println("⚡ Servidor en ejecución")
        println("💡 Presiona ENTER para detener")
        println("═══════════════════════════════════════\n")

        readlnOrNull()

        println("\n🛑 Deteniendo servidor...")
        server.stop()
        println("✅ Servidor cerrado correctamente")

    } catch (e: Exception) {
        println("\n❌ Error fatal en el servidor: ${e.message}")
        e.printStackTrace()
        server.stop()
    }
}