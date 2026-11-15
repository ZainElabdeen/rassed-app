package app.injaz.starter.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/* Civic look: white, a trustworthy deep green, one amber (tertiary) for notices. Light in every setting. */
private val White = Color(0xFFFFFFFF)
private val Ink = Color(0xFF0E1F1A)

private val RaseedColors = lightColorScheme(
    primary = Color(0xFF0F6B4F),
    onPrimary = White,
    primaryContainer = Color(0xFFD5EBE1),
    onPrimaryContainer = Color(0xFF06382A),
    secondary = Color(0xFF3F6B5B),
    onSecondary = White,
    secondaryContainer = Color(0xFFE9F2EE),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF8A5A00),
    onTertiary = White,
    tertiaryContainer = Color(0xFFFDF3DC),
    onTertiaryContainer = Color(0xFF6F4C05),
    error = Color(0xFFB3261E),
    onError = White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = White,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2F6F4),
    onSurfaceVariant = Color(0xFF4A5C55),
    outline = Color(0xFF6F817A),
    outlineVariant = Color(0xFFDCE6E1),
    surfaceContainerLowest = White,
    surfaceContainerLow = Color(0xFFF2F6F4),
    surfaceContainer = Color(0xFFEDF3F0),
    surfaceContainerHigh = Color(0xFFE9F2EE),
    surfaceContainerHighest = Color(0xFFE2EBE7),
)

private val RaseedShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

/* System sans (Arabic falls back to the phone's Arabic font); character comes from weight and generous Arabic line height. */
private val base = Typography()
private val RaseedTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontSize = 22.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontSize = 20.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = base.bodySmall.copy(fontSize = 12.sp, lineHeight = 19.sp),
    labelLarge = base.labelLarge.copy(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)

object AppLanguage {
    const val AR = "ar"
    const val EN = "en"
    fun isRtl(language: String): Boolean = language == AR
}

/** A context whose resources answer in the chosen language, while everything else still reaches the activity. */
private class LocalizedContext(base: Context, private val res: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = res
}

/**
 * The app's theme. [language] is "ar" (default, right to left) or "en" (left to right): it picks
 * the string resources and the layout direction without any library.
 */
@Composable
fun RaseedTheme(language: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val localized = remember(base, language) {
        val locale = Locale.forLanguageTag(language)
        val conf = Configuration(base.resources.configuration)
        conf.setLocale(locale)
        conf.setLayoutDirection(locale)
        LocalizedContext(base, base.createConfigurationContext(conf).resources)
    }
    val direction = if (AppLanguage.isRtl(language)) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalResources provides localized.resources,
        LocalConfiguration provides localized.resources.configuration,
        LocalLayoutDirection provides direction,
    ) {
        MaterialTheme(colorScheme = RaseedColors, shapes = RaseedShapes, typography = RaseedTypography, content = content)
    }
}
