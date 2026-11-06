package io.github.carvalhoxx.gamevault.domain.model

enum class GameStatus(val displayName: String) {
    PLAYING("Jogando"),
    BACKLOG("Na Fila"),
    DAY_ONE("Lançamento"),
    WISHLIST("Lista de Desejos"),
    COMPLETED("Zerado"),
    PAUSED("Pausado"),
    DROPPED("Abandonado")
}
