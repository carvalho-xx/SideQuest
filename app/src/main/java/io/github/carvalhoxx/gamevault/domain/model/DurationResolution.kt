package io.github.carvalhoxx.gamevault.domain.model

/**
 * Representa a resolução de duração para o Otimizador de Cronograma (Seção 5.2 ARCHITECTURE.md).
 * Nunca lança NullPointerException em jogos Day One sem HLTB (ex: GTA VI).
 */
sealed interface DurationResolution {
    data class Resolved(
        val durationSeconds: Long,
        val isUserCustomEstimate: Boolean
    ) : DurationResolution

    /**
     * Nível 3 de Fallback: Quando userCustomEstimateSeconds e HLTB oficial são nulos.
     * Ativa o estado visual "🔒 Agenda Dedicada (Sem previsão de término — Aguardando dados)".
     */
    data object DurationTbdLock : DurationResolution
}
