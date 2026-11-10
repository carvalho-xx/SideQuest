package io.github.carvalhoxx.gamevault.ui.screens.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.carvalhoxx.gamevault.ui.components.GameCoverArtwork
import io.github.carvalhoxx.gamevault.ui.theme.AmberBrass
import io.github.carvalhoxx.gamevault.ui.theme.AshGrey
import io.github.carvalhoxx.gamevault.ui.theme.BoneCream
import io.github.carvalhoxx.gamevault.ui.theme.CarbonBorderHairline
import io.github.carvalhoxx.gamevault.ui.theme.CarbonBorderSubtle
import io.github.carvalhoxx.gamevault.ui.theme.CarbonMatteBackground
import io.github.carvalhoxx.gamevault.ui.theme.CarbonSurface
import io.github.carvalhoxx.gamevault.ui.theme.EmberOrangeGlow
import io.github.carvalhoxx.gamevault.ui.theme.EmberOrangePrimary
import io.github.carvalhoxx.gamevault.ui.theme.SageClear
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerEvent
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerReleaseUi
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerSuggestionUi
import io.github.carvalhoxx.gamevault.ui.viewmodel.PlannerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(uiState: PlannerUiState, onEvent: (PlannerEvent) -> Unit) {
    var gapAberta by remember { mutableStateOf(false) }
    val gapFillers = (uiState as? PlannerUiState.Ready)?.sugestoes.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CarbonMatteBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Agenda", style = MaterialTheme.typography.headlineSmall.copy(color = BoneCream, fontWeight = FontWeight.ExtraBold))
                Surface(
                    onClick = { onEvent(PlannerEvent.AbrirRitmoGlobal) }, shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1C1F26), border = BorderStroke(1.dp, EmberOrangePrimary.copy(alpha = .5f))
                ) {
                    Text("Ritmo: ${uiState.ritmoOuPadrao()}", Modifier.padding(horizontal = 12.dp, vertical = 9.dp), color = EmberOrangeGlow, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
        item { Text("Planeje seu foco atual entre as próximas estreias.", color = AshGrey, style = MaterialTheme.typography.bodySmall) }
        when (uiState) {
            PlannerUiState.Idle -> item { MessageCard("Nenhum jogo em foco para planejar.") }
            PlannerUiState.Loading -> item { MessageCard("Calculando sua agenda...") }
            is PlannerUiState.Ready -> {
                item { ConflictCard(uiState) }
                if (uiState.lancamentos.isNotEmpty()) item { ReleaseCarousel(uiState.lancamentos) }
                uiState.avisoSobreposicao?.let { item { MessageCard(it, SageClear) } }
                if (uiState.sugestoes.isNotEmpty()) item { GapFillersCard(uiState.sugestoes) { gapAberta = true } }
            }
            is PlannerUiState.AgendaDedicadaTbd -> {
                item { DedicatedAgendaCard(uiState) }
                if (uiState.lancamentos.isNotEmpty()) item { ReleaseCarousel(uiState.lancamentos) }
            }
        }
    }

    if (gapAberta && gapFillers.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { gapAberta = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = CarbonSurface
        ) {
            GapFillersSheet(gapFillers) { id ->
                onEvent(PlannerEvent.IniciarJornada(id))
                gapAberta = false
            }
        }
    }
}

private fun PlannerUiState.ritmoOuPadrao() = when (this) {
    is PlannerUiState.Ready -> ritmoDiario
    is PlannerUiState.AgendaDedicadaTbd -> ritmoDiario
    else -> "—"
}

@Composable
private fun ConflictCard(state: PlannerUiState.Ready) {
    PlannerCard {
        SectionLabel("CRONOGRAMA ATÉ A PRÓXIMA ESTREIA")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameInfoBox("JOGANDO AGORA", state.jogoAtual.titulo, "Restam ${state.tempoRestante}", state.jogoAtual.coverUrl, EmberOrangeGlow, Modifier.weight(1f))
            state.proximoLancamento?.let { GameInfoBox("PRÓXIMA ESTREIA", it.titulo, "${it.dataLancamento} • ${it.contagemRegressiva}", it.coverUrl, AmberBrass, Modifier.weight(1f)) }
        }
        Text("Ritmo diário: ${state.ritmoDiario}  •  ${state.diasRestantes}", color = AshGrey, style = MaterialTheme.typography.bodySmall)
        Text("Tempo jogável disponível até a estreia: ${state.tempoDisponivel}", color = BoneCream, style = MaterialTheme.typography.bodyMedium)
        state.avisoSobreposicao?.let { Text(it, color = SageClear, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
    }
}

@Composable
private fun ReleaseCarousel(releases: List<PlannerReleaseUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("PRÓXIMAS ESTREIAS", EmberOrangeGlow)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(releases, key = { it.id }) { release ->
                Column(Modifier.size(width = 116.dp, height = 190.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GameCoverArtwork(release.titulo, release.coverUrl, modifier = Modifier.size(width = 116.dp, height = 145.dp), cornerRadius = 10.dp)
                    Text(release.titulo, color = BoneCream, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                    Text(release.contagemRegressiva, color = EmberOrangeGlow, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun GapFillersCard(games: List<PlannerSuggestionUi>, onOpen: () -> Unit) {
    PlannerCard(container = Color(0xFF171B20), border = CarbonBorderHairline) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("PREENCHIMENTOS DE LACUNA", EmberOrangeGlow)
            Text("VER FILA", color = EmberOrangeGlow, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
        Text("Jogos curtos que cabem no tempo disponível.", color = AshGrey, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            games.take(3).forEach { game ->
                GameCoverArtwork(game.titulo, game.coverUrl, modifier = Modifier.size(56.dp, 78.dp), cornerRadius = 8.dp)
            }
        }
    }
}

@Composable
private fun GapFillersSheet(games: List<PlannerSuggestionUi>, onStart: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Escolha seu próximo preenchimento", color = BoneCream, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
        Text("A jornada atual será encerrada e este jogo passará a ser seu foco principal.", color = AshGrey, style = MaterialTheme.typography.bodySmall)
        games.forEach { game ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameCoverArtwork(game.titulo, game.coverUrl, modifier = Modifier.size(58.dp, 78.dp), cornerRadius = 8.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(game.titulo, color = BoneCream, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(game.duracao, color = EmberOrangeGlow, style = MaterialTheme.typography.labelMedium)
                    Button(onClick = { onStart(game.id) }, colors = ButtonDefaults.buttonColors(containerColor = EmberOrangePrimary, contentColor = CarbonMatteBackground), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("Iniciar jornada", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DedicatedAgendaCard(state: PlannerUiState.AgendaDedicadaTbd) {
    PlannerCard(container = Color(0xFF1F1912), border = AmberBrass) {
        SectionLabel("AGENDA DEDICADA — DURAÇÃO A DEFINIR", AmberBrass)
        Text(state.jogo.titulo, color = BoneCream, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
        Text(state.mensagem.removePrefix("🔒 "), color = AshGrey, style = MaterialTheme.typography.bodySmall)
        Text("Ritmo diário: ${state.ritmoDiario}", color = AshGrey, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable private fun PlannerCard(container: Color = CarbonSurface, border: Color = CarbonBorderSubtle, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().border(1.dp, border, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

@Composable private fun InfoBox(label: String, title: String, meta: String, accent: Color, modifier: Modifier) {
    Column(modifier.border(1.dp, accent.copy(alpha = .4f), RoundedCornerShape(12.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = accent, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Text(title, color = BoneCream, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), maxLines = 2)
        Text(meta, color = AshGrey, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun GameInfoBox(label: String, title: String, meta: String, coverUrl: String?, accent: Color, modifier: Modifier) {
    Row(modifier.border(1.dp, accent.copy(alpha = .4f), RoundedCornerShape(12.dp)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GameCoverArtwork(title, coverUrl, modifier = Modifier.size(58.dp, 78.dp), cornerRadius = 8.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = accent, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
            Text(title, color = BoneCream, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), maxLines = 2)
            Text(meta, color = AshGrey, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun SectionLabel(text: String, color: Color = AshGrey) = Text(text, color = color, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
@Composable private fun MessageCard(message: String, color: Color = AshGrey) = PlannerCard { Text(message, color = color, style = MaterialTheme.typography.bodyMedium) }
