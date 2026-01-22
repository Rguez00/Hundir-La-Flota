package com.mario.hlf.domain.model

data class Ship(
    val type: ShipType,
    val cells: Set<Coordinate>,
    private val hits: Set<Coordinate> = emptySet()
) {
    init {
        require(cells.size == type.size) { "Ship cells must match type size" }
    }

    fun registerHit(at: Coordinate): Ship =
        if (at in cells) copy(hits = hits + at) else this

    fun isSunk(): Boolean = hits.containsAll(cells)
}
