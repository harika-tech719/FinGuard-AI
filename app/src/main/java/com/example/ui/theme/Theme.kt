package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class FinGuardExtendedColors(
    val obsidian: Color = ObsidianBlack,
    val midnight: Color = DeepMidnightBlue,
    val graphiteSurface: Color = GraphiteNavy,
    val secondarySurface: Color = SecondarySurface,
    val cardBorder: Color = SubtleBorder,
    val oceanBlue: Color = OceanBlue,
    val deepOceanBlue: Color = DeepOceanBlue,
    val tropicalAqua: Color = TropicalAqua,
    val auroraMint: Color = AuroraMint,
    val lightCyan: Color = LightCyan,
    val emerald: Color = EmeraldGreen,
    val luxuryMint: Color = LuxuryMintGreen,
    val champagneGold: Color = ChampagneGold,
    val softGold: Color = SoftGold,
    val goldBg: Color = GoldContainerBg,
    // Semantic & warning aliases preserved for consistent screen usage
    val neonCyan: Color = LightCyan,
    val royalPurple: Color = ChampagneGold,
    val safe: Color = AuroraMint,
    val safeBg: Color = BudgetHealthyBg,
    val moderateLime: Color = BudgetModerateLime,
    val moderateBg: Color = BudgetModerateBg,
    val caution: Color = BudgetCautionGold,
    val cautionBg: Color = BudgetCautionBg,
    val highOrange: Color = BudgetHighOrange,
    val highOrangeBg: Color = BudgetHighBg,
    val danger: Color = BudgetExceededCoral,
    val dangerBg: Color = BudgetExceededBg,
    val critical: Color = BudgetExceededCoral,
    val mutedText: Color = TextMuted
)

val LocalFinGuardColors = staticCompositionLocalOf { FinGuardExtendedColors() }

private val FinGuardDarkColorScheme = darkColorScheme(
    primary = OceanBlue,
    onPrimary = TextPrimary,
    primaryContainer = DeepOceanBlue,
    onPrimaryContainer = AuroraMint,
    secondary = TropicalAqua,
    onSecondary = ObsidianBlack,
    secondaryContainer = DeepAquaContainer,
    onSecondaryContainer = AuroraMint,
    tertiary = AuroraMint,
    onTertiary = ObsidianBlack,
    tertiaryContainer = BudgetHealthyBg,
    onTertiaryContainer = LuxuryMintGreen,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = DeepMidnightBlue,
    onSurface = TextPrimary,
    surfaceVariant = GraphiteNavy,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = DeepMidnightBlue,
    surfaceContainer = GraphiteNavy,
    surfaceContainerHigh = SecondarySurface,
    surfaceContainerHighest = ElevatedOceanSurface,
    outline = SubtleBorder,
    outlineVariant = SubtleOutlineVariant,
    error = BudgetExceededCoral,
    onError = TextPrimary,
    errorContainer = BudgetExceededBg,
    onErrorContainer = Color(0xFFFFD8D8)
)

val FinGuardShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun FinGuardTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalFinGuardColors provides FinGuardExtendedColors()) {
        MaterialTheme(
            colorScheme = FinGuardDarkColorScheme,
            typography = FinGuardTypography,
            shapes = FinGuardShapes,
            content = content
        )
    }
}
