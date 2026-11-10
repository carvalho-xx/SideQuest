package io.github.carvalhoxx.gamevault.data.remote

import io.github.carvalhoxx.gamevault.BuildConfig
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Serviço de metadados multiplataforma (Fase 3):
 * - Metadados, capas, plataformas e datas de lançamento (incluindo jogos futuros): IGDB API, RAWG API e HowLongToBeat.
 * - Tempos de conclusão (Campanha, História + Extras, 100%): HowLongToBeat (HLTB).
 * - Jogos ainda não lançados mantêm tempos HLTB estritamente nulos (DURATION_TBD_LOCK).
 */
class GameMetadataService(
    private val rawgApiKeyProvider: () -> String = { BuildConfig.RAWG_API_KEY },
    private val igdbClientIdProvider: () -> String = { BuildConfig.IGDB_CLIENT_ID },
    private val igdbClientSecretProvider: () -> String = { BuildConfig.IGDB_CLIENT_SECRET },
    private val httpGet: (url: String, headers: Map<String, String>) -> String? = ::defaultHttpGet,
    private val httpPost: (url: String, headers: Map<String, String>, body: String) -> String? = ::defaultHttpPost
) {

    @Volatile
    private var cachedIgdbAccessToken: String? = null

    /**
     * Busca correspondências enquanto o usuário digita (Autocomplete multiplataforma):
     * Executa IGDB, RAWG e HowLongToBeat em paralelo e combina com o Catálogo de Referência.
     */
    suspend fun searchGames(query: String): List<RemoteGameMetadata> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()

        val catalogMatches = HltbReferenceCatalog.search(cleanQuery)

        val (igdbMatches, rawgMatches, hltbMatches) = coroutineScope {
            val igdbDeferred = async { runCatching { searchIgdbGames(cleanQuery) }.getOrDefault(emptyList()) }
            val rawgDeferred = async { runCatching { searchRawgGames(cleanQuery) }.getOrDefault(emptyList()) }
            val hltbDeferred = async { runCatching { searchHowLongToBeat(cleanQuery) }.getOrDefault(emptyList()) }
            Triple(igdbDeferred.await(), rawgDeferred.await(), hltbDeferred.await())
        }

        val merged = mutableListOf<RemoteGameMetadata>()
        val seenKeys = mutableSetOf<String>()
        val seenTitles = mutableSetOf<String>()

        // 1. Resultados da IGDB e RAWG enriquecidos apenas com correspondência exata do HowLongToBeat
        for (meta in (igdbMatches + rawgMatches)) {
            val normTitle = normalizeTitle(meta.title)
            val key = dedupKey(meta)
            if (normTitle.isNotEmpty() && seenKeys.add(key)) {
                val hltb = findMatchingHltb(meta, hltbMatches)
                    ?: HltbReferenceCatalog.findExact(meta.title)
                merged.add(mergeMetadataAndHltb(meta, hltb))
                seenTitles.add(normTitle)
            }
        }

        // 2. Resultados diretos do HowLongToBeat (mantém versões de anos diferentes, ex.: 2005 vs 2023)
        for (hltbItem in hltbMatches) {
            val normTitle = normalizeTitle(hltbItem.title)
            val key = dedupKey(hltbItem)
            if (normTitle.isNotEmpty() && seenKeys.add(key)) {
                val catalogRef = HltbReferenceCatalog.findExact(hltbItem.title)?.takeIf {
                    isCompatibleReleaseYear(hltbItem.releaseDateMillis, it.releaseDateMillis)
                }
                merged.add(
                    if (catalogRef != null) mergeMetadataAndHltb(hltbItem, catalogRef) else hltbItem
                )
                seenTitles.add(normTitle)
            }
        }

        // 3. Catálogo de referência (apenas se o título ainda não veio da busca online)
        for (cat in catalogMatches) {
            val normTitle = normalizeTitle(cat.title)
            val key = dedupKey(cat)
            if (normTitle.isNotEmpty() && normTitle !in seenTitles && seenKeys.add(key)) {
                merged.add(cat)
            }
        }

        val normQuery = normalizeTitle(cleanQuery)
        merged.sortedWith(
            compareBy<RemoteGameMetadata> {
                val nt = normalizeTitle(it.title)
                when {
                    nt == normQuery -> 0
                    nt.startsWith(normQuery) -> 1
                    nt.contains(normQuery) -> 2
                    else -> 3
                }
            }
                // Entre resultados igualmente relevantes, prefira lançamentos novos/futuros.
                // Isso evita que um título legado da mesma franquia ocupe o primeiro lugar
                // quando o usuário busca por um próximo lançamento (ex.: "GTA 6").
                .thenBy { if (it.isUnreleased) 0 else 1 }
                .thenByDescending { it.releaseDateMillis ?: Long.MIN_VALUE }
                .thenBy { if ((it.hltbMainSeconds ?: 0L) > 0L) 0 else 1 }
        ).take(10)
    }

    /**
     * Enriquece diretamente um candidato já selecionado na lista de busca sem refazer toda a busca por texto,
     * preservando a versão/ano exato escolhido pelo usuário (ex.: Resident Evil 4 de 2005 vs 2023).
     */
    suspend fun enrichSelectedCandidate(candidate: RemoteGameMetadata): RemoteGameMetadata =
        withContext(Dispatchers.IO) {
            val hltbId = candidate.externalApiId?.takeIf { it.startsWith("hltb:") }?.removePrefix("hltb:")
            if (!hltbId.isNullOrBlank()) {
                return@withContext enrichFromHltbGamePage(candidate, hltbId)
            }
            fetchBestMatchForTitle(candidate.title, candidate.externalApiId) ?: candidate
        }

    /**
     * Busca a melhor correspondência completa (IGDB/RAWG + página de detalhes do HowLongToBeat)
     * para preencher ou atualizar um jogo selecionado ou já cadastrado.
     */
    suspend fun fetchBestMatchForTitle(
        title: String,
        existingExternalId: String? = null
    ): RemoteGameMetadata? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanTitleForOnlineSearch(title)
        if (cleanTitle.isEmpty()) return@withContext null

        val wantsRemake = title.contains("remake", ignoreCase = true) ||
            title.contains("remaster", ignoreCase = true)

        val results = searchGames(cleanTitle)
        val exactMatches = results.filter {
            (!existingExternalId.isNullOrBlank() && it.externalApiId == existingExternalId) ||
                normalizeTitle(it.title) == normalizeTitle(cleanTitle) ||
                normalizeTitle(it.title) == normalizeTitle(title)
        }
        val exact = if (wantsRemake && exactMatches.size > 1) {
            exactMatches.maxByOrNull { it.releaseDateMillis ?: 0L }
        } else {
            exactMatches.firstOrNull()
        } ?: results.firstOrNull()

        val hltbCandidates = results.filter {
            it.externalApiId?.startsWith("hltb:") == true &&
                normalizeTitle(it.title) == normalizeTitle(cleanTitle)
        }
        val hltbHit = (if (wantsRemake && hltbCandidates.size > 1) {
            hltbCandidates.maxByOrNull { it.releaseDateMillis ?: 0L }
        } else {
            hltbCandidates.firstOrNull()
        }) ?: results.firstOrNull { it.externalApiId?.startsWith("hltb:") == true }
            ?: runCatching { searchHowLongToBeat(cleanTitle).firstOrNull() }.getOrNull()
            ?: HltbReferenceCatalog.findExactOrFuzzy(cleanTitle)
            ?: HltbReferenceCatalog.findExactOrFuzzy(title)

        val combined = when {
            exact != null && hltbHit != null && exact.externalApiId != hltbHit.externalApiId &&
                normalizeTitle(exact.title) == normalizeTitle(hltbHit.title) ->
                mergeMetadataAndHltb(exact, hltbHit)
            exact != null -> exact
            else -> hltbHit
        } ?: return@withContext null

        val hltbIdToEnrich = existingExternalId?.takeIf { it.startsWith("hltb:") }
            ?: combined.externalApiId?.takeIf { it.startsWith("hltb:") }
            ?: hltbHit?.externalApiId?.takeIf { it.startsWith("hltb:") }

        if (hltbIdToEnrich != null) {
            return@withContext enrichFromHltbGamePage(
                combined.copy(externalApiId = combined.externalApiId ?: hltbIdToEnrich),
                hltbIdToEnrich.removePrefix("hltb:")
            )
        }

        combined
    }

    /**
     * Enriquecimento a partir da página do jogo no HowLongToBeat (`https://howlongtobeat.com/game/{id}`),
     * que fornece a data de lançamento exata (YYYY-MM-DD em `release_world`/`release_na`) e os gêneros (`profile_genre`).
     */
    internal fun enrichFromHltbGamePage(base: RemoteGameMetadata, hltbNumericId: String): RemoteGameMetadata {
        val cleanId = hltbNumericId.trim()
        if (cleanId.isEmpty() || !cleanId.all { it.isDigit() }) return base

        val html = httpGet(
            "https://howlongtobeat.com/game/$cleanId",
            mapOf(
                "User-Agent" to BROWSER_USER_AGENT,
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Referer" to "https://howlongtobeat.com/"
            )
        ) ?: return base

        return parseHltbGamePageHtml(base, html)
    }

    internal fun parseHltbGamePageHtml(base: RemoteGameMetadata, html: String): RemoteGameMetadata {
        val releaseIso = listOf("release_world", "release_na", "release_eu", "release_jp")
            .firstNotNullOfOrNull { key ->
                extractJsonString(html, key)?.takeIf {
                    it.isNotBlank() && !it.startsWith("0000")
                }
            }

        val exactReleaseMillis = parseIsoDateToMillis(releaseIso) ?: base.releaseDateMillis
        val now = System.currentTimeMillis()
        val hasRealHltbTimes = (base.hltbMainSeconds ?: 0L) > 0L ||
            (base.hltbExtraSeconds ?: 0L) > 0L ||
            (base.hltbCompletionistSeconds ?: 0L) > 0L
        val isUnreleased = when {
            hasRealHltbTimes -> false
            exactReleaseMillis != null && releaseIso != null && releaseIso.length >= 10 -> exactReleaseMillis > now
            else -> base.isUnreleased
        }

        val parsedGenres = extractJsonString(html, "profile_genre")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.let(::formatGenresListPtBr)
            .orEmpty()

        val parsedPlatforms = extractJsonString(html, "profile_platform")
            ?.takeIf { it.isNotBlank() }
            ?.let(::shortenPlatformsList)

        return base.copy(
            releaseDateMillis = exactReleaseMillis,
            genres = base.genres.ifBlank { parsedGenres },
            platform = if (base.platform.isBlank() || base.platform == "Multiplataforma") {
                parsedPlatforms ?: base.platform
            } else {
                base.platform
            },
            hltbMainSeconds = if (isUnreleased) null else base.hltbMainSeconds,
            hltbExtraSeconds = if (isUnreleased) null else base.hltbExtraSeconds,
            hltbCompletionistSeconds = if (isUnreleased) null else base.hltbCompletionistSeconds,
            isUnreleased = isUnreleased
        )
    }

    /**
     * Busca diretamente no HowLongToBeat (`/api/search/site/init` + `/api/search/site`)
     * extraindo tempos de Campanha (`comp_main`), Extras (`comp_plus`) e 100% (`comp_100`) em segundos.
     * Utiliza cache de token em memória para resposta rápida enquanto o usuário digita.
     */
    internal fun searchHowLongToBeat(query: String): List<RemoteGameMetadata> {
        val cleaned = cleanTitleForOnlineSearch(query)
        val terms = cleaned
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it != "+" && it != "-" }
        if (terms.isEmpty()) return emptyList()

        val primary = searchHowLongToBeatTerms(terms)
        if (primary.isNotEmpty()) return primary

        val lastLower = terms.last().lowercase(Locale.ROOT)
        if (terms.size >= 2 && (lastLower == "1" || lastLower == "remake" || lastLower == "remastered")) {
            return searchHowLongToBeatTerms(terms.dropLast(1))
        }
        return emptyList()
    }

    private fun searchHowLongToBeatTerms(terms: List<String>): List<RemoteGameMetadata> {
        val payload = buildHltbSearchPayload(terms)

        // 1. Tenta com o token em cache primeiro para evitar 2 requisições por tecla digitada
        val existingSession = getCachedHltbSession()
        if (existingSession != null) {
            val cachedResponse = executeHltbPost(existingSession, payload)
            if (cachedResponse != null) {
                val parsed = parseHowLongToBeatResponseJson(cachedResponse)
                if (parsed.isNotEmpty() || cachedResponse.contains("\"data\"")) {
                    return parsed
                }
            }
            clearCachedHltbSession()
        }

        // 2. Obtém um novo token de sessão no endpoint de inicialização do HLTB (sincronizado para chamadas paralelas)
        val freshSession = synchronized(sessionLock) {
            getCachedHltbSession() ?: fetchHltbSession()
        } ?: return emptyList()
        val responseJson = executeHltbPost(freshSession, payload) ?: return emptyList()
        return parseHowLongToBeatResponseJson(responseJson)
    }

    private fun executeHltbPost(session: HltbSession, payload: String): String? {
        val postHeaders = mutableMapOf(
            "User-Agent" to BROWSER_USER_AGENT,
            "Origin" to "https://howlongtobeat.com",
            "Referer" to "https://howlongtobeat.com/",
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "x-auth-token" to session.token
        )
        if (!session.hpKey.isNullOrBlank() && session.hpVal != null) {
            postHeaders["x-hp-key"] = session.hpKey
            postHeaders["x-hp-val"] = session.hpVal
        }
        return httpPost("https://howlongtobeat.com${session.searchPath}", postHeaders, payload)
    }

    private fun fetchHltbSession(): HltbSession? {
        val baseHeaders = mapOf(
            "User-Agent" to BROWSER_USER_AGENT,
            "Origin" to "https://howlongtobeat.com",
            "Referer" to "https://howlongtobeat.com/",
            "Accept" to "application/json"
        )
        val now = System.currentTimeMillis()
        val candidateSearchPaths = listOf(
            "/api/search/site",
            "/api/search",
            "/api/find"
        )

        for (searchPath in candidateSearchPaths) {
            val initJson = httpGet("https://howlongtobeat.com$searchPath/init?t=$now", baseHeaders) ?: continue
            val token = extractJsonString(initJson, "token")
            if (!token.isNullOrBlank()) {
                val hpKey = extractJsonString(initJson, "hpKey")
                val hpVal = extractJsonString(initJson, "hpVal")
                val session = HltbSession(
                    searchPath = searchPath,
                    token = token,
                    hpKey = hpKey,
                    hpVal = hpVal,
                    timestampMs = now
                )
                cacheHltbSession(session)
                return session
            }
        }
        return null
    }

    internal fun buildHltbSearchPayload(
        terms: List<String>,
        hpKey: String? = null,
        hpVal: String? = null
    ): String {
        val termsJson = terms.joinToString(",") { "\"${escapeJson(it)}\"" }
        val hpField = if (!hpKey.isNullOrBlank() && hpVal != null) {
            "\"${escapeJson(hpKey)}\":\"${escapeJson(hpVal)}\","
        } else {
            ""
        }
        return """
            {
              $hpField
              "searchType": "games",
              "searchTerms": [$termsJson],
              "searchPage": 1,
              "size": 10,
              "searchOptions": {
                "games": {
                  "userId": 0,
                  "platform": "",
                  "sortCategory": "popular",
                  "rangeCategory": "main",
                  "rangeTime": {"min": null, "max": null},
                  "gameplay": {
                    "perspective": {"mode": "include", "values": []},
                    "flow": {"mode": "include", "values": []},
                    "genre": {"mode": "include", "values": []}
                  },
                  "year": "",
                  "modifier": ""
                },
                "users": {"sortCategory": "postcount"},
                "lists": {"sortCategory": "follows"},
                "filter": "",
                "sort": 0,
                "randomizer": 0
              },
              "useCache": true
            }
        """.trimIndent()
    }

    internal fun parseHowLongToBeatResponseJson(json: String): List<RemoteGameMetadata> {
        val dataBlock = extractJsonArrayBlock(json, "data") ?: return emptyList()
        val objects = splitTopLevelJsonObjects(dataBlock)
        val currentYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR)
        val results = mutableListOf<RemoteGameMetadata>()

        for (obj in objects) {
            val gameId = extractJsonLong(obj, "game_id") ?: continue
            val gameName = extractJsonString(obj, "game_name")?.takeIf { it.isNotBlank() } ?: continue
            val gameImage = extractJsonString(obj, "game_image")?.takeIf { it.isNotBlank() }
            val platforms = extractJsonString(obj, "profile_platform")
                ?.takeIf { it.isNotBlank() }
                ?.let(::shortenPlatformsList)
                ?: "Multiplataforma"

            val releaseYear = extractJsonLong(obj, "release_world")?.toInt()
            val releaseMillis = if (releaseYear != null && releaseYear in 1970..2100) {
                TimeFormatUtils.parseDayMonthAndYearToMillis("01/01", releaseYear.toString())
            } else {
                null
            }

            val compMain = extractJsonLong(obj, "comp_main")?.takeIf { it > 0L }
            val compPlus = extractJsonLong(obj, "comp_plus")?.takeIf { it > 0L }
            val comp100 = extractJsonLong(obj, "comp_100")?.takeIf { it > 0L }

            val isUnreleased = (releaseYear != null && releaseYear > currentYear) ||
                (compMain == null && compPlus == null && comp100 == null && releaseYear != null && releaseYear >= currentYear)

            val coverUrl = gameImage?.let { img ->
                if (img.startsWith("http")) img else "https://howlongtobeat.com/games/$img"
            }

            results.add(
                RemoteGameMetadata(
                    externalApiId = "hltb:$gameId",
                    title = gameName,
                    coverUrl = coverUrl,
                    backdropUrl = coverUrl,
                    releaseDateMillis = releaseMillis,
                    genres = "",
                    platform = platforms,
                    hltbMainSeconds = if (isUnreleased) null else compMain,
                    hltbExtraSeconds = if (isUnreleased) null else compPlus,
                    hltbCompletionistSeconds = if (isUnreleased) null else comp100,
                    isUnreleased = isUnreleased
                )
            )
        }
        return results
    }

    /**
     * Busca jogos de todas as plataformas (incluindo lançamentos futuros) na API RAWG.
     */
    internal fun searchRawgGames(query: String): List<RemoteGameMetadata> {
        val apiKey = rawgApiKeyProvider().trim()
        if (apiKey.isEmpty()) return emptyList()

        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.rawg.io/api/games?search=$encoded&page_size=8&ordering=-popularity&key=$apiKey"
        val json = httpGet(url, mapOf("Accept" to "application/json", "User-Agent" to "GameVault/1.0"))
            ?: return emptyList()

        return parseRawgSearchJson(json)
    }

    internal fun parseRawgSearchJson(json: String): List<RemoteGameMetadata> {
        val resultsBlock = extractJsonArrayBlock(json, "results") ?: return emptyList()
        val objects = splitTopLevelJsonObjects(resultsBlock)
        val now = System.currentTimeMillis()
        val list = mutableListOf<RemoteGameMetadata>()

        for (obj in objects) {
            val topLevel = stripNestedStructures(obj)
            val id = extractJsonLong(topLevel, "id") ?: continue
            val name = extractJsonString(topLevel, "name")?.takeIf { it.isNotBlank() } ?: continue
            val releasedStr = extractJsonString(topLevel, "released")
            val tba = extractJsonBoolean(topLevel, "tba") ?: false
            val backgroundImage = extractJsonString(topLevel, "background_image")

            val releaseMillis = parseIsoDateToMillis(releasedStr)
            val isUnreleased = tba || (releaseMillis != null && releaseMillis > now)

            val genres = extractNestedNames(obj, "genres")
                .map(::translateGenreToPtBr)
                .distinct()
                .joinToString(", ")

            val platforms = extractRawgPlatformNames(obj)
                .let { if (it.isEmpty()) "Multiplataforma" else shortenPlatformsList(it.joinToString(", ")) }

            list.add(
                RemoteGameMetadata(
                    externalApiId = "rawg:$id",
                    title = name,
                    coverUrl = backgroundImage,
                    backdropUrl = backgroundImage,
                    releaseDateMillis = releaseMillis,
                    genres = genres,
                    platform = platforms,
                    hltbMainSeconds = null,
                    hltbExtraSeconds = null,
                    hltbCompletionistSeconds = null,
                    isUnreleased = isUnreleased
                )
            )
        }
        return list
    }

    /**
     * Busca jogos de todas as plataformas (incluindo lançamentos futuros) na API IGDB (Twitch).
     */
    internal fun searchIgdbGames(query: String): List<RemoteGameMetadata> {
        val clientId = igdbClientIdProvider().trim()
        val clientSecret = igdbClientSecretProvider().trim()
        if (clientId.isEmpty() || clientSecret.isEmpty()) return emptyList()

        val token = getOrFetchIgdbToken(clientId, clientSecret) ?: return emptyList()
        val sanitizedQuery = escapeJson(query.trim())
        val body = """
            search "$sanitizedQuery";
            fields name, first_release_date, cover.image_id, genres.name, platforms.abbreviation, platforms.name, popularity;
            sort popularity desc;
            limit 8;
        """.trimIndent()

        val response = httpPost(
            "https://api.igdb.com/v4/games",
            mapOf(
                "Client-ID" to clientId,
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            ),
            body
        ) ?: return emptyList()

        return parseIgdbGamesJson(response)
    }

    internal fun parseIgdbGamesJson(json: String): List<RemoteGameMetadata> {
        val trimmed = json.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val inner = trimmed.substring(1, trimmed.length - 1)
        val objects = splitTopLevelJsonObjects(inner)
        val now = System.currentTimeMillis()
        val results = mutableListOf<RemoteGameMetadata>()

        for (obj in objects) {
            val topLevel = stripNestedStructures(obj)
            val id = extractJsonLong(topLevel, "id") ?: continue
            val name = extractJsonString(topLevel, "name")?.takeIf { it.isNotBlank() } ?: continue
            val firstReleaseEpochSec = extractJsonLong(topLevel, "first_release_date")
            val releaseMillis = firstReleaseEpochSec?.times(1000L)
            val isUnreleased = releaseMillis != null && releaseMillis > now

            val coverImageId = extractNestedObjectString(obj, "cover", "image_id")
            val coverUrl = coverImageId?.let { "https://images.igdb.com/igdb/image/upload/t_cover_big/$it.jpg" }

            val genres = extractNestedNames(obj, "genres")
                .let(::formatGenresListPtBr)

            val platforms = extractNestedNames(obj, "platforms")
                .let { if (it.isEmpty()) "Multiplataforma" else shortenPlatformsList(it.joinToString(", ")) }

            results.add(
                RemoteGameMetadata(
                    externalApiId = "igdb:$id",
                    title = name,
                    coverUrl = coverUrl,
                    backdropUrl = coverUrl,
                    releaseDateMillis = releaseMillis,
                    genres = genres,
                    platform = platforms,
                    hltbMainSeconds = null,
                    hltbExtraSeconds = null,
                    hltbCompletionistSeconds = null,
                    isUnreleased = isUnreleased
                )
            )
        }
        return results
    }

    private fun getOrFetchIgdbToken(clientId: String, clientSecret: String): String? {
        cachedIgdbAccessToken?.let { return it }
        val url = "https://id.twitch.tv/oauth2/token?client_id=$clientId&client_secret=$clientSecret&grant_type=client_credentials"
        val response = httpPost(url, mapOf("Accept" to "application/json"), "") ?: return null
        val token = extractJsonString(response, "access_token")
        if (!token.isNullOrBlank()) {
            cachedIgdbAccessToken = token
        }
        return token
    }

    private fun mergeMetadataAndHltb(
        primary: RemoteGameMetadata,
        hltb: RemoteGameMetadata?
    ): RemoteGameMetadata {
        if (hltb == null) return primary
        val mergedMain = hltb.hltbMainSeconds ?: primary.hltbMainSeconds
        val mergedExtra = hltb.hltbExtraSeconds ?: primary.hltbExtraSeconds
        val mergedComp = hltb.hltbCompletionistSeconds ?: primary.hltbCompletionistSeconds
        val hasRealHltbTimes = (mergedMain != null && mergedMain > 0L) ||
            (mergedExtra != null && mergedExtra > 0L) ||
            (mergedComp != null && mergedComp > 0L)
        val unreleased = if (hasRealHltbTimes) {
            false
        } else {
            primary.isUnreleased && hltb.isUnreleased
        }
        return primary.copy(
            coverUrl = primary.coverUrl ?: hltb.coverUrl,
            backdropUrl = primary.backdropUrl ?: hltb.backdropUrl ?: primary.coverUrl,
            releaseDateMillis = primary.releaseDateMillis ?: hltb.releaseDateMillis,
            genres = primary.genres.ifBlank { hltb.genres },
            platform = if (primary.platform.isBlank() || primary.platform == "Multiplataforma") {
                hltb.platform.ifBlank { primary.platform }
            } else {
                primary.platform
            },
            hltbMainSeconds = if (unreleased) null else mergedMain,
            hltbExtraSeconds = if (unreleased) null else mergedExtra,
            hltbCompletionistSeconds = if (unreleased) null else mergedComp,
            isUnreleased = unreleased
        )
    }

    internal fun parseIsoDateToMillis(isoDate: String?): Long? {
        if (isoDate.isNullOrBlank() || isoDate.startsWith("0000")) return null
        val parts = isoDate.trim().split("-")
        if (parts.size < 3) return null
        val yearInt = parts[0].toIntOrNull() ?: return null
        if (yearInt !in 1970..2100) return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].take(2).toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return TimeFormatUtils.parseDayMonthAndYearToMillis("$day/$month", yearInt.toString())
    }

    internal data class HltbSession(
        val searchPath: String,
        val token: String,
        val hpKey: String? = null,
        val hpVal: String? = null,
        val timestampMs: Long = System.currentTimeMillis()
    )

    companion object {
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val HLTB_SESSION_TTL_MS = 10 * 60 * 1_000L

        @Volatile
        private var cachedHltbSession: HltbSession? = null
        private val sessionLock = Any()

        internal fun cleanTitleForOnlineSearch(raw: String): String {
            return raw
                .replace(Regex("\\[[^]]*]"), " ")
                .replace(Regex("\\([^)]*(?:dlc|remaster|edition|goty|19\\d\\d|20\\d\\d)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("\\+\\s*dlcs?\\b", RegexOption.IGNORE_CASE), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .ifEmpty { raw.trim() }
        }

        internal fun getCachedHltbSession(): HltbSession? {
            val session = cachedHltbSession ?: return null
            if (System.currentTimeMillis() - session.timestampMs > HLTB_SESSION_TTL_MS) {
                cachedHltbSession = null
                return null
            }
            return session
        }

        internal fun cacheHltbSession(session: HltbSession) {
            cachedHltbSession = session
        }

        internal fun clearCachedHltbSession() {
            cachedHltbSession = null
        }

        internal fun normalizeTitle(raw: String): String {
            return raw.lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9\\u00C0-\\u017F ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun extractReleaseYear(millis: Long?): Int? {
            if (millis == null || millis <= 0L) return null
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
            return cal.get(Calendar.YEAR)
        }

        private fun isCompatibleReleaseYear(millisA: Long?, millisB: Long?): Boolean {
            val yearA = extractReleaseYear(millisA) ?: return true
            val yearB = extractReleaseYear(millisB) ?: return true
            return kotlin.math.abs(yearA - yearB) <= 1
        }

        private fun dedupKey(meta: RemoteGameMetadata): String {
            val norm = normalizeTitle(meta.title)
            val year = extractReleaseYear(meta.releaseDateMillis)
            return if (year != null) "${norm}_$year" else norm
        }

        private fun findMatchingHltb(
            meta: RemoteGameMetadata,
            hltbMatches: List<RemoteGameMetadata>
        ): RemoteGameMetadata? {
            val norm = normalizeTitle(meta.title)
            val sameTitle = hltbMatches.filter { normalizeTitle(it.title) == norm }
            if (sameTitle.isEmpty()) return null
            return sameTitle.firstOrNull {
                isCompatibleReleaseYear(meta.releaseDateMillis, it.releaseDateMillis)
            } ?: sameTitle.first()
        }

        internal fun normalizeGenresPtBr(rawGenres: String): String {
            if (rawGenres.isBlank()) return ""
            val tokens = rawGenres.split(Regex("[,;/|]"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return formatGenresListPtBr(tokens)
        }

        internal fun formatGenresListPtBr(rawGenres: List<String>): String {
            val expanded = rawGenres
                .flatMap { it.split(Regex("[,;/|]")) }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val cameraPerspectives = setOf(
                "first-person", "third-person", "top-down", "isometric", "side-scrolling", "scrolling",
                "primeira pessoa", "terceira pessoa", "visao superior", "visão superior", "isometrico", "isométrico"
            )
            val nonPerspective = expanded.filterNot { it.lowercase(Locale.ROOT) in cameraPerspectives }
            val chosen = (if (nonPerspective.isNotEmpty()) nonPerspective else expanded)
                .map(::translateGenreToPtBr)
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase(Locale.ROOT) }
                .take(3)
            return chosen.joinToString(", ")
        }

        internal fun translateGenreToPtBr(genre: String): String {
            val trimmed = unescapeJson(genre).trim()
            if (trimmed.isEmpty()) return ""

            val lower = trimmed.lowercase(Locale.ROOT)
                .replace(Regex("\\s+"), " ")
            val asciiKey = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .replace(Regex("[\\uFFFD?]+"), "")
                .trim()

            return when (asciiKey) {
                "action", "acao", "ao", "aco" -> "Ação"
                "adventure", "action-adventure", "action adventure", "aventura" -> "Aventura"
                "rpg", "role-playing", "role-playing (rpg)", "role-playing-games-rpg", "action rpg", "jrpg", "arpg" -> "RPG"
                "strategy", "turn-based strategy (tbs)", "real-time strategy (rts)", "estrategia", "estratgia" -> "Estratégia"
                "turn-based", "turn-based combat", "combate por turnos", "turnos" -> "Combate por Turnos"
                "open world", "open-world", "mundo aberto" -> "Mundo Aberto"
                "shooter", "first-person shooter", "third-person shooter", "fps", "tps", "tiro", "run and gun" -> "Tiro"
                "puzzle", "puzzles", "quebra-cabeca", "quebra cabeca", "quebra-cabea", "quebra cabea" -> "Quebra-Cabeça"
                "racing", "driving", "corrida" -> "Corrida"
                "sports", "sport", "esportes", "esporte" -> "Esportes"
                "fighting", "luta", "artes marciais", "martial arts" -> "Luta"
                "platformer", "platform", "plataforma" -> "Plataforma"
                "simulation", "simulator", "simulacao", "simulao" -> "Simulação"
                "indie" -> "Indie"
                "arcade" -> "Arcade"
                "hack and slash", "hack & slash", "hack 'n' slash", "hack and slash/beat 'em up" -> "Hack and Slash"
                "beat 'em up", "beat em up", "brawler" -> "Beat 'em up"
                "tactical", "tatico", "ttico" -> "Tático"
                "survival horror" -> "Survival Horror"
                "psychological horror", "terror psicologico", "terror psicolgico" -> "Terror Psicológico"
                "horror", "terror" -> "Terror"
                "survival", "sobrevivencia", "sobrevivncia" -> "Sobrevivência"
                "stealth", "furtividade" -> "Furtividade"
                "sandbox" -> "Sandbox"
                "roguelike", "roguelite", "rogue like", "rogue-like", "rogue-lite" -> "Roguelike"
                "soulslike", "souls like", "souls-like" -> "Soulslike"
                "metroidvania" -> "Metroidvania"
                "visual novel" -> "Visual Novel"
                "point-and-click", "point and click" -> "Point-and-Click"
                "coop", "co-op", "cooperativo", "co-op/multiplayer" -> "Cooperativo"
                "mystery", "misterio", "mistrio", "suspense" -> "Mistério"
                "superhero", "super-heroi", "super heroi", "super-heri" -> "Super-Herói"
                "sci-fi", "science fiction", "ficcao cientifica", "fico cientfica" -> "Ficção Científica"
                "choices", "escolha", "escolhas", "interactive drama" -> "Escolhas"
                "card game", "cards", "cartas", "deckbuilder" -> "Cartas"
                "music", "rhythm", "ritmo", "musica", "msica" -> "Ritmo"
                "culinaria", "culinria", "cooking" -> "Culinária"
                "first-person", "primeira pessoa" -> "Primeira Pessoa"
                "third-person", "terceira pessoa" -> "Terceira Pessoa"
                "top-down", "visao superior" -> "Visão Superior"
                "isometric", "isometrico", "isomtrico" -> "Isométrico"
                else -> formatStandardTitleCase(trimmed.replace("\uFFFD", ""))
            }
        }

        private fun formatStandardTitleCase(raw: String): String {
            val clean = raw.trim().replace(Regex("\\s+"), " ")
            if (clean.isEmpty()) return ""
            val lowercaseConnectors = setOf("de", "da", "do", "das", "dos", "e", "por", "em", "and", "of", "the", "'em")
            return clean.split(" ").mapIndexed { index, word ->
                val lowerWord = word.lowercase(Locale.ROOT)
                if (index > 0 && lowerWord in lowercaseConnectors) {
                    lowerWord
                } else {
                    lowerWord.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                    }
                }
            }.joinToString(" ")
        }

        internal fun shortenPlatformsList(raw: String): String {
            val mapped = raw.split(",")
                .map { p ->
                    val clean = p.trim()
                    when {
                        clean.contains("PlayStation 5", ignoreCase = true) -> "PS5"
                        clean.contains("PlayStation 4", ignoreCase = true) -> "PS4"
                        clean.contains("Xbox Series", ignoreCase = true) -> "Xbox Series X/S"
                        clean.contains("Xbox One", ignoreCase = true) -> "Xbox One"
                        clean.contains("Nintendo Switch 2", ignoreCase = true) -> "Switch 2"
                        clean.contains("Nintendo Switch", ignoreCase = true) -> "Switch"
                        clean.contains("Microsoft Windows", ignoreCase = true) || clean.equals("PC", ignoreCase = true) -> "PC"
                        else -> clean
                    }
                }
                .distinct()
                .take(3)
            return mapped.joinToString(", ").ifBlank { "Multiplataforma" }
        }

        private fun defaultHttpGet(urlStr: String, headers: Map<String, String>): String? {
            return runCatching {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    instanceFollowRedirects = true
                    headers.forEach { (k, v) -> setRequestProperty(k, v) }
                }
                try {
                    if (conn.responseCode in 200..299) {
                        conn.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        null
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }

        private fun defaultHttpPost(urlStr: String, headers: Map<String, String>, body: String): String? {
            return runCatching {
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 6_000
                    readTimeout = 6_000
                    doOutput = true
                    instanceFollowRedirects = true
                    headers.forEach { (k, v) -> setRequestProperty(k, v) }
                }
                try {
                    conn.outputStream.use { os ->
                        os.write(body.toByteArray(Charsets.UTF_8))
                    }
                    if (conn.responseCode in 200..299) {
                        conn.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        null
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }

        private fun escapeJson(s: String): String {
            return s.replace("\\", "\\\\").replace("\"", "\\\"")
        }

        internal fun stripNestedStructures(jsonObject: String): String {
            val sb = StringBuilder(jsonObject.length)
            var braceDepth = 0
            var bracketDepth = 0
            var inString = false
            var escaped = false
            for (c in jsonObject) {
                if (escaped) {
                    if (braceDepth <= 1 && bracketDepth == 0) sb.append(c)
                    escaped = false
                    continue
                }
                if (c == '\\') {
                    if (braceDepth <= 1 && bracketDepth == 0) sb.append(c)
                    escaped = true
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    if (braceDepth <= 1 && bracketDepth == 0) sb.append(c)
                    continue
                }
                if (!inString) {
                    when (c) {
                        '{' -> {
                            braceDepth++
                            if (braceDepth == 1) {
                                sb.append(c)
                            } else if (braceDepth == 2 && bracketDepth == 0) {
                                sb.append("null")
                            }
                        }
                        '}' -> {
                            if (braceDepth == 1) sb.append(c)
                            braceDepth--
                        }
                        '[' -> {
                            if (braceDepth == 1 && bracketDepth == 0) sb.append("[]")
                            bracketDepth++
                        }
                        ']' -> {
                            bracketDepth--
                        }
                        else -> {
                            if (braceDepth <= 1 && bracketDepth == 0) sb.append(c)
                        }
                    }
                } else {
                    if (braceDepth <= 1 && bracketDepth == 0) sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun extractJsonString(json: String, key: String): String? {
            val regex = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            val match = regex.find(json) ?: return null
            return unescapeJson(match.groupValues[1])
        }

        private fun extractJsonLong(json: String, key: String): Long? {
            val regex = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(\\d+)")
            return regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
        }

        private fun extractJsonBoolean(json: String, key: String): Boolean? {
            val regex = Regex("\"${Regex.escape(key)}\"\\s*:\\s*(true|false)")
            return regex.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
        }

        private fun extractNestedObjectString(json: String, objectKey: String, fieldKey: String): String? {
            val idx = json.indexOf("\"$objectKey\"")
            if (idx < 0) return null
            val openBrace = json.indexOf('{', idx)
            val closeBrace = json.indexOf('}', openBrace)
            if (openBrace < 0 || closeBrace < 0) return null
            return extractJsonString(json.substring(openBrace, closeBrace + 1), fieldKey)
        }

        private fun extractNestedNames(json: String, arrayKey: String): List<String> {
            val block = extractJsonArrayBlock(json, arrayKey) ?: return emptyList()
            val objs = splitTopLevelJsonObjects(block)
            return objs.mapNotNull { extractJsonString(it, "name") }
        }

        private fun extractRawgPlatformNames(json: String): List<String> {
            val block = extractJsonArrayBlock(json, "platforms") ?: return emptyList()
            val objs = splitTopLevelJsonObjects(block)
            return objs.mapNotNull { extractNestedObjectString(it, "platform", "name") }
        }

        private fun extractJsonArrayBlock(json: String, key: String): String? {
            val keyIdx = json.indexOf("\"$key\"")
            if (keyIdx < 0) return null
            val openBracket = json.indexOf('[', keyIdx)
            if (openBracket < 0) return null
            var depth = 0
            var inString = false
            var escaped = false
            for (i in openBracket until json.length) {
                val c = json[i]
                if (escaped) {
                    escaped = false
                    continue
                }
                if (c == '\\') {
                    escaped = true
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    continue
                }
                if (!inString) {
                    if (c == '[') depth++
                    else if (c == ']') {
                        depth--
                        if (depth == 0) {
                            return json.substring(openBracket + 1, i)
                        }
                    }
                }
            }
            return null
        }

        private fun splitTopLevelJsonObjects(arrayContent: String): List<String> {
            val result = mutableListOf<String>()
            var depth = 0
            var start = -1
            var inString = false
            var escaped = false
            for (i in arrayContent.indices) {
                val c = arrayContent[i]
                if (escaped) {
                    escaped = false
                    continue
                }
                if (c == '\\') {
                    escaped = true
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    continue
                }
                if (!inString) {
                    if (c == '{') {
                        if (depth == 0) start = i
                        depth++
                    } else if (c == '}') {
                        depth--
                        if (depth == 0 && start >= 0) {
                            result.add(arrayContent.substring(start, i + 1))
                            start = -1
                        }
                    }
                }
            }
            return result
        }

        internal fun unescapeJson(raw: String): String {
            val unicodeUnescaped = Regex("\\\\u([0-9a-fA-F]{4})").replace(raw) { match ->
                match.groupValues[1].toInt(16).toChar().toString()
            }
            return unicodeUnescaped
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\n", " ")
                .replace("\\t", " ")
                .replace("\\\\", "\\")
        }
    }
}
