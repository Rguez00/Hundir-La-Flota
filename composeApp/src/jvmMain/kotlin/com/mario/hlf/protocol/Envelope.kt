package com.mario.hlf.protocol

import kotlinx.serialization.Serializable

@Serializable
data class Envelope(
    val type: String,
    val requestId: String? = null,
    val payload: Msg
)
