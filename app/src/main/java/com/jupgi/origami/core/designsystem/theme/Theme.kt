package com.jupgi.origami.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary = PaperAccent,
        onPrimary = PaperCream,
        background = PaperCream,
        onBackground = PaperInk,
        surface = PaperCream,
        onSurface = PaperInk,
    )

private val DarkColors =
    darkColorScheme(
        primary = PaperAccent,
        onPrimary = DarkSurface,
        background = DarkSurface,
        onBackground = DarkOnSurface,
        surface = DarkPaper,
        onSurface = DarkOnSurface,
    )

@Composable
fun JupgiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = JupgiTypography,
        content = content,
    )
}
