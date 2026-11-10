package io.github.carvalhoxx.gamevault.ui.screens.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.carvalhoxx.gamevault.ui.theme.AmberBrass
import io.github.carvalhoxx.gamevault.ui.theme.AshGrey
import io.github.carvalhoxx.gamevault.ui.theme.CarbonBorderHairline
import io.github.carvalhoxx.gamevault.ui.theme.CarbonBorderSubtle
import io.github.carvalhoxx.gamevault.ui.theme.CarbonMatteBackground
import io.github.carvalhoxx.gamevault.ui.theme.CarbonSurface
import io.github.carvalhoxx.gamevault.ui.theme.EmberOrangeGlow
import io.github.carvalhoxx.gamevault.ui.theme.EmberOrangePrimary
import io.github.carvalhoxx.gamevault.ui.theme.SageClear
import io.github.carvalhoxx.gamevault.ui.theme.BoneCream
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerEvent
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerUiState

@Composable
fun PlannerScreen(
    uiState: PlannerUiState,
    onEvent: (PlannerEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonMatteBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Agenda & Day One",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = BoneCream,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Surface(
                    onClick = { onEvent(PlannerEvent.AbrirRitmoGlobal) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1C1F26),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        EmberOrangePrimary.copy(alpha = .5f)
                    )
                ) {
                    Text(
                        text = "Ritmo: ${uiState.ritmoOuPadrao()}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        color = EmberOrangeGlow,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        item {
            Text(
                text = "Cálculo de conflito entre seu foco atual e lançamentos Day One",
                color = AshGrey,
                style = MaterialTheme.typography.bodySmall
            )
        }
        when (uiState) {
            PlannerUiState.Idle -> item { MessageCard("Nenhum jogo em foco para planejar.") }
            PlannerUiState.Loading -> item { MessageCard("Calculando sua agenda...") }
            is PlannerUiState.Ready -> {
                item { ConflictCard(uiState) }
                uiState.proximoLancamento?.let { release ->
                    item { ReleaseCard(release.titulo, release.dataLancamento, release.contagemRegressiva) }
                }
                uiState.avisoSobreposicao?.let { item { MessageCard(it, SageClear) } }
                if (uiState.sugestoes.isNotEmpty()) {
                    item { SuggestionsCard(uiState.sugestoes) }
                }
            }
            is PlannerUiState.AgendaDedicadaTbd -> {
                item { DedicatedAgendaCard(uiState, onEvent) }
            }
        }
    }
}

private fun PlannerUiState.ritmoOuPadrao(): String = when (this) {
    is PlannerUiState.Ready -> ritmoDiario
    is PlannerUiState.AgendaDedicadaTbd -> ritmoDiario
    else -> "—"
}

@Composable
private fun ConflictCard(state: PlannerUiState.Ready) {
    PlannerCard {
        SectionLabel("CRONOGRAMA ATÉ A PRÓXIMA ESTREIA")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoBox("JOGANDO AGORA (FOCO)", state.jogoAtual.titulo, "Restam ${state.tempoRestante}", EmberOrangeGlow, Modifier.weight(1f))
            state.proximoLancamento?.let {
                InfoBox("ESTREIA DAY ONE", it.titulo, "${it.dataLancamento} • ${it.contagemRegressiva}", AmberBrass, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Ritmo diário: ${state.ritmoDiario}  •  ${state.diasRestantes}", color = AshGrey, style = MaterialTheme.typography.bodySmall)
        Text("Tempo de jogo disponível até a estreia: ${state.tempoDisponivel}", color = BoneCream, style = MaterialTheme.typography.bodyMedium)
        state.avisoSobreposicao?.let { Text(it, color = SageClear, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
    }
}

@Composable
private fun ReleaseCard(title: String, date: String, countdown: String) {
    PlannerCard {
        SectionLabel("PRÓXIMA ESTREIA DAY ONE", EmberOrangeGlow)
        Text(title, color = BoneCream, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
        Text("$date • $countdown", color = AshGrey, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DedicatedAgendaCard(state: PlannerUiState.AgendaDedicadaTbd, onEvent: (PlannerEvent) -> Unit) {
    PlannerCard(container = Color(0xFF1F1912), border = AmberBrass) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            SectionLabel("AGENDA DEDICADA — DURAÇÃO A DEFINIR", AmberBrass)
            state.proximoLancamento?.let { Text(it.dataLancamento, color = AmberBrass, style = MaterialTheme.typography.labelMedium) }
        }
        Text(state.jogo.titulo, color = BoneCream, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
        Text("Agenda Dedicada — Duração a definir", color = AmberBrass, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Text(state.mensagem.removePrefix("🔒 "), color = AshGrey, style = MaterialTheme.typography.bodySmall)
        Text("Ritmo diário: ${state.ritmoDiario}", color = AshGrey, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onEvent(PlannerEvent.DefinirEstimativa) },
                colors = ButtonDefaults.buttonColors(containerColor = AmberBrass, contentColor = Color(0xFF1F1400)),
                shape = RoundedCornerShape(19.dp)
            ) { Text("Definir estimativa", fontWeight = FontWeight.ExtraBold) }
            Button(
                onClick = { onEvent(PlannerEvent.ManterDuracaoAdefinir) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberBrass),
                shape = RoundedCornerShape(19.dp)
            ) { Text("Manter aberto") }
        }
    }
}

@Composable
private fun SuggestionsCard(sugestoes: List<io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerSuggestionUi>) {
    PlannerCard(container = Color(0xFF171B20), border = CarbonBorderHairline) {
        SectionLabel("JOGOS QUE CABEM NESSA LACUNA", EmberOrangeGlow)
        Text(
            "Se quiser manter o ritmo, estes jogos da sua fila podem ser concluídos antes da estreia:",
            color = AshGrey,
            style = MaterialTheme.typography.bodySmall
        )
        sugestoes.forEach { sugestao ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(sugestao.titulo, color = BoneCream, style = MaterialTheme.typography.bodyMedium)
                Text(sugestao.duracao, color = EmberOrangeGlow, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PlannerCard(
    container: Color = CarbonSurface,
    border: Color = CarbonBorderSubtle,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, border, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

@Composable
private fun InfoBox(label: String, title: String, meta: String, accent: Color, modifier: Modifier) {
    Column(modifier.border(1.dp, accent.copy(alpha = .4f), RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = accent, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Text(title, color = BoneCream, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), maxLines = 2)
        Text(meta, color = AshGrey, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionLabel(text: String, color: Color = AshGrey) {
    Text(text, color = color, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
}

@Composable
private fun MessageCard(message: String, color: Color = AshGrey) {
    PlannerCard { Text(message, color = color, style = MaterialTheme.typography.bodyMedium) }
}
