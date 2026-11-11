package io.github.carvalhoxx.gamevault.ui.screens.dashboard

import androidx.compose.foundation.Image
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.carvalhoxx.gamevault.R
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.DurationResolution
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import io.github.carvalhoxx.gamevault.ui.components.AppIcons
import io.github.carvalhoxx.gamevault.ui.components.GameCoverArtwork
import io.github.carvalhoxx.gamevault.ui.components.GameFormDialog
import io.github.carvalhoxx.gamevault.ui.components.ManualTimePickerDialog
import io.github.carvalhoxx.gamevault.ui.theme.AmberBrass
import io.github.carvalhoxx.gamevault.ui.theme.AshGrey
import io.github.carvalhoxx.gamevault.ui.theme.BoneCream
import io.github.carvalhoxx.gamevault.ui.theme.CarbonBorderHairline
import io.github.carvalhoxx.gamevault.ui.theme.CarbonBorderSubtle
import io.github.carvalhoxx.gamevault.ui.theme.CarbonElevated
import io.github.carvalhoxx.gamevault.ui.theme.CarbonMatteBackground
import io.github.carvalhoxx.gamevault.ui.theme.EmberOrangePrimary
import io.github.carvalhoxx.gamevault.ui.theme.SageClear
import io.github.carvalhoxx.gamevault.ui.theme.SlateMuted
import io.github.carvalhoxx.gamevault.ui.viewmodel.DashboardUiState
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNavigateToLibrary: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenGameDetails: (Long) -> Unit,
    onToggleSessionTimer: (GameEntity) -> Unit,
    onUpdateExactTime: (gameId: Long, hours: Long, minutes: Long, seconds: Long) -> Unit,
    onSaveNewGame: (GameEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var manualTimeTargetGame by remember { mutableStateOf<GameEntity?>(null) }
    var showAddGameDialog by remember { mutableStateOf(false) }

    val currentYear = uiState.currentYear

    // Filtra estritamente os jogos zerados no ano vigente (completionDate dentro de currentYear)
    val completedThisYearCount = remember(uiState.completedThisYearGames, uiState.allGames, currentYear) {
        if (uiState.completedThisYearGames.isNotEmpty()) {
            uiState.completedThisYearGames.size
        } else {
            uiState.allGames.count { game ->
                game.status == GameStatus.COMPLETED &&
                    TimeFormatUtils.isTimestampInYear(game.completionDate, currentYear)
            }
        }
    }

    val backlogCount = remember(uiState.allGames) {
        uiState.allGames.count { it.status == GameStatus.BACKLOG && it.isOwned }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonMatteBackground),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 1. Edge-to-Edge Backdrop + Cabeçalho + Jogando Agora (Sem cards flutuantes)
        item {
            val mainGame = uiState.mainPlayingGame
            if (mainGame != null) {
                ConsolePlayingHeroSection(
                    game = mainGame,
                    nowMillis = uiState.nowMillis,
                    userName = uiState.userName,
                    onAddGameClick = { showAddGameDialog = true },
                    onNavigateToProfile = onNavigateToProfile,
                    onCardClick = { onOpenGameDetails(mainGame.id) },
                    onToggleTimer = { onToggleSessionTimer(mainGame) },
                    onOpenManualInput = { manualTimeTargetGame = mainGame }
                )
            } else {
                ConsoleEmptyPlayingHeroSection(
                    userName = uiState.userName,
                    onAddGameClick = { showAddGameDialog = true },
                    onOpenLibraryClick = onNavigateToLibrary,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }

        // 2. Faixa de Metas e Fila (Sem caixas, dividida por 1px #1F2229)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            UnboxedGoalsAndQueueStrip(
                completedCount = completedThisYearCount,
                annualGoal = 20,
                currentYear = currentYear,
                backlogCount = backlogCount,
                onOpenLibrary = onNavigateToLibrary
            )
        }

        // 3. Próximos Lançamentos (Pôsteres 2:3 de 174x252dp + Placeholder Ember Orange)
        item {
            Spacer(modifier = Modifier.height(26.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                ConsoleSectionHeader(
                    title = "Próximos Lançamentos",
                    actionText = "Ver agenda →",
                    onActionClick = onNavigateToPlanner
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.dayOneGames.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.dayOneGames, key = { it.id }) { dayOneGame ->
                            UpcomingReleasePosterItem(
                                game = dayOneGame,
                                nowMillis = uiState.nowMillis,
                                onClick = { onOpenGameDetails(dayOneGame.id) }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddGameDialog = true }
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Nenhum lançamento monitorado ainda",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = BoneCream,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Toque para cadastrar estreias aguardadas (ex: Grand Theft Auto VI, Death Stranding 2) com suporte a duração a definir.",
                            style = MaterialTheme.typography.bodySmall.copy(color = AshGrey)
                        )
                    }
                }
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
private fun ConsoleTopBar(
    userName: String,
    onAddGameClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sidequest_logo),
                contentDescription = "SideQuest Logo",
                modifier = Modifier.size(44.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SIDE",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = BoneCream,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "QUEST",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = EmberOrangePrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CarbonElevated)
                    .border(1.dp, CarbonBorderSubtle, CircleShape)
                    .clickable(onClick = onNavigateToProfile),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "C",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = BoneCream,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            IconButton(
                onClick = onAddGameClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CarbonElevated)
                    .border(1.dp, EmberOrangePrimary.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = "Adicionar jogo",
                    tint = BoneCream,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ConsolePlayingHeroSection(
    game: GameEntity,
    nowMillis: Long,
    userName: String,
    onAddGameClick: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onCardClick: () -> Unit,
    onToggleTimer: () -> Unit,
    onOpenManualInput: () -> Unit
) {
    val heroContent = consolePlayingHeroContent(game, nowMillis)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2E1B16),
                        Color(0xFF1B1618),
                        CarbonMatteBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            ConsoleTopBar(
                userName = userName,
                onAddGameClick = onAddGameClick,
                onNavigateToProfile = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(22.dp))

            ConsoleHeroGameSummary(
                game = game,
                isTimerRunning = heroContent.isTimerRunning,
                metaLine = heroContent.metaLine,
                onCardClick = onCardClick
            )

            Spacer(modifier = Modifier.height(22.dp))

            ActiveTimerSection(
                formattedStopwatch = heroContent.formattedStopwatch,
                hltbEstimateLabel = heroContent.hltbEstimateLabel
            )

            Spacer(modifier = Modifier.height(10.dp))

            HeroProgressSection(
                fillFraction = heroContent.fillFraction,
                isBeyondHltb = heroContent.isBeyondHltb,
                progressHumanSubtext = heroContent.progressHumanSubtext
            )

            Spacer(modifier = Modifier.height(18.dp))

            HeroSessionActions(
                isTimerRunning = heroContent.isTimerRunning,
                onToggleTimer = onToggleTimer,
                onOpenManualInput = onOpenManualInput
            )
        }
    }
}

private data class ConsolePlayingHeroContent(
    val isTimerRunning: Boolean,
    val formattedStopwatch: String,
    val hltbEstimateLabel: String,
    val fillFraction: Float,
    val isBeyondHltb: Boolean,
    val progressHumanSubtext: String,
    val metaLine: String
)

private fun consolePlayingHeroContent(
    game: GameEntity,
    nowMillis: Long
): ConsolePlayingHeroContent {
    val liveTotalSeconds = game.getLiveTimePlayedSeconds(nowMillis)
    val isTimerRunning = game.activeTimerStartTime != null
    val hltbTargetSeconds = game.getEffectiveDurationSeconds()
    val isBeyondHltb = hltbTargetSeconds != null && liveTotalSeconds > hltbTargetSeconds
    val formattedStopwatch = String.format(
        Locale.US,
        "%02d:%02d:%02d",
        liveTotalSeconds / 3600L,
        (liveTotalSeconds % 3600L) / 60L,
        liveTotalSeconds % 60L
    )
    val hltbEstimateLabel = if (hltbTargetSeconds != null && hltbTargetSeconds > 0L) {
        "de ${hltbTargetSeconds / 3600L}h estimadas"
    } else {
        "duração a definir"
    }
    val fillFraction = when {
        hltbTargetSeconds == null || hltbTargetSeconds <= 0L -> 0.35f
        liveTotalSeconds >= hltbTargetSeconds -> 1f
        else -> (liveTotalSeconds.toFloat() / hltbTargetSeconds.toFloat()).coerceIn(0.04f, 1f)
    }
    val progressHumanSubtext = when {
        hltbTargetSeconds == null || hltbTargetSeconds <= 0L ->
            "No seu próprio ritmo • Duração oficial ainda não definida"
        isBeyondHltb -> {
            val extraHours = (liveTotalSeconds - hltbTargetSeconds) / 3600L
            "Explorando além da média (+${extraHours}h)"
        }
        else -> {
            val remainingHours = ((hltbTargetSeconds - liveTotalSeconds) / 3600L).coerceAtLeast(1L)
            val pct = ((liveTotalSeconds * 100L) / hltbTargetSeconds).coerceIn(1L, 99L)
            "Restam ~${remainingHours}h para a média da campanha • ${pct}%"
        }
    }
    val genreFirst = game.genres.split(",").firstOrNull()?.trim().orEmpty()
    val metaLine = buildString {
        append(game.platform)
        if (genreFirst.isNotBlank()) {
            append(" • ")
            append(genreFirst)
        }
        append(" • ")
        append(game.targetCompletionStyle.shortLabel)
    }
    return ConsolePlayingHeroContent(
        isTimerRunning = isTimerRunning,
        formattedStopwatch = formattedStopwatch,
        hltbEstimateLabel = hltbEstimateLabel,
        fillFraction = fillFraction,
        isBeyondHltb = isBeyondHltb,
        progressHumanSubtext = progressHumanSubtext,
        metaLine = metaLine
    )
}

@Composable
private fun ConsoleHeroGameSummary(
    game: GameEntity,
    isTimerRunning: Boolean,
    metaLine: String,
    onCardClick: () -> Unit
) {
    // Pôster 2:3 (104x156dp) + Metadados limpos ao lado
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        GameCoverArtwork(
            title = game.title,
            coverUrl = game.coverUrl,
            isHallOfFame = game.isHallOfFame,
            modifier = Modifier
                .width(104.dp)
                .height(156.dp),
            cornerRadius = 10.dp
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isTimerRunning) SageClear else EmberOrangePrimary)
                )
                Text(
                    text = if (isTimerRunning) "SESSÃO AO VIVO" else "JOGANDO AGORA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isTimerRunning) SageClear else EmberOrangePrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                )
            }
            Text(
                text = game.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = BoneCream,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 28.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = AshGrey,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveTimerSection(
    formattedStopwatch: String,
    hltbEstimateLabel: String
) {
    // Cronômetro Manual + Referência HLTB em destaque hierárquico direto no fundo
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = formattedStopwatch,
            style = MaterialTheme.typography.headlineLarge.copy(
                color = BoneCream,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                letterSpacing = (-0.5).sp
            )
        )
        Text(
            text = hltbEstimateLabel,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AshGrey,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 5.dp)
        )
    }
}

@Composable
private fun HeroProgressSection(
    fillFraction: Float,
    isBeyondHltb: Boolean,
    progressHumanSubtext: String
) {
    // Barra de progresso fina de 5dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(50))
            .background(CarbonBorderHairline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fillFraction)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (isBeyondHltb) {
                        Brush.horizontalGradient(listOf(EmberOrangePrimary, AmberBrass))
                    } else {
                        Brush.horizontalGradient(listOf(EmberOrangePrimary, EmberOrangePrimary))
                    }
                )
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = progressHumanSubtext,
        style = MaterialTheme.typography.bodySmall.copy(color = AshGrey)
    )
}

@Composable
private fun HeroSessionActions(
    isTimerRunning: Boolean,
    onToggleTimer: () -> Unit,
    onOpenManualInput: () -> Unit
) {
    // Identidade Tátil: ▶ CONTINUAR SESSÃO (Ember Orange, 52dp) + Tempo manual (52dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onToggleTimer,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTimerRunning) AmberBrass else EmberOrangePrimary
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        ) {
            Text(
                text = if (isTimerRunning) "⏹ PARAR SESSÃO" else "▶ CONTINUAR SESSÃO",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = CarbonMatteBackground,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            color = CarbonElevated,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .width(134.dp)
                .height(52.dp)
                .border(1.dp, CarbonBorderSubtle, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenManualInput)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Tempo manual",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = BoneCream,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun ConsoleEmptyPlayingHeroSection(
    userName: String,
    onAddGameClick: () -> Unit,
    onOpenLibraryClick: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF261713),
                        CarbonMatteBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ConsoleTopBar(
                userName = userName,
                onAddGameClick = onAddGameClick,
                onNavigateToProfile = onNavigateToProfile
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "JOGANDO AGORA",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = EmberOrangePrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            )
            Text(
                text = "Nenhuma campanha ativa no momento",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = BoneCream,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                text = "Escolha um jogo da sua fila ou cadastre um novo título para acompanhar suas sessões.",
                style = MaterialTheme.typography.bodyMedium.copy(color = AshGrey)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onAddGameClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmberOrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = "+ Novo Jogo",
                        color = CarbonMatteBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    color = CarbonElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .border(1.dp, CarbonBorderSubtle, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenLibraryClick)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    ) {
                        Text(
                            text = "Abrir Biblioteca",
                            color = BoneCream,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnboxedGoalsAndQueueStrip(
    completedCount: Int,
    annualGoal: Int,
    currentYear: Int,
    backlogCount: Int,
    onOpenLibrary: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        HorizontalDivider(thickness = 1.dp, color = CarbonBorderHairline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$completedCount",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = BoneCream,
                            fontWeight = FontWeight.Black
                        )
                    )
                    Text(
                        text = "de $annualGoal",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AshGrey,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = "Zerados em $currentYear",
                    style = MaterialTheme.typography.bodySmall.copy(color = AshGrey)
                )
            }

            VerticalDivider(
                modifier = Modifier.height(36.dp),
                thickness = 1.dp,
                color = CarbonBorderHairline
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "$backlogCount jogos",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = BoneCream,
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    text = "Na sua fila",
                    style = MaterialTheme.typography.bodySmall.copy(color = AshGrey)
                )
            }

            VerticalDivider(
                modifier = Modifier.height(36.dp),
                thickness = 1.dp,
                color = CarbonBorderHairline
            )

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenLibrary)
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Em dúvida?",
                    style = MaterialTheme.typography.bodySmall.copy(color = AshGrey)
                )
                Text(
                    text = "Sortear fila →",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = EmberOrangePrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = CarbonBorderHairline)
    }
}

@Composable
private fun ConsoleSectionHeader(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = BoneCream,
                fontWeight = FontWeight.ExtraBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Text(
            text = actionText,
            style = MaterialTheme.typography.labelLarge.copy(
                color = EmberOrangePrimary,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onActionClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun UpcomingReleasePosterItem(
    game: GameEntity,
    nowMillis: Long,
    onClick: () -> Unit
) {
    val release = game.releaseDate
    val isUnder24Hours = release != null && release > nowMillis &&
        (release - nowMillis) < (24L * 60L * 60L * 1000L)

    val badgeText = remember(release, nowMillis) {
        TimeFormatUtils.formatReleaseCountdownBadge(release, nowMillis)
    }

    val durationHumanText = when (val res = game.resolveDuration()) {
        is DurationResolution.DurationTbdLock -> "${game.platform} • Duração a definir"
        is DurationResolution.Resolved -> {
            val hours = (res.durationSeconds / 3600L).coerceAtLeast(1L)
            "${game.platform} • Média: ~${hours}h"
        }
    }

    val hasOfficialCover = !game.coverUrl.isNullOrBlank()

    Column(
        modifier = Modifier
            .width(174.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pôster 2:3 (174x252dp) com Placeholder elegante em Ember Orange quando sem coverUrl
        Box(
            modifier = Modifier
                .width(174.dp)
                .height(252.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (hasOfficialCover) {
                GameCoverArtwork(
                    title = game.title,
                    coverUrl = game.coverUrl,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 12.dp
                )
            } else {
                // Placeholder elegante na cor Ember Orange para jogos ainda sem coverUrl (ex: GTA VI)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    EmberOrangePrimary,
                                    Color(0xFF8C2A12),
                                    Color(0xFF261410)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            EmberOrangePrimary.copy(alpha = 0.45f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = game.title
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() },
                        style = MaterialTheme.typography.displaySmall.copy(
                            color = BoneCream.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Text(
                        text = "ARTE OFICIAL PENDENTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BoneCream.copy(alpha = 0.72f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }

            // Badge superior de data/contagem regressiva (destaca em laranja quando faltar < 24h)
            Surface(
                color = if (isUnder24Hours) {
                    EmberOrangePrimary.copy(alpha = 0.94f)
                } else {
                    CarbonMatteBackground.copy(alpha = 0.88f)
                },
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isUnder24Hours) CarbonMatteBackground else BoneCream,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = if (isUnder24Hours) FontFamily.Monospace else FontFamily.Default,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Text(
            text = game.title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = BoneCream,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = durationHumanText,
            style = MaterialTheme.typography.bodySmall.copy(
                color = SlateMuted,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
