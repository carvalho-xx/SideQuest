package io.github.carvalhoxx.gamevault

import io.github.carvalhoxx.gamevault.data.remote.GameMetadataService
import io.github.carvalhoxx.gamevault.data.remote.HltbReferenceCatalog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameMetadataServiceTest {

    @Test
    fun `parse HowLongToBeat response extracts comp_main comp_plus comp_100 and multi-platform info`() {
        val sampleHltbJson = """
            {
              "color": "blue",
              "title": "",
              "category": "games",
              "count": 1,
              "pageCurrent": 1,
              "pageTotal": 1,
              "pageSize": 20,
              "data": [
                {
                  "game_id": 68151,
                  "game_name": "Elden Ring",
                  "game_image": "68151_Elden_Ring.jpg",
                  "profile_platform": "PC, PlayStation 4, PlayStation 5, Xbox One, Xbox Series X/S",
                  "release_world": 2022,
                  "comp_main": 216000,
                  "comp_plus": 363600,
                  "comp_100": 486000
                }
              ]
            }
        """.trimIndent()

        val service = GameMetadataService(
            httpGet = { _, _ -> null },
            httpPost = { _, _, _ -> null }
        )
        val parsed = service.parseHowLongToBeatResponseJson(sampleHltbJson)

        assertEquals(1, parsed.size)
        val elden = parsed.first()
        assertEquals("Elden Ring", elden.title)
        assertEquals(60L * 3600L, elden.hltbMainSeconds)
        assertEquals(101L * 3600L, elden.hltbExtraSeconds)
        assertEquals(135L * 3600L, elden.hltbCompletionistSeconds)
        assertTrue(elden.platform.contains("PS5"))
        assertEquals("https://howlongtobeat.com/games/68151_Elden_Ring.jpg", elden.coverUrl)
        assertFalse(elden.isUnreleased)
    }

    @Test
    fun `parse IGDB and RAWG multi-platform responses and keep unreleased Day One HLTB null`() = runBlocking {
        val sampleIgdbJson = """
            [
              {
                "id": 52189,
                "cover": {"id": 351337, "image_id": "co7j3d"},
                "first_release_date": 1826582400,
                "genres": [{"id": 31, "name": "Adventure"}, {"id": 5, "name": "Shooter"}],
                "name": "Grand Theft Auto VI",
                "platforms": [{"id": 167, "name": "PlayStation 5"}, {"id": 169, "name": "Xbox Series X|S"}]
              }
            ]
        """.trimIndent()

        val sampleRawgJson = """
            {
              "results": [
                {
                  "id": 58175,
                  "name": "God of War Ragnarök",
                  "released": "2022-11-09",
                  "tba": false,
                  "background_image": "https://media.rawg.io/media/games/ragnarok.jpg",
                  "genres": [{"name": "Action"}, {"name": "Adventure"}],
                  "platforms": [
                    {"platform": {"name": "PlayStation 5"}},
                    {"platform": {"name": "PlayStation 4"}}
                  ]
                }
              ]
            }
        """.trimIndent()

        val service = GameMetadataService(
            httpGet = { _, _ -> null },
            httpPost = { _, _, _ -> null }
        )

        val igdbList = service.parseIgdbGamesJson(sampleIgdbJson)
        assertEquals(1, igdbList.size)
        val gta = igdbList.first()
        assertEquals("Grand Theft Auto VI", gta.title)
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/co7j3d.jpg", gta.coverUrl)
        assertEquals("Aventura, Tiro", gta.genres)
        assertTrue(gta.platform.contains("PS5"))
        assertTrue(gta.isUnreleased)
        assertNull(gta.hltbMainSeconds)

        val rawgList = service.parseRawgSearchJson(sampleRawgJson)
        assertEquals(1, rawgList.size)
        val gow = rawgList.first()
        assertEquals("God of War Ragnarök", gow.title)
        assertEquals("Ação, Aventura", gow.genres)
        assertTrue(gow.platform.contains("PS5"))
        assertNotNull(gow.releaseDateMillis)

        val gtaCatalog = HltbReferenceCatalog.findExactOrFuzzy("Grand Theft Auto VI")
        assertNotNull(gtaCatalog)
        assertTrue(gtaCatalog!!.isUnreleased)
        assertNull(gtaCatalog.hltbMainSeconds)
    }

    @Test
    fun `searchHowLongToBeat uses api search site init and enriches exact release date and genres from game page`() = runBlocking {
        GameMetadataService.clearCachedHltbSession()

        val fakeInitJson = """{"token":"test-hltb-token-123"}"""
        val fakeSearchJson = """
            {
              "color": "blue",
              "data": [
                {
                  "game_id": 2224,
                  "game_name": "Dark Souls II",
                  "game_image": "2224_Dark_Souls_II.jpg",
                  "profile_platform": "PC, PlayStation 3,PlayStation 4, Xbox 360, Xbox One",
                  "release_world": 2014,
                  "comp_main": 165600,
                  "comp_plus": 223200,
                  "comp_100": 313200
                }
              ]
            }
        """.trimIndent()
        val fakeGamePageHtml = """
            <html><script id="__NEXT_DATA__" type="application/json">
            {"props":{"pageProps":{"game":{"data":{"game":[{"game_id":2224,"profile_genre":"Third-Person, Action, Open World, Role-Playing","release_world":"2014-03-11","release_na":"2014-03-11"}]}}}}}
            </script></html>
        """.trimIndent()

        var initCalls = 0
        var postCalls = 0
        val service = GameMetadataService(
            rawgApiKeyProvider = { "" },
            igdbClientIdProvider = { "" },
            igdbClientSecretProvider = { "" },
            httpGet = { url, _ ->
                when {
                    url.contains("/api/search/site/init") -> {
                        initCalls++
                        fakeInitJson
                    }
                    url.contains("/game/2224") -> fakeGamePageHtml
                    else -> null
                }
            },
            httpPost = { url, headers, body ->
                if (url.endsWith("/api/search/site") &&
                    headers["x-auth-token"] == "test-hltb-token-123" &&
                    body.contains("\"mode\": \"include\"")
                ) {
                    postCalls++
                    fakeSearchJson
                } else {
                    null
                }
            }
        )

        val searchResults = service.searchGames("Dark Souls II")
        assertTrue(searchResults.isNotEmpty())
        assertEquals(1, initCalls)
        assertEquals(1, postCalls)

        // Second search reuses cached token without calling /init again
        val bestMatch = service.fetchBestMatchForTitle("Dark Souls II", "hltb:2224")
        assertNotNull(bestMatch)
        assertEquals(1, initCalls)
        assertEquals("Ação, Mundo Aberto, RPG", bestMatch!!.genres)
        assertNotNull(bestMatch.releaseDateMillis)
        assertEquals(46L * 3600L, bestMatch.hltbMainSeconds)
        GameMetadataService.clearCachedHltbSession()
    }

    @Test
    fun `normalizeGenresPtBr repairs corrupted encoding and standardizes casing and synonyms`() {
        assertEquals(
            "Ação, Furtividade",
            GameMetadataService.normalizeGenresPtBr("A\uFFFD\uFFFDo/Stealth")
        )
        assertEquals(
            "Sobrevivência",
            GameMetadataService.normalizeGenresPtBr("sobrevivência")
        )
        assertEquals(
            "Sobrevivência",
            GameMetadataService.normalizeGenresPtBr("Sobrevivencia")
        )
        assertEquals(
            "Terror, Quebra-Cabeça",
            GameMetadataService.normalizeGenresPtBr("Terror/puzzle")
        )
        assertEquals(
            "Ação, Aventura, Beat 'em up",
            GameMetadataService.normalizeGenresPtBr("Third-Person, Action, Adventure, Beat em Up")
        )
        assertEquals(
            "Beat 'em up",
            GameMetadataService.normalizeGenresPtBr("Beat \\u0027em Up")
        )
    }
}
