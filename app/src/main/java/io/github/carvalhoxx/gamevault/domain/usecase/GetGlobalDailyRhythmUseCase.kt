package io.github.carvalhoxx.gamevault.domain.usecase

import io.github.carvalhoxx.gamevault.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Fornece o ritmo diário global usado pelo planejador.
 *
 * O perfil é observado como um fluxo para que alterações persistidas sejam
 * propagadas automaticamente. Quando ainda não há perfil, são consideradas
 * duas horas de jogo por dia.
 */
class GetGlobalDailyRhythmUseCase(
    private val userRepository: UserRepository? = null
) {
    /** Ritmo configurado pelo usuário, em segundos por dia. */
    fun observe(): Flow<Long> = userRepository
        ?.userProfile
        ?.map { profile -> profile?.globalDailySeconds ?: DAILY_RHYTHM_SECONDS }
        ?: flowOf(DAILY_RHYTHM_SECONDS)

    /**
     * Mantém a API síncrona usada pelo planejador legado enquanto ele ainda
     * não coleta o fluxo do perfil.
     */
    operator fun invoke(): Long = DAILY_RHYTHM_SECONDS

    private companion object {
        const val DAILY_RHYTHM_SECONDS = 7_200L
    }
}
