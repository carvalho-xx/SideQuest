package io.github.carvalhoxx.gamevault.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.data.repository.GameRepository
import io.github.carvalhoxx.gamevault.domain.model.CompletionStyle
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.PriorityTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LibraryFilterState(
    val searchQuery: String = "",
    val selectedStatus: GameStatus? = null,
    val selectedPlatform: String? = null,
    val selectedPriority: PriorityTier? = null
)

data class DashboardUiState(
    val allGames: List<GameEntity> = emptyList(),
    val mainPlayingGame: GameEntity? = null,
    val dayOneGames: List<GameEntity> = emptyList(),
    val hallOfFameGames: List<GameEntity> = emptyList(),
    val personalGotyGames: List<GameEntity> = emptyList(),
    val wishlistGames: List<GameEntity> = emptyList(),
    val nowMillis: Long = System.currentTimeMillis(),
    val statusMessage: String? = null
)

class GameVaultViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _libraryFilter = MutableStateFlow(LibraryFilterState())
    val libraryFilter: StateFlow<LibraryFilterState> = _libraryFilter.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _rouletteWinner = MutableStateFlow<GameEntity?>(null)
    val rouletteWinner: StateFlow<GameEntity?> = _rouletteWinner.asStateFlow()

    val allGames: StateFlow<List<GameEntity>> = repository.observeAllGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dashboardState: StateFlow<DashboardUiState> = combine(
        repository.observeAllGames(),
        repository.observeMainFocusGame(),
        repository.observeDayOneGames(),
        repository.observeHallOfFameGames(),
        repository.observePersonalGotyGames()
    ) { all, mainFocus, dayOne, hallOfFame, goty ->
        DashboardUiState(
            allGames = all,
            mainPlayingGame = mainFocus,
            dayOneGames = dayOne,
            hallOfFameGames = hallOfFame,
            personalGotyGames = goty,
            wishlistGames = all.filter { it.status == GameStatus.WISHLIST || !it.isOwned }
        )
    }.combine(_nowMillis) { state, now ->
        state.copy(nowMillis = now)
    }.combine(_statusMessage) { state, msg ->
        state.copy(statusMessage = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    val filteredLibraryGames: StateFlow<List<GameEntity>> = combine(
        allGames,
        _libraryFilter
    ) { games, filter ->
        games.filter { game ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                game.title.contains(filter.searchQuery, ignoreCase = true) ||
                game.genres.contains(filter.searchQuery, ignoreCase = true) ||
                game.platform.contains(filter.searchQuery, ignoreCase = true)

            val matchesStatus = filter.selectedStatus == null || game.status == filter.selectedStatus
            val matchesPlatform = filter.selectedPlatform == null ||
                game.platform.equals(filter.selectedPlatform, ignoreCase = true)
            val matchesPriority = filter.selectedPriority == null || game.priority == filter.selectedPriority

            matchesQuery && matchesStatus && matchesPlatform && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Relógio de precisão de 1 segundo para atualizar o cronômetro de sessão ao vivo
        viewModelScope.launch {
            while (isActive) {
                _nowMillis.value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _libraryFilter.value = _libraryFilter.value.copy(searchQuery = query)
    }

    fun selectStatusTab(status: GameStatus?) {
        _libraryFilter.value = _libraryFilter.value.copy(selectedStatus = status)
    }

    fun selectPlatformFilter(platform: String?) {
        _libraryFilter.value = _libraryFilter.value.copy(selectedPlatform = platform)
    }

    fun selectPriorityFilter(priority: PriorityTier?) {
        _libraryFilter.value = _libraryFilter.value.copy(selectedPriority = priority)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun toggleSessionTimer(game: GameEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (game.activeTimerStartTime != null) {
                repository.stopSessionTimer(game.id, now)
            } else {
                repository.startSessionTimer(game.id, now)
            }
            _nowMillis.value = System.currentTimeMillis()
        }
    }

    fun updateExactTimePlayed(gameId: Long, hours: Long, minutes: Long, seconds: Long) {
        viewModelScope.launch {
            val totalSeconds = (hours.coerceAtLeast(0L) * 3600L) +
                (minutes.coerceIn(0L, 59L) * 60L) +
                seconds.coerceIn(0L, 59L)
            repository.updateExactTimePlayed(gameId, totalSeconds)
            _statusMessage.value = "Tempo atualizado com precisão."
        }
    }

    fun updateRatingAndMemory(
        gameId: Long,
        rating: Float?,
        coreMemoryNote: String?,
        isPersonalGoty: Boolean
    ) {
        viewModelScope.launch {
            repository.updateRatingAndMemory(
                gameId = gameId,
                rating = rating,
                coreMemoryNote = coreMemoryNote,
                isPersonalGoty = isPersonalGoty
            )
            _statusMessage.value = if (rating != null && rating >= 11.0f) {
                "✨ Adicionado ao Hall da Fama (11/10 Jogo da Vida)!"
            } else {
                "Avaliação salva."
            }
        }
    }

    fun markGameCompleted(game: GameEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val finalSeconds = game.getLiveTimePlayedSeconds(now)
            repository.saveGame(
                game.copy(
                    status = GameStatus.COMPLETED,
                    isMainFocus = false,
                    timePlayedSeconds = finalSeconds,
                    activeTimerStartTime = null,
                    completionDate = game.completionDate ?: now
                )
            )
            _statusMessage.value = "🏆 ${game.title} marcado como Zerado!"
        }
    }

    fun saveGame(game: GameEntity) {
        viewModelScope.launch {
            val savedId = repository.saveGame(game)
            _statusMessage.value = "Jogo salvo: ${game.title}"
            if (game.coverUrl.isNullOrBlank() || game.hltbMainSeconds == null) {
                repository.refreshGameMetadata(savedId)
            }
        }
    }

    fun refreshGameMetadata(gameId: Long) {
        viewModelScope.launch {
            _statusMessage.value = "Buscando capa, data e duração online..."
            val updated = repository.refreshGameMetadata(gameId)
            _statusMessage.value = if (updated) {
                "Dados e capa atualizados com sucesso!"
            } else {
                "Nenhuma informação adicional encontrada online."
            }
        }
    }

    fun syncMissingLibraryMetadata() {
        viewModelScope.launch {
            _statusMessage.value = "Sincronizando capas e durações da biblioteca..."
            val count = repository.syncMissingMetadataBatch()
            _statusMessage.value = if (count > 0) {
                "$count jogos atualizados com capas e durações!"
            } else {
                "Biblioteca já está sincronizada."
            }
        }
    }

    fun deleteGame(gameId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
            _statusMessage.value = "Jogo removido."
            onDeleted()
        }
    }

    fun setMainFocusGame(game: GameEntity) {
        viewModelScope.launch {
            repository.saveGame(
                game.copy(
                    status = GameStatus.PLAYING,
                    isMainFocus = true,
                    startDate = game.startDate ?: System.currentTimeMillis()
                )
            )
            _statusMessage.value = "${game.title} definido como foco principal."
        }
    }

    fun spinBacklogRoulette() {
        val candidates = allGames.value.filter {
            it.status == GameStatus.BACKLOG
        }
        if (candidates.isEmpty()) {
            _rouletteWinner.value = null
            _statusMessage.value = "Nenhum jogo na fila para sortear! Adicione jogos na fila primeiro."
            return
        }
        // Ponderação por prioridade: Nível S tem peso 4, Nível A peso 3, Nível B peso 2, Nível C peso 1
        val weightedPool = candidates.flatMap { game ->
            val weight = when (game.priority) {
                PriorityTier.TIER_S -> 4
                PriorityTier.TIER_A -> 3
                PriorityTier.TIER_B -> 2
                PriorityTier.TIER_C -> 1
            }
            List(weight) { game }
        }
        _rouletteWinner.value = weightedPool.random()
    }

    fun dismissRouletteWinner() {
        _rouletteWinner.value = null
    }

    /**
     * Importa sob demanda o arquivo CSV enviado pelo amigo no seletor de arquivos do Android.
     */
    fun importFriendCsv(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            val result = repository.importCsvFromUri(contentResolver, uri)
            result.fold(
                onSuccess = { summary ->
                    _statusMessage.value =
                        "CSV importado: ${summary.completedCount} zerados, ${summary.playingCount} jogando (${summary.skippedDuplicateCount} duplicados ignorados)."
                    repository.syncMissingMetadataBatch()
                },
                onFailure = { error ->
                    _statusMessage.value = "Erro ao importar CSV: ${error.localizedMessage ?: "formato inválido"}"
                }
            )
        }
    }

    companion object {
        fun provideFactory(repository: GameRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GameVaultViewModel(repository) as T
                }
            }
    }
}
