package com.mario.hlf.domain.errors

sealed class DomainError(message: String) : IllegalArgumentException(message) {

    class OutOfBounds : DomainError("Coordenada fuera del tablero")
    class Overlap : DomainError("El barco se solapa con otro")
    class AdjacentNotAllowed : DomainError("El barco toca a otro (adyacente)")

    // FASE 3 (Game / use cases)
    class InvalidPhase : DomainError("Acción no permitida en la fase actual")
    class ShipNotAvailable : DomainError("Ese tipo de barco ya no está disponible")
}

