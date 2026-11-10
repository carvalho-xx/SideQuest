package io.github.carvalhoxx.gamevault.data.repository

import android.content.ContentResolver
import android.net.Uri
import io.github.carvalhoxx.gamevault.data.importer.CsvGameImporter
import io.github.carvalhoxx.gamevault.data.importer.CsvImportSummary
import io.github.carvalhoxx.gamevault.data.local.dao.GameDao
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.data.remote.GameMetadataService
import io.github.carvalhoxx.gamevault.data.remote.RemoteGameMetadata
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream

class GameRepository(
    private val gameDao: GameDao,
    private val csvGameImporter: CsvGameImporter = CsvGameImporter(),
    private val metadataService: GameMetadataService = GameMetadataService()
) {
    fun observeAllGames(): Flow<List<GameEntity>> = gameDao.observeAllGames()

    fun observeGamesByStatus(status: GameStatus): Flow<List<GameEntity>> =
        gameDao.observeGamesByStatus(status)

    fun observePlayingGames(): Flow<List<GameEntity>> = gameDao.observePlayingGames()

    fun observeMainFocusGame(): Flow<GameEntity?> = gameDao.observeMainFocusGame()

    fun observeDayOneGames(): Flow<List<GameEntity>> = gameDao.observeDayOneGames()

    fun observeCompletedGamesInYear(
        startOfYearMillis: Long,
        endOfYearMillis: Long
    ): Flow<List<GameEntity>> =
        gameDao.observeCompletedGamesInDateRange(startOfYearMillis, endOfYearMillis)

    fun observeHallOfFameGames(): Flow<List<GameEntity>> = gameDao.observeHallOfFameGames()

    fun observePersonalGotyGames(): Flow<List<GameEntity>> = gameDao.observePersonalGotyGames()

    fun observeWishlistGames(): Flow<List<GameEntity>> = gameDao.observeWishlistGames()

    fun observeGameById(gameId: Long): Flow<GameEntity?> = gameDao.observeGameById(gameId)

    suspend fun getGameById(gameId: Long): GameEntity? = gameDao.getGameById(gameId)

    suspend fun saveGame(game: GameEntity): Long {
        val normalized = game.copy(
            genres = GameMetadataService.normalizeGenresPtBr(game.genres),
            isHallOfFame = (game.personalRating != null && game.personalRating >= 11.0f) || game.isHallOfFame
        )
        val savedId = if (normalized.id == 0L) {
            gameDao.insertGame(normalized)
        } else {
            gameDao.updateGame(normalized)
            normalized.id
        }
        if (normalized.isMainFocus) {
            gameDao.clearMainFocusExcept(savedId)
        }
        return savedId
    }

    suspend fun deleteGame(gameId: Long) {
        gameDao.deleteGameById(gameId)
    }

    /**
     * Inicia o cronômetro de sessão ao vivo salvando o timestamp atual (Seção 5.1 ARCHITECTURE.md).
     */
    suspend fun startSessionTimer(gameId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val current = gameDao.getGameById(gameId) ?: return
        if (current.activeTimerStartTime != null) return
        gameDao.updateGame(
            current.copy(
                status = GameStatus.PLAYING,
                activeTimerStartTime = nowMillis,
                startDate = current.startDate ?: nowMillis
            )
        )
    }

    /**
     * Para o cronômetro de sessão ao vivo e soma os segundos decorridos em timePlayedSeconds.
     */
    suspend fun stopSessionTimer(gameId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val current = gameDao.getGameById(gameId) ?: return
        val startMillis = current.activeTimerStartTime ?: return
        val elapsedSeconds = ((nowMillis - startMillis) / 1000L).coerceAtLeast(0L)
        gameDao.updateGame(
            current.copy(
                timePlayedSeconds = current.timePlayedSeconds + elapsedSeconds,
                activeTimerStartTime = null
            )
        )
    }

    /** Troca o foco da agenda de forma atômica do ponto de vista da regra de negócio. */
    suspend fun startPlannerJourney(gameId: Long, nowMillis: Long = System.currentTimeMillis()) {
        val currentFocus = gameDao.getAllGamesSnapshot().firstOrNull { it.isMainFocus }
        if (currentFocus != null && currentFocus.id != gameId) {
            stopSessionTimer(currentFocus.id, nowMillis)
            gameDao.getGameById(currentFocus.id)?.let {
                gameDao.updateGame(it.copy(isMainFocus = false))
            }
        }
        val selected = gameDao.getGameById(gameId) ?: return
        saveGame(selected.copy(
            status = GameStatus.PLAYING,
            isMainFocus = true,
            startDate = selected.startDate ?: nowMillis
        ))
    }

    /**
     * Atualiza o tempo exato manualmente (HH:MM:SS do save do console/PC).
     */
    suspend fun updateExactTimePlayed(gameId: Long, exactSeconds: Long) {
        val current = gameDao.getGameById(gameId) ?: return
        gameDao.updateGame(
            current.copy(
                timePlayedSeconds = exactSeconds.coerceAtLeast(0L),
                activeTimerStartTime = null
            )
        )
    }

    /**
     * Atualiza a avaliação (0.0 a 10.0 + 11.0 "Jogo da Vida" / Hall da Fama) e a Memória Afetiva.
     */
    suspend fun updateRatingAndMemory(
        gameId: Long,
        rating: Float?,
        coreMemoryNote: String?,
        isPersonalGoty: Boolean = false
    ) {
        val current = gameDao.getGameById(gameId) ?: return
        val clampedRating = rating?.coerceIn(0.0f, 11.0f)
        val isLifeChanging = clampedRating != null && clampedRating >= 11.0f
        gameDao.updateGame(
            current.copy(
                personalRating = clampedRating,
                isHallOfFame = isLifeChanging,
                coreMemoryNote = coreMemoryNote?.trim()?.takeIf { it.isNotEmpty() },
                isPersonalGoty = isPersonalGoty
            )
        )
    }

    /**
     * Importa sob demanda o arquivo CSV selecionado pelo amigo no seletor de arquivos do Android.
     * Não popula nenhum dado por padrão na instalação do app.
     */
    suspend fun importCsvFromUri(
        contentResolver: ContentResolver,
        uri: Uri
    ): Result<CsvImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Não foi possível abrir o arquivo CSV selecionado.")
            importCsvFromStream(inputStream)
        }
    }

    suspend fun importCsvFromStream(inputStream: InputStream): CsvImportSummary =
        withContext(Dispatchers.IO) {
            val existingTitles = gameDao.getAllNormalizedTitles().toSet()
            val summary = csvGameImporter.parseCsvStream(
                inputStream = inputStream,
                existingNormalizedTitles = existingTitles
            )
            if (summary.importedGames.isNotEmpty()) {
                gameDao.insertGames(summary.importedGames)
            }
            summary
        }

    /**
     * Fase 3: Busca candidatos de jogos na API online (Steam pt-BR + Catálogo HLTB).
     */
    suspend fun searchOnlineGames(query: String): List<RemoteGameMetadata> {
        return metadataService.searchGames(query)
    }

    /**
     * Fase 3: Busca detalhes completos (data, gêneros em pt-BR, tempos HLTB) de um candidato selecionado.
     */
    suspend fun resolveFullMetadata(candidate: RemoteGameMetadata): RemoteGameMetadata {
        return metadataService.fetchBestMatchForTitle(
            title = candidate.title,
            existingExternalId = candidate.externalApiId
        ) ?: candidate
    }

    /**
     * Fase 3: Atualiza capa, data de lançamento, gêneros padronizados e tempos HLTB de um jogo existente
     * preservando o tempo jogado, nota e memória afetiva do usuário.
     */
    suspend fun refreshGameMetadata(gameId: Long): Boolean {
        val current = gameDao.getGameById(gameId) ?: return false
        val remote = metadataService.fetchBestMatchForTitle(
            title = current.title,
            existingExternalId = current.externalApiId
        ) ?: return false

        val resolvedGenres = GameMetadataService.normalizeGenresPtBr(
            remote.genres.ifBlank { current.genres }
        )

        val updated = current.copy(
            externalApiId = remote.externalApiId ?: current.externalApiId,
            coverUrl = remote.coverUrl ?: current.coverUrl,
            backdropUrl = remote.backdropUrl ?: current.backdropUrl,
            releaseDate = remote.releaseDateMillis ?: current.releaseDate,
            genres = resolvedGenres,
            hltbMainSeconds = remote.hltbMainSeconds ?: current.hltbMainSeconds,
            hltbExtraSeconds = remote.hltbExtraSeconds ?: current.hltbExtraSeconds,
            hltbCompletionistSeconds = remote.hltbCompletionistSeconds ?: current.hltbCompletionistSeconds
        )
        gameDao.updateGame(updated)
        return true
    }

    /**
     * Padroniza localmente os gêneros de todos os jogos já cadastrados no banco
     * (evita diferenças como "sobrevivência" vs "Sobrevivência" ou caracteres corrompidos de CSV).
     */
    suspend fun normalizeAllExistingGenres(): Int = withContext(Dispatchers.IO) {
        val all = gameDao.getAllGamesSnapshot()
        var normalizedCount = 0
        for (game in all) {
            val cleanGenres = GameMetadataService.normalizeGenresPtBr(game.genres)
            val cleanTitle = GameMetadataService.unescapeJson(game.title).trim()
            if (cleanGenres != game.genres || cleanTitle != game.title) {
                gameDao.updateGame(game.copy(title = cleanTitle, genres = cleanGenres))
                normalizedCount++
            }
        }
        normalizedCount
    }

    /**
     * Fase 3: Sincroniza em lote todos os jogos da biblioteca (priorizando jogos sem capa/duração/gênero
     * e atualizando também os demais com gêneros padronizados, capas e tempos online).
     */
    suspend fun syncMissingMetadataBatch(
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val localUpdated = normalizeAllExistingGenres()
        val all = gameDao.getAllGamesSnapshot()
        if (all.isEmpty()) return@withContext 0

        val prioritized = all.sortedBy { game ->
            if (game.coverUrl.isNullOrBlank() || game.hltbMainSeconds == null || game.genres.isBlank()) 0 else 1
        }

        var updatedCount = localUpdated
        var processed = 0
        val total = prioritized.size
        onProgress(0, total)

        for (chunk in prioritized.chunked(5)) {
            val results = coroutineScope {
                chunk.map { game ->
                    async {
                        val remote = runCatching {
                            metadataService.fetchBestMatchForTitle(
                                title = game.title,
                                existingExternalId = game.externalApiId
                            )
                        }.getOrNull()
                        game to remote
                    }
                }.awaitAll()
            }

            for ((game, remote) in results) {
                processed++
                if (remote == null) continue

                val newCover = remote.coverUrl ?: game.coverUrl
                val newBackdrop = remote.backdropUrl ?: game.backdropUrl ?: newCover
                val newMain = remote.hltbMainSeconds ?: game.hltbMainSeconds
                val newExtra = remote.hltbExtraSeconds ?: game.hltbExtraSeconds
                val newComp = remote.hltbCompletionistSeconds ?: game.hltbCompletionistSeconds
                val newGenres = GameMetadataService.normalizeGenresPtBr(
                    remote.genres.ifBlank { game.genres }
                )
                val newRelease = remote.releaseDateMillis ?: game.releaseDate
                val newExternalId = remote.externalApiId ?: game.externalApiId

                if (newCover != game.coverUrl ||
                    newMain != game.hltbMainSeconds ||
                    newExtra != game.hltbExtraSeconds ||
                    newComp != game.hltbCompletionistSeconds ||
                    newGenres != game.genres ||
                    newRelease != game.releaseDate ||
                    newExternalId != game.externalApiId
                ) {
                    gameDao.updateGame(
                        game.copy(
                            externalApiId = newExternalId,
                            coverUrl = newCover,
                            backdropUrl = newBackdrop,
                            releaseDate = newRelease,
                            genres = newGenres,
                            hltbMainSeconds = newMain,
                            hltbExtraSeconds = newExtra,
                            hltbCompletionistSeconds = newComp
                        )
                    )
                    updatedCount++
                }
            }
            onProgress(processed, total)
        }
        updatedCount
    }
}
