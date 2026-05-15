package lab3.egor.chat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppleBlue,
    onPrimary = White,
    background = White,
    onBackground = TextBlack,
    surface = AppleGray,
    onSurface = TextBlack,
    error = ErrorRed
)

@Composable
fun ChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
