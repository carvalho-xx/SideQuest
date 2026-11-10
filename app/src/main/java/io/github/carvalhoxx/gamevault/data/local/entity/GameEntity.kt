package io.github.carvalhoxx.gamevault.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.carvalhoxx.gamevault.domain.model.CompletionStyle
import io.github.carvalhoxx.gamevault.domain.model.DurationResolution
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.PriorityTier

@Entity(
    tableName = "games",
    indices = [
        Index(value = ["status"]),
        Index(value = ["status", "releaseDate"]),
        Index(value = ["isMainFocus"]),
        Index(value = ["isHallOfFame"]),
        Index(value = ["isPersonalGoty"])
    ]
)
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val externalApiId: String? = null,
    val dealApiId: String? = null,
    val title: String,
    val coverUrl: String? = null,
    val backdropUrl: String? = null,
    val releaseDate: Long? = null,
    val releaseYear: Int? = null,
    val genres: String = "",
    val platform: String = "PC",
    val status: GameStatus = GameStatus.BACKLOG,
    val priority: PriorityTier = PriorityTier.TIER_B,
    val isMainFocus: Boolean = false,
    // Métricas de Tempo (Flexíveis e Precisas — Null-safe para lançamentos inéditos)
    val hltbMainSeconds: Long? = null,
    val hltbExtraSeconds: Long? = null,
    val hltbCompletionistSeconds: Long? = null,
    val userCustomEstimateSeconds: Long? = null,
    val targetCompletionStyle: CompletionStyle = CompletionStyle.MAIN_STORY,
    val timePlayedSeconds: Long = 0L,
    val activeTimerStartTime: Long? = null,
    val startDate: Long? = null,
    val completionDate: Long? = null,
    // Avaliação (0.0 a 10.0 + 11.0 "Jogo da Vida" / Hall of Fame) e GOTY
    val personalRating: Float? = null,
    val isHallOfFame: Boolean = false,
    val coreMemoryNote: String? = null,
    val isPersonalGoty: Boolean = false,
    // Wishlist e Promoções
    val isOwned: Boolean = true,
    val targetPrice: Double? = null,
    val currentLowestPrice: Double? = null,
    val discountPercentage: Int? = null
) {
    /**
     * Retorna a estimativa oficial do HLTB conforme o estilo de conclusão escolhido,
     * com fallback seguro entre estilos caso algum campo específico seja nulo.
     */
    fun getOfficialHltbTargetSeconds(): Long? {
        return when (targetCompletionStyle) {
            CompletionStyle.MAIN_STORY ->
                hltbMainSeconds ?: hltbExtraSeconds ?: hltbCompletionistSeconds
            CompletionStyle.MAIN_EXTRA ->
                hltbExtraSeconds ?: hltbMainSeconds ?: hltbCompletionistSeconds
            CompletionStyle.COMPLETIONIST_100 ->
                hltbCompletionistSeconds ?: hltbExtraSeconds ?: hltbMainSeconds
        }?.takeIf { it > 0L }
    }

    /**
     * Lógica de Fallback em 3 Níveis (Seção 5.2 ARCHITECTURE.md):
     * 1. Tenta usar userCustomEstimateSeconds.
     * 2. Se null, tenta usar a estimativa oficial da API (HLTB).
     * 3. Se ambos forem null, retorna null (ativando DURATION_TBD_LOCK sem quebrar).
     */
    fun getEffectiveDurationSeconds(): Long? {
        val custom = userCustomEstimateSeconds?.takeIf { it > 0L }
        if (custom != null) return custom
        return getOfficialHltbTargetSeconds()
    }

    /**
     * Resolve o estado de duração para o Planner Day One (nunca lança NPE).
     */
    fun resolveDuration(): DurationResolution {
        val custom = userCustomEstimateSeconds?.takeIf { it > 0L }
        if (custom != null) {
            return DurationResolution.Resolved(
                durationSeconds = custom,
                isUserCustomEstimate = true
            )
        }
        val official = getOfficialHltbTargetSeconds()
        if (official != null) {
            return DurationResolution.Resolved(
                durationSeconds = official,
                isUserCustomEstimate = false
            )
        }
        return DurationResolution.DurationTbdLock
    }

    /**
     * Calcula o tempo total em segundos incluindo a sessão ativa em andamento (se houver).
     */
    fun getLiveTimePlayedSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val startMillis = activeTimerStartTime ?: return timePlayedSeconds
        val elapsedSeconds = ((nowMillis - startMillis) / 1000L).coerceAtLeast(0L)
        return timePlayedSeconds + elapsedSeconds
    }
}
