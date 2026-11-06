package io.github.carvalhoxx.gamevault.data.remote

/**
 * Representa um resultado de busca de metadados e tempos de jogo (API Online + Catálogo HLTB).
 * Campos de tempo são estritamente Nullable para suportar lançamentos inéditos (Day One / TBD).
 */
data class RemoteGameMetadata(
    val externalApiId: String,
    val title: String,
    val coverUrl: String? = null,
    val backdropUrl: String? = null,
    val releaseDateMillis: Long? = null,
    val genres: String = "",
    val platform: String = "PC",
    val hltbMainSeconds: Long? = null,
    val hltbExtraSeconds: Long? = null,
    val hltbCompletionistSeconds: Long? = null,
    val isUnreleased: Boolean = false
)
