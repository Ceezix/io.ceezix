package io.ceezix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CosmicCyan,
    secondary = NebulaPurple,
    tertiary = ElectricLime,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceCard,
    onBackground = OnDarkSurface,
    onSurface = OnDarkSurface,
    onPrimary = OnDarkPrimary,
    onSecondary = OnDarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = CosmicCyan,
    secondary = NebulaPurple,
    tertiary = ElectricLime,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightBackground,
    onBackground = OnLightSurface,
    onSurface = OnLightSurface,
    onPrimary = OnDarkPrimary,
    onSecondary = OnLightSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // We enforce our spectacular custom science-noir dark colors by default!
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
