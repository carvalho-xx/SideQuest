package io.github.carvalhoxx.gamevault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.carvalhoxx.gamevault.data.local.entity.UserProfileEntity
import io.github.carvalhoxx.gamevault.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = userRepository
        .observeUserProfile()
        .map { profile ->
            if (profile == null) {
                ProfileUiState(isLoading = false)
            } else {
                ProfileUiState(
                    isLoading = false,
                    userName = profile.name,
                    dailyHours = (profile.globalDailySeconds / SECONDS_PER_HOUR).toInt(),
                    dailyMinutes = ((profile.globalDailySeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE).toInt()
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState()
        )

    /** Persiste o nome e o ritmo diário informados pela tela de perfil. */
    fun saveProfile(name: String, dailyHours: Int, dailyMinutes: Int) {
        viewModelScope.launch {
            val safeHours = dailyHours.coerceAtLeast(0)
            val safeMinutes = dailyMinutes.coerceIn(0, 59)
            userRepository.saveUserProfile(
                UserProfileEntity(
                    name = name,
                    globalDailySeconds = safeHours * SECONDS_PER_HOUR +
                        safeMinutes * SECONDS_PER_MINUTE
                )
            )
        }
    }

    companion object {
        const val SECONDS_PER_MINUTE = 60L
        const val SECONDS_PER_HOUR = 3_600L

        fun provideFactory(repository: UserRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProfileViewModel(repository) as T
            }
    }
}
