package io.github.carvalhoxx.gamevault.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeFormatUtils {

    /**
     * Formata segundos totais no padrão preciso HH:MM:SS (ex: 00:45:12 ou 125:30:00).
     */
    fun formatSecondsToHhMmSs(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0L)
        val hours = safeSeconds / 3600L
        val minutes = (safeSeconds % 3600L) / 60L
        val seconds = safeSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Formata uma referência de tempo nullable (HLTB) de forma segura.
     * Retorna "TBD" quando nulo (ex: jogos Day One ainda não lançados).
     */
    fun formatHltbReferenceOrTbd(seconds: Long?): String {
        if (seconds == null || seconds <= 0L) return "A definir"
        val hours = seconds / 3600L
        val minutes = (seconds % 3600L) / 60L
        return if (minutes > 0L) {
            "~${hours}h ${minutes}m"
        } else {
            "~${hours}h"
        }
    }

    /**
     * Converte strings de tempo como "11:30:00", "21:05:27.000", "0:10:30" ou "95:30"
     * em segundos totais (Long), tratando milissegundos e valores inválidos com segurança.
     */
    fun parseDurationToSeconds(rawTime: String?): Long {
        if (rawTime.isNullOrBlank()) return 0L
        val cleaned = rawTime.trim().substringBefore(".")
        val parts = cleaned.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            3 -> (parts[0].coerceAtLeast(0L) * 3600L) +
                (parts[1].coerceIn(0L, 59L) * 60L) +
                parts[2].coerceIn(0L, 59L)
            2 -> (parts[0].coerceAtLeast(0L) * 3600L) +
                (parts[1].coerceIn(0L, 59L) * 60L)
            1 -> parts[0].coerceAtLeast(0L) * 3600L
            else -> 0L
        }
    }

    /**
     * Converte dia/mês ("15/10" ou "4/2") e ano ("2013") em timestamp UTC seguro (Long?).
     * Nunca quebra com valores vazios ou malformados, retornando null (TBD).
     */
    fun parseDayMonthAndYearToMillis(dayMonth: String?, yearStr: String?): Long? {
        if (yearStr.isNullOrBlank()) return null
        val year = yearStr.trim().toIntOrNull() ?: return null
        if (year < 1970 || year > 2100) return null

        // Um ano sem mês/dia é uma previsão, não deve virar 01/01.
        if (dayMonth.isNullOrBlank()) return null
        val dmParts = dayMonth.trim().split("/")
        if (dmParts.size < 2) return null
        val day = dmParts[0].trim().toIntOrNull()?.takeIf { it in 1..31 } ?: return null
        val month = dmParts[1].trim().toIntOrNull()?.takeIf { it in 1..12 } ?: return null

        return runCatching {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 12)
            }
            calendar.timeInMillis
        }.getOrNull()
    }

    /**
     * Formata timestamp nullable em data legível ("dd/MM/yyyy") ou "TBD" se nulo.
     */
    fun formatDateOrTbd(timestampMillis: Long?): String {
        if (timestampMillis == null || timestampMillis <= 0L) return "A definir"
        return runCatching {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            formatter.format(Date(timestampMillis))
        }.getOrDefault("A definir")
    }

    fun formatDateOrYearOrTbd(timestampMillis: Long?, year: Int?): String {
        if (timestampMillis != null && timestampMillis > 0L) return formatDateOrTbd(timestampMillis)
        return year?.takeIf { it in 1970..2100 }?.toString() ?: "A definir"
    }

    /**
     * Retorna os limites [inícioDoAnoMillis, fimDoAnoMillis] em UTC para filtrar
     * jogos zerados no ano vigente com segurança contra desvios de fuso horário.
     */
    fun getUtcYearBoundsMillis(year: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }

    /**
     * Verifica se o timestamp (completionDate) pertence ao ano especificado (tanto em UTC quanto no fuso local).
     */
    fun isTimestampInYear(timestampMillis: Long?, targetYear: Int): Boolean {
        if (timestampMillis == null || timestampMillis <= 0L) return false
        val utcYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = timestampMillis
        }.get(Calendar.YEAR)
        if (utcYear == targetYear) return true

        val localYear = Calendar.getInstance().apply {
            timeInMillis = timestampMillis
        }.get(Calendar.YEAR)
        return localYear == targetYear
    }

    /**
     * Formata a contagem regressiva de lançamentos:
     * - Quando faltar >= 24h: exibe a data + dias restantes (ex: "19 NOV • 12d").
     * - Quando faltar < 24h (e > 0s): exibe o contador regressivo em tempo real HH:MM:SS (ex: "⏱ 14:22:09").
     * - Quando já tiver atingido o horário: exibe "DISPONÍVEL AGORA".
     */
    fun formatReleaseCountdownBadge(releaseMillis: Long?, nowMillis: Long): String {
        if (releaseMillis == null || releaseMillis <= 0L) return "DATA A DEFINIR"
        val diffMillis = releaseMillis - nowMillis
        if (diffMillis <= 0L) return "DISPONÍVEL AGORA"

        val oneDayMillis = 24L * 60L * 60L * 1000L
        return if (diffMillis < oneDayMillis) {
            val totalSeconds = (diffMillis / 1000L).coerceAtLeast(0L)
            "⏱ ${formatSecondsToHhMmSs(totalSeconds)}"
        } else {
            val dateStr = SimpleDateFormat("dd MMM", Locale.forLanguageTag("pt-BR")).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(releaseMillis)).uppercase().replace(".", "")
            val days = diffMillis / oneDayMillis
            "$dateStr • ${days}d"
        }
    }
}
