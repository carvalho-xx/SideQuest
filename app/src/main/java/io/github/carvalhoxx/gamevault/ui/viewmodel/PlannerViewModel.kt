package io.github.carvalhoxx.gamevault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.data.repository.GameRepository
import io.github.carvalhoxx.gamevault.domain.model.DurationResolution
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.ScheduleOverlapResult
import io.github.carvalhoxx.gamevault.domain.usecase.CalculateScheduleOverlapUseCase
import io.github.carvalhoxx.gamevault.domain.usecase.GetEffectiveDurationUseCase
import io.github.carvalhoxx.gamevault.domain.usecase.GetGlobalDailyRhythmUseCase
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.ceil

class PlannerViewModel(
    private val repository: GameRepository,
    private val getEffectiveDurationUseCase: GetEffectiveDurationUseCase = GetEffectiveDurationUseCase(),
    private val calculateScheduleOverlapUseCase: CalculateScheduleOverlapUseCase = CalculateScheduleOverlapUseCase(),
    private val getGlobalDailyRhythmUseCase: GetGlobalDailyRhythmUseCase = GetGlobalDailyRhythmUseCase(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _state = MutableStateFlow<PlannerUiState>(PlannerUiState.Idle)
    val state: StateFlow<PlannerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeMainFocusGame(),
                repository.observeDayOneGames(),
                repository.observeAllGames()
            ) { jogoAtual, lancamentos, jogos -> Triple(jogoAtual, lancamentos, jogos) }
                .collectLatest { (jogoAtual, lancamentos, jogos) ->
                    _state.value = PlannerUiState.Loading
                    _state.value = buildState(jogoAtual, lancamentos, jogos, nowMillis())
                }
        }
    }

    private fun buildState(
        jogoAtual: GameEntity?,
        lancamentos: List<GameEntity>,
        jogos: List<GameEntity>,
        now: Long
    ): PlannerUiState {
        if (jogoAtual == null) return PlannerUiState.Idle

        val ritmo = getGlobalDailyRhythmUseCase()
        val overlap = calculateScheduleOverlapUseCase(jogoAtual, lancamentos, now)
        val proximoLancamento = overlap.nextDayOne?.toReleaseUi(now)
        val jogoUi = jogoAtual.toPlannerUi(now)

        if (getEffectiveDurationUseCase(jogoAtual) is DurationResolution.DurationTbdLock) {
            return PlannerUiState.AgendaDedicadaTbd(
                jogo = jogoUi,
                ritmoDiario = formatDailyRhythm(ritmo),
                proximoLancamento = proximoLancamento
            )
        }

        val sugestoes = jogos
            .asSequence()
            .filter { it.id != jogoAtual.id && it.status == GameStatus.BACKLOG }
            .mapNotNull { jogo ->
                val duration = jogo.getEffectiveDurationSeconds()
                    ?.takeIf { it > 0L && it <= overlap.availablePlayingSeconds }
                duration?.let { jogo to it }
            }
            .sortedWith(compareBy<Pair<GameEntity, Long>> { it.first.priority.ordinal }.thenBy { it.second })
            .take(5)
            .map { (jogo, duration) ->
                PlannerSuggestionUi(
                    id = jogo.id,
                    titulo = jogo.title,
                    duracao = TimeFormatUtils.formatSecondsToHhMmSs(duration)
                )
            }
            .toList()

        return overlap.toReadyState(jogoUi, ritmo, proximoLancamento, sugestoes)
    }

    private fun ScheduleOverlapResult.toReadyState(
        jogo: PlannerGameUi,
        ritmoDiarioSegundos: Long,
        proximoLancamento: PlannerReleaseUi?,
        sugestoes: List<PlannerSuggestionUi>
    ): PlannerUiState.Ready {
        val dias = if (ritmoDiarioSegundos > 0L) {
            ceil(remainingSeconds.toDouble() / ritmoDiarioSegundos).toLong()
        } else {
            0L
        }
        val aviso = if (willOverlap && nextDayOne != null) {
            "Você pode acabar não zerando ${jogo.titulo} até o lançamento de ${nextDayOne.title} com esse ritmo de ${formatDailyRhythm(ritmoDiarioSegundos)}."
        } else if (nextDayOne != null) {
            "Você tem tempo para terminar antes do próximo lançamento."
        } else {
            null
        }
        return PlannerUiState.Ready(
            jogoAtual = jogo,
            ritmoDiario = formatDailyRhythm(ritmoDiarioSegundos),
            proximoLancamento = proximoLancamento,
            tempoRestante = TimeFormatUtils.formatSecondsToHhMmSs(remainingSeconds),
            tempoDisponivel = TimeFormatUtils.formatSecondsToHhMmSs(availablePlayingSeconds),
            diasRestantes = "$dias ${if (dias == 1L) "dia" else "dias"} restantes",
            avisoSobreposicao = aviso,
            sugestoes = if (willOverlap) sugestoes else emptyList()
        )
    }

    private fun GameEntity.toPlannerUi(now: Long): PlannerGameUi = PlannerGameUi(
        id = id,
        titulo = title,
        tempoJogado = TimeFormatUtils.formatSecondsToHhMmSs(getLiveTimePlayedSeconds(now))
    )

    private fun GameEntity.toReleaseUi(now: Long): PlannerReleaseUi = PlannerReleaseUi(
        id = id,
        titulo = title,
        dataLancamento = TimeFormatUtils.formatDateOrTbd(releaseDate),
        contagemRegressiva = TimeFormatUtils.formatReleaseCountdownBadge(releaseDate, now),
        duracaoAdefinir = getEffectiveDurationUseCase(this) is DurationResolution.DurationTbdLock
    )

    private fun formatDailyRhythm(seconds: Long): String {
        val hours = seconds / 3_600L
        return "$hours h/dia"
    }

    companion object {
        fun provideFactory(repository: GameRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PlannerViewModel(repository) as T
            }
    }
}
