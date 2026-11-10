package io.github.carvalhoxx.gamevault.domain.model

import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity

/** Resultado seguro do cálculo de conflito entre o jogo atual e um lançamento. */
data class ScheduleOverlapResult(
    val willOverlap: Boolean,
    val remainingSeconds: Long,
    /** Tempo efetivo de jogo disponível até a próxima estreia, respeitando o ritmo diário. */
    val availablePlayingSeconds: Long,
    /** Tempo corrido de calendário até a próxima estreia. */
    val calendarSecondsUntilRelease: Long,
    val nextDayOne: GameEntity?,
    val currentDurationIsTbd: Boolean = false
)
