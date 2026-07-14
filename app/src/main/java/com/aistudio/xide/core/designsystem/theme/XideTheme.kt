package com.aistudio.xide.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.aistudio.xide.core.designsystem.tokens.ColorTokens

private val DarkColorScheme = darkColorScheme(
    primary = ColorTokens.Primary,
    secondary = ColorTokens.Secondary,
    background = ColorTokens.Background,
    surface = ColorTokens.Surface,
    error = ColorTokens.Error,
    onPrimary = ColorTokens.OnPrimary,
    onSecondary = ColorTokens.OnSecondary,
    onBackground = ColorTokens.OnBackground,
    onSurface = ColorTokens.OnSurface,
    onError = ColorTokens.OnError
)

private val LightColorScheme = lightColorScheme(
    primary = ColorTokens.Primary,
    secondary = ColorTokens.Secondary,
    background = ColorTokens.Background,
    surface = ColorTokens.Surface,
    error = ColorTokens.Error,
    onPrimary = ColorTokens.OnPrimary,
    onSecondary = ColorTokens.OnSecondary,
    onBackground = ColorTokens.OnBackground,
    onSurface = ColorTokens.OnSurface,
    onError = ColorTokens.OnError
)

@Composable
fun XideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
