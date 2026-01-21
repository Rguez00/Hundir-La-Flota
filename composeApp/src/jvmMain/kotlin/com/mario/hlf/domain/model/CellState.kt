package com.mario.hlf.domain.model

enum class CellState {
    EMPTY,   // agua sin disparar
    SHIP,    // barco sin disparar
    HIT,     // impacto
    MISS     // agua disparada
}
