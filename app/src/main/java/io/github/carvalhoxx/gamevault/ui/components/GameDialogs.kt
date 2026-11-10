package io.github.carvalhoxx.gamevault.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import io.github.carvalhoxx.gamevault.ui.theme.AmberBrass
import io.github.carvalhoxx.gamevault.ui.theme.CarbonMatteBackground
import io.github.carvalhoxx.gamevault.ui.theme.EmberOrangePrimary
import io.github.carvalhoxx.gamevault.ui.theme.DangerCrimson
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateCard
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateElevated
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateSurface
import io.github.carvalhoxx.gamevault.ui.theme.HallOfFameGold
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurpleGlow
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurplePrimary
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurpleSecondary
import io.github.carvalhoxx.gamevault.ui.theme.SageClear
import io.github.carvalhoxx.gamevault.ui.theme.TextMuted
import io.github.carvalhoxx.gamevault.ui.theme.TextPrimary
import io.github.carvalhoxx.gamevault.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

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
        listOf(Color(0xFFFF5A26), Color(0xFF9E2B10), Color(0xFF261410)),
        listOf(Color(0xFF262B38), Color(0xFF1A1E29), Color(0xFF121316)),
        listOf(Color(0xFFE5A93C), Color(0xFF6B4710), Color(0xFF1A150E)),
        listOf(Color(0xFF8F2D19), Color(0xFF3D1710), Color(0xFF141110)),
        listOf(Color(0xFF24342B), Color(0xFF18221D), Color(0xFF121514))
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
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(cornerRadius))
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
private fun LegacyManualTimePickerDialog(
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
private fun LegacyCompleteGameDialog(
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
private fun LegacyBacklogRouletteDialog(
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

private val ModalBackground = Brush.linearGradient(
    listOf(Color(0xFF25272A), Color(0xFF1E2023), Color(0xFF191B1E))
)
private val ModalFieldBorder = Color(0xFF2C2D33)

@Composable
private fun SystemModal(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ModalBackground)
                .border(1.dp, Color(0xFF37393D), RoundedCornerShape(16.dp))
                .verticalScroll(rememberScrollState())
                .padding(26.dp),
            content = content
        )
    }
}

@Composable
private fun ModalPrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EmberOrangePrimary,
            contentColor = Color(0xFF121316)
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ModalOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = ModalFieldBorder,
            focusedBorderColor = EmberOrangePrimary,
            unfocusedLabelColor = TextSecondary,
            focusedLabelColor = EmberOrangePrimary,
            unfocusedTextColor = TextPrimary,
            focusedTextColor = TextPrimary,
            cursorColor = EmberOrangePrimary
        )
    )
}

/** Modal vertical da roleta, conforme a referência visual. */
@Composable
fun BacklogRouletteDialog(
    winner: GameEntity,
    onDismiss: () -> Unit,
    onSpinAgain: () -> Unit,
    onStartPlaying: (GameEntity) -> Unit,
    onOpenDetails: (Long) -> Unit
) {
    SystemModal(onDismiss) {
        Text("O que jogar agora?", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        GameCoverArtwork(
            title = winner.title,
            coverUrl = winner.coverUrl,
            isHallOfFame = winner.isHallOfFame,
            modifier = Modifier.align(Alignment.CenterHorizontally).size(118.dp, 180.dp).clickable { onOpenDetails(winner.id) },
            cornerRadius = 8.dp
        )
        Spacer(Modifier.height(12.dp))
        Text(winner.title, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Prioridade ${winner.priority.label}", color = EmberOrangePrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
        ModalPrimaryButton("Definir como foco atual") { onStartPlaying(winner) }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSpinAgain, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Sortear outro", color = TextSecondary) }
    }
}

/** Ajuste preciso em três campos lado a lado: HH:MM:SS. */
@Composable
fun ManualTimePickerDialog(
    gameTitle: String,
    initialSeconds: Long,
    onDismiss: () -> Unit,
    onConfirm: (hours: Long, minutes: Long, seconds: Long) -> Unit
) {
    var hours by remember { mutableStateOf((initialSeconds / 3600).toString().padStart(2, '0')) }
    var minutes by remember { mutableStateOf(((initialSeconds / 60) % 60).toString().padStart(2, '0')) }
    var seconds by remember { mutableStateOf((initialSeconds % 60).toString().padStart(2, '0')) }
    SystemModal(onDismiss) {
        Text("Ajustar Tempo Jogado", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(gameTitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ModalOutlinedField(hours, { hours = it.filter(Char::isDigit).take(4) }, "Horas", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ModalOutlinedField(minutes, { minutes = it.filter(Char::isDigit).take(2) }, "Minutos", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ModalOutlinedField(seconds, { seconds = it.filter(Char::isDigit).take(2) }, "Segundos", Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, ModalFieldBorder)) { Text("Cancelar", color = TextPrimary) }
            Button(onClick = { onConfirm(hours.toLongOrNull() ?: 0, (minutes.toLongOrNull() ?: 0).coerceIn(0, 59), (seconds.toLongOrNull() ?: 0).coerceIn(0, 59)) }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = EmberOrangePrimary)) { Text("Salvar", color = CarbonMatteBackground, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Finalização com escala numérica, selo 11/10 e memória afetiva. */
@Composable
fun CompleteGameDialog(
    gameTitle: String,
    initialSeconds: Long,
    initialCompletionDateMillis: Long?,
    initialRating: Float? = 9.5f,
    initialMemoryNote: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (totalSeconds: Long, completionDateMillis: Long, rating: Float, memory: String) -> Unit
) {
    var rating by remember { mutableStateOf(initialRating ?: 9.5f) }
    var memory by remember { mutableStateOf(initialMemoryNote.orEmpty()) }
    val completionMillis = initialCompletionDateMillis ?: System.currentTimeMillis()
    SystemModal(onDismiss) {
        Text("Finalizar Jornada", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Tempo final ${TimeFormatUtils.formatSecondsToHhMmSs(initialSeconds)}", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(22.dp))
        Text("Sua nota", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(value = rating, onValueChange = { rating = (it * 2).roundToInt() / 2f }, valueRange = 0f..11f, modifier = Modifier.weight(1f), colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = EmberOrangePrimary, activeTrackColor = EmberOrangePrimary))
            Text(String.format(Locale.US, "%.1f", rating), color = EmberOrangePrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(58.dp), textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Brush.verticalGradient(listOf(Color(0xFFF3D89A), Color(0xFFB88C3C)))).padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text("11/10 • Jogo da Vida", color = Color(0xFF20190D), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        ModalOutlinedField(memory, { memory = it }, "Memória Afetiva", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
        Spacer(Modifier.height(14.dp))
        ModalPrimaryButton("Confirmar") { onConfirm(initialSeconds, completionMillis, rating, memory) }
    }
}

/** Desempate horizontal para qualquer quantidade de jogos (incluindo 3+). */
@Composable
fun GotyTiebreakerDialog(
    games: List<GameEntity>,
    selectedGameId: Long? = games.firstOrNull()?.id,
    onDismiss: () -> Unit,
    onSelected: (GameEntity) -> Unit,
    onConfirm: (GameEntity) -> Unit
) {
    var selectedId by remember(selectedGameId) { mutableStateOf(selectedGameId) }
    SystemModal(onDismiss) {
        Text("Desempate GOTY", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${games.size} jogos empatados com nota máxima", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(18.dp))
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(games, key = { it.id }) { game ->
                val selected = selectedId == game.id
                Column(Modifier.width(116.dp).clickable { selectedId = game.id; onSelected(game) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    GameCoverArtwork(game.title, game.coverUrl, game.isHallOfFame, Modifier.size(116.dp, 174.dp), cornerRadius = 8.dp)
                    if (selected) Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(2.dp).background(EmberOrangePrimary))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        ModalPrimaryButton("Confirmar escolha") { games.firstOrNull { it.id == selectedId }?.let(onConfirm) }
    }
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
    var completionStyle by remember { mutableStateOf(initialGame?.targetCompletionStyle ?: CompletionStyle.MAIN_STORY) }
    var isMainFocus by remember { mutableStateOf(initialGame?.isMainFocus ?: false) }
    var selectedRating by remember { mutableStateOf(initialGame?.personalRating) }

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
    var isFutureReleaseDetected by remember {
        mutableStateOf(
            initialGame?.status == GameStatus.DAY_ONE ||
                ((initialGame?.releaseDate ?: 0L) > System.currentTimeMillis())
        )
    }

    val initDayMonth = remember(initialGame?.releaseDate) {
        formatDayMonth(initialGame?.releaseDate)
    }
    val initYear = remember(initialGame?.releaseDate, initialGame?.releaseYear) {
        initialGame?.releaseYear?.toString() ?: formatYear(initialGame?.releaseDate)
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
        } else if (meta.releaseYear != null) {
            releaseDayMonthText = ""
            releaseYearText = meta.releaseYear.toString()
            hasOnlineReleaseDate = true
        }
        val isFuture = meta.isUnreleased ||
            (meta.releaseDateMillis != null && meta.releaseDateMillis > System.currentTimeMillis())
        isFutureReleaseDetected = isFuture
        if (isFuture) {
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

                // Banner Sutil em Amber Brass: Detecção Inteligente de Lançamento (Day One)
                val parsedCurrentReleaseMillis = TimeFormatUtils.parseDayMonthAndYearToMillis(
                    releaseDayMonthText,
                    releaseYearText
                ) ?: initialGame?.releaseDate
                val showLaunchDetectedBanner = isFutureReleaseDetected ||
                    status == GameStatus.DAY_ONE ||
                    (parsedCurrentReleaseMillis != null && parsedCurrentReleaseMillis > System.currentTimeMillis())

                if (showLaunchDetectedBanner) {
                    val releaseDateLabel = buildString {
                        if (releaseDayMonthText.isNotBlank() && releaseYearText.isNotBlank()) {
                            append("$releaseDayMonthText/$releaseYearText")
                        } else if (releaseYearText.isNotBlank()) {
                            append(releaseYearText)
                        } else {
                            append("data a definir")
                        }
                    }
                    val isAddedToRadar = status == GameStatus.DAY_ONE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberBrass.copy(alpha = 0.12f))
                            .border(1.5.dp, AmberBrass.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AmberBrass)
                                )
                                Text(
                                    text = "LANÇAMENTO DETECTADO (DAY ONE)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AmberBrass,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AmberBrass.copy(alpha = if (isAddedToRadar) 0.24f else 0.14f))
                                    .border(1.dp, AmberBrass, RoundedCornerShape(6.dp))
                                    .clickable { status = GameStatus.DAY_ONE }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAddedToRadar) "+ No Radar ✓" else "+ Radar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AmberBrass,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Text(
                            text = "Este jogo ainda não foi lançado (Previsto para $releaseDateLabel). Deseja adicioná-lo ao Radar de Lançamentos?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                fontSize = 11.5.sp
                            )
                        )
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

                // Status na Biblioteca (Alterna os blocos condicionais abaixo dinamicamente)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Status na Biblioteca:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
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

                // Blocos Condicionais Dinâmicos conforme o Status Escolhido (Seção 5.6 ARCHITECTURE.md)
                when (status) {
                    GameStatus.PLAYING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepSlateCard.copy(alpha = 0.65f))
                                .border(1.dp, DeepSlateElevated, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MODO JOGANDO AGORA • TEMPO & META",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonPurpleGlow,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                val hltbRefLabel = hltbMainHoursText.toLongOrNull()?.takeIf { it > 0L }
                                    ?.let { "Ref. HLTB: ~${it}h" }
                                    ?: "Ritmo livre"
                                Text(
                                    text = hltbRefLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = SageClear,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Tempo Inicial Jogado (HH:MM:SS):",
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
                                    text = "Estilo de Conclusão Alvo:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CompletionStyle.entries.forEach { styleOption ->
                                        val isSelected = completionStyle == styleOption
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) NeonPurplePrimary.copy(alpha = 0.22f) else DeepSlateSurface
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) NeonPurplePrimary else DeepSlateElevated,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { completionStyle = styleOption },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = styleOption.shortLabel,
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

                            if (!hasOnlineDuration) {
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

                    GameStatus.BACKLOG -> {
                        // Modo Na Fila (Backlog): exclusivamente Prioridade (Tier S, A, B, C) + nota de Ritmo Global no Perfil
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepSlateCard.copy(alpha = 0.65f))
                                .border(1.dp, DeepSlateElevated, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "MODO NA FILA (BACKLOG) • SELETOR DE PRIORIDADE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Prioridade na Fila (Roleta & Ordem da Agenda):",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
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
                                                if (isSelected) NeonPurpleSecondary.copy(alpha = 0.35f) else DeepSlateSurface
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
                                            text = "Tier ${tier.badge}",
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
                            Text(
                                text = "ℹ Ritmo diário (h/dia) é global e configurado no Perfil do Usuário",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SageClear,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    GameStatus.COMPLETED -> {
                        // Modo Zerado: Tempo Final Exato (HH:MM:SS), Data de Conclusão (GOTY) e Nota 0 a 11 (11/10 • Jogo da Vida)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DeepSlateCard.copy(alpha = 0.75f))
                                .border(1.5.dp, AmberBrass.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "MODO ZERADO • TEMPO FINAL, DATA & NOTA 0–11",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberBrass,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Tempo Final Exato (HH:MM:SS):",
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
                                    text = "Data de Conclusão (Filtro de GOTY por Ano):",
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
                                        placeholder = { Text("07/11", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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

                            // Seletor de Nota de 0 a 11 com Selo Especial "11/10 • Jogo da Vida"
                            val baseIntRating = selectedRating?.let { floor(it).toInt() }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Avaliação Pessoal (0 a 11):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary
                                    )
                                    val ratingLabel = when {
                                        selectedRating == null -> "Sem nota"
                                        selectedRating!! >= 11.0f -> "★ 11/10 • Jogo da Vida"
                                        else -> "${formatCompactRatingNumber(selectedRating!!)}/10"
                                    }
                                    Text(
                                        text = ratingLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (selectedRating != null) AmberBrass else TextMuted,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                listOf(0..5, 6..10).forEach { range ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        range.forEach { score ->
                                            val isSelected = baseIntRating == score && (selectedRating ?: 0f) < 11.0f
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) NeonPurplePrimary.copy(alpha = 0.28f) else DeepSlateSurface
                                                    )
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) NeonPurpleGlow else DeepSlateElevated,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedRating = score.toFloat() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = score.toString(),
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        color = if (isSelected) TextPrimary else TextSecondary,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }

                                if (baseIntRating != null && baseIntRating in 0..9) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(0.0f, 0.25f, 0.5f, 0.75f).forEach { fraction ->
                                            val candidate = baseIntRating + fraction
                                            val isExactSelected = selectedRating == candidate
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isExactSelected) AmberBrass.copy(alpha = 0.22f) else DeepSlateSurface
                                                    )
                                                    .border(
                                                        width = if (isExactSelected) 1.5.dp else 1.dp,
                                                        color = if (isExactSelected) AmberBrass else DeepSlateElevated,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedRating = candidate },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = formatCompactRatingNumber(candidate),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isExactSelected) AmberBrass else TextSecondary,
                                                        fontWeight = if (isExactSelected) FontWeight.ExtraBold else FontWeight.Medium
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }

                                val is11Selected = selectedRating != null && selectedRating!! >= 11.0f
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(AmberBrass.copy(alpha = if (is11Selected) 0.26f else 0.14f))
                                        .border(
                                            width = if (is11Selected) 1.8.dp else 1.2.dp,
                                            color = AmberBrass,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedRating = 11.0f }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "★ 11/10 • JOGO DA VIDA (SELO ESPECIAL)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = AmberBrass,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Ativa Hall da Fama e habilita disputa de GOTY Pessoal do ano",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextPrimary,
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // Modo Lançamento (Day One) e demais status: Prioridade para o Radar + Duração estimada opcional
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                text = "Tier ${tier.badge}",
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

                            if (!hasOnlineDuration) {
                                OutlinedTextField(
                                    value = hltbMainHoursText,
                                    onValueChange = { hltbMainHoursText = it.filter(Char::isDigit).take(4) },
                                    label = { Text("Duração Estimada da Campanha (h)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    placeholder = { Text("Deixe vazio para manter \"Duração a definir\"", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
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
                                onValueChange = {
                                    releaseDayMonthText = it.take(5)
                                    val candidateRelease = TimeFormatUtils.parseDayMonthAndYearToMillis(
                                        releaseDayMonthText,
                                        releaseYearText
                                    )
                                    if (candidateRelease != null && candidateRelease > System.currentTimeMillis()) {
                                        isFutureReleaseDetected = true
                                        if (initialGame == null) status = GameStatus.DAY_ONE
                                    }
                                },
                                label = { Text("Dia/Mês", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("19/11", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = releaseYearText,
                                onValueChange = {
                                    releaseYearText = it.filter(Char::isDigit).take(4)
                                    val candidateRelease = TimeFormatUtils.parseDayMonthAndYearToMillis(
                                        releaseDayMonthText,
                                        releaseYearText
                                    )
                                    if (candidateRelease != null && candidateRelease > System.currentTimeMillis()) {
                                        isFutureReleaseDetected = true
                                        if (initialGame == null) status = GameStatus.DAY_ONE
                                    }
                                },
                                label = { Text("Ano", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("2026", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                    ) ?: if (releaseYearText.isBlank()) initialGame?.releaseDate else null
                    val parsedReleaseYear = releaseYearText.toIntOrNull()?.takeIf { it in 1970..2100 }

                    val parsedCompletionDate = TimeFormatUtils.parseDayMonthAndYearToMillis(
                        completionDayMonthText,
                        completionYearText
                    )

                    val base = initialGame ?: GameEntity(title = cleanTitle)
                    val finalTimePlayed = if ((status == GameStatus.COMPLETED || status == GameStatus.PLAYING) && completedSec != null) {
                        completedSec
                    } else {
                        base.timePlayedSeconds
                    }
                    val finalCompletionDate = if (status == GameStatus.COMPLETED) {
                        parsedCompletionDate ?: base.completionDate ?: System.currentTimeMillis()
                    } else {
                        base.completionDate
                    }
                    val finalRating = if (status == GameStatus.COMPLETED) {
                        selectedRating ?: base.personalRating
                    } else {
                        base.personalRating
                    }
                    val finalHallOfFame = (finalRating != null && finalRating >= 11.0f) || base.isHallOfFame

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
                            personalRating = finalRating,
                            isHallOfFame = finalHallOfFame,
                            hltbMainSeconds = hltbMainSec ?: (if (status == GameStatus.COMPLETED) completedSec else null),
                            hltbExtraSeconds = hltbExtraSec ?: base.hltbExtraSeconds,
                            hltbCompletionistSeconds = hltb100Sec,
                            userCustomEstimateSeconds = null,
                            releaseDate = parsedRelease,
                            releaseYear = parsedReleaseYear,
                            isOwned = status != GameStatus.WISHLIST
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary)
            ) {
                Text("Salvar na Biblioteca", color = Color(0xFF140526), fontWeight = FontWeight.Bold)
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

private fun formatCompactRatingNumber(rating: Float): String {
    val hundredths = (rating * 100f).roundToInt()
    return when {
        hundredths % 100 == 0 -> (hundredths / 100).toString()
        hundredths % 10 == 0 -> String.format(Locale.US, "%.1f", rating)
        else -> String.format(Locale.US, "%.2f", rating)
    }
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
