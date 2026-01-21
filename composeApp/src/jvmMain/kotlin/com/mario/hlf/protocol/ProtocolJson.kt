package com.mario.hlf.protocol

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val ProtocolJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "_t"
    serializersModule = SerializersModule {
        // sealed Msg se registra automáticamente con @SerialName en Kotlinx
        // (si te diera problemas, lo ajustamos aquí)
    }
}
