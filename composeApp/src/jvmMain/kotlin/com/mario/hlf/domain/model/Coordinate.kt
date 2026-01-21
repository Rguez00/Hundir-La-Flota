package com.mario.hlf.domain.model

data class Coordinate(val row: Int, val col: Int) {
    fun isInside(size: Int = 10): Boolean =
        row in 0 until size && col in 0 until size
}
