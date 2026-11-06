package io.github.carvalhoxx.gamevault.data.importer

import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.data.remote.GameMetadataService
import io.github.carvalhoxx.gamevault.domain.model.CompletionStyle
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.PriorityTier
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class CsvImportSummary(
    val importedGames: List<GameEntity>,
    val completedCount: Int,
    val playingCount: Int,
    val skippedDuplicateCount: Int
)

/**
 * Importador temporário de CSV para carregar a planilha pessoal do amigo (formato ref.csv)
 * sob demanda via seletor de arquivo (sem popular o banco por padrão).
 */
class CsvGameImporter {

    fun parseCsvStream(
        inputStream: InputStream,
        existingNormalizedTitles: Set<String> = emptySet()
    ): CsvImportSummary {
        val rawBytes = inputStream.use { it.readBytes() }
        val utf8Text = String(rawBytes, StandardCharsets.UTF_8)
        val decodedText = if (utf8Text.contains('\uFFFD')) {
            String(rawBytes, Charset.forName("windows-1252"))
        } else {
            utf8Text
        }
        val rawLines = decodedText.lines()

        val parsedRows = rawLines.map { parseCsvLine(it) }

        // 1. Mapeia o bloco lateral "TOP 10 PREFERIDOS" (colunas 11 = posição, 12 = título)
        val topFavoritesRankByTitle = extractTopFavorites(parsedRows)

        // 2. Mapeia o bloco lateral "Jogando atualmente" (ex: Darkwood na coluna 22)
        val currentlyPlayingTitles = extractCurrentlyPlayingTitles(parsedRows)

        val imported = mutableListOf<GameEntity>()
        val seenTitles = existingNormalizedTitles.toMutableSet()
        var completedCount = 0
        var playingCount = 0
        var skippedDuplicates = 0

        for (cols in parsedRows) {
            if (cols.size < 2) continue
            val rowNumber = cols.getOrNull(0)?.trim()?.toIntOrNull() ?: continue
            if (rowNumber <= 0) continue

            val rawTitle = cols.getOrNull(1)?.trim().orEmpty()
            if (rawTitle.isBlank()) continue

            val normalizedTitle = rawTitle.lowercase()
            if (!seenTitles.add(normalizedTitle)) {
                skippedDuplicates++
                continue
            }

            val releaseDayMonth = cols.getOrNull(2)?.trim()
            val releaseYear = cols.getOrNull(3)?.trim()
            val releaseDateMillis = TimeFormatUtils.parseDayMonthAndYearToMillis(releaseDayMonth, releaseYear)

            val genres = cols.getOrNull(4)?.trim()
                ?.let { GameMetadataService.normalizeGenresPtBr(it) }
                .orEmpty()

            val timePlayedSeconds = TimeFormatUtils.parseDurationToSeconds(cols.getOrNull(5))

            val completionDayMonth = cols.getOrNull(6)?.trim()
            val completionYear = cols.getOrNull(7)?.trim()
            val completionDateMillis = TimeFormatUtils.parseDayMonthAndYearToMillis(
                completionDayMonth,
                completionYear
            )

            val rating = cols.getOrNull(8)?.trim()
                ?.replace(",", ".")
                ?.toFloatOrNull()
                ?.coerceIn(0.0f, 11.0f)

            val platform = cols.getOrNull(9)?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "PC"

            val favoriteRank = findFavoriteRank(rawTitle, topFavoritesRankByTitle)
            val isHallOfFame = (rating != null && rating >= 11.0f)
            val memoryNote = favoriteRank?.let { "Top #$it Preferido Pessoal (Importado da Planilha)" }

            imported.add(
                GameEntity(
                    title = rawTitle,
                    releaseDate = releaseDateMillis,
                    genres = genres,
                    platform = platform,
                    status = GameStatus.COMPLETED,
                    priority = PriorityTier.TIER_B,
                    isMainFocus = false,
                    hltbMainSeconds = null,
                    hltbExtraSeconds = null,
                    hltbCompletionistSeconds = null,
                    userCustomEstimateSeconds = null,
                    targetCompletionStyle = CompletionStyle.MAIN_STORY,
                    timePlayedSeconds = timePlayedSeconds,
                    activeTimerStartTime = null,
                    startDate = null,
                    completionDate = completionDateMillis,
                    personalRating = rating,
                    isHallOfFame = isHallOfFame,
                    coreMemoryNote = memoryNote,
                    isPersonalGoty = false,
                    isOwned = true
                )
            )
            completedCount++
        }

        // Adiciona jogos da seção "Jogando atualmente" da planilha (ex: Darkwood)
        for ((index, playingTitle) in currentlyPlayingTitles.withIndex()) {
            val normalized = playingTitle.lowercase()
            if (!seenTitles.add(normalized)) {
                skippedDuplicates++
                continue
            }
            imported.add(
                GameEntity(
                    title = playingTitle,
                    status = GameStatus.PLAYING,
                    priority = PriorityTier.TIER_S,
                    isMainFocus = (index == 0),
                    platform = "PC",
                    isOwned = true
                )
            )
            playingCount++
        }

        return CsvImportSummary(
            importedGames = imported,
            completedCount = completedCount,
            playingCount = playingCount,
            skippedDuplicateCount = skippedDuplicates
        )
    }

    private fun extractTopFavorites(rows: List<List<String>>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        var inTop10Section = false
        for (cols in rows) {
            val col11 = cols.getOrNull(11)?.trim().orEmpty()
            val col12 = cols.getOrNull(12)?.trim().orEmpty()
            if (col11.contains("TOP 10 PREFERIDOS", ignoreCase = true)) {
                inTop10Section = true
                continue
            }
            if (inTop10Section) {
                val rank = col11.toIntOrNull()
                if (rank != null && col12.isNotBlank()) {
                    result[col12.lowercase()] = rank
                } else if (col11.isBlank() && col12.isBlank()) {
                    inTop10Section = false
                }
            }
        }
        return result
    }

    private fun findFavoriteRank(title: String, favoritesMap: Map<String, Int>): Int? {
        val normalized = title.trim().lowercase()
        favoritesMap[normalized]?.let { return it }
        return favoritesMap.entries.firstOrNull { (favKey, _) ->
            normalized.startsWith(favKey) || favKey.startsWith(normalized)
        }?.value
    }

    private fun extractCurrentlyPlayingTitles(rows: List<List<String>>): List<String> {
        val result = mutableListOf<String>()
        var playingColIndex = -1
        var readingPlayingRows = false

        for (cols in rows) {
            if (!readingPlayingRows) {
                val foundIdx = cols.indexOfFirst {
                    it.trim().equals("Jogando atualmente", ignoreCase = true)
                }
                if (foundIdx >= 0) {
                    playingColIndex = foundIdx
                    readingPlayingRows = true
                }
            } else if (playingColIndex >= 0) {
                val candidate = cols.getOrNull(playingColIndex)?.trim().orEmpty()
                if (candidate.isNotBlank()) {
                    result.add(candidate)
                } else {
                    break
                }
            }
        }
        return result
    }

    /**
     * Parser RFC-4180 para linhas CSV (suporta vírgulas dentro de aspas, ex: "No, i'm not a human").
     */
    fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    tokens.add(current.toString())
                    current.clear()
                }
                ch != '\r' -> {
                    current.append(ch)
                }
            }
            i++
        }
        tokens.add(current.toString())
        return tokens
    }
}
