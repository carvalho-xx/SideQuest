package io.github.carvalhoxx.gamevault.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.carvalhoxx.gamevault.ui.theme.AshGrey
import io.github.carvalhoxx.gamevault.ui.theme.BoneCream
import io.github.carvalhoxx.gamevault.ui.theme.CarbonBorderSubtle
import io.github.carvalhoxx.gamevault.ui.theme.CarbonMatteBackground
import io.github.carvalhoxx.gamevault.ui.theme.CarbonSurface
import io.github.carvalhoxx.gamevault.ui.theme.EmberOrangePrimary
import io.github.carvalhoxx.gamevault.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var dailyHours by remember { mutableStateOf("") }
    var dailyMinutes by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (!uiState.isLoading) {
            name = uiState.userName
            dailyHours = uiState.dailyHours.toString()
            dailyMinutes = uiState.dailyMinutes.toString().padStart(2, '0')
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CarbonMatteBackground
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(24.dp),
                color = EmberOrangePrimary
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileHeader(onNavigateBack = onNavigateBack)

                ProfileCard(title = "IDENTIDADE DO JOGADOR") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nome do usuário") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }

                ProfileCard(
                    title = "RITMO DIÁRIO GLOBAL",
                    subtitle = "Parâmetro usado para prever datas de término na Agenda"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = dailyHours,
                            onValueChange = { dailyHours = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Horas por dia") },
                            suffix = { Text("h") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = dailyMinutes,
                            onValueChange = { dailyMinutes = it.filter(Char::isDigit).take(2) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Minutos") },
                            suffix = { Text("min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                    Text(
                        text = "Use o ritmo médio diário para manter o planejamento realista.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AshGrey
                    )
                }

                Button(
                    onClick = {
                        viewModel.saveProfile(
                            name = name.trim(),
                            dailyHours = dailyHours.toIntOrNull() ?: 0,
                            dailyMinutes = dailyMinutes.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmberOrangePrimary,
                        contentColor = CarbonMatteBackground
                    )
                ) {
                    Text("SALVAR CONFIGURAÇÕES", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProfileHeader(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onNavigateBack,
            modifier = Modifier.height(48.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = CarbonSurface,
                contentColor = BoneCream
            )
        ) { Text("‹") }
        Text(
            text = "Perfil & Ritmo Global",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.titleLarge,
            color = BoneCream,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ProfileCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CarbonSurface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = EmberOrangePrimary,
                fontWeight = FontWeight.Bold
            )
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = AshGrey)
            }
            content()
        }
    }
}
