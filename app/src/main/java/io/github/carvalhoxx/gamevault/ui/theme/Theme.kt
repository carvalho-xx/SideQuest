package io.github.carvalhoxx.gamevault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SideQuestDarkColorScheme = darkColorScheme(
    primary = NeonPurplePrimary,
    onPrimary = Color(0xFF140526),
    primaryContainer = DeepSlateElevated,
    onPrimaryContainer = NeonPurpleGlow,
    secondary = NeonPurpleSecondary,
    onSecondary = TextPrimary,
    secondaryContainer = DeepSlateCard,
    onSecondaryContainer = TextPrimary,
    tertiary = HallOfFameGold,
    onTertiary = Color(0xFF261A00),
    tertiaryContainer = Color(0xFF3B2B07),
    onTertiaryContainer = HallOfFameGold,
    background = DeepSlateBackground,
    onBackground = TextPrimary,
    surface = DeepSlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeepSlateCard,
    onSurfaceVariant = TextSecondary,
    error = DangerCrimson,
    onError = Color.White
)

@Composable
fun GameVaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SideQuestDarkColorScheme,
        typography = Typography,
        content = content
    )
}
