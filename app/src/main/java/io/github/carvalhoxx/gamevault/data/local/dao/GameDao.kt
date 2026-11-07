package io.github.carvalhoxx.gamevault.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games ORDER BY isMainFocus DESC, priority ASC, title ASC")
    fun observeAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games ORDER BY id ASC")
    suspend fun getAllGamesSnapshot(): List<GameEntity>

    @Query("SELECT * FROM games WHERE status = :status ORDER BY isMainFocus DESC, priority ASC, completionDate DESC, title ASC")
    fun observeGamesByStatus(status: GameStatus): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = 'PLAYING' ORDER BY isMainFocus DESC, activeTimerStartTime DESC, id DESC")
    fun observePlayingGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = 'PLAYING' ORDER BY isMainFocus DESC, activeTimerStartTime DESC, id DESC LIMIT 1")
    fun observeMainFocusGame(): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE status = 'DAY_ONE' ORDER BY CASE WHEN releaseDate IS NULL THEN 1 ELSE 0 END, releaseDate ASC")
    fun observeDayOneGames(): Flow<List<GameEntity>>

    @Query(
        "SELECT * FROM games " +
            "WHERE status = 'COMPLETED' " +
            "AND completionDate IS NOT NULL " +
            "AND completionDate BETWEEN :startOfYearMillis AND :endOfYearMillis " +
            "ORDER BY completionDate DESC"
    )
    fun observeCompletedGamesInDateRange(
        startOfYearMillis: Long,
        endOfYearMillis: Long
    ): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isHallOfFame = 1 OR personalRating >= 11.0 ORDER BY completionDate DESC, title ASC")
    fun observeHallOfFameGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isPersonalGoty = 1 ORDER BY completionDate DESC")
    fun observePersonalGotyGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = 'WISHLIST' OR isOwned = 0 ORDER BY discountPercentage DESC, priority ASC")
    fun observeWishlistGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    fun observeGameById(gameId: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGameById(gameId: Long): GameEntity?

    @Query("SELECT LOWER(TRIM(title)) FROM games")
    suspend fun getAllNormalizedTitles(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>): List<Long>

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: Long)

    @Query("UPDATE games SET isMainFocus = 0 WHERE id != :exceptGameId AND isMainFocus = 1")
    suspend fun clearMainFocusExcept(exceptGameId: Long)

    @Query(
        "SELECT DISTINCT TRIM(platform) FROM games " +
            "WHERE TRIM(platform) != '' " +
            "AND LOWER(TRIM(platform)) NOT IN ('não definida', 'sem plataforma')"
    )
    suspend fun getDistinctRegisteredPlatforms(): List<String>

    @Query(
        "UPDATE games SET platform = 'Sem plataforma' " +
            "WHERE LOWER(TRIM(platform)) = LOWER(TRIM(:platformName))"
    )
    suspend fun clearPlatformFromAllGames(platformName: String)
}
