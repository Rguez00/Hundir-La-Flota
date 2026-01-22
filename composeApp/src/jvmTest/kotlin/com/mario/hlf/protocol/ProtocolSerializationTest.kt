package com.mario.hlf.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

class ProtocolSerializationTest {

    @Test
    fun `envelope with hello round-trip`() {
        val original = Envelope(
            requestId = "req-1",
            gameId = null,
            payload = Hello(
                clientVersion = "1.0.0",
                playerName = "Mario"
            )
        )

        val encoded = ProtocolJson.encodeToString(Envelope.serializer(), original)
        val decoded = ProtocolJson.decodeFromString(Envelope.serializer(), encoded)

        assertEquals(original, decoded)
    }
    @Test
    fun `envelope with place ship round-trip`() {
        val original = Envelope(
            requestId = "req-2",
            gameId = "game-1",
            payload = PlaceShip(
                gameId = "game-1",
                player = PlayerId.P1,
                row = 0,
                col = 0,
                ship = ShipTypeId.DESTROYER,
                orientation = OrientationId.HORIZONTAL
            )
        )

        val encoded = ProtocolJson.encodeToString(Envelope.serializer(), original)
        val decoded = ProtocolJson.decodeFromString(Envelope.serializer(), encoded)

        assertEquals(original, decoded)
    }
    @Test
    fun `envelope with game state event round-trip`() {
        val state = GameStateDto(
            phase = PhaseId.BATTLE,
            currentTurn = PlayerId.P1,
            winner = null,
            self = BoardStateDto(
                size = 2,
                cells = listOf(
                    listOf(CellViewId.SHIP, CellViewId.EMPTY),
                    listOf(CellViewId.HIT, CellViewId.MISS)
                )
            ),
            opponent = BoardStateDto(
                size = 2,
                cells = listOf(
                    listOf(CellViewId.UNKNOWN, CellViewId.UNKNOWN),
                    listOf(CellViewId.HIT, CellViewId.MISS)
                )
            )
        )

        val original = Envelope(
            requestId = "req-3",
            gameId = "game-1",
            payload = GameStateEvent(
                gameId = "game-1",
                state = state
            )
        )

        val encoded = ProtocolJson.encodeToString(serializer = Envelope.serializer(), value = original)
        val decoded = ProtocolJson.decodeFromString(deserializer = Envelope.serializer(), string = encoded)

        assertEquals(expected = original, actual = decoded)
    }

}

