package com.mario.hlf.domain.model

data class ShipPlacement(
    val start: Coordinate,
    val orientation: Orientation,
    val type: ShipType
) {
    fun cells(): Set<Coordinate> {
        val length = type.size
        return (0 until length).map { offset ->
            when (orientation) {
                Orientation.HORIZONTAL -> Coordinate(start.row, start.col + offset)
                Orientation.VERTICAL -> Coordinate(start.row + offset, start.col)
            }
        }.toSet()
    }
}
