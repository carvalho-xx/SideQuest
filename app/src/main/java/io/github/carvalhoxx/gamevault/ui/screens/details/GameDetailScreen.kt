package io.github.carvalhoxx.gamevault.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import io.github.carvalhoxx.gamevault.ui.components.GameCoverArtwork
import io.github.carvalhoxx.gamevault.ui.components.GameFormDialog
import io.github.carvalhoxx.gamevault.ui.components.ManualTimePickerDialog
import io.github.carvalhoxx.gamevault.ui.theme.DangerCrimson
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateBackground
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateCard
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateElevated
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateSurface
import io.github.carvalhoxx.gamevault.ui.theme.HallOfFameGold
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurpleGlow
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurplePrimary
import io.github.carvalhoxx.gamevault.ui.theme.TextMuted
import io.github.carvalhoxx.gamevault.ui.theme.TextPrimary
import io.github.carvalhoxx.gamevault.ui.theme.TextSecondary
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameDetailScreen(
    game: GameEntity?,
    nowMillis: Long,
    onNavigateBack: () -> Unit,
    onToggleStopwatch: (GameEntity) -> Unit,
    onUpdateExactTime: (gameId: Long, hours: Long, minutes: Long, seconds: Long) -> Unit,
    onUpdateRatingAndMemory: (gameId: Long, rating: Float?, memoryNote: String?, isPersonalGoty: Boolean) -> Unit,
    onMarkAsCompleted: (GameEntity) -> Unit,
    onSaveGame: (GameEntity) -> Unit,
    onDeleteGame: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (game == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DeepSlateBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Carregando detalhes do jogo...", color = TextSecondary)
        }
        return
    }

    var showManualTimeDialog by remember { mutableStateOf(false) }
    var showEditGameDialog by remember { mutableStateOf(false) }

    var selectedRating by remember(game.id, game.personalRating) {
        mutableStateOf(game.personalRating)
    }
    var memoryText by remember(game.id, game.coreMemoryNote) {
        mutableStateOf(game.coreMemoryNote.orEmpty())
    }
    var isPersonalGoty by remember(game.id, game.isPersonalGoty) {
        mutableStateOf(game.isPersonalGoty)
    }

    LaunchedEffect(game.personalRating, game.coreMemoryNote, game.isPersonalGoty) {
        selectedRating = game.personalRating
        memoryText = game.coreMemoryNote.orEmpty()
        isPersonalGoty = game.isPersonalGoty
    }

    val liveSeconds = game.getLiveTimePlayedSeconds(nowMillis)
    val hours = liveSeconds / 3600L
    val minutes = (liveSeconds % 3600L) / 60L
    val seconds = liveSeconds % 60L
    val isStopwatchRunning = game.activeTimerStartTime != null

    val releaseYearStr = remember(game.releaseDate) {
        val ts = game.releaseDate
        if (ts == null || ts <= 0L) {
            "TBD"
        } else {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = ts }
            cal.get(Calendar.YEAR).toString()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Barra Superior + Header com Arte do Jogo (03_game_details_rating11.png)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E2049),
                            DeepSlateSurface
                        )
                    )
                )
                .border(1.dp, DeepSlateElevated, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DeepSlateCard)
                    ) {
                        Text("←", color = TextPrimary, fontSize = 20.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showEditGameDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DeepSlateCard)
                        ) {
                            Text("✎", color = NeonPurpleGlow, fontSize = 16.sp)
                        }
                        IconButton(
                            onClick = { onDeleteGame(game.id) },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DeepSlateCard)
                        ) {
                            Text("🗑", color = DangerCrimson, fontSize = 16.sp)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    GameCoverArtwork(
                        title = game.title,
                        isHallOfFame = selectedRating != null && selectedRating!! >= 11.0f,
                        modifier = Modifier.size(112.dp),
                        cornerRadius = 18.dp
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "$releaseYearStr • ${game.status.displayName}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailTagPill(text = game.platform, bgColor = DeepSlateElevated)
                            game.genres.split(",")
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .take(3)
                                .forEach { genre ->
                                    DetailTagPill(
                                        text = genre,
                                        bgColor = NeonPurplePrimary.copy(alpha = 0.22f),
                                        textColor = NeonPurpleGlow
                                    )
                                }
                        }
                    }
                }
            }
        }

        // 2. Card Time Tracker & Session (03_game_details_rating11.png)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DeepSlateSurface)
                .border(1.2.dp, NeonPurplePrimary.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Time Tracker & Session",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = NeonPurpleGlow,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                text = "${String.format("%02d", hours)}h : ${String.format("%02d", minutes)}m : ${String.format("%02d", seconds)}s",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.clickable { showManualTimeDialog = true }
            )

            // Pílulas de referência HLTB (Null-safe -> exibe TBD quando nulo)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetailTagPill(
                    text = "🕒 HLTB Main: ${TimeFormatUtils.formatHltbReferenceOrTbd(game.hltbMainSeconds)}",
                    bgColor = DeepSlateCard,
                    textColor = TextSecondary
                )
                DetailTagPill(
                    text = "🏆 100%: ${TimeFormatUtils.formatHltbReferenceOrTbd(game.hltbCompletionistSeconds)}",
                    bgColor = DeepSlateCard,
                    textColor = TextSecondary
                )
            }

            // Status flexível quando passa do HLTB
            val targetSec = game.getEffectiveDurationSeconds()
            if (targetSec != null && liveSeconds > targetSec) {
                val extraSec = liveSeconds - targetSec
                val extraH = extraSec / 3600L
                val extraM = (extraSec % 3600L) / 60L
                DetailTagPill(
                    text = "✨ Explorando além da média (+${extraH}h ${extraM}m) • Na reta final / No seu próprio ritmo",
                    bgColor = HallOfFameGold.copy(alpha = 0.18f),
                    textColor = HallOfFameGold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onToggleStopwatch(game) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isStopwatchRunning) HallOfFameGold else NeonPurplePrimary
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isStopwatchRunning) "⏹ Stop Stopwatch" else "⏱ Start Live Stopwatch",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF140526),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                OutlinedButton(
                    onClick = { showManualTimeDialog = true },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+ Manual HH:MM:SS Input",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // 3. Card Personal Evaluation (0 to 11 Scale) + Botão Dourado 11/10 Life-Changing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DeepSlateSurface)
                .border(1.2.dp, NeonPurplePrimary.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Evaluation (0 to 11 Scale)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                if (selectedRating != null) {
                    Text(
                        text = if (selectedRating!! >= 11.0f) "11/10" else "${selectedRating}/10",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = HallOfFameGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }

            // Linha de botões 0 a 10
            val baseIntRating = selectedRating?.let { floor(it).toInt() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (0..10).forEach { score ->
                    val isSelected = baseIntRating == score && (selectedRating ?: 0f) < 11.0f
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) HallOfFameGold.copy(alpha = 0.24f) else DeepSlateCard
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) HallOfFameGold else DeepSlateElevated,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                val newRating = score.toFloat()
                                selectedRating = newRating
                                onUpdateRatingAndMemory(game.id, newRating, memoryText, isPersonalGoty)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = score.toString(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (isSelected) HallOfFameGold else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Ajuste decimal fino opcional (.0, .25, .5, .75) quando nota entre 0 e 9
            if (baseIntRating != null && baseIntRating in 0..9) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Precisão:",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                    listOf(0.0f, 0.25f, 0.5f, 0.75f).forEach { fraction ->
                        val candidate = baseIntRating + fraction
                        val isExactSelected = selectedRating == candidate
                        FilterChip(
                            selected = isExactSelected,
                            onClick = {
                                selectedRating = candidate
                                onUpdateRatingAndMemory(game.id, candidate, memoryText, isPersonalGoty)
                            },
                            label = { Text(candidate.toString(), fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Botão Dourado Especial: 11/10 • Life-Changing Game (Hall of Fame)
            val is11Selected = selectedRating != null && selectedRating!! >= 11.0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (is11Selected) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF4B370E), Color(0xFF6B4E12))
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2A2111), Color(0xFF1F1910))
                            )
                        }
                    )
                    .border(
                        width = if (is11Selected) 2.dp else 1.2.dp,
                        color = HallOfFameGold,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable {
                        selectedRating = 11.0f
                        onUpdateRatingAndMemory(game.id, 11.0f, memoryText, isPersonalGoty)
                    }
                    .padding(vertical = 14.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏆 11/10 • Life-Changing Game",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = HallOfFameGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }
        }

        // 4. Card Core Memory / Why it marked my life (Borda Dourada #F5C451) + Personal GOTY
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1E1912))
                .border(1.3.dp, HallOfFameGold, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "💛 Core Memory / Why it marked my life",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            OutlinedTextField(
                value = memoryText,
                onValueChange = { memoryText = it },
                placeholder = {
                    Text(
                        text = "Registre a memória afetiva dessa jornada (ex: Primeiro co-op zerado com meu melhor amigo)...",
                        color = TextMuted
                    )
                },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DeepSlateSurface.copy(alpha = 0.6f),
                    unfocusedContainerColor = DeepSlateSurface.copy(alpha = 0.6f),
                    focusedBorderColor = HallOfFameGold,
                    unfocusedBorderColor = HallOfFameGold.copy(alpha = 0.4f),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🏆 Personal GOTY (Jogo do Ano)",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = HallOfFameGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Destaca como seu vencedor do ano",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
                Switch(
                    checked = isPersonalGoty,
                    onCheckedChange = { checked ->
                        isPersonalGoty = checked
                        onUpdateRatingAndMemory(game.id, selectedRating, memoryText, checked)
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    onUpdateRatingAndMemory(game.id, selectedRating, memoryText, isPersonalGoty)
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Salvar Memória Afetiva", color = HallOfFameGold, fontWeight = FontWeight.Bold)
            }
        }

        // 5. Botão Principal Inferior: ✓ Mark as Completed 🏆
        Button(
            onClick = { onMarkAsCompleted(game) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (game.status == GameStatus.COMPLETED) {
                    "✓ Completed 🏆 (Atualizar Conclusão)"
                } else {
                    "✓ Mark as Completed 🏆"
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF140526),
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (showManualTimeDialog) {
        ManualTimePickerDialog(
            gameTitle = game.title,
            initialSeconds = liveSeconds,
            onDismiss = { showManualTimeDialog = false },
            onConfirm = { h, m, s ->
                onUpdateExactTime(game.id, h, m, s)
                showManualTimeDialog = false
            }
        )
    }

    if (showEditGameDialog) {
        GameFormDialog(
            initialGame = game,
            onDismiss = { showEditGameDialog = false },
            onSave = { updated ->
                onSaveGame(updated)
                showEditGameDialog = false
            }
        )
    }
}

@Composable
private fun DetailTagPill(
    text: String,
    bgColor: Color,
    textColor: Color = TextPrimary
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
