package io.github.carvalhoxx.gamevault

import io.github.carvalhoxx.gamevault.data.importer.CsvGameImporter
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.DurationResolution
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CsvGameImporterTest {

    @Test
    fun `parse friend ref csv extracts all 112 completed games and currently playing game safely`() {
        val csvFile = sequenceOf(
            File("csv_ref/ref.csv"),
            File("app/csv_ref/ref.csv")
        ).firstOrNull { it.exists() }

        assertNotNull("Arquivo ref.csv deve existir para validação do importador", csvFile)

        val importer = CsvGameImporter()
        val summary = csvFile!!.inputStream().use { stream ->
            importer.parseCsvStream(stream)
        }

        assertEquals(112, summary.completedCount)
        assertEquals(1, summary.playingCount)
        assertEquals(113, summary.importedGames.size)

        val quotedGame = summary.importedGames.firstOrNull { it.title == "No, i'm not a human" }
        assertNotNull(quotedGame)
        assertEquals(4L * 3600L + 30L * 60L, quotedGame!!.timePlayedSeconds)

        val millisFormattedGame = summary.importedGames.firstOrNull { it.title == "Demon's Souls" }
        assertNotNull(millisFormattedGame)
        assertEquals(21L * 3600L + 5L * 60L + 27L, millisFormattedGame!!.timePlayedSeconds)

        val top1Game = summary.importedGames.firstOrNull { it.title == "Elden Ring" }
        assertNotNull(top1Game)
        assertTrue(top1Game!!.coreMemoryNote?.contains("Top #1") == true)

        val playingGame = summary.importedGames.firstOrNull { it.status == GameStatus.PLAYING }
        assertNotNull(playingGame)
        assertEquals("Darkwood", playingGame!!.title)
        assertTrue(playingGame.isMainFocus)
    }

    @Test
    fun `unreleased Day One game with null HLTB resolves to DurationTbdLock without NPE`() {
        val gtaVi = GameEntity(
            title = "Grand Theft Auto VI",
            status = GameStatus.DAY_ONE,
            hltbMainSeconds = null,
            hltbExtraSeconds = null,
            hltbCompletionistSeconds = null,
            userCustomEstimateSeconds = null
        )

        val resolution = gtaVi.resolveDuration()
        assertTrue(resolution is DurationResolution.DurationTbdLock)
    }
}
