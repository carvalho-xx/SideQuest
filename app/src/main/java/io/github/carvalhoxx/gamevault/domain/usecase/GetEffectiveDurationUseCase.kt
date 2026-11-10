package io.github.carvalhoxx.gamevault.domain.usecase

import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.DurationResolution

/**
 * Resolve a duração que deve ser usada pelo planejador.
 *
 * A ordem é deliberadamente explícita: uma estimativa informada pelo usuário
 * tem precedência sobre a estimativa HLTB. Quando nenhuma das duas existe,
 * o jogo fica bloqueado para o cálculo de término, mas continua válido para
 * a agenda (caso de um lançamento Day One ainda sem dados).
 */
class GetEffectiveDurationUseCase {
    operator fun invoke(game: GameEntity): DurationResolution {
        game.userCustomEstimateSeconds?.takeIf { it > 0L }?.let { customEstimate ->
            return DurationResolution.Resolved(
                durationSeconds = customEstimate,
                isUserCustomEstimate = true
            )
        }

        game.getOfficialHltbTargetSeconds()?.let { hltbEstimate ->
            return DurationResolution.Resolved(
                durationSeconds = hltbEstimate,
                isUserCustomEstimate = false
            )
        }

        return DurationResolution.DurationTbdLock
    }
}
