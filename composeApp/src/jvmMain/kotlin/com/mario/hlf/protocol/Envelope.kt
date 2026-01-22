package com.mario.hlf.protocol

import kotlinx.serialization.Serializable

@Serializable
data class Envelope(
    val v: Int = 1,
    val requestId: String? = null,
    val gameId: String? = null,
    val payload: Msg
)
