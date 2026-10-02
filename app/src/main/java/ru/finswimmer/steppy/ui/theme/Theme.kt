package ru.finswimmer.steppy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SteppyYellow = Color(0xFFFFD928)
val SteppyLime = Color(0xFFC9F23A)
val SteppyInk = Color(0xFF171717)
val SteppyCream = Color(0xFFFFFDF7)
val SteppyGreen = Color(0xFF16855B)
val SteppyOrange = Color(0xFFFF7A45)

private val LightColors = lightColorScheme(
    primary = SteppyInk,
    onPrimary = Color.White,
    secondary = SteppyGreen,
    tertiary = SteppyYellow,
    background = SteppyCream,
    surface = Color.White,
    onSurface = SteppyInk,
    surfaceVariant = Color(0xFFF3F1EA),
    outline = Color(0xFFD9D6CC),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = SteppyYellow,
    onPrimary = SteppyInk,
    secondary = SteppyLime,
    background = Color(0xFF10110F),
    surface = Color(0xFF1B1C19),
    onSurface = Color(0xFFF2F1EA),
    surfaceVariant = Color(0xFF292A26),
)

@Composable
fun SteppyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
