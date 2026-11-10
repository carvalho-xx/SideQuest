package io.github.carvalhoxx.gamevault.domain.usecase

import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.DurationResolution
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.ScheduleOverlapResult

/**
 * Verifica se o jogo em andamento ultrapassa o próximo lançamento Day One.
 *
 * A duração do Day One não participa deste cálculo: somente a duração restante
 * do jogo atual, o ritmo global (2h/dia por enquanto) e sua data de lançamento
 * são necessários. Assim, um Day One com DURATION_TBD_LOCK não causa NPE.
 */
class CalculateScheduleOverlapUseCase(
    private val getGlobalDailyRhythmUseCase: GetGlobalDailyRhythmUseCase =
        GetGlobalDailyRhythmUseCase(),
    private val getEffectiveDurationUseCase: GetEffectiveDurationUseCase =
        GetEffectiveDurationUseCase()
) {
    operator fun invoke(
        currentGame: GameEntity,
        dayOneGames: List<GameEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleOverlapResult {
        val nextDayOne = dayOneGames
            .asSequence()
            .filter { it.status == GameStatus.DAY_ONE }
            .filter { releaseDate ->
                releaseDate.releaseDate != null && releaseDate.releaseDate >= nowMillis
            }
            .minByOrNull { it.releaseDate!! }

        val duration = getEffectiveDurationUseCase(currentGame)
        val resolvedDuration = duration as? DurationResolution.Resolved
        if (currentGame.status != GameStatus.PLAYING || resolvedDuration == null) {
            return ScheduleOverlapResult(
                willOverlap = false,
                remainingSeconds = 0L,
                availablePlayingSeconds = 0L,
                calendarSecondsUntilRelease = nextDayOne?.releaseDate
                    ?.let { secondsUntil(it, nowMillis) }
                    ?: 0L,
                nextDayOne = nextDayOne,
                currentDurationIsTbd = duration is DurationResolution.DurationTbdLock
            )
        }

        val timePlayedSeconds = currentGame.getLiveTimePlayedSeconds(nowMillis)
        val effectiveDuration = resolvedDuration.durationSeconds
        val remainingSeconds = maxOf(0L, effectiveDuration - timePlayedSeconds)
        val calendarSecondsUntilRelease = nextDayOne?.releaseDate
            ?.let { secondsUntil(it, nowMillis) }
            ?: 0L
        val dailyRhythmSeconds = getGlobalDailyRhythmUseCase()
        val availablePlayingSeconds = if (dailyRhythmSeconds > 0L) {
            calendarSecondsUntilRelease / 86_400L * dailyRhythmSeconds +
                (calendarSecondsUntilRelease % 86_400L * dailyRhythmSeconds) / 86_400L
        } else {
            0L
        }

        return ScheduleOverlapResult(
            willOverlap = nextDayOne != null && remainingSeconds > availablePlayingSeconds,
            remainingSeconds = remainingSeconds,
            availablePlayingSeconds = availablePlayingSeconds,
            calendarSecondsUntilRelease = calendarSecondsUntilRelease,
            nextDayOne = nextDayOne
        )
    }

    private fun secondsUntil(releaseDateMillis: Long, nowMillis: Long): Long =
        ((releaseDateMillis - nowMillis) / 1_000L).coerceAtLeast(0L)
}
