package io.github.carvalhoxx.gamevault.data.remote

import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils

/**
 * Catálogo multiplataforma de referência (HowLongToBeat + IGDB/RAWG) incluindo lançamentos futuros
 * (Day One com HLTB estritamente null) e jogos de todas as plataformas (PS5, Switch, Xbox, PC).
 */
object HltbReferenceCatalog {

    private fun igdbCover(imageId: String) =
        if (imageId.startsWith("http")) imageId else "https://images.igdb.com/igdb/image/upload/t_cover_big/$imageId.jpg"

    private fun entry(
        id: String,
        title: String,
        igdbCoverId: String? = null,
        dayMonth: String? = null,
        year: String? = null,
        genres: String,
        platform: String,
        mainHours: Long? = null,
        extraHours: Long? = null,
        completionistHours: Long? = null,
        isUnreleased: Boolean = false
    ): RemoteGameMetadata {
        return RemoteGameMetadata(
            externalApiId = "igdb:$id",
            title = title,
            coverUrl = igdbCoverId?.let(::igdbCover),
            backdropUrl = igdbCoverId?.let(::igdbCover),
            releaseDateMillis = TimeFormatUtils.parseDayMonthAndYearToMillis(dayMonth, year),
            genres = GameMetadataService.normalizeGenresPtBr(genres),
            platform = platform,
            hltbMainSeconds = if (isUnreleased) null else mainHours?.times(3600L),
            hltbExtraSeconds = if (isUnreleased) null else extraHours?.times(3600L),
            hltbCompletionistSeconds = if (isUnreleased) null else completionistHours?.times(3600L),
            isUnreleased = isUnreleased
        )
    }

    val entries: List<RemoteGameMetadata> = listOf(
        // Lançamentos Futuros / Day One (Multiplataforma — HLTB null -> DurationTbdLock)
        entry(
            id = "gta_vi",
            title = "Grand Theft Auto VI",
            igdbCoverId = "https://howlongtobeat.com/games/104609_Grand_Theft_Auto_VI.jpg",
            dayMonth = "19/11",
            year = "2026",
            genres = "Ação, Aventura, Mundo Aberto",
            platform = "PS5, Xbox Series X/S",
            isUnreleased = true
        ),
        entry(
            id = "ghost_of_yotei",
            title = "Ghost of Yōtei",
            igdbCoverId = "https://howlongtobeat.com/games/157301_Ghost_of_Ytei.png",
            dayMonth = "02/10",
            year = "2025",
            genres = "Ação, Aventura, Mundo Aberto",
            platform = "PS5",
            mainHours = 30L,
            extraHours = 47L,
            completionistHours = 61L
        ),
        entry(
            id = "death_stranding_2",
            title = "Death Stranding 2: On the Beach",
            igdbCoverId = "https://howlongtobeat.com/games/118225_Death_Stranding_2_On_The_Beach.png",
            dayMonth = "26/06",
            year = "2025",
            genres = "Ação, Aventura, Ficção Científica",
            platform = "PS5, PC",
            mainHours = 36L,
            extraHours = 64L,
            completionistHours = 118L
        ),
        entry(
            id = "phantom_blade_zero",
            title = "Phantom Blade Zero",
            igdbCoverId = "https://howlongtobeat.com/games/129235_Phantom_Blade_Zero.jpg",
            dayMonth = "15/09",
            year = "2026",
            genres = "RPG, Ação, Hack and Slash",
            platform = "PS5, PC",
            mainHours = 19L,
            extraHours = 40L,
            completionistHours = 44L
        ),
        entry(
            id = "metroid_prime_4",
            title = "Metroid Prime 4: Beyond",
            igdbCoverId = "https://howlongtobeat.com/games/46437_Metroid_Prime_4_Beyond.jpg",
            dayMonth = "04/12",
            year = "2025",
            genres = "Ação, Aventura, Ficção Científica",
            platform = "Switch",
            mainHours = 13L,
            extraHours = 16L,
            completionistHours = 18L
        ),
        entry(
            id = "hollow_knight_silksong",
            title = "Hollow Knight: Silksong",
            igdbCoverId = "https://howlongtobeat.com/games/65945_Hollow_Knight_Silksong.jpg",
            dayMonth = "04/09",
            year = "2025",
            genres = "Metroidvania, Ação, Plataforma",
            platform = "PC, PS5, Switch, Xbox",
            mainHours = 28L,
            extraHours = 47L,
            completionistHours = 66L
        ),
        entry(
            id = "marvels_wolverine",
            title = "Marvel's Wolverine",
            igdbCoverId = "https://howlongtobeat.com/games/97761_Marvels_Wolverine.png",
            dayMonth = "15/10",
            year = "2026",
            genres = "Ação, Aventura",
            platform = "PS5",
            mainHours = 15L,
            extraHours = 17L,
            completionistHours = 20L
        ),
        entry(
            id = "crimson_desert",
            title = "Crimson Desert",
            igdbCoverId = "https://howlongtobeat.com/games/94122_Crimson_Desert.jpg",
            dayMonth = "19/03",
            year = "2026",
            genres = "RPG, Ação, Mundo Aberto",
            platform = "PC, PS5, Xbox Series X/S",
            mainHours = 71L,
            extraHours = 141L,
            completionistHours = 257L
        ),
        entry(
            id = "the_witcher_4",
            title = "The Witcher IV",
            igdbCoverId = "https://howlongtobeat.com/games/127655_The_Witcher_IV.jpg",
            dayMonth = null,
            year = "2027",
            genres = "RPG, Ação, Mundo Aberto",
            platform = "PC, PS5, Xbox Series X/S",
            isUnreleased = true
        ),

        // Jogos Lançados (Todas as plataformas — Tempos oficiais HowLongToBeat)
        entry(
            id = "elden_ring",
            title = "Elden Ring",
            igdbCoverId = "co4jni",
            dayMonth = "25/02",
            year = "2022",
            genres = "RPG de Ação, Soulslike, Mundo Aberto",
            platform = "PC, PS5, Xbox Series X/S",
            mainHours = 60L,
            extraHours = 101L,
            completionistHours = 135L
        ),
        entry(
            id = "darkwood",
            title = "Darkwood",
            igdbCoverId = "co1r7x",
            dayMonth = "17/08",
            year = "2017",
            genres = "Terror de Sobrevivência, Exploração",
            platform = "PC, PS5, Switch, Xbox",
            mainHours = 18L,
            extraHours = 26L,
            completionistHours = 42L
        ),
        entry(
            id = "baldurs_gate_3",
            title = "Baldur's Gate 3",
            igdbCoverId = "co670h",
            dayMonth = "03/08",
            year = "2023",
            genres = "RPG, Estratégia em Turnos, Aventura",
            platform = "PC, PS5, Xbox Series X/S",
            mainHours = 70L,
            extraHours = 112L,
            completionistHours = 166L
        ),
        entry(
            id = "cyberpunk_2077",
            title = "Cyberpunk 2077",
            igdbCoverId = "co7497",
            dayMonth = "10/12",
            year = "2020",
            genres = "RPG de Ação, Ficção Científica, Mundo Aberto",
            platform = "PC, PS5, Xbox Series X/S",
            mainHours = 25L,
            extraHours = 63L,
            completionistHours = 106L
        ),
        entry(
            id = "red_dead_redemption_2",
            title = "Red Dead Redemption 2",
            igdbCoverId = "co1q1f",
            dayMonth = "26/10",
            year = "2018",
            genres = "Ação, Aventura, Mundo Aberto",
            platform = "PS4, Xbox One, PC",
            mainHours = 50L,
            extraHours = 84L,
            completionistHours = 187L
        ),
        entry(
            id = "the_witcher_3",
            title = "The Witcher 3: Wild Hunt",
            igdbCoverId = "co1wyy",
            dayMonth = "18/05",
            year = "2015",
            genres = "RPG de Ação, Fantasia, Mundo Aberto",
            platform = "PC, PS5, Switch, Xbox",
            mainHours = 51L,
            extraHours = 103L,
            completionistHours = 173L
        ),
        entry(
            id = "bloodborne",
            title = "Bloodborne",
            igdbCoverId = "co1rba",
            dayMonth = "24/03",
            year = "2015",
            genres = "RPG de Ação, Soulslike, Terror Gótico",
            platform = "PS4",
            mainHours = 33L,
            extraHours = 44L,
            completionistHours = 76L
        ),
        entry(
            id = "demons_souls",
            title = "Demon's Souls",
            igdbCoverId = "co2gpu",
            dayMonth = "12/11",
            year = "2020",
            genres = "RPG de Ação, Soulslike",
            platform = "PS5",
            mainHours = 24L,
            extraHours = 32L,
            completionistHours = 57L
        ),
        entry(
            id = "sekiro",
            title = "Sekiro: Shadows Die Twice",
            igdbCoverId = "co1w9v",
            dayMonth = "22/03",
            year = "2019",
            genres = "Ação, Aventura, Soulslike",
            platform = "PC, PS4, Xbox One",
            mainHours = 30L,
            extraHours = 42L,
            completionistHours = 70L
        ),
        entry(
            id = "dark_souls_3",
            title = "Dark Souls III",
            igdbCoverId = "co1vcf",
            dayMonth = "11/04",
            year = "2016",
            genres = "RPG de Ação, Soulslike",
            platform = "PC, PS4, Xbox One",
            mainHours = 32L,
            extraHours = 48L,
            completionistHours = 96L
        ),
        entry(
            id = "dark_souls_remastered",
            title = "Dark Souls: Remastered",
            igdbCoverId = "co1x78",
            dayMonth = "24/05",
            year = "2018",
            genres = "RPG de Ação, Soulslike",
            platform = "PC, PS4, Switch, Xbox",
            mainHours = 29L,
            extraHours = 43L,
            completionistHours = 68L
        ),
        entry(
            id = "lies_of_p",
            title = "Lies of P",
            igdbCoverId = "co6l1u",
            dayMonth = "18/09",
            year = "2023",
            genres = "RPG de Ação, Soulslike",
            platform = "PC, PS5, Xbox Series X/S",
            mainHours = 29L,
            extraHours = 37L,
            completionistHours = 58L
        ),
        entry(
            id = "black_myth_wukong",
            title = "Black Myth: Wukong",
            igdbCoverId = "co8h3y",
            dayMonth = "20/08",
            year = "2024",
            genres = "RPG de Ação, Mitologia",
            platform = "PC, PS5",
            mainHours = 32L,
            extraHours = 47L,
            completionistHours = 68L
        ),
        entry(
            id = "god_of_war_2018",
            title = "God of War",
            igdbCoverId = "co1tmu",
            dayMonth = "20/04",
            year = "2018",
            genres = "Ação, Aventura, Mitologia",
            platform = "PS4, PC",
            mainHours = 21L,
            extraHours = 33L,
            completionistHours = 51L
        ),
        entry(
            id = "god_of_war_ragnarok",
            title = "God of War Ragnarök",
            igdbCoverId = "co5s5v",
            dayMonth = "09/11",
            year = "2022",
            genres = "Ação, Aventura, Mitologia",
            platform = "PS5, PS4, PC",
            mainHours = 26L,
            extraHours = 41L,
            completionistHours = 54L
        ),
        entry(
            id = "ghost_of_tsushima",
            title = "Ghost of Tsushima",
            igdbCoverId = "co2crj",
            dayMonth = "17/07",
            year = "2020",
            genres = "Ação, Aventura, Mundo Aberto",
            platform = "PS5, PS4, PC",
            mainHours = 25L,
            extraHours = 46L,
            completionistHours = 62L
        ),
        entry(
            id = "clair_obscur",
            title = "Clair Obscur: Expedition 33",
            igdbCoverId = "co895p",
            dayMonth = "24/04",
            year = "2025",
            genres = "RPG em Turnos, Fantasia",
            platform = "PC, PS5, Xbox Series X/S",
            mainHours = 30L,
            extraHours = 45L,
            completionistHours = 65L
        ),
        entry(
            id = "persona_5_royal",
            title = "Persona 5 Royal",
            igdbCoverId = "co1nic",
            dayMonth = "31/10",
            year = "2019",
            genres = "JRPG, Simulação Social",
            platform = "PS5, Switch, PC, Xbox",
            mainHours = 101L,
            extraHours = 123L,
            completionistHours = 144L
        ),
        entry(
            id = "metaphor_refantazio",
            title = "Metaphor: ReFantazio",
            igdbCoverId = "co7s7b",
            dayMonth = "11/10",
            year = "2024",
            genres = "JRPG, Fantasia",
            platform = "PC, PS5, Xbox Series X/S",
            mainHours = 65L,
            extraHours = 86L,
            completionistHours = 108L
        ),
        entry(
            id = "hollow_knight",
            title = "Hollow Knight",
            igdbCoverId = "co1rgi",
            dayMonth = "24/02",
            year = "2017",
            genres = "Metroidvania, Ação, Plataforma",
            platform = "PC, Switch, PS4, Xbox",
            mainHours = 27L,
            extraHours = 42L,
            completionistHours = 64L
        ),
        entry(
            id = "resident_evil_4_remake",
            title = "Resident Evil 4",
            igdbCoverId = "co5w3k",
            dayMonth = "24/03",
            year = "2023",
            genres = "Terror de Sobrevivência, Ação",
            platform = "PC, PS5, Xbox Series X/S",
            mainHours = 16L,
            extraHours = 22L,
            completionistHours = 57L
        ),
        entry(
            id = "silent_hill_2_remake",
            title = "Silent Hill 2",
            igdbCoverId = "co87vg",
            dayMonth = "08/10",
            year = "2024",
            genres = "Terror Psicológico, Sobrevivência",
            platform = "PS5, PC",
            mainHours = 16L,
            extraHours = 20L,
            completionistHours = 32L
        ),
        entry(
            id = "the_last_of_us_part_1",
            title = "The Last of Us Part I",
            igdbCoverId = "co5xex",
            dayMonth = "02/09",
            year = "2022",
            genres = "Ação, Aventura, Narrativa",
            platform = "PS5, PC",
            mainHours = 15L,
            extraHours = 18L,
            completionistHours = 25L
        ),
        entry(
            id = "the_last_of_us_part_2",
            title = "The Last of Us Part II",
            igdbCoverId = "co5ziw",
            dayMonth = "19/06",
            year = "2020",
            genres = "Ação, Aventura, Narrativa",
            platform = "PS5, PS4, PC",
            mainHours = 24L,
            extraHours = 30L,
            completionistHours = 43L
        ),
        entry(
            id = "disco_elysium",
            title = "Disco Elysium",
            igdbCoverId = "co1sfj",
            dayMonth = "15/10",
            year = "2019",
            genres = "RPG, Investigação, Narrativa",
            platform = "PC, PS5, Switch, Xbox",
            mainHours = 24L,
            extraHours = 32L,
            completionistHours = 47L
        ),
        entry(
            id = "outer_wilds",
            title = "Outer Wilds",
            igdbCoverId = "co65ac",
            dayMonth = "28/05",
            year = "2019",
            genres = "Exploração, Mistério, Aventura",
            platform = "PC, PS5, Switch, Xbox",
            mainHours = 17L,
            extraHours = 23L,
            completionistHours = 32L
        ),
        entry(
            id = "hades",
            title = "Hades",
            igdbCoverId = "co39vc",
            dayMonth = "17/09",
            year = "2020",
            genres = "Roguelike, Ação",
            platform = "PC, Switch, PS5, Xbox",
            mainHours = 23L,
            extraHours = 49L,
            completionistHours = 95L
        ),
        entry(
            id = "celeste",
            title = "Celeste",
            igdbCoverId = "co3byy",
            dayMonth = "25/01",
            year = "2018",
            genres = "Plataforma, Indie",
            platform = "PC, Switch, PS4, Xbox",
            mainHours = 8L,
            extraHours = 14L,
            completionistHours = 39L
        ),
        entry(
            id = "astro_bot",
            title = "Astro Bot",
            igdbCoverId = "co8cef",
            dayMonth = "06/09",
            year = "2024",
            genres = "Plataforma, Aventura",
            platform = "PS5",
            mainHours = 11L,
            extraHours = 15L,
            completionistHours = 19L
        ),
        entry(
            id = "zelda_botw",
            title = "The Legend of Zelda: Breath of the Wild",
            igdbCoverId = "co3p2d",
            dayMonth = "03/03",
            year = "2017",
            genres = "Aventura, Mundo Aberto, Ação",
            platform = "Switch",
            mainHours = 50L,
            extraHours = 98L,
            completionistHours = 189L
        ),
        entry(
            id = "zelda_totk",
            title = "The Legend of Zelda: Tears of the Kingdom",
            igdbCoverId = "co5vmg",
            dayMonth = "12/05",
            year = "2023",
            genres = "Aventura, Mundo Aberto, Ação",
            platform = "Switch",
            mainHours = 59L,
            extraHours = 112L,
            completionistHours = 242L
        )
    )

    fun search(query: String): List<RemoteGameMetadata> {
        val normQuery = GameMetadataService.normalizeTitle(query)
        if (normQuery.isEmpty()) return emptyList()
        return entries.filter { item ->
            val normTitle = GameMetadataService.normalizeTitle(item.title)
            normTitle.contains(normQuery)
        }
    }

    fun findExact(title: String): RemoteGameMetadata? {
        val norm = GameMetadataService.normalizeTitle(title)
        if (norm.isEmpty()) return null
        return entries.firstOrNull { GameMetadataService.normalizeTitle(it.title) == norm }
    }

    fun findExactOrFuzzy(title: String): RemoteGameMetadata? {
        findExact(title)?.let { return it }
        val cleaned = GameMetadataService.cleanTitleForOnlineSearch(title)
        if (cleaned != title) {
            findExact(cleaned)?.let { return it }
        }
        return null
    }
}
