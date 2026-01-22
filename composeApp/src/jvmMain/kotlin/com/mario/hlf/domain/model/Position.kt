package hundirlaflota.domain.model

data class Position(val x: Int, val y: Int) {
    fun isInside(size: BoardSize): Boolean =
        x in 0 until size.value && y in 0 until size.value
}
