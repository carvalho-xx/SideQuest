package io.github.carvalhoxx.gamevault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SideQuestDarkColorScheme = darkColorScheme(
    primary = EmberOrangePrimary,
    onPrimary = CarbonMatteBackground,
    primaryContainer = CarbonElevated,
    onPrimaryContainer = EmberOrangeGlow,
    secondary = EmberOrangeSecondary,
    onSecondary = BoneCream,
    secondaryContainer = CarbonElevated,
    onSecondaryContainer = BoneCream,
    tertiary = AmberBrass,
    onTertiary = CarbonMatteBackground,
    tertiaryContainer = Color(0xFF2B2212),
    onTertiaryContainer = AmberBrass,
    background = CarbonMatteBackground,
    onBackground = BoneCream,
    surface = CarbonSurface,
    onSurface = BoneCream,
    surfaceVariant = CarbonElevated,
    onSurfaceVariant = AshGrey,
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
