package com.nexo.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NexoPalette(
    val accent: Color,
    val background: Color,
    val surface: Color,
    val highlight: Color
)

val PaletteOriginal = NexoPalette(
    accent = Color(0xFFC837AB),
    background = Color(0xFF0B1019),
    surface = Color(0xFF161C28),
    highlight = Color(0xFFFF508E)
)

val LocalNexoPalette = compositionLocalOf { PaletteOriginal }
val LocalNexoProfileIcon = staticCompositionLocalOf { "👤" }

fun parseNexoPalette(value: String): NexoPalette {
    val colors = value.split('|').mapNotNull { color ->
        runCatching { Color(android.graphics.Color.parseColor(color)) }.getOrNull()
    }
    return if (colors.size == 4) NexoPalette(colors[0], colors[1], colors[2], colors[3]) else PaletteOriginal
}
