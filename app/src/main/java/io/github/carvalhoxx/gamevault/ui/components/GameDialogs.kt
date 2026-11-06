package io.github.carvalhoxx.gamevault.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.CompletionStyle
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.PriorityTier
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
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
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Capa estilizada com gradiente dinâmico baseado no título do jogo (Null-safe quando sem URL).
 */
@Composable
fun GameCoverArtwork(
    title: String,
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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(colors))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
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
                    isHallOfFame = winner.isHallOfFame,
                    modifier = Modifier.size(88.dp)
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
 * Modal de Criação/Edição de Jogo com suporte completo a campos HLTB/Data opcionais.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameFormDialog(
    initialGame: GameEntity? = null,
    onDismiss: () -> Unit,
    onSave: (GameEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialGame?.title.orEmpty()) }
    var platform by remember { mutableStateOf(initialGame?.platform ?: "PC") }
    var genres by remember { mutableStateOf(initialGame?.genres.orEmpty()) }
    var status by remember { mutableStateOf(initialGame?.status ?: GameStatus.BACKLOG) }
    var priority by remember { mutableStateOf(initialGame?.priority ?: PriorityTier.TIER_B) }
    var completionStyle by remember {
        mutableStateOf(initialGame?.targetCompletionStyle ?: CompletionStyle.MAIN_STORY)
    }
    var isMainFocus by remember { mutableStateOf(initialGame?.isMainFocus ?: false) }

    val initHltbMainHours = initialGame?.hltbMainSeconds?.let { (it / 3600L).toString() }.orEmpty()
    val initHltb100Hours = initialGame?.hltbCompletionistSeconds?.let { (it / 3600L).toString() }.orEmpty()
    val initCustomHours = initialGame?.userCustomEstimateSeconds?.let { (it / 3600L).toString() }.orEmpty()

    var hltbMainHoursText by remember { mutableStateOf(initHltbMainHours) }
    var hltb100HoursText by remember { mutableStateOf(initHltb100Hours) }
    var customHoursText by remember { mutableStateOf(initCustomHours) }

    val initDayMonth = remember(initialGame?.releaseDate) {
        val ts = initialGame?.releaseDate
        if (ts == null || ts <= 0L) {
            ""
        } else {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = ts }
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val m = cal.get(Calendar.MONTH) + 1
            String.format(Locale.US, "%02d/%02d", d, m)
        }
    }
    val initYear = remember(initialGame?.releaseDate) {
        val ts = initialGame?.releaseDate
        if (ts == null || ts <= 0L) {
            ""
        } else {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = ts }
            cal.get(Calendar.YEAR).toString()
        }
    }

    var releaseDayMonthText by remember { mutableStateOf(initDayMonth) }
    var releaseYearText by remember { mutableStateOf(initYear) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título do Jogo *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = platform,
                        onValueChange = { platform = it },
                        label = { Text("Plataforma", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("PC, PS5...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = genres,
                        onValueChange = { genres = it },
                        label = { Text("Gêneros", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("RPG, Ação...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Status:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GameStatus.entries.forEach { item ->
                        FilterChip(
                            selected = status == item,
                            onClick = { status = item },
                            label = {
                                Text(
                                    text = item.displayName,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurplePrimary.copy(alpha = 0.25f),
                                selectedLabelColor = NeonPurpleGlow
                            )
                        )
                    }
                }

                Text("Prioridade:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityTier.entries.forEach { tier ->
                        val isSelected = priority == tier
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) NeonPurpleSecondary.copy(alpha = 0.35f) else DeepSlateCard
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) NeonPurplePrimary else DeepSlateElevated,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { priority = tier },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nível ${tier.badge}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) NeonPurpleGlow else TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Text("Estilo de Meta:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompletionStyle.entries.forEach { style ->
                        FilterChip(
                            selected = completionStyle == style,
                            onClick = { completionStyle = style },
                            label = {
                                Text(
                                    text = style.shortLabel,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurplePrimary.copy(alpha = 0.25f),
                                selectedLabelColor = NeonPurpleGlow
                            )
                        )
                    }
                }

                Text(
                    text = "Duração Estimada em Horas (Deixe vazio se a definir):",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hltbMainHoursText,
                        onValueChange = { hltbMainHoursText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Campanha (h)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("A definir", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hltb100HoursText,
                        onValueChange = { hltb100HoursText = it.filter(Char::isDigit).take(4) },
                        label = { Text("100% (h)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("A definir", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = customHoursText,
                    onValueChange = { customHoursText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Meta Pessoal em Horas (Opcional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    placeholder = { Text("Ex: 40", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

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
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isEmpty()) return@Button

                    val hltbMainSec = hltbMainHoursText.toLongOrNull()?.takeIf { it > 0L }?.times(3600L)
                    val hltb100Sec = hltb100HoursText.toLongOrNull()?.takeIf { it > 0L }?.times(3600L)
                    val customSec = customHoursText.toLongOrNull()?.takeIf { it > 0L }?.times(3600L)
                    val parsedRelease = TimeFormatUtils.parseDayMonthAndYearToMillis(
                        releaseDayMonthText,
                        releaseYearText
                    ) ?: initialGame?.releaseDate

                    val base = initialGame ?: GameEntity(title = cleanTitle)
                    onSave(
                        base.copy(
                            title = cleanTitle,
                            platform = platform.trim().ifEmpty { "PC" },
                            genres = genres.trim(),
                            status = status,
                            priority = priority,
                            targetCompletionStyle = completionStyle,
                            isMainFocus = isMainFocus,
                            hltbMainSeconds = hltbMainSec,
                            hltbCompletionistSeconds = hltb100Sec,
                            userCustomEstimateSeconds = customSec,
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
}
