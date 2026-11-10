package io.github.carvalhoxx.gamevault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.carvalhoxx.gamevault.ui.navigation.SideQuestNavGraph
import io.github.carvalhoxx.gamevault.ui.theme.GameVaultTheme
import io.github.carvalhoxx.gamevault.ui.viewmodel.GameVaultViewModel
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as GameVaultApplication).container

        setContent {
            GameVaultTheme {
                val viewModel: GameVaultViewModel = viewModel(
                    factory = GameVaultViewModel.provideFactory(appContainer.gameRepository)
                )
                val plannerViewModel: PlannerViewModel = viewModel(
                    factory = PlannerViewModel.provideFactory(appContainer.gameRepository)
                )
                SideQuestNavGraph(
                    viewModel = viewModel,
                    plannerViewModel = plannerViewModel
                )
            }
        }
    }
}
