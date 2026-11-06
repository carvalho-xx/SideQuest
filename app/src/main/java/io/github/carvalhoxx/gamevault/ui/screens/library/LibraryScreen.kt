package io.github.carvalhoxx.gamevault.ui.screens.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.carvalhoxx.gamevault.data.local.entity.GameEntity
import io.github.carvalhoxx.gamevault.domain.model.GameStatus
import io.github.carvalhoxx.gamevault.domain.model.PriorityTier
import io.github.carvalhoxx.gamevault.domain.util.TimeFormatUtils
import io.github.carvalhoxx.gamevault.ui.components.AppIcons
import io.github.carvalhoxx.gamevault.ui.components.BacklogRouletteDialog
import io.github.carvalhoxx.gamevault.ui.components.GameCoverArtwork
import io.github.carvalhoxx.gamevault.ui.components.GameFormDialog
import io.github.carvalhoxx.gamevault.ui.theme.DealEmeraldGreen
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
import io.github.carvalhoxx.gamevault.ui.viewmodel.LibraryFilterState
import java.util.Locale

@Composable
fun LibraryScreen(
    allGames: List<GameEntity>,
    filteredGames: List<GameEntity>,
    filterState: LibraryFilterState,
    rouletteWinner: GameEntity?,
    nowMillis: Long,
    onSearchQueryChange: (String) -> Unit,
    onSelectStatusTab: (GameStatus?) -> Unit,
    onSelectPlatform: (String?) -> Unit,
    onSelectPriority: (PriorityTier?) -> Unit,
    onSpinRoulette: () -> Unit,
    onDismissRoulette: () -> Unit,
    onStartPlayingFromRoulette: (GameEntity) -> Unit,
    onOpenGameDetails: (Long) -> Unit,
    onSaveGame: (GameEntity) -> Unit,
    onImportCsvUri: (Uri) -> Unit,
    onSyncMissingMetadata: () -> Unit = {},
    isSyncingLibrary: Boolean = false,
    syncProgressLabel: String? = null,
    modifier: Modifier = Modifier
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showAddGameDialog by remember { mutableStateOf(false) }

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportCsvUri(uri)
        }
    }

    val availablePlatforms = remember(allGames) {
        allGames.map { it.platform.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Barra de Busca + Botão de Filtros com ícones vetoriais padronizados
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Buscar na biblioteca e fila...",
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = AppIcons.Search,
                        contentDescription = "Buscar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DeepSlateSurface,
                    unfocusedContainerColor = DeepSlateSurface,
                    focusedBorderColor = NeonPurplePrimary,
                    unfocusedBorderColor = DeepSlateElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showAdvancedFilters = !showAdvancedFilters },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (showAdvancedFilters) NeonPurplePrimary.copy(alpha = 0.25f) else DeepSlateCard)
                    .border(
                        1.dp,
                        if (showAdvancedFilters) NeonPurplePrimary else DeepSlateElevated,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = AppIcons.FilterTune,
                    contentDescription = "Filtros",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = { showAddGameDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DeepSlateCard)
                    .border(1.5.dp, NeonPurplePrimary, CircleShape)
            ) {
                Icon(
                    imageVector = AppIcons.Add,
                    contentDescription = "Adicionar jogo",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // 2. Linha de Ações: Importador CSV + Sincronizar Online + Roleta da Fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    csvPickerLauncher.launch(
                        arrayOf("text/csv", "text/comma-separated-values", "text/*", "*/*")
                    )
                },
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "📥 Importar CSV",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = NeonPurpleGlow,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            IconButton(
                onClick = onSyncMissingMetadata,
                enabled = !isSyncingLibrary,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isSyncingLibrary) NeonPurplePrimary.copy(alpha = 0.25f) else DeepSlateCard)
                    .border(
                        1.dp,
                        if (isSyncingLibrary) NeonPurplePrimary else DeepSlateElevated,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = AppIcons.Sync,
                    contentDescription = "Sincronizar capas, durações e gêneros",
                    tint = if (isSyncingLibrary) NeonPurpleGlow else TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Button(
                onClick = onSpinRoulette,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "🎲 Roleta da Fila",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFF140526),
                        fontWeight = FontWeight.ExtraBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!syncProgressLabel.isNullOrBlank()) {
            Surface(
                color = NeonPurplePrimary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonPurplePrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "🔄 $syncProgressLabel",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = NeonPurpleGlow,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // Painel expansível de filtros por Plataforma e Prioridade
        if (showAdvancedFilters) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DeepSlateSurface)
                    .border(1.dp, DeepSlateElevated, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtros de Plataforma e Prioridade",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (filterState.selectedPriority != null || filterState.selectedPlatform != null) {
                        Text(
                            text = "Limpar filtros",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = NeonPurpleGlow,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable {
                                onSelectPriority(null)
                                onSelectPlatform(null)
                            }
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filterState.selectedPriority == null,
                            onClick = { onSelectPriority(null) },
                            label = { Text("Todas Prioridades") }
                        )
                    }
                    items(PriorityTier.entries) { tier ->
                        FilterChip(
                            selected = filterState.selectedPriority == tier,
                            onClick = {
                                onSelectPriority(
                                    if (filterState.selectedPriority == tier) null else tier
                                )
                            },
                            label = { Text("Nível ${tier.badge}") }
                        )
                    }
                }

                if (availablePlatforms.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = filterState.selectedPlatform == null,
                                onClick = { onSelectPlatform(null) },
                                label = { Text("Todas Plataformas") }
                            )
                        }
                        items(availablePlatforms) { platform ->
                            FilterChip(
                                selected = filterState.selectedPlatform.equals(platform, ignoreCase = true),
                                onClick = {
                                    onSelectPlatform(
                                        if (filterState.selectedPlatform.equals(platform, ignoreCase = true)) {
                                            null
                                        } else {
                                            platform
                                        }
                                    )
                                },
                                label = { Text(platform) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Abas de Status com Contadores Reais em Português
        val statusCounts = remember(allGames) {
            allGames.groupingBy { it.status }.eachCount()
        }
        val statusTabs = listOf(
            null to "Todos (${allGames.size})",
            GameStatus.PLAYING to "Jogando (${statusCounts[GameStatus.PLAYING] ?: 0})",
            GameStatus.BACKLOG to "Na Fila (${statusCounts[GameStatus.BACKLOG] ?: 0})",
            GameStatus.DAY_ONE to "Lançamento (${statusCounts[GameStatus.DAY_ONE] ?: 0})",
            GameStatus.COMPLETED to "Zerados (${statusCounts[GameStatus.COMPLETED] ?: 0})",
            GameStatus.DROPPED to "Abandonados (${statusCounts[GameStatus.DROPPED] ?: 0})",
            GameStatus.PAUSED to "Pausados (${statusCounts[GameStatus.PAUSED] ?: 0})"
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(statusTabs) { (tabStatus, label) ->
                val isSelected = filterState.selectedStatus == tabStatus
                Surface(
                    color = if (isSelected) NeonPurplePrimary.copy(alpha = 0.22f) else Color.Transparent,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonPurplePrimary else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onSelectStatusTab(tabStatus) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = if (isSelected) NeonPurpleGlow else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 4. Lista de Jogos
        if (filteredGames.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Nenhum jogo encontrado neste filtro",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Toque em '📥 Importar CSV' para carregar sua planilha ou adicione um novo jogo.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    Button(
                        onClick = { showAddGameDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurplePrimary),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("+ Adicionar Jogo", color = Color(0xFF140526), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredGames, key = { it.id }) { game ->
                    LibraryGameCard(
                        game = game,
                        nowMillis = nowMillis,
                        onClick = { onOpenGameDetails(game.id) }
                    )
                }
            }
        }
    }

    if (rouletteWinner != null) {
        BacklogRouletteDialog(
            winner = rouletteWinner,
            onDismiss = onDismissRoulette,
            onSpinAgain = onSpinRoulette,
            onStartPlaying = { winner ->
                onStartPlayingFromRoulette(winner)
                onDismissRoulette()
            },
            onOpenDetails = { gameId ->
                onDismissRoulette()
                onOpenGameDetails(gameId)
            }
        )
    }

    if (showAddGameDialog) {
        GameFormDialog(
            onDismiss = { showAddGameDialog = false },
            onSave = { newGame ->
                onSaveGame(newGame)
                showAddGameDialog = false
            }
        )
    }
}

@Composable
private fun LibraryGameCard(
    game: GameEntity,
    nowMillis: Long,
    onClick: () -> Unit
) {
    val isLifeChanging = game.isHallOfFame || (game.personalRating != null && game.personalRating >= 11.0f)
    val liveSeconds = game.getLiveTimePlayedSeconds(nowMillis)
    val borderColor = when {
        isLifeChanging -> HallOfFameGold
        game.status == GameStatus.PLAYING -> NeonPurplePrimary
        else -> NeonPurplePrimary.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DeepSlateSurface)
            .border(1.2.dp, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GameCoverArtwork(
            title = game.title,
            coverUrl = game.coverUrl,
            isHallOfFame = isLifeChanging,
            modifier = Modifier.size(92.dp),
            cornerRadius = 16.dp
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Linha superior: Nível ou Nota na esquerda + Status na direita
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityOrRatingBadge(game = game)

                Surface(
                    color = DeepSlateCard,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = game.status.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Título + Plataforma
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (game.genres.isNotBlank()) "${game.platform} • ${game.genres}" else game.platform,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Linha inferior: Pílula de Tempo (HH:MM:SS ou ~Xh HLTB) + Pílula Dourada 11/10
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeBadgeText = if (liveSeconds > 0L) {
                    "🕒 ${TimeFormatUtils.formatSecondsToHhMmSs(liveSeconds)}"
                } else {
                    "🕒 ${TimeFormatUtils.formatHltbReferenceOrTbd(game.getEffectiveDurationSeconds())}"
                }

                Surface(
                    color = DeepSlateCard,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = timeBadgeText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                if (isLifeChanging) {
                    Surface(
                        color = HallOfFameGold,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "🏆 11/10",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF261A00),
                                fontWeight = FontWeight.ExtraBold
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityOrRatingBadge(game: GameEntity) {
    val rating = game.personalRating
    if (game.status == GameStatus.COMPLETED && rating != null && rating < 11.0f) {
        val formattedRating = if (rating % 1.0f == 0f) {
            rating.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", rating).trimEnd('0').trimEnd('.').replace('.', ',')
        }
        Surface(
            color = HallOfFameGold.copy(alpha = 0.16f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.border(
                1.dp,
                HallOfFameGold.copy(alpha = 0.6f),
                RoundedCornerShape(50)
            )
        ) {
            Text(
                text = "★ $formattedRating/10",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = HallOfFameGold,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    } else {
        val badgeColor = when (game.priority) {
            PriorityTier.TIER_S -> Color(0xFFFF8A8A)
            PriorityTier.TIER_A -> DealEmeraldGreen
            PriorityTier.TIER_B -> NeonPurpleGlow
            PriorityTier.TIER_C -> TextSecondary
        }
        Surface(
            color = badgeColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(50),
            modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.7f), RoundedCornerShape(50))
        ) {
            Text(
                text = "Nível ${game.priority.badge}",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
