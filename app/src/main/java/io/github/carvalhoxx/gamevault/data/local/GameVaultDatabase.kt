package io.github.carvalhoxx.gamevault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.carvalhoxx.gamevault.data.local.converter.RoomConverters
import io.github.carvalhoxx.gamevault.data.local.dao.GameDao
import io.github.carvalhoxx.gamevault.data.local.dao.UserProfileDao
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.data.local.entity.UserProfileEntity

@Database(
    entities = [GameEntity::class, UserProfileEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class GameVaultDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    abstract fun userProfileDao(): UserProfileDao

    companion object {
        private const val DATABASE_NAME = "sidequest_gamevault.db"

        @Volatile
        private var INSTANCE: GameVaultDatabase? = null

        fun getInstance(context: Context): GameVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GameVaultDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE games ADD COLUMN releaseYear INTEGER")
            }
        }
    }
}
