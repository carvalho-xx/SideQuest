package io.github.carvalhoxx.gamevault.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateBackground
import io.github.carvalhoxx.gamevault.ui.theme.DeepSlateElevated
import io.github.carvalhoxx.gamevault.ui.theme.NeonPurplePrimary
import io.github.carvalhoxx.gamevault.ui.theme.TextPrimary
import io.github.carvalhoxx.gamevault.ui.theme.TextSecondary
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerUiState

@Composable
fun PlannerScreen(
    uiState: PlannerUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlateBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Agenda",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold
            )
        )

        when (uiState) {
            PlannerUiState.Idle -> PlannerMessage("Nenhum jogo em foco para planejar.")
            PlannerUiState.Loading -> PlannerMessage("Calculando sua agenda...")
            is PlannerUiState.Ready -> {
                PlannerCard(
                    title = uiState.jogoAtual.titulo,
                    lines = listOf(
                        "Ritmo diário: ${uiState.ritmoDiario}",
                        "Tempo restante: ${uiState.tempoRestante}",
                        "Tempo disponível: ${uiState.tempoDisponivel}",
                        uiState.diasRestantes
                    )
                )
                uiState.proximoLancamento?.let { release ->
                    PlannerCard(
                        title = "Próximo lançamento",
                        lines = listOf(
                            release.titulo,
                            release.dataLancamento,
                            release.contagemRegressiva
                        )
                    )
                }
                uiState.avisoSobreposicao?.let { aviso -> PlannerMessage(aviso) }
            }
            is PlannerUiState.AgendaDedicadaTbd -> {
                PlannerCard(
                    title = "Agenda dedicada",
                    lines = listOf(
                        uiState.jogo.titulo,
                        uiState.mensagem,
                        "Ritmo diário: ${uiState.ritmoDiario}"
                    )
                )
            }
        }
    }
}

@Composable
private fun PlannerCard(title: String, lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DeepSlateElevated)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = NeonPurplePrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            lines.forEach { line ->
                Text(text = line, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun PlannerMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.fillMaxWidth(),
        color = TextSecondary,
        style = MaterialTheme.typography.bodyLarge
    )
}
