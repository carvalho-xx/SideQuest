package io.github.carvalhoxx.gamevault.data.local

import android.content.Context
import io.github.carvalhoxx.gamevault.data.local.dao.GameDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Armazena a lista personalizada de plataformas do usuário:
 * - Sincroniza automaticamente plataformas que já estão cadastradas nos jogos do banco.
 * - Ao excluir uma plataforma, remove-a da lista e limpa a plataforma de todos os jogos que a utilizavam.
 */
object UserPlatformStore {
    private const val PREFS_NAME = "gamevault_user_platforms"
    private const val KEY_PLATFORMS = "custom_platforms_v1"
    private const val DELIMITER = "|||"
    private val IGNORED_LABELS = setOf("não definida", "sem plataforma", "multiplataforma")

    fun getPlatforms(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PLATFORMS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.lowercase() !in IGNORED_LABELS }
    }

    suspend fun syncWithDatabasePlatforms(context: Context, gameDao: GameDao): List<String> =
        withContext(Dispatchers.IO) {
            val current = getPlatforms(context).toMutableList()
            val dbPlatforms = runCatching { gameDao.getDistinctRegisteredPlatforms() }
                .getOrDefault(emptyList())

            var changed = false
            for (dbPlat in dbPlatforms) {
                val clean = dbPlat.trim()
                if (clean.isNotEmpty() &&
                    clean.lowercase() !in IGNORED_LABELS &&
                    current.none { it.equals(clean, ignoreCase = true) }
                ) {
                    current.add(clean)
                    changed = true
                }
            }
            if (changed) {
                savePlatforms(context, current)
            }
            current
        }

    fun addPlatform(context: Context, platformName: String): List<String> {
        val clean = platformName.trim()
        if (clean.isEmpty() || clean.lowercase() in IGNORED_LABELS) return getPlatforms(context)
        val current = getPlatforms(context).toMutableList()
        if (current.none { it.equals(clean, ignoreCase = true) }) {
            current.add(clean)
            savePlatforms(context, current)
        }
        return current
    }

    suspend fun deletePlatformEverywhere(
        context: Context,
        gameDao: GameDao,
        platformName: String
    ): List<String> = withContext(Dispatchers.IO) {
        val clean = platformName.trim()
        runCatching { gameDao.clearPlatformFromAllGames(clean) }
        val updated = getPlatforms(context).filterNot { it.equals(clean, ignoreCase = true) }
        savePlatforms(context, updated)
        updated
    }

    private fun savePlatforms(context: Context, list: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PLATFORMS, list.joinToString(DELIMITER))
            .apply()
    }
}
