package com.mario.hlf.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Msg

@Serializable
@SerialName("HELLO")
data class Hello(
    val clientVersion: String,
    val playerName: String
) : Msg

@Serializable
@SerialName("WELCOME")
data class Welcome(
    val serverVersion: String,
    val config: ServerConfigDto,
    val records: RecordsDto
) : Msg

@Serializable
@SerialName("ERROR")
data class ErrorMsg(
    val code: String,
    val message: String
) : Msg
