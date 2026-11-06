package io.github.carvalhoxx.gamevault.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.DurationResolution
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
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
import io.github.carvalhoxx.gamevault.ui.viewmodel.DashboardUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onOpenGameDetails: (Long) -> Unit,
    onToggleSessionTimer: (GameEntity) -> Unit,
    onUpdateExactTime: (gameId: Long, hours: Long, minutes: Long, seconds: Long) -> Unit,
    onSaveNewGame: (GameEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualTimeTargetGame by remember { mutableStateOf<GameEntity?>(null) }
    var showAddGameDialog by remember { mutableStateOf(false) }

    val formattedToday = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale.forLanguageTag("pt-BR"))
            .format(Date())
            .replaceFirstChar { it.uppercase() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlateBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Cabeçalho SIDEQUEST (01_dashboard_home.png)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🎮  S I D E Q U E S T",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = NeonPurplePrimary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.6.sp
                        )
                    )
                    Text(
                        text = "Your next chapter.",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = formattedToday,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DeepSlateCard)
                            .border(1.5.dp, NeonPurplePrimary, CircleShape)
                            .clickable { showAddGameDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = NeonPurpleGlow,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // 2. Seção Currently Playing (Card Hero com Cronômetro HH:MM:SS e Comparador Flexível)
        item {
            SectionHeader(
                title = "Currently Playing",
                actionText = "Library →",
                onActionClick = onNavigateToLibrary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val mainGame = uiState.mainPlayingGame
            if (mainGame != null) {
                CurrentlyPlayingHeroCard(
                    game = mainGame,
                    nowMillis = uiState.nowMillis,
                    onCardClick = { onOpenGameDetails(mainGame.id) },
                    onToggleTimer = { onToggleSessionTimer(mainGame) },
                    onOpenManualInput = { manualTimeTargetGame = mainGame }
                )
            } else {
                EmptyPlayingHeroCard(
                    onAddGameClick = { showAddGameDialog = true },
                    onOpenLibraryClick = onNavigateToLibrary
                )
            }
        }

        // 3. Seção Day One Radar
        item {
            SectionHeader(
                title = "Day One Radar",
                subtitle = "Big releases. A little room to plan.",
                actionText = "Planner →",
                onActionClick = onNavigateToPlanner
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.dayOneGames.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.dayOneGames, key = { it.id }) { dayOneGame ->
                        DayOneRadarCard(
                            game = dayOneGame,
                            nowMillis = uiState.nowMillis,
                            onClick = { onOpenGameDetails(dayOneGame.id) }
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DeepSlateSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DeepSlateElevated, RoundedCornerShape(18.dp))
                        .clickable { showAddGameDialog = true }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nenhum lançamento Day One monitorado ainda",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Toque para cadastrar jogos aguardados (ex: GTA VI, Ghost of Yōtei) — suporte total a duração TBD.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 4. Seção Hall of Fame (11/10 Life-Changing Games) & Personal GOTY
        item {
            SectionHeader(
                title = "Hall of Fame",
                subtitle = "11/10 Life-Changing Games",
                titleColor = HallOfFameGold,
                actionText = "View all →",
                onActionClick = onNavigateToLibrary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.hallOfFameGames.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.hallOfFameGames, key = { it.id }) { hofGame ->
                        HallOfFameCard(
                            game = hofGame,
                            onClick = { onOpenGameDetails(hofGame.id) }
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DeepSlateSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, HallOfFameGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✨ Sua Galeria Dourada 11/10",
                            style = MaterialTheme.typography.titleSmall,
                            color = HallOfFameGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Avalie um jogo com a nota especial 11/10 (Life-Changing) nos detalhes para imortalizá-lo aqui.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            val featuredGoty = uiState.personalGotyGames.firstOrNull()
                ?: uiState.hallOfFameGames.firstOrNull()
            if (featuredGoty != null) {
                Spacer(modifier = Modifier.height(14.dp))
                PersonalGotyBannerCard(
                    game = featuredGoty,
                    onClick = { onOpenGameDetails(featuredGoty.id) }
                )
            }
        }
    }

    manualTimeTargetGame?.let { targetGame ->
        ManualTimePickerDialog(
            gameTitle = targetGame.title,
            initialSeconds = targetGame.getLiveTimePlayedSeconds(uiState.nowMillis),
            onDismiss = { manualTimeTargetGame = null },
            onConfirm = { h, m, s ->
                onUpdateExactTime(targetGame.id, h, m, s)
                manualTimeTargetGame = null
            }
        )
    }

    if (showAddGameDialog) {
        GameFormDialog(
            onDismiss = { showAddGameDialog = false },
            onSave = { newGame ->
                onSaveNewGame(newGame)
                showAddGameDialog = false
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    titleColor: Color = TextPrimary,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = titleColor,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
        Text(
            text = actionText,
            style = MaterialTheme.typography.labelLarge.copy(
                color = NeonPurplePrimary,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onActionClick)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CurrentlyPlayingHeroCard(
    game: GameEntity,
    nowMillis: Long,
    onCardClick: () -> Unit,
    onToggleTimer: () -> Unit,
    onOpenManualInput: () -> Unit
) {
    val liveTotalSeconds = game.getLiveTimePlayedSeconds(nowMillis)
    val isTimerRunning = game.activeTimerStartTime != null
    val currentSessionSeconds = if (isTimerRunning) {
        ((nowMillis - (game.activeTimerStartTime ?: nowMillis)) / 1000L).coerceAtLeast(0L)
    } else {
        0L
    }

    val hltbTargetSeconds = game.getEffectiveDurationSeconds()
    val isBeyondHltb = hltbTargetSeconds != null && liveTotalSeconds > hltbTargetSeconds
    val extraSecondsBeyond = if (isBeyondHltb) {
        liveTotalSeconds - hltbTargetSeconds
    } else {
        0L
    }

    val paceLabel = when {
        hltbTargetSeconds == null -> "On your own pace • HLTB TBD"
        isBeyondHltb -> {
            val extraH = extraSecondsBeyond / 3600L
            val extraM = (extraSecondsBeyond % 3600L) / 60L
            "Explorando além da média (+${extraH}h ${extraM}m)"
        }
        else -> "On your own pace"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF281C40),
                        Color(0xFF191528),
                        DeepSlateSurface
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (isTimerRunning) NeonPurplePrimary else DeepSlateElevated,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onCardClick)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Badge topo: IN PROGRESS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = NeonPurplePrimary.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isTimerRunning) "● LIVE SESSION ACTIVE" else "▷ IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonPurpleGlow,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = "Priority ${game.priority.badge}",
                    style = MaterialTheme.typography.labelSmall.copy(color = HallOfFameGold)
                )
            }

            // Título + Plataforma/Gênero/Meta
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = game.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = HallOfFameGold,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallTagPill(text = game.platform, backgroundColor = DeepSlateElevated)
                    if (game.genres.isNotBlank()) {
                        SmallTagPill(
                            text = game.genres.split(",").first().trim(),
                            backgroundColor = DeepSlateElevated
                        )
                    }
                    SmallTagPill(
                        text = "${game.targetCompletionStyle.shortLabel} Goal",
                        backgroundColor = NeonPurpleSecondary.copy(alpha = 0.28f),
                        textColor = NeonPurpleGlow
                    )
                }
            }

            // Comparador Flexível de Tempo (Sem Barra Rígida que quebra acima de 100%)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val h = liveTotalSeconds / 3600L
                    val m = (liveTotalSeconds % 3600L) / 60L
                    val s = liveTotalSeconds % 60L
                    Text(
                        text = "${h}h ${String.format("%02d", m)}m ${String.format("%02d", s)}s",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    SmallTagPill(
                        text = paceLabel,
                        backgroundColor = if (isBeyondHltb) {
                            HallOfFameGold.copy(alpha = 0.2f)
                        } else {
                            NeonPurplePrimary.copy(alpha = 0.2f)
                        },
                        textColor = if (isBeyondHltb) HallOfFameGold else NeonPurpleGlow
                    )
                }

                // Indicador visual suave (Se passar de 100% do HLTB, brilha em dourado/roxo sem quebrar!)
                val fillFraction = when {
                    hltbTargetSeconds == null || hltbTargetSeconds <= 0L -> 0.35f
                    liveTotalSeconds >= hltbTargetSeconds -> 1f
                    else -> (liveTotalSeconds.toFloat() / hltbTargetSeconds.toFloat()).coerceIn(0.05f, 1f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(DeepSlateElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isBeyondHltb) {
                                    Brush.horizontalGradient(listOf(NeonPurplePrimary, HallOfFameGold))
                                } else {
                                    Brush.horizontalGradient(listOf(NeonPurpleSecondary, NeonPurpleGlow))
                                }
                            )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallTagPill(
                        text = TimeFormatUtils.formatSecondsToHhMmSs(liveTotalSeconds),
                        backgroundColor = DeepSlateElevated,
                        textColor = TextPrimary
                    )
                    Text(
                        text = "vs",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                    SmallTagPill(
                        text = "HLTB Avg: ${TimeFormatUtils.formatHltbReferenceOrTbd(hltbTargetSeconds)}",
                        backgroundColor = NeonPurplePrimary.copy(alpha = 0.18f),
                        textColor = NeonPurpleGlow
                    )
                }
            }

            // Botão Live Session Timer + Botão Input Manual Exato (✎)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleTimer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTimerRunning) HallOfFameGold else NeonPurplePrimary
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val sessionFormatted = TimeFormatUtils.formatSecondsToHhMmSs(currentSessionSeconds)
                    Text(
                        text = if (isTimerRunning) {
                            "⏹ Stop Session • $sessionFormatted"
                        } else {
                            "▶ Start Session Timer • 00:00:00"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF140526),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                IconButton(
                    onClick = onOpenManualInput,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DeepSlateElevated)
                        .border(1.dp, NeonPurplePrimary.copy(alpha = 0.5f), CircleShape)
                ) {
                    Text("✎", color = TextPrimary, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyPlayingHeroCard(
    onAddGameClick: () -> Unit,
    onOpenLibraryClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = DeepSlateSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonPurplePrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nenhum jogo em andamento no momento",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Adicione um jogo como 'Jogando' ou abra a Biblioteca para importar a planilha CSV ou girar a Roleta do Backlog.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddGameClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("+ Novo Jogo", color = Color(0xFF140526), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onOpenLibraryClick,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepSlateElevated),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Abrir Biblioteca", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun DayOneRadarCard(
    game: GameEntity,
    nowMillis: Long,
    onClick: () -> Unit
) {
    val countdownText = remember(game.releaseDate, nowMillis) {
        val release = game.releaseDate
        if (release == null || release <= 0L) {
            "🕒 Data TBD"
        } else {
            val diffMillis = (release - nowMillis).coerceAtLeast(0L)
            val days = diffMillis / (1000L * 60L * 60L * 24L)
            val hours = (diffMillis / (1000L * 60L * 60L)) % 24L
            "🕒 ${days}d : ${String.format("%02d", hours)}h"
        }
    }

    val durationResolution = game.resolveDuration()

    Column(
        modifier = Modifier
            .width(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DeepSlateSurface)
            .border(1.dp, DeepSlateElevated, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            GameCoverArtwork(
                title = game.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp),
                cornerRadius = 14.dp
            )
            Surface(
                color = Color.Black.copy(alpha = 0.68f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = NeonPurpleGlow,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Text(
            text = game.title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = game.platform,
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )

        when (durationResolution) {
            is DurationResolution.DurationTbdLock -> {
                Surface(
                    color = NeonPurplePrimary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "⚠ Duration TBD • Schedule Lock on Release",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonPurpleGlow,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
            is DurationResolution.Resolved -> {
                val hours = durationResolution.durationSeconds / 3600L
                Surface(
                    color = HallOfFameGold.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "📅 Space in your schedule (~${hours}h)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = HallOfFameGold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HallOfFameCard(
    game: GameEntity,
    onClick: () -> Unit
) {
    val hoursSpent = (game.timePlayedSeconds / 3600L).coerceAtLeast(1L)
    val completedDateStr = TimeFormatUtils.formatDateOrTbd(game.completionDate)

    Column(
        modifier = Modifier
            .width(265.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2B2212),
                        DeepSlateSurface
                    )
                )
            )
            .border(1.2.dp, HallOfFameGold, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = HallOfFameGold,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = "✨ 11/10",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF261A00),
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Text(
            text = game.title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = HallOfFameGold,
                fontWeight = FontWeight.ExtraBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "\"${game.coreMemoryNote ?: "Um jogo transformador que marcou minha história."}\"",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextPrimary.copy(alpha = 0.9f),
                fontStyle = FontStyle.Italic
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "FINISHED $completedDateStr • ${hoursSpent}H WELL SPENT",
            style = MaterialTheme.typography.labelSmall.copy(
                color = HallOfFameGold.copy(alpha = 0.85f),
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
            )
        )
    }
}

@Composable
private fun PersonalGotyBannerCard(
    game: GameEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E1A14))
            .border(1.dp, HallOfFameGold.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GameCoverArtwork(
            title = game.title,
            isHallOfFame = true,
            modifier = Modifier.size(64.dp),
            cornerRadius = 12.dp
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "PERSONAL GOTY",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = HallOfFameGold,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
            )
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.coreMemoryNote ?: "Eleito o melhor jogo do ano na sua jornada.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text("🏆", fontSize = 26.sp)
    }
}

@Composable
private fun SmallTagPill(
    text: String,
    backgroundColor: Color,
    textColor: Color = TextSecondary
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
