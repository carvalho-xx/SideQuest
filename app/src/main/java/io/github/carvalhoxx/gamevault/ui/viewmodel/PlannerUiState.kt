package io.github.carvalhoxx.gamevault.ui.viewmodel

/**
 * Estado imutável consumido pela tela Agenda.
 *
 * Nenhuma entidade do Room atravessa esta fronteira: a tela recebe somente
 * valores já preparados para apresentação.
 */
sealed interface PlannerUiState {
    data object Idle : PlannerUiState

    data object Loading : PlannerUiState

    data class Ready(
        val jogoAtual: PlannerGameUi,
        val ritmoDiario: String,
        val proximoLancamento: PlannerReleaseUi?,
        val tempoRestante: String,
        val tempoDisponivel: String,
        val diasRestantes: String,
        val avisoSobreposicao: String?,
        val sugestoes: List<PlannerSuggestionUi> = emptyList()
    ) : PlannerUiState

    /** Estado visual da duração indefinida (DURATION_TBD_LOCK). */
    data class AgendaDedicadaTbd(
        val jogo: PlannerGameUi,
        val ritmoDiario: String,
        val proximoLancamento: PlannerReleaseUi?,
        val mensagem: String = "🔒 Agenda dedicada: duração a definir — aguardando dados de duração."
    ) : PlannerUiState
}

/** Eventos da tela Agenda. A tela permanece stateless e só comunica intenções. */
sealed interface PlannerEvent {
    data object AbrirRitmoGlobal : PlannerEvent
    data object DefinirEstimativa : PlannerEvent
    data object ManterDuracaoAdefinir : PlannerEvent
}

data class PlannerGameUi(
    val id: Long,
    val titulo: String,
    val tempoJogado: String
)

data class PlannerReleaseUi(
    val id: Long,
    val titulo: String,
    val dataLancamento: String,
    val contagemRegressiva: String,
    val duracaoAdefinir: Boolean
)

data class PlannerSuggestionUi(
    val id: Long,
    val titulo: String,
    val duracao: String
)
