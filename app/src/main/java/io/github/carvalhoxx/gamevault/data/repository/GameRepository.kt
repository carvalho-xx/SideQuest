package io.github.carvalhoxx.gamevault.data.repository

import android.content.ContentResolver
import android.net.Uri
import io.github.carvalhoxx.gamevault.data.importer.CsvGameImporter
import io.github.carvalhoxx.gamevault.data.importer.CsvImportSummary
import io.github.carvalhoxx.gamevault.data.local.dao.GameDao
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream

class GameRepository(
    private val gameDao: GameDao,
    private val csvGameImporter: CsvGameImporter = CsvGameImporter()
) {
    fun observeAllGames(): Flow<List<GameEntity>> = gameDao.observeAllGames()

    fun observeGamesByStatus(status: GameStatus): Flow<List<GameEntity>> =
        gameDao.observeGamesByStatus(status)

    fun observePlayingGames(): Flow<List<GameEntity>> = gameDao.observePlayingGames()

    fun observeMainFocusGame(): Flow<GameEntity?> = gameDao.observeMainFocusGame()

    fun observeDayOneGames(): Flow<List<GameEntity>> = gameDao.observeDayOneGames()

    fun observeHallOfFameGames(): Flow<List<GameEntity>> = gameDao.observeHallOfFameGames()

    fun observePersonalGotyGames(): Flow<List<GameEntity>> = gameDao.observePersonalGotyGames()

    fun observeWishlistGames(): Flow<List<GameEntity>> = gameDao.observeWishlistGames()

    fun observeGameById(gameId: Long): Flow<GameEntity?> = gameDao.observeGameById(gameId)

    suspend fun getGameById(gameId: Long): GameEntity? = gameDao.getGameById(gameId)

    suspend fun saveGame(game: GameEntity): Long {
        val normalized = game.copy(
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
}
