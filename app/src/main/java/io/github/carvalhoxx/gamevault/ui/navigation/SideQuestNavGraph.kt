package io.github.carvalhoxx.gamevault.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.carvalhoxx.gamevault.ui.components.AppIcons
import io.github.carvalhoxx.gamevault.ui.screens.dashboard.DashboardScreen
import io.github.carvalhoxx.gamevault.ui.screens.details.GameDetailScreen
import io.github.carvalhoxx.gamevault.ui.screens.library.LibraryScreen
import io.github.carvalhoxx.gamevault.ui.screens.planner.PlannerScreen
import io.github.carvalhoxx.gamevault.ui.screens.profile.ProfileScreen
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateBackground
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateElevated
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateSurface
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurplePrimary
import io.github.carvalhoxx.gamevault.ui.theme.TextPrimary
import io.github.carvalhoxx.gamevault.ui.theme.TextSecondary
import io.github.carvalhoxx.gamevault.ui.viewmodel.GameVaultViewModel
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerViewModel
import io.github.carvalhoxx.gamevault.ui.viewmodel.ProfileViewModel

enum class BottomNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Início", AppIcons.Dashboard),
    LIBRARY("library", "Biblioteca", AppIcons.Library),
    PLANNER("planner", "Agenda", AppIcons.Calendar),
    WISHLIST("wishlist", "Desejos", AppIcons.Heart)
}

@Composable
fun SideQuestNavGraph(
    viewModel: GameVaultViewModel,
    plannerViewModel: PlannerViewModel,
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: BottomNavTab.DASHBOARD.route
    val highlightedTabRoute = remember(currentRoute, backStackEntry) {
        if (BottomNavTab.entries.any { it.route == currentRoute }) {
            currentRoute
        } else {
            navController.previousBackStackEntry?.destination?.route ?: BottomNavTab.DASHBOARD.route
        }
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val dashboardState by viewModel.dashboardState.collectAsState()
    val allGames by viewModel.allGames.collectAsState()
    val filteredGames by viewModel.filteredLibraryGames.collectAsState()
    val filterState by viewModel.libraryFilter.collectAsState()
    val rouletteWinner by viewModel.rouletteWinner.collectAsState()
    val nowMillis by viewModel.nowMillis.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isSyncingLibrary by viewModel.isSyncingLibrary.collectAsState()
    val syncProgressLabel by viewModel.syncProgressLabel.collectAsState()
    val plannerState by plannerViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage(msg)
        }
    }

    fun navigateToTab(tab: BottomNavTab) {
        if (tab == BottomNavTab.DASHBOARD) {
            val popped = navController.popBackStack(BottomNavTab.DASHBOARD.route, inclusive = false)
            if (!popped && navController.currentDestination?.route != BottomNavTab.DASHBOARD.route) {
                navController.navigate(BottomNavTab.DASHBOARD.route) {
                    launchSingleTop = true
                }
            }
            return
        }
        if (navController.currentDestination?.route != tab.route) {
            navController.navigate(tab.route) {
                popUpTo(BottomNavTab.DASHBOARD.route) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepSlateBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            SideQuestBottomBar(
                currentRoute = highlightedTabRoute,
                onSelectTab = ::navigateToTab
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavTab.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavTab.DASHBOARD.route) {
                DashboardScreen(
                    uiState = dashboardState,
                    onNavigateToLibrary = { navigateToTab(BottomNavTab.LIBRARY) },
                    onNavigateToPlanner = { navigateToTab(BottomNavTab.PLANNER) },
                    onOpenGameDetails = { gameId ->
                        navController.navigate("game_details/$gameId")
                    },
                    onToggleSessionTimer = viewModel::toggleSessionTimer,
                    onUpdateExactTime = viewModel::updateExactTimePlayed,
                    onSaveNewGame = viewModel::saveGame
                )
            }

            composable(BottomNavTab.LIBRARY.route) {
                LibraryScreen(
                    allGames = allGames,
                    filteredGames = filteredGames,
                    filterState = filterState,
                    rouletteWinner = rouletteWinner,
                    nowMillis = nowMillis,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSelectStatusTab = viewModel::selectStatusTab,
                    onSelectPlatform = viewModel::selectPlatformFilter,
                    onSelectPriority = viewModel::selectPriorityFilter,
                    onSpinRoulette = viewModel::spinBacklogRoulette,
                    onDismissRoulette = viewModel::dismissRouletteWinner,
                    onStartPlayingFromRoulette = viewModel::setMainFocusGame,
                    onOpenGameDetails = { gameId ->
                        navController.navigate("game_details/$gameId")
                    },
                    onSaveGame = viewModel::saveGame,
                    onImportCsvUri = { uri ->
                        viewModel.importFriendCsv(context.contentResolver, uri)
                    },
                    onSyncMissingMetadata = viewModel::syncMissingLibraryMetadata,
                    isSyncingLibrary = isSyncingLibrary,
                    syncProgressLabel = syncProgressLabel
                )
            }

            composable(
                route = "game_details/{gameId}",
                arguments = listOf(navArgument("gameId") { type = NavType.LongType })
            ) { entry ->
                val gameId = entry.arguments?.getLong("gameId") ?: 0L
                val selectedGame = allGames.firstOrNull { it.id == gameId }
                GameDetailScreen(
                    game = selectedGame,
                    nowMillis = nowMillis,
                    onNavigateBack = { navController.popBackStack() },
                    onToggleStopwatch = viewModel::toggleSessionTimer,
                    onUpdateExactTime = viewModel::updateExactTimePlayed,
                    onUpdateRatingAndMemory = viewModel::updateRatingAndMemory,
                    onMarkAsCompleted = viewModel::markGameCompleted,
                    onSaveGame = viewModel::saveGame,
                    onDeleteGame = { id ->
                        viewModel.deleteGame(id) {
                            navController.popBackStack()
                        }
                    },
                    onRefreshOnlineMetadata = viewModel::refreshGameMetadata
                )
            }

            composable(BottomNavTab.PLANNER.route) {
                PlannerScreen(
                    uiState = plannerState,
                    onEvent = { event ->
                        if (event is io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerEvent.IniciarJornada) {
                            plannerViewModel.iniciarJornada(event.jogoId)
                        }
                    }
                )
            }

            composable(BottomNavTab.WISHLIST.route) {
                PhasePlaceholderScreen(
                    title = "Lista de Desejos e Jogo do Ano",
                    subtitle = "Radar de promoções e Desempate de Jogo do Ano serão expandidos nas Fases 5 e 6 (${dashboardState.wishlistGames.size} itens na lista de desejos)."
                )
            }

            composable("profile") {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun SideQuestBottomBar(
    currentRoute: String,
    onSelectTab: (BottomNavTab) -> Unit
) {
    Surface(
        color = Color(0xFF15171C),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1F2229), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab.entries.forEach { tab ->
                val isSelected = currentRoute == tab.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) NeonPurplePrimary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) DeepSlateBackground else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) NeonPurplePrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PhasePlaceholderScreen(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
        }
    }
}
