package io.github.carvalhoxx.gamevault.domain.model

import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity

/** Resultado seguro do cálculo de conflito entre o jogo atual e um lançamento. */
data class ScheduleOverlapResult(
    val willOverlap: Boolean,
    val remainingSeconds: Long,
    val availableSeconds: Long,
    val nextDayOne: GameEntity?,
    val currentDurationIsTbd: Boolean = false
)
