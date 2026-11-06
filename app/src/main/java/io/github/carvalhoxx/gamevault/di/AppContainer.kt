package io.github.carvalhoxx.gamevault.di

import android.content.Context
import io.github.carvalhoxx.gamevault.data.importer.CsvGameImporter
import io.github.carvalhoxx.gamevault.data.local.GameVaultDatabase
import io.github.carvalhoxx.gamevault.data.repository.GameRepository

class AppContainer(context: Context) {

    private val database: GameVaultDatabase by lazy {
        GameVaultDatabase.getInstance(context)
    }

    val csvGameImporter: CsvGameImporter by lazy {
        CsvGameImporter()
    }

    val gameRepository: GameRepository by lazy {
        GameRepository(
            gameDao = database.gameDao(),
            csvGameImporter = csvGameImporter
        )
    }
}
