package com.nexo.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

data class NexoPalette(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val highlight: Color
) {
    /** Contrasting foreground colors for user-selected light or dark theme colors. */
    val onBackground: Color get() = contrastingTextColor(background)
    val onSurface: Color get() = contrastingTextColor(surface)
    val onAccent: Color get() = contrastingTextColor(accent)
    val onHighlight: Color get() = contrastingTextColor(highlight)
    val accentTextOnSurface: Color get() = readableForeground(accent, surface)
    val highlightTextOnSurface: Color get() = readableForeground(highlight, surface)
    val accentTextOnBackground: Color get() = readableForeground(accent, background)
    val secondaryOnBackground: Color get() = onBackground.copy(alpha = 0.72f)
    val secondaryOnSurface: Color get() = onSurface.copy(alpha = 0.72f)
}

val PaletteOriginal = NexoPalette(
    accent = Color(0xFFC837AB),
    background = Color(0xFF0B1019),
    surface = Color(0xFF161C28),
    highlight = Color(0xFFFF508E)
)

val LocalNexoPalette = compositionLocalOf { PaletteOriginal }
val LocalNexoProfileIcon = staticCompositionLocalOf { "👤" }

/** Chooses black or white by WCAG contrast against the supplied theme color. */
fun contrastingTextColor(background: Color): Color {
    val luminance = relativeLuminance(background)
    val contrastWithBlack = (luminance + 0.05f) / 0.05f
    val contrastWithWhite = 1.05f / (luminance + 0.05f)
    return if (contrastWithBlack >= contrastWithWhite) Color.Black else Color.White
}

fun readableForeground(preferred: Color, background: Color): Color =
    if (contrastRatio(preferred, background) >= 4.5f) preferred else contrastingTextColor(background)

private fun contrastRatio(first: Color, second: Color): Float {
    val luminanceA = relativeLuminance(first)
    val luminanceB = relativeLuminance(second)
    val lighter = maxOf(luminanceA, luminanceB)
    val darker = minOf(luminanceA, luminanceB)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun relativeLuminance(color: Color): Float {
    fun channel(value: Float): Float = if (value <= 0.04045f) value / 12.92f else ((value + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return 0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue)
}

fun parseNexoPalette(value: String): NexoPalette {
    val colors = value.split('|').mapNotNull { color ->
        runCatching { Color(android.graphics.Color.parseColor(color)) }.getOrNull()
    }
    return if (colors.size == 4) NexoPalette(colors[0], colors[1], colors[2], colors[3]) else PaletteOriginal
}
