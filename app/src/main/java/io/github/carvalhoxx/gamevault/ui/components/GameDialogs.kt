package io.github.carvalhoxx.gamevault.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.carvalhoxx.gamevault.data.local.GameVaultDatabase
import io.github.carvalhoxx.gamevault.data.local.UserPlatformStore
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.data.remote.GameMetadataService
import io.github.carvalhoxx.gamevault.data.remote.HltbReferenceCatalog
import io.github.carvalhoxx.gamevault.data.remote.RemoteGameMetadata
import io.github.carvalhoxx.gamevault.domain.model.CompletionStyle
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.PriorityTier
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import io.github.carvalhoxx.gamevault.ui.theme.DangerCrimson
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateCard
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateElevated
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateSurface
import io.github.carvalhoxx.gamevault.ui.theme.HallOfFameGold
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurpleGlow
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurplePrimary
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurpleSecondary
import io.github.carvalhoxx.gamevault.ui.theme.TextMuted
import io.github.carvalhoxx.gamevault.ui.theme.TextPrimary
import io.github.carvalhoxx.gamevault.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Capa oficial do jogo carregada via URL com cache em disco/memória e fallback estilizado Null-safe.
 */
@Composable
fun GameCoverArtwork(
    title: String,
    coverUrl: String? = null,
    isHallOfFame: Boolean = false,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val hash = abs(title.hashCode())
    val palette = listOf(
        listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B)),
        listOf(Color(0xFF1F1C2C), Color(0xFF6A3093), Color(0xFFA044FF)),
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)),
        listOf(Color(0xFF23074D), Color(0xFFCC5333), Color(0xFFF5C451)),
        listOf(Color(0xFF141E30), Color(0xFF243B55), Color(0xFFB85BFF))
    )
    val colors = palette[hash % palette.size]
    val initials = title.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "GV" }

    val borderColor = if (isHallOfFame) HallOfFameGold else NeonPurplePrimary.copy(alpha = 0.45f)
    val remoteBitmap = rememberRemoteImageBitmap(coverUrl)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(colors))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (remoteBitmap != null) {
            Image(
                bitmap = remoteBitmap,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(6.dp)
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Seletor numérico exato de Horas : Minutos : Segundos (Seção 5.1 ARCHITECTURE.md).
 */
@Composable
fun ManualTimePickerDialog(
    gameTitle: String,
    initialSeconds: Long,
    onDismiss: () -> Unit,
    onConfirm: (hours: Long, minutes: Long, seconds: Long) -> Unit
) {
    val initH = (initialSeconds / 3600L).coerceAtLeast(0L)
    val initM = ((initialSeconds % 3600L) / 60L).coerceIn(0L, 59L)
    val initS = (initialSeconds % 60L).coerceIn(0L, 59L)

    var hoursText by remember { mutableStateOf(initH.toString()) }
    var minutesText by remember { mutableStateOf(String.format(Locale.US, "%02d", initM)) }
    var secondsText by remember { mutableStateOf(String.format(Locale.US, "%02d", initS)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSlateSurface,
        titleContentColor = TextPrimary,
        title = {
            Column {
                Text(
                    text = "Tempo Manual Exato (HH:MM:SS)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = gameTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonPurpleGlow
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Digite o tempo exato que consta no arquivo de salvamento do seu Console ou PC:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Horas", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", style = MaterialTheme.typography.titleLarge, color = NeonPurplePrimary)
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Min", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", style = MaterialTheme.typography.titleLarge, color = NeonPurplePrimary)
                    OutlinedTextField(
                        value = secondsText,
                        onValueChange = { secondsText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Seg", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursText.toLongOrNull() ?: 0L
                    val m = (minutesText.toLongOrNull() ?: 0L).coerceIn(0L, 59L)
                    val s = (secondsText.toLongOrNull() ?: 0L).coerceIn(0L, 59L)
                    onConfirm(h, m, s)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
            ) {
                Text("Salvar Tempo", color = Color(0xFF140526), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

/**
 * Modal de conclusão de jogo: solicita Tempo para Zerar (Horas, Minutos, Segundos) e Data em que Zerou (Dia/Mês e Ano).
 */
@Composable
fun CompleteGameDialog(
    gameTitle: String,
    initialSeconds: Long,
    initialCompletionDateMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (totalSeconds: Long, completionDateMillis: Long) -> Unit
) {
    val initH = (initialSeconds / 3600L).coerceAtLeast(0L)
    val initM = ((initialSeconds % 3600L) / 60L).coerceIn(0L, 59L)
    val initS = (initialSeconds % 60L).coerceIn(0L, 59L)

    var hoursText by remember { mutableStateOf(if (initialSeconds > 0L) initH.toString() else "") }
    var minutesText by remember { mutableStateOf(if (initialSeconds > 0L) String.format(Locale.US, "%02d", initM) else "") }
    var secondsText by remember { mutableStateOf(if (initialSeconds > 0L) String.format(Locale.US, "%02d", initS) else "") }

    val defaultMillis = remember(initialCompletionDateMillis) {
        initialCompletionDateMillis?.takeIf { it > 0L } ?: System.currentTimeMillis()
    }
    var dayMonthText by remember { mutableStateOf(formatDayMonth(defaultMillis)) }
    var yearText by remember { mutableStateOf(formatYear(defaultMillis)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSlateSurface,
        titleContentColor = TextPrimary,
        title = {
            Column {
                Text(
                    text = "🏆 Marcar como Zerado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = gameTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonPurpleGlow
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Tempo para Zerar:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = { hoursText = it.filter(Char::isDigit).take(4) },
                            label = { Text("Horas", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minutesText,
                            onValueChange = { minutesText = it.filter(Char::isDigit).take(2) },
                            label = { Text("Min", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = secondsText,
                            onValueChange = { secondsText = it.filter(Char::isDigit).take(2) },
                            label = { Text("Seg", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Data em que Zerou:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dayMonthText,
                            onValueChange = { dayMonthText = it.take(5) },
                            label = { Text("Dia/Mês", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("06/11", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = yearText,
                            onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                            label = { Text("Ano", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("2026", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                    val m = (minutesText.toLongOrNull() ?: 0L).coerceIn(0L, 59L)
                    val s = (secondsText.toLongOrNull() ?: 0L).coerceIn(0L, 59L)
                    val totalSec = (h * 3600L) + (m * 60L) + s
                    val completionMillis = TimeFormatUtils.parseDayMonthAndYearToMillis(dayMonthText, yearText)
                        ?: defaultMillis
                    onConfirm(totalSec, completionMillis)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
            ) {
                Text("Salvar Conclusão", color = Color(0xFF140526), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

/**
 * Modal da Roleta da Fila (sorteia apenas jogos na fila).
 */
@Composable
fun BacklogRouletteDialog(
    winner: GameEntity,
    onDismiss: () -> Unit,
    onSpinAgain: () -> Unit,
    onStartPlaying: (GameEntity) -> Unit,
    onOpenDetails: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSlateSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎲", fontSize = 24.sp)
                Column {
                    Text(
                        text = "Roleta da Fila",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "O destino escolheu sua próxima jornada!",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurpleGlow
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DeepSlateCard)
                    .border(1.dp, NeonPurplePrimary, RoundedCornerShape(18.dp))
                    .clickable { onOpenDetails(winner.id) }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GameCoverArtwork(
                    title = winner.title,
                    coverUrl = winner.coverUrl,
                    isHallOfFame = winner.isHallOfFame,
                    modifier = Modifier.size(width = 128.dp, height = 176.dp)
                )
                Text(
                    text = winner.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${winner.platform} • Prioridade ${winner.priority.badge} (${winner.priority.label})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                val refDuration = TimeFormatUtils.formatHltbReferenceOrTbd(winner.getEffectiveDurationSeconds())
                Text(
                    text = "Duração estimada: $refDuration",
                    style = MaterialTheme.typography.labelMedium,
                    color = HallOfFameGold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartPlaying(winner) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
            ) {
                Text("▶ Jogar Agora", color = Color(0xFF140526), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onSpinAgain) {
                    Text("🎲 Sortear Outro", color = NeonPurpleGlow)
                }
                TextButton(onClick = onDismiss) {
                    Text("Fechar", color = TextMuted)
                }
            }
        }
    )
}

/**
 * Modal de Criação/Edição de Jogo com busca online de metadados (Fase 3: Capas, Datas e Tempos HLTB).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameFormDialog(
    initialGame: GameEntity? = null,
    onDismiss: () -> Unit,
    onSave: (GameEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val metadataService = remember { GameMetadataService() }
    val gameDao = remember(context) { GameVaultDatabase.getInstance(context).gameDao() }

    var userPlatforms by remember { mutableStateOf(UserPlatformStore.getPlatforms(context)) }
    var isAddingPlatform by remember { mutableStateOf(userPlatforms.isEmpty()) }
    var isManagingPlatforms by remember { mutableStateOf(false) }
    var platformPendingDeletion by remember { mutableStateOf<String?>(null) }
    var newPlatformInput by remember { mutableStateOf("") }

    var title by remember { mutableStateOf(initialGame?.title.orEmpty()) }
    var externalApiId by remember { mutableStateOf(initialGame?.externalApiId) }
    var coverUrl by remember { mutableStateOf(initialGame?.coverUrl) }
    var backdropUrl by remember { mutableStateOf(initialGame?.backdropUrl) }
    var platform by remember {
        mutableStateOf(
            initialGame?.platform
                ?.takeIf { !it.equals("Sem plataforma", ignoreCase = true) && !it.equals("Não definida", ignoreCase = true) }
                ?: userPlatforms.firstOrNull().orEmpty()
        )
    }
    var genres by remember { mutableStateOf(initialGame?.genres.orEmpty()) }

    LaunchedEffect(Unit) {
        val synced = UserPlatformStore.syncWithDatabasePlatforms(context, gameDao)
        userPlatforms = synced
        if (platform.isBlank() && synced.isNotEmpty()) {
            platform = synced.first()
        }
        isAddingPlatform = synced.isEmpty()
    }
    var status by remember { mutableStateOf(initialGame?.status ?: GameStatus.BACKLOG) }
    var priority by remember { mutableStateOf(initialGame?.priority ?: PriorityTier.TIER_B) }
    val completionStyle = initialGame?.targetCompletionStyle ?: CompletionStyle.MAIN_STORY
    var isMainFocus by remember { mutableStateOf(initialGame?.isMainFocus ?: false) }

    val initHltbMainHours = initialGame?.hltbMainSeconds?.let { (it / 3600L).toString() }.orEmpty()
    val initHltbExtraHours = initialGame?.hltbExtraSeconds?.let { (it / 3600L).toString() }.orEmpty()
    val initHltb100Hours = initialGame?.hltbCompletionistSeconds?.let { (it / 3600L).toString() }.orEmpty()

    val initPlayedSec = initialGame?.timePlayedSeconds ?: 0L
    val initCompletedHours = if (initPlayedSec > 0L) (initPlayedSec / 3600L).toString() else ""
    val initCompletedMinutes = if (initPlayedSec > 0L) ((initPlayedSec % 3600L) / 60L).toString() else ""
    val initCompletedSeconds = if (initPlayedSec > 0L) (initPlayedSec % 60L).toString() else ""

    var hltbMainHoursText by remember { mutableStateOf(initHltbMainHours) }
    var hltbExtraHoursText by remember { mutableStateOf(initHltbExtraHours) }
    var hltb100HoursText by remember { mutableStateOf(initHltb100Hours) }
    var completedHoursText by remember { mutableStateOf(initCompletedHours) }
    var completedMinutesText by remember { mutableStateOf(initCompletedMinutes) }
    var completedSecondsText by remember { mutableStateOf(initCompletedSeconds) }

    val defaultCompletionMillis = remember(initialGame?.completionDate) {
        initialGame?.completionDate?.takeIf { it > 0L } ?: System.currentTimeMillis()
    }
    var completionDayMonthText by remember { mutableStateOf(formatDayMonth(defaultCompletionMillis)) }
    var completionYearText by remember { mutableStateOf(formatYear(defaultCompletionMillis)) }

    var hasOnlineDuration by remember {
        mutableStateOf((initialGame?.hltbMainSeconds ?: 0L) > 0L)
    }
    var hasOnlineReleaseDate by remember {
        mutableStateOf((initialGame?.releaseDate ?: 0L) > 0L)
    }

    val initDayMonth = remember(initialGame?.releaseDate) {
        formatDayMonth(initialGame?.releaseDate)
    }
    val initYear = remember(initialGame?.releaseDate) {
        formatYear(initialGame?.releaseDate)
    }

    var releaseDayMonthText by remember { mutableStateOf(initDayMonth) }
    var releaseYearText by remember { mutableStateOf(initYear) }

    var isSearchingOnline by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<RemoteGameMetadata>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        val clean = searchQuery.trim()
        if (clean.length < 2) {
            searchResults = emptyList()
            isSearchingOnline = false
            return@LaunchedEffect
        }
        delay(220L)
        isSearchingOnline = true
        try {
            val remote = metadataService.searchGames(clean)
            if (remote.isNotEmpty()) {
                searchResults = remote
            } else {
                searchResults = HltbReferenceCatalog.search(clean)
            }
        } finally {
            isSearchingOnline = false
        }
    }

    fun applyRemoteMetadata(meta: RemoteGameMetadata) {
        searchQuery = ""
        title = meta.title
        externalApiId = meta.externalApiId
        coverUrl = meta.coverUrl
        backdropUrl = meta.backdropUrl ?: meta.coverUrl
        if (meta.genres.isNotBlank()) {
            genres = meta.genres
        }
        if (meta.releaseDateMillis != null && meta.releaseDateMillis > 0L) {
            releaseDayMonthText = formatDayMonth(meta.releaseDateMillis)
            releaseYearText = formatYear(meta.releaseDateMillis)
            hasOnlineReleaseDate = true
        }
        if (meta.isUnreleased) {
            hasOnlineDuration = false
            hltbMainHoursText = ""
            hltbExtraHoursText = ""
            hltb100HoursText = ""
            if (initialGame == null) {
                status = GameStatus.DAY_ONE
            }
        } else {
            val hasMain = (meta.hltbMainSeconds ?: 0L) > 0L
            hasOnlineDuration = hasMain
            meta.hltbMainSeconds?.let { hltbMainHoursText = (it / 3600L).toString() }
            meta.hltbExtraSeconds?.let { hltbExtraHoursText = (it / 3600L).toString() }
            meta.hltbCompletionistSeconds?.let { hltb100HoursText = (it / 3600L).toString() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSlateSurface,
        title = {
            Text(
                text = if (initialGame == null) "Novo Jogo" else "Editar Jogo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!coverUrl.isNullOrBlank()) {
                        GameCoverArtwork(
                            title = title.ifBlank { "GV" },
                            coverUrl = coverUrl,
                            modifier = Modifier.size(width = 56.dp, height = 78.dp),
                            cornerRadius = 10.dp
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            searchQuery = it
                            if (it.trim().isEmpty()) {
                                hasOnlineDuration = false
                                hasOnlineReleaseDate = false
                            }
                        },
                        label = { Text("Título do Jogo *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isSearchingOnline) {
                    Text(
                        text = "Buscando correspondências...",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurpleGlow
                    )
                }

                if (searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeepSlateCard)
                            .border(1.dp, NeonPurplePrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        searchResults.take(6).forEach { candidate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepSlateSurface)
                                    .clickable {
                                        searchResults = emptyList()
                                        applyRemoteMetadata(candidate)
                                        coroutineScope.launch {
                                            val detailed = metadataService.enrichSelectedCandidate(candidate)
                                            applyRemoteMetadata(detailed)
                                        }
                                    }
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GameCoverArtwork(
                                    title = candidate.title,
                                    coverUrl = candidate.coverUrl,
                                    modifier = Modifier.size(width = 40.dp, height = 54.dp),
                                    cornerRadius = 6.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val candidateYear = formatYear(candidate.releaseDateMillis).takeIf { it.isNotBlank() }
                                    val durationLabel = if (candidate.isUnreleased) {
                                        "Inédito • Duração a definir"
                                    } else {
                                        "Campanha: ${TimeFormatUtils.formatHltbReferenceOrTbd(candidate.hltbMainSeconds)}"
                                    }
                                    val subtitlePrefix = if (candidateYear != null) {
                                        "$candidateYear • ${candidate.platform}"
                                    } else {
                                        candidate.platform
                                    }
                                    Text(
                                        text = "$subtitlePrefix • $durationLabel",
                                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Plataformas com pílulas compactas (sem o espaçamento vertical exagerado do FilterChip)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isManagingPlatforms) {
                                "Toque na plataforma para excluir:"
                            } else {
                                "Minhas Plataformas:"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isManagingPlatforms) DangerCrimson else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (userPlatforms.isNotEmpty()) {
                            Text(
                                text = if (isManagingPlatforms) "✕ Cancelar" else "Gerenciar",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isManagingPlatforms) NeonPurpleGlow else TextMuted,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { isManagingPlatforms = !isManagingPlatforms }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        userPlatforms.forEach { option ->
                            val isSelected = platform.equals(option, ignoreCase = true)
                            CompactSelectPill(
                                text = if (isManagingPlatforms) "🗑 $option" else option,
                                selected = if (isManagingPlatforms) false else isSelected,
                                isDanger = isManagingPlatforms,
                                onClick = {
                                    if (isManagingPlatforms) {
                                        platformPendingDeletion = option
                                    } else {
                                        platform = option
                                    }
                                }
                            )
                        }
                        if (isManagingPlatforms) {
                            CompactSelectPill(
                                text = "← Voltar",
                                selected = false,
                                isAction = true,
                                onClick = { isManagingPlatforms = false }
                            )
                        } else {
                            CompactSelectPill(
                                text = "+ Nova Plataforma",
                                selected = isAddingPlatform,
                                isAction = true,
                                onClick = { isAddingPlatform = !isAddingPlatform }
                            )
                        }
                    }
                }

                if (isAddingPlatform) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPlatformInput,
                            onValueChange = { newPlatformInput = it },
                            label = { Text("Criar Plataforma", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("Ex: PC, PS5, Switch...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val cleanNew = newPlatformInput.trim()
                                if (cleanNew.isNotEmpty()) {
                                    userPlatforms = UserPlatformStore.addPlatform(context, cleanNew)
                                    platform = cleanNew
                                    newPlatformInput = ""
                                    isAddingPlatform = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Adicionar", color = Color(0xFF140526), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Status com pílulas compactas
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Status:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GameStatus.entries.forEach { item ->
                            CompactSelectPill(
                                text = item.displayName,
                                selected = status == item,
                                onClick = { status = item }
                            )
                        }
                    }
                }

                // Prioridade compacta em 1 linha
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Prioridade:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PriorityTier.entries.forEach { tier ->
                            val isSelected = priority == tier
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) NeonPurpleSecondary.copy(alpha = 0.35f) else DeepSlateCard
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) NeonPurplePrimary else DeepSlateElevated,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { priority = tier },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nível ${tier.badge}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) NeonPurpleGlow else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Duração e Data de Conclusão:
                // - Se o usuário selecionar Zerado: pede Horas, Minutos e Segundos + Data em que Zerou (Base do GOTY).
                // - Se NÃO tiver duração no BD online (ex: lançamento futuro ou jogo sem dados): permite opinar a duração da campanha.
                // - Se o jogo já tiver sido lançado e já tiver duração no BD online: fica oculto para manter o menu limpo.
                if (status == GameStatus.COMPLETED) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Tempo para Zerar:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = completedHoursText,
                                onValueChange = { completedHoursText = it.filter(Char::isDigit).take(4) },
                                label = { Text("Horas", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = completedMinutesText,
                                onValueChange = { completedMinutesText = it.filter(Char::isDigit).take(2) },
                                label = { Text("Min", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = completedSecondsText,
                                onValueChange = { completedSecondsText = it.filter(Char::isDigit).take(2) },
                                label = { Text("Seg", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("00", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Data em que Zerou:",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = completionDayMonthText,
                                onValueChange = { completionDayMonthText = it.take(5) },
                                label = { Text("Dia/Mês", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("06/11", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = completionYearText,
                                onValueChange = { completionYearText = it.filter(Char::isDigit).take(4) },
                                label = { Text("Ano", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("2026", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else if (!hasOnlineDuration) {
                    OutlinedTextField(
                        value = hltbMainHoursText,
                        onValueChange = { hltbMainHoursText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Duração Estimada da Campanha (h)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("Opcional (deixe vazio se a definir)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Só exibe campos manuais de Gêneros / Data de Lançamento se o BD online não tiver preenchido
                if (genres.isBlank()) {
                    OutlinedTextField(
                        value = genres,
                        onValueChange = { genres = it },
                        label = { Text("Gêneros (Opcional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("RPG, Ação...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!hasOnlineReleaseDate) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Data de Lançamento (Opcional):",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = releaseDayMonthText,
                                onValueChange = { releaseDayMonthText = it.take(5) },
                                label = { Text("Dia/Mês", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("19/11", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = releaseYearText,
                                onValueChange = { releaseYearText = it.filter(Char::isDigit).take(4) },
                                label = { Text("Ano", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("2026", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (status == GameStatus.PLAYING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Jogo Principal do Momento",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isMainFocus,
                            onCheckedChange = { isMainFocus = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isEmpty()) return@Button

                    val pendingNewPlatform = newPlatformInput.trim()
                    val chosenPlatform = when {
                        pendingNewPlatform.isNotEmpty() -> {
                            UserPlatformStore.addPlatform(context, pendingNewPlatform)
                            pendingNewPlatform
                        }
                        platform.trim().isNotEmpty() -> {
                            UserPlatformStore.addPlatform(context, platform.trim())
                            platform.trim()
                        }
                        else -> "Sem plataforma"
                    }

                    val hltbMainSec = hltbMainHoursText.toLongOrNull()?.takeIf { it > 0L }?.times(3600L)
                    val hltbExtraSec = hltbExtraHoursText.toLongOrNull()?.takeIf { it > 0L }?.times(3600L)
                    val hltb100Sec = hltb100HoursText.toLongOrNull()?.takeIf { it > 0L }?.times(3600L)

                    val compH = completedHoursText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                    val compM = completedMinutesText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                    val compS = completedSecondsText.toLongOrNull()?.coerceIn(0L, 59L) ?: 0L
                    val completedSec = ((compH * 3600L) + (compM * 60L) + compS).takeIf { it > 0L }

                    val parsedRelease = TimeFormatUtils.parseDayMonthAndYearToMillis(
                        releaseDayMonthText,
                        releaseYearText
                    ) ?: initialGame?.releaseDate

                    val parsedCompletionDate = TimeFormatUtils.parseDayMonthAndYearToMillis(
                        completionDayMonthText,
                        completionYearText
                    )

                    val base = initialGame ?: GameEntity(title = cleanTitle)
                    val finalTimePlayed = if (status == GameStatus.COMPLETED && completedSec != null) {
                        completedSec
                    } else {
                        base.timePlayedSeconds
                    }
                    val finalCompletionDate = if (status == GameStatus.COMPLETED) {
                        parsedCompletionDate ?: base.completionDate ?: System.currentTimeMillis()
                    } else {
                        base.completionDate
                    }

                    onSave(
                        base.copy(
                            title = cleanTitle,
                            externalApiId = externalApiId ?: base.externalApiId,
                            coverUrl = coverUrl ?: base.coverUrl,
                            backdropUrl = backdropUrl ?: base.backdropUrl,
                            platform = chosenPlatform,
                            genres = genres.trim(),
                            status = status,
                            priority = priority,
                            targetCompletionStyle = completionStyle,
                            isMainFocus = status == GameStatus.PLAYING && isMainFocus,
                            timePlayedSeconds = finalTimePlayed,
                            completionDate = finalCompletionDate,
                            hltbMainSeconds = hltbMainSec ?: (if (status == GameStatus.COMPLETED) completedSec else null),
                            hltbExtraSeconds = hltbExtraSec ?: base.hltbExtraSeconds,
                            hltbCompletionistSeconds = hltb100Sec,
                            userCustomEstimateSeconds = null,
                            releaseDate = parsedRelease,
                            isOwned = status != GameStatus.WISHLIST
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
            ) {
                Text("Salvar", color = Color(0xFF140526), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )

    platformPendingDeletion?.let { targetPlatform ->
        AlertDialog(
            onDismissRequest = { platformPendingDeletion = null },
            containerColor = DeepSlateSurface,
            title = {
                Text(
                    text = "Excluir plataforma \"$targetPlatform\"?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DangerCrimson
                )
            },
            text = {
                Text(
                    text = "Todos os jogos cadastrados com a plataforma \"$targetPlatform\" perderão essa plataforma (ficarão como \"Sem plataforma\"). Deseja continuar?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val updated = UserPlatformStore.deletePlatformEverywhere(
                                context = context,
                                gameDao = gameDao,
                                platformName = targetPlatform
                            )
                            userPlatforms = updated
                            if (platform.equals(targetPlatform, ignoreCase = true)) {
                                platform = updated.firstOrNull().orEmpty()
                            }
                            if (updated.isEmpty()) {
                                isManagingPlatforms = false
                                isAddingPlatform = true
                            }
                            platformPendingDeletion = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerCrimson)
                ) {
                    Text("Sim, Excluir Plataforma", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { platformPendingDeletion = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

private fun formatDayMonth(timestampMillis: Long?): String {
    if (timestampMillis == null || timestampMillis <= 0L) return ""
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = timestampMillis }
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val m = cal.get(Calendar.MONTH) + 1
    return String.format(Locale.US, "%02d/%02d", d, m)
}

private fun formatYear(timestampMillis: Long?): String {
    if (timestampMillis == null || timestampMillis <= 0L) return ""
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = timestampMillis }
    return cal.get(Calendar.YEAR).toString()
}

@Composable
private fun CompactSelectPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    isAction: Boolean = false
) {
    val bgColor = when {
        isDanger -> DangerCrimson.copy(alpha = 0.14f)
        selected -> NeonPurplePrimary.copy(alpha = 0.25f)
        isAction -> DeepSlateElevated
        else -> DeepSlateCard
    }
    val borderColor = when {
        isDanger -> DangerCrimson.copy(alpha = 0.7f)
        selected -> NeonPurplePrimary
        else -> DeepSlateElevated
    }
    val textColor = when {
        isDanger -> DangerCrimson
        selected || isAction -> NeonPurpleGlow
        else -> TextSecondary
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
