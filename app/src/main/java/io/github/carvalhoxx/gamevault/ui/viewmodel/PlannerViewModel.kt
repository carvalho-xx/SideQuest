package io.github.carvalhoxx.gamevault.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.data.repository.GameRepository
import io.github.carvalhoxx.gamevault.data.repository.UserRepository
import io.github.carvalhoxx.gamevault.domain.model.DurationResolution
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.ScheduleOverlapResult
import io.github.carvalhoxx.gamevault.domain.usecase.CalculateScheduleOverlapUseCase
import io.github.carvalhoxx.gamevault.domain.usecase.GetEffectiveDurationUseCase
import io.github.carvalhoxx.gamevault.domain.usecase.GetGlobalDailyRhythmUseCase
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.ceil

class PlannerViewModel(
    private val repository: GameRepository,
    private val getEffectiveDurationUseCase: GetEffectiveDurationUseCase = GetEffectiveDurationUseCase(),
    private val calculateScheduleOverlapUseCase: CalculateScheduleOverlapUseCase = CalculateScheduleOverlapUseCase(),
    private val getGlobalDailyRhythmUseCase: GetGlobalDailyRhythmUseCase = GetGlobalDailyRhythmUseCase(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    fun iniciarJornada(jogoId: Long) {
        viewModelScope.launch { repository.startPlannerJourney(jogoId) }
    }

    private val _state = MutableStateFlow<PlannerUiState>(PlannerUiState.Idle)
    val state: StateFlow<PlannerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeMainFocusGame(),
                repository.observeDayOneGames(),
                repository.observeAllGames(),
                getGlobalDailyRhythmUseCase.observe()
            ) { jogoAtual, lancamentos, jogos, ritmo ->
                PlannerInputs(jogoAtual, lancamentos, jogos, ritmo)
            }.collectLatest { inputs ->
                    _state.value = PlannerUiState.Loading
                    _state.value = buildState(
                        inputs.jogoAtual,
                        inputs.lancamentos,
                        inputs.jogos,
                        inputs.ritmo,
                        nowMillis()
                    )
                }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun buildState(
        jogoAtual: GameEntity?,
        lancamentos: List<GameEntity>,
        jogos: List<GameEntity>,
        ritmo: Long,
        now: Long
    ): PlannerUiState {
        val lancamentosUi = lancamentos.map { it.toReleaseUi(now) }
        val proximoLancamento = lancamentos
            .asSequence()
            .filter { it.status == GameStatus.DAY_ONE }
            .filter { it.releaseDate != null && it.releaseDate >= now }
            .minByOrNull { it.releaseDate!! }
            ?.toReleaseUi(now)

        val proximoLancamentoEntity = lancamentos
            .asSequence()
            .filter { it.status == GameStatus.DAY_ONE }
            .filter { it.releaseDate != null && it.releaseDate >= now }
            .minByOrNull { it.releaseDate!! }
        val tempoDisponivelParaSugestoes = proximoLancamentoEntity
            ?.releaseDate
            ?.let { releaseDate ->
                calculateAvailablePlayingSeconds(
                    secondsUntilRelease = ((releaseDate - now) / 1_000L).coerceAtLeast(0L),
                    dailyRhythmSeconds = ritmo
                )
            }
            ?: 0L

        val sugestoes = jogos
            .asSequence()
            .filter { it.status == GameStatus.BACKLOG }
            .filter { it.id != jogoAtual?.id }
            .mapNotNull { jogo ->
                val duration = jogo.getEffectiveDurationSeconds()?.takeIf { it > 0L }
                val remaining = duration?.let {
                    (it - jogo.getLiveTimePlayedSeconds(now)).coerceAtLeast(0L)
                }
                remaining?.takeIf { it > 0L && it <= tempoDisponivelParaSugestoes }
                    ?.let { jogo to it }
            }
            .sortedWith(compareBy<Pair<GameEntity, Long>> { it.first.priority.ordinal }.thenBy { it.second })
            .take(5)
            .map { (jogo, duration) ->
                PlannerSuggestionUi(
                    id = jogo.id,
                    titulo = jogo.title,
                    duracao = TimeFormatUtils.formatSecondsToHhMmSs(duration),
                    coverUrl = jogo.coverUrl
                )
            }
            .toList()

        if (jogoAtual == null) {
            val tempoDisponivel = if (proximoLancamento != null) {
                val releaseTimestamp = lancamentos
                    .asSequence()
                    .filter { it.status == GameStatus.DAY_ONE }
                    .filter { it.releaseDate != null && it.releaseDate >= now }
                    .minByOrNull { it.releaseDate!! }
                    ?.releaseDate ?: now
                val secondsUntilRelease = ((releaseTimestamp - now) / 1_000L).coerceAtLeast(0L)
                val daily = ritmo
                    val available = calculateAvailablePlayingSeconds(secondsUntilRelease, daily)
                TimeFormatUtils.formatSecondsToHhMmSs(available)
            } else {
                "0:00:00"
            }

            val diasRestantes = if (proximoLancamento != null) {
                val releaseTimestamp = lancamentos
                    .asSequence()
                    .filter { it.status == GameStatus.DAY_ONE }
                    .filter { it.releaseDate != null && it.releaseDate >= now }
                    .minByOrNull { it.releaseDate!! }
                    ?.releaseDate ?: now
                val secondsUntilRelease = ((releaseTimestamp - now) / 1_000L).coerceAtLeast(0L)
                val dias = if (ritmo > 0L) ceil(secondsUntilRelease.toDouble() / ritmo.toDouble()).toLong() else 0L
                "$dias ${if (dias == 1L) "dia" else "dias"} restantes"
            } else {
                "Sem lançamentos próximos"
            }

            return PlannerUiState.Livre(
                ritmoDiario = formatDailyRhythm(ritmo),
                proximoLancamento = proximoLancamento,
                tempoDisponivel = tempoDisponivel,
                diasRestantes = diasRestantes,
                sugestoes = sugestoes,
                lancamentos = lancamentosUi
            )
        }

        val overlap = calculateScheduleOverlapUseCase(
            currentGame = jogoAtual,
            dayOneGames = lancamentos,
            nowMillis = now,
            dailyRhythmSeconds = ritmo
        )
        val jogoUi = jogoAtual.toPlannerUi(now)

        if (getEffectiveDurationUseCase(jogoAtual) is DurationResolution.DurationTbdLock) {
            return PlannerUiState.AgendaDedicadaTbd(
                jogo = jogoUi,
                ritmoDiario = formatDailyRhythm(ritmo),
                proximoLancamento = proximoLancamento,
                lancamentos = lancamentosUi
            )
        }

        return overlap.toReadyState(jogoUi, ritmo, proximoLancamento, sugestoes, lancamentosUi)
    }

    private fun ScheduleOverlapResult.toReadyState(
        jogo: PlannerGameUi,
        ritmoDiarioSegundos: Long,
        proximoLancamento: PlannerReleaseUi?,
        sugestoes: List<PlannerSuggestionUi>,
        lancamentos: List<PlannerReleaseUi>
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
            sugestoes = if (willOverlap) sugestoes else emptyList(),
            lancamentos = lancamentos
        )
    }

    private fun GameEntity.toPlannerUi(now: Long): PlannerGameUi = PlannerGameUi(
        id = id,
        titulo = title,
        tempoJogado = TimeFormatUtils.formatSecondsToHhMmSs(getLiveTimePlayedSeconds(now)),
        coverUrl = coverUrl
    )

    private fun GameEntity.toReleaseUi(now: Long): PlannerReleaseUi = PlannerReleaseUi(
        id = id,
        titulo = title,
        dataLancamento = TimeFormatUtils.formatDateOrYearOrTbd(releaseDate, releaseYear),
        contagemRegressiva = TimeFormatUtils.formatReleaseCountdownBadge(releaseDate, now),
        duracaoAdefinir = getEffectiveDurationUseCase(this) is DurationResolution.DurationTbdLock,
        coverUrl = coverUrl,
        anoLancamento = releaseYear
    )

    private fun formatDailyRhythm(seconds: Long): String {
        val hours = seconds / 3_600L
        return "$hours h/dia"
    }

    /** Converte o tempo de calendário em tempo efetivamente jogável. */
    private fun calculateAvailablePlayingSeconds(
        secondsUntilRelease: Long,
        dailyRhythmSeconds: Long
    ): Long = if (dailyRhythmSeconds > 0L) {
        secondsUntilRelease / 86_400L * dailyRhythmSeconds +
            ((secondsUntilRelease % 86_400L) * dailyRhythmSeconds) / 86_400L
    } else {
        0L
    }

    companion object {
        fun provideFactory(
            repository: GameRepository,
            userRepository: UserRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PlannerViewModel(
                        repository = repository,
                        getGlobalDailyRhythmUseCase = GetGlobalDailyRhythmUseCase(userRepository)
                    ) as T
            }
    }

    private data class PlannerInputs(
        val jogoAtual: GameEntity?,
        val lancamentos: List<GameEntity>,
        val jogos: List<GameEntity>,
        val ritmo: Long
    )
}
