package io.github.carvalhoxx.gamevault.ui.screens.details

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import io.github.carvalhoxx.gamevault.ui.components.AppIcons
import io.github.carvalhoxx.gamevault.ui.components.GameCoverArtwork
import io.github.carvalhoxx.gamevault.ui.components.GameFormDialog
import io.github.carvalhoxx.gamevault.ui.components.ManualTimePickerDialog
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateBackground
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
    var isMemoryExpanded by remember(game.id) { mutableStateOf(false) }

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
            "Data a definir"
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
        // 1. Barra Superior + Cabeçalho com Arte do Jogo (Ícones padronizados 22.dp em botões 42.dp)
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
                            .border(1.dp, DeepSlateElevated, CircleShape)
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showEditGameDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DeepSlateCard)
                                .border(1.dp, DeepSlateElevated, CircleShape)
                        ) {
                            Icon(
                                imageVector = AppIcons.Edit,
                                contentDescription = "Editar jogo",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteGame(game.id) },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DeepSlateCard)
                                .border(1.dp, DeepSlateElevated, CircleShape)
                        ) {
                            Icon(
                                imageVector = AppIcons.Delete,
                                contentDescription = "Excluir jogo",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
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

        // 2. Card Cronômetro e Sessão
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
                text = "Cronômetro e Sessão",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = NeonPurpleGlow,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Text(
                text = "${String.format(Locale.US, "%02d", hours)}h : ${String.format(Locale.US, "%02d", minutes)}m : ${String.format(Locale.US, "%02d", seconds)}s",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.clickable { showManualTimeDialog = true }
            )

            // Pílulas de referência HLTB em FlowRow para nunca quebrar
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailTagPill(
                    text = "🕒 Campanha HLTB: ${TimeFormatUtils.formatHltbReferenceOrTbd(game.hltbMainSeconds)}",
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
                    text = "✨ Explorando além da média (+${extraH}h ${extraM}m) • No seu próprio ritmo",
                    bgColor = HallOfFameGold.copy(alpha = 0.18f),
                    textColor = HallOfFameGold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
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
                        text = if (isStopwatchRunning) "⏹ Parar Cronômetro" else "⏱ Iniciar Cronômetro",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF140526),
                            fontWeight = FontWeight.ExtraBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = { showManualTimeDialog = true },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "+ Tempo Manual",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. Card Avaliação Pessoal (Escala 0 a 11) reformulado
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(DeepSlateSurface)
                .border(1.2.dp, NeonPurplePrimary.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Avaliação Pessoal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Escala de 0 a 10 • ou 11/10 para Jogo da Vida",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Surface(
                    color = if (selectedRating != null) {
                        HallOfFameGold.copy(alpha = 0.18f)
                    } else {
                        DeepSlateCard
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(
                        width = 1.2.dp,
                        color = if (selectedRating != null) HallOfFameGold else DeepSlateElevated,
                        shape = RoundedCornerShape(50)
                    )
                ) {
                    Text(
                        text = formatRatingBadge(selectedRating),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (selectedRating != null) HallOfFameGold else TextMuted,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Grade limpa de notas inteiras (0 a 5 na primeira linha, 6 a 10 na segunda linha)
            val baseIntRating = selectedRating?.let { floor(it).toInt() }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0..5, 6..10).forEach { range ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        range.forEach { score ->
                            val isSelected = baseIntRating == score && (selectedRating ?: 0f) < 11.0f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            Brush.verticalGradient(
                                                listOf(
                                                    NeonPurplePrimary.copy(alpha = 0.45f),
                                                    NeonPurpleSecondary.copy(alpha = 0.35f)
                                                )
                                            )
                                        } else {
                                            Brush.verticalGradient(
                                                listOf(DeepSlateCard, DeepSlateCard)
                                            )
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) NeonPurpleGlow else DeepSlateElevated,
                                        shape = RoundedCornerShape(12.dp)
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
                                    style = MaterialTheme.typography.titleSmall.copy(
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
            }

            // Ajuste decimal fino (.0, .25, .5, .75) quando nota entre 0 e 9 — sem quebrar linha
            if (baseIntRating != null && baseIntRating in 0..9) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Precisão da nota:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.0f, 0.25f, 0.5f, 0.75f).forEach { fraction ->
                            val candidate = baseIntRating + fraction
                            val isExactSelected = selectedRating == candidate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isExactSelected) HallOfFameGold.copy(alpha = 0.22f) else DeepSlateCard
                                    )
                                    .border(
                                        width = if (isExactSelected) 1.5.dp else 1.dp,
                                        color = if (isExactSelected) HallOfFameGold else DeepSlateElevated,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedRating = candidate
                                        onUpdateRatingAndMemory(game.id, candidate, memoryText, isPersonalGoty)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = formatRatingNumber(candidate),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (isExactSelected) HallOfFameGold else TextSecondary,
                                        fontWeight = if (isExactSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Botão Dourado Especial: 11/10 • Jogo da Vida (Hall da Fama)
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
                    text = "🏆 11/10 • Jogo da Vida",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = HallOfFameGold,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. Card Memória Afetiva + Jogo do Ano Pessoal (Menu Colapsável)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1E1912))
                .border(1.3.dp, HallOfFameGold, RoundedCornerShape(22.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isMemoryExpanded = !isMemoryExpanded }
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💛 Memória Afetiva",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    val summaryNote = memoryText.trim()
                    Text(
                        text = if (summaryNote.isNotEmpty()) {
                            "\"$summaryNote\""
                        } else {
                            "Toque para registrar por que este jogo marcou sua vida..."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (summaryNote.isNotEmpty()) HallOfFameGold else TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DeepSlateCard)
                        .border(1.dp, HallOfFameGold.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMemoryExpanded) AppIcons.ChevronUp else AppIcons.ChevronDown,
                        contentDescription = if (isMemoryExpanded) "Recolher" else "Expandir",
                        tint = HallOfFameGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isMemoryExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = memoryText,
                        onValueChange = { memoryText = it },
                        placeholder = {
                            Text(
                                text = "Registre a memória afetiva dessa jornada (opcional)...",
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = "🏆 Jogo do Ano Pessoal",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = HallOfFameGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Destaca como seu grande vencedor do ano",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onUpdateRatingAndMemory(game.id, selectedRating, memoryText, isPersonalGoty)
                                isMemoryExpanded = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HallOfFameGold),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Salvar Memória",
                                color = Color(0xFF261A00),
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 5. Botão Principal Inferior: Marcar como Zerado (visível apenas quando ainda não está zerado)
        if (game.status != GameStatus.COMPLETED) {
            Button(
                onClick = { onMarkAsCompleted(game) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(vertical = 15.dp, horizontal = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✓ Marcar como Zerado 🏆",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF140526),
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
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

private fun formatRatingNumber(rating: Float): String {
    return if (rating % 1.0f == 0f) {
        rating.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", rating)
            .trimEnd('0')
            .trimEnd('.')
            .replace('.', ',')
    }
}

private fun formatRatingBadge(rating: Float?): String {
    if (rating == null) return "Sem nota"
    if (rating >= 11.0f) return "🏆 11/10"
    return "★ ${formatRatingNumber(rating)}/10"
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
