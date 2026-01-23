package com.mario.hlf.protocol

import kotlinx.serialization.json.Json

val ProtocolJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
    classDiscriminator = "_t"

    // Robustez "versión final"
    isLenient = true
    coerceInputValues = true
}
