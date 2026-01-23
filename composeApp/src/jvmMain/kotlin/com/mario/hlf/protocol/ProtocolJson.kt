package com.mario.hlf.protocol

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.json.Json

private val protocolModule = SerializersModule {
    polymorphic(Msg::class) {
        // Handshake
        subclass(Hello::class, Hello.serializer())
        subclass(Welcome::class, Welcome.serializer())

        // Errors
        subclass(ErrorMsg::class, ErrorMsg.serializer())

        // Game actions (cliente → servidor)
        subclass(StartGame::class, StartGame.serializer())
        subclass(PlaceShip::class, PlaceShip.serializer())
        subclass(Shoot::class, Shoot.serializer())

        // Game events (servidor → cliente)
        subclass(GameStateEvent::class, GameStateEvent.serializer())
        subclass(GameOverEvent::class, GameOverEvent.serializer())
        subclass(RoomUpdateEvent::class, RoomUpdateEvent.serializer()) // ✅ CORREGIDO: estaba faltando
        subclass(PlayerDisconnectedEvent::class, PlayerDisconnectedEvent.serializer()) // ✅ NUEVO
    }
}

val ProtocolJson: Json = Json {
    serializersModule = protocolModule

    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false

    // 🔑 Necesario porque usas @SerialName("WELCOME") etc.
    classDiscriminator = "_t"

    // Robustez
    isLenient = true
    coerceInputValues = true
    prettyPrint = false // ✅ NUEVO: compacto para red (cambiar a true para debugging)
}