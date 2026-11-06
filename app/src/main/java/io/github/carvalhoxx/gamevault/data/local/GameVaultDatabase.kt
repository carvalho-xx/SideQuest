package io.github.carvalhoxx.gamevault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.carvalhoxx.gamevault.data.local.converter.RoomConverters
import io.github.carvalhoxx.gamevault.data.local.dao.GameDao
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity

@Database(
    entities = [GameEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class GameVaultDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
