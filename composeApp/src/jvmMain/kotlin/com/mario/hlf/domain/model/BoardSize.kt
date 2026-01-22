package hundirlaflota.domain.model

@JvmInline
value class BoardSize(val value: Int) {
    init {
        require(value in 5..30) { "BoardSize must be between 5 and 30" }
    }
}
