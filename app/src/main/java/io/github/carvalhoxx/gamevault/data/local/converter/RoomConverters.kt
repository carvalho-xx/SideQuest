package io.github.carvalhoxx.gamevault.data.local.converter

import androidx.room.TypeConverter
import io.github.carvalhoxx.gamevault.domain.model.CompletionStyle
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.PriorityTier

class RoomConverters {

    @TypeConverter
    fun fromGameStatus(status: GameStatus?): String {
        return (status ?: GameStatus.BACKLOG).name
    }

    @TypeConverter
    fun toGameStatus(value: String?): GameStatus {
        if (value.isNullOrBlank()) return GameStatus.BACKLOG
        return runCatching { GameStatus.valueOf(value) }.getOrDefault(GameStatus.BACKLOG)
    }

    @TypeConverter
    fun fromPriorityTier(priority: PriorityTier?): String {
        return (priority ?: PriorityTier.TIER_B).name
    }

    @TypeConverter
    fun toPriorityTier(value: String?): PriorityTier {
        if (value.isNullOrBlank()) return PriorityTier.TIER_B
        return runCatching { PriorityTier.valueOf(value) }.getOrDefault(PriorityTier.TIER_B)
    }

    @TypeConverter
    fun fromCompletionStyle(style: CompletionStyle?): String {
        return (style ?: CompletionStyle.MAIN_STORY).name
    }

    @TypeConverter
    fun toCompletionStyle(value: String?): CompletionStyle {
        if (value.isNullOrBlank()) return CompletionStyle.MAIN_STORY
        return runCatching { CompletionStyle.valueOf(value) }.getOrDefault(CompletionStyle.MAIN_STORY)
    }
}
