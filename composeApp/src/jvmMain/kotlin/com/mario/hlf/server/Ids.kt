package com.mario.hlf.server

import java.util.UUID

@JvmInline value class ClientId(val value: String)
@JvmInline value class RoomId(val value: String)
@JvmInline value class GameId(val value: String)

fun newClientId() = ClientId(UUID.randomUUID().toString())
fun newRoomId() = RoomId(UUID.randomUUID().toString())
fun newGameId() = GameId(UUID.randomUUID().toString())
