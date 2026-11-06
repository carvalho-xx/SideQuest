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

        var day = 1
        var month = 1
        if (!dayMonth.isNullOrBlank()) {
            val dmParts = dayMonth.trim().split("/")
            if (dmParts.size >= 2) {
                day = dmParts[0].trim().toIntOrNull()?.coerceIn(1, 31) ?: 1
                month = dmParts[1].trim().toIntOrNull()?.coerceIn(1, 12) ?: 1
            }
        }

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
}
