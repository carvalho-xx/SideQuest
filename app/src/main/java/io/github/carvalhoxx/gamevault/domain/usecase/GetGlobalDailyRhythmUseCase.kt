package io.github.carvalhoxx.gamevault.domain.usecase

/**
 * Fornece o ritmo diário global usado pelo planejador.
 *
 * O Perfil do Usuário ainda será implementado na Fase 5. Até lá, o valor
 * provisório representa duas horas de jogo por dia.
 */
class GetGlobalDailyRhythmUseCase {
    operator fun invoke(): Long = DAILY_RHYTHM_SECONDS

    private companion object {
        const val DAILY_RHYTHM_SECONDS = 7_200L
    }
}
