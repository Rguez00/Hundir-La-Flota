package com.mario.hlf.domain.errors

sealed class DomainError(message: String) : IllegalArgumentException(message) {
    class OutOfBounds : DomainError("Coordenada fuera del tablero")
    class Overlap : DomainError("El barco se solapa con otro")
    class AdjacentNotAllowed : DomainError("El barco toca a otro (adyacente)")
}
