package io.github.carvalhoxx.gamevault.ui.viewmodel

/** Estado imutável consumido pela tela de perfil. */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val dailyHours: Int = 0,
    val dailyMinutes: Int = 0
)
