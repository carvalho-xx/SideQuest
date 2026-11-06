package io.github.carvalhoxx.gamevault.domain.model

enum class GameStatus(val displayName: String) {
    PLAYING("Jogando"),
    BACKLOG("Backlog"),
    DAY_ONE("Day One"),
    WISHLIST("Wishlist"),
    COMPLETED("Zerado"),
    PAUSED("Pausado"),
    DROPPED("Dropado")
}
