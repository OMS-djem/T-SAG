package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TelecomBlueLight,
    onPrimary = Color.Black,
    secondary = TelecomBlueAccent,
    onSecondary = Color.White,
    tertiary = AccessAccordedGreenLight,
    background = DarkBackground,
    surface = DarkSurfaceCard,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    error = AccessRefusedRedLight
)

private val LightColorScheme = lightColorScheme(
    primary = TelecomNavyPrimary,
    onPrimary = Color.White,
    secondary = TelecomBlueAccent,
    onSecondary = Color.White,
    tertiary = AccessAccordedGreen,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = AccessRefusedRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our consistent Telecom Navy branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
