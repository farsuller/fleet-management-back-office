package org.solodev.fleet.mngt.theme

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import fleetmanagementbackoffice.composeapp.generated.resources.Res
import fleetmanagementbackoffice.composeapp.generated.resources.*

// ─── Color Tokens ─────────────────────────────────────────────────────────────

object FleetColors {
    // Brand
    val Primary     = Color(0xFFF97316)  // orange (dark mode)
    val Secondary   = Color(0xFFFB923C)  // light orange
    val Accent      = Color(0xFFFBBF24)  // amber

    // Surface (dark)
    val Surface     = Color(0xFF0D1117)
    val Surface2    = Color(0xFF161B2C)
    val Border      = Color(0xFF252D42)

    // Text (dark)
    val Text1       = Color(0xFFE2E8F0)
    val Text2       = Color(0xFF64748B)

    // ── Light mode palette ───────────────────────────────────────────────────
    val LightPrimary     = Color(0xFF116FDD)  // blue
    val LightSecondary   = Color(0xFF3B8EF0)
    val LightSurface     = Color(0xFFFFFFFF)
    val LightSurface2    = Color(0xFFF1F5F9)
    val LightBorder      = Color(0xFFE2E8F0)
    val LightText1       = Color(0xFF0F172A)
    val LightText2       = Color(0xFF64748B)
    val LightSurfaceVariant = Color(0xFFE8EEF6)

    // Status — Vehicle / Rental
    val Available   = Color(0xFF22C55E)
    val Rented      = Color(0xFF3B82F6)
    val Maintenance = Color(0xFFF59E0B)

    val InProgress   = Color(0xFF22C55E)
    val Retired     = Color(0xFF94A3B8)
    val Reserved    = Color(0xFFEAB308)
    val Active      = Color(0xFF22C55E)
    val Completed   = Color(0xFF94A3B8)
    val Cancelled   = Color(0xFFEF4444)

    // Priority
    val PriorityLow    = Color(0xFF94A3B8)
    val PriorityNormal = Color(0xFF3B82F6)
    val PriorityHigh   = Color(0xFFF97316)
    val PriorityUrgent = Color(0xFFEF4444)

    // Fleet Map
    val MapBg      = Color(0xFF0F172A)
    val MapRoute   = Color(0xFF334155)
    val MapConnect = Color(0xFF22C55E)
    val MapOffline = Color(0xFFEF4444)

    // Semantic aliases
    val Error   = Cancelled
    val Warning = Accent
    val Success = Available
    val Info    = Color(0xFF3B82F6)
}

// ─── Color Schemes ────────────────────────────────────────────────────────────

private val FleetDarkColorScheme = darkColorScheme(
    primary          = FleetColors.Primary,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF431407),
    secondary        = FleetColors.Secondary,
    onSecondary      = Color.White,
    tertiary         = FleetColors.Accent,
    background       = FleetColors.Surface,
    surface          = FleetColors.Surface2,
    surfaceVariant   = Color(0xFF1C2436),
    onBackground     = FleetColors.Text1,
    onSurface        = FleetColors.Text1,
    onSurfaceVariant = FleetColors.Text2,
    outline          = FleetColors.Border,
    error            = FleetColors.Error,
    onError          = Color.White,
)

private val FleetLightColorScheme = lightColorScheme(
    primary          = FleetColors.LightPrimary,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFDBEAFB),
    secondary        = FleetColors.LightSecondary,
    onSecondary      = Color.White,
    tertiary         = FleetColors.Accent,
    background       = FleetColors.LightSurface,
    surface          = FleetColors.LightSurface2,
    surfaceVariant   = FleetColors.LightSurfaceVariant,
    onBackground     = FleetColors.LightText1,
    onSurface        = FleetColors.LightText1,
    onSurfaceVariant = FleetColors.LightText2,
    outline          = FleetColors.LightBorder,
    error            = FleetColors.Error,
    onError          = Color.White,
)

// ─── Typography ───────────────────────────────────────────────────────────────

@Composable
fun FunnelDisplayFamily() = FontFamily(
    Font(Res.font.funneldisplay_regular, FontWeight.Normal),
    Font(Res.font.funneldisplay_medium, FontWeight.Medium),
    Font(Res.font.funneldisplay_semibold, FontWeight.SemiBold),
    Font(Res.font.funneldisplay_bold, FontWeight.Bold),
    Font(Res.font.funneldisplay_extrabold, FontWeight.ExtraBold),
    Font(Res.font.funneldisplay_light, FontWeight.Light),
)

private val InterFamily = FontFamily.Default

@Composable
fun fleetTypography() = Typography(
    displayLarge = TextStyle(
        fontFamily = FunnelDisplayFamily(),
        fontWeight = FontWeight.ExtraBold,
        fontSize   = 36.sp,
        lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FunnelDisplayFamily(),
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        lineHeight = 38.4.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FunnelDisplayFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize   = 24.sp,
        lineHeight = 31.2.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FunnelDisplayFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        lineHeight = 25.2.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FunnelDisplayFamily(),
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 21.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 11.sp,
    ),
)

// ─── Spacing / Shape tokens ───────────────────────────────────────────────────

object FleetSpacing {
    val unit: Dp      = 8.dp
    val xs: Dp        = 4.dp
    val sm: Dp        = 8.dp
    val md: Dp        = 16.dp
    val lg: Dp        = 24.dp
    val xl: Dp        = 32.dp

    val cardPaddingSm: Dp   = 16.dp
    val cardPaddingLg: Dp   = 24.dp
    val bentoGap: Dp        = 16.dp
    val sidebarWidth: Dp    = 240.dp
    val headerHeight: Dp    = 56.dp
    val tableRowHeight: Dp  = 44.dp
    val cardRadius: Dp      = 12.dp
}

// ─── Theme state ──────────────────────────────────────────────────────────────

class ThemeState(initialDark: Boolean = true) {
    var isDark by mutableStateOf(initialDark)
}

val LocalThemeState = staticCompositionLocalOf { ThemeState() }

// ─── Extra tokens delivered via CompositionLocal ──────────────────────────────

data class FleetExtendedColors(
    val available:      Color,
    val rented:         Color,
    val maintenance:    Color,
    val retired:        Color,
    val reserved:       Color,
    val active:         Color,
    val inProgress:    Color,
    val completed:      Color,
    val cancelled:      Color,
    val priorityLow:    Color,
    val priorityNormal: Color,
    val priorityHigh:   Color,
    val priorityUrgent: Color,
    val mapBg:          Color,
    val mapRoute:       Color,
    val mapConnect:     Color,
    val mapOffline:     Color,
    val surface2:       Color,
    val border:         Color,
    val text1:          Color,
    val text2:          Color,
    val background:     Color,
    val surface:        Color,
    val primary:        Color,
    val onPrimary:      Color,
    val onBackground:   Color,
    val onSurface:      Color,
    val overdue:        Color,
    val paid:           Color,
    val surfaceVariant: Color,
)

val LocalFleetColors = staticCompositionLocalOf {
    FleetExtendedColors(
        available      = FleetColors.Available,
        rented         = FleetColors.Rented,
        maintenance    = FleetColors.Maintenance,
        retired        = FleetColors.Retired,
        reserved       = FleetColors.Reserved,
        active         = FleetColors.Active,
        completed      = FleetColors.Completed,
        cancelled      = FleetColors.Cancelled,
        inProgress     = FleetColors.InProgress,
        priorityLow    = FleetColors.PriorityLow,
        priorityNormal = FleetColors.PriorityNormal,
        priorityHigh   = FleetColors.PriorityHigh,
        priorityUrgent = FleetColors.PriorityUrgent,
        mapBg          = FleetColors.MapBg,
        mapRoute       = FleetColors.MapRoute,
        mapConnect     = FleetColors.MapConnect,
        mapOffline     = FleetColors.MapOffline,
        surface2       = FleetColors.Surface2,
        border         = FleetColors.Border,
        text1          = FleetColors.Text1,
        text2          = FleetColors.Text2,
        background     = FleetColors.Surface,
        surface        = FleetColors.Surface2,
        primary        = FleetColors.Primary,
        onPrimary      = Color.White,
        onBackground   = FleetColors.Text1,
        onSurface      = FleetColors.Text1,
        overdue        = FleetColors.Cancelled,
        paid           = FleetColors.Active,
        surfaceVariant = Color(0xFF1C2436),
    )
}

private fun buildExtendedColors(dark: Boolean) = if (dark) {
    FleetExtendedColors(
        available      = FleetColors.Available,
        rented         = FleetColors.Rented,
        maintenance    = FleetColors.Maintenance,
        retired        = FleetColors.Retired,
        reserved       = FleetColors.Reserved,
        active         = FleetColors.Active,
        completed      = FleetColors.Completed,
        cancelled      = FleetColors.Cancelled,
        inProgress     = FleetColors.InProgress,
        priorityLow    = FleetColors.PriorityLow,
        priorityNormal = FleetColors.PriorityNormal,
        priorityHigh   = FleetColors.PriorityHigh,
        priorityUrgent = FleetColors.PriorityUrgent,
        mapBg          = FleetColors.MapBg,
        mapRoute       = FleetColors.MapRoute,
        mapConnect     = FleetColors.MapConnect,
        mapOffline     = FleetColors.MapOffline,
        surface2       = FleetColors.Surface2,
        border         = FleetColors.Border,
        text1          = FleetColors.Text1,
        text2          = FleetColors.Text2,
        background     = FleetColors.Surface,
        surface        = FleetColors.Surface2,
        primary        = FleetColors.Primary,
        onPrimary      = Color.White,
        onBackground   = FleetColors.Text1,
        onSurface      = FleetColors.Text1,
        overdue        = FleetColors.Cancelled,
        paid           = FleetColors.Active,
        surfaceVariant = Color(0xFF1C2436),
    )
} else {
    FleetExtendedColors(
        available      = FleetColors.Available,
        rented         = FleetColors.Rented,
        maintenance    = FleetColors.Maintenance,
        retired        = FleetColors.Retired,
        reserved       = FleetColors.Reserved,
        active         = FleetColors.Active,
        completed      = FleetColors.Completed,
        cancelled      = FleetColors.Cancelled,
        inProgress     = FleetColors.InProgress,
        priorityLow    = FleetColors.PriorityLow,
        priorityNormal = FleetColors.PriorityNormal,
        priorityHigh   = FleetColors.PriorityHigh,
        priorityUrgent = FleetColors.PriorityUrgent,
        mapBg          = Color(0xFFE2E8F0),
        mapRoute       = Color(0xFF94A3B8),
        mapConnect     = FleetColors.Available,
        mapOffline     = FleetColors.Cancelled,
        surface2       = FleetColors.LightSurface2,
        border         = FleetColors.LightBorder,
        text1          = FleetColors.LightText1,
        text2          = FleetColors.LightText2,
        background     = FleetColors.LightSurface,
        surface        = FleetColors.LightSurface2,
        primary        = FleetColors.LightPrimary,
        onPrimary      = Color.White,
        onBackground   = FleetColors.LightText1,
        onSurface      = FleetColors.LightText1,
        overdue        = FleetColors.Cancelled,
        paid           = FleetColors.Active,
        surfaceVariant = FleetColors.LightSurfaceVariant,
    )
}

// ─── Theme entry point ────────────────────────────────────────────────────────

@Composable
fun FleetTheme(
    themeState: ThemeState = remember { ThemeState(initialDark = true) },
    content: @Composable () -> Unit,
) {
    val extendedColors = buildExtendedColors(themeState.isDark)
    val colorScheme = if (themeState.isDark) FleetDarkColorScheme else FleetLightColorScheme

    CompositionLocalProvider(
        LocalFleetColors provides extendedColors,
        LocalThemeState provides themeState,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = fleetTypography(),
            content     = content,
        )
    }
}

/** Convenience accessor — use inside any @Composable. */
val fleetColors: FleetExtendedColors
    @Composable get() = LocalFleetColors.current

/** Convenience accessor for the mutable theme state. */
val fleetThemeState: ThemeState
    @Composable get() = LocalThemeState.current
