package com.karishma.swiggyanimation.music

import androidx.compose.ui.graphics.Color

internal data class TrackPalette(
    val bg: Color,
    val surface: Color,
    val accent: Color,
    val accentSoft: Color,
) {
    companion object {
        private val Fallbacks = listOf(
            TrackPalette(Color(0xFF0E0B1E), Color(0xFF1A1530), Color(0xFFB14EFF), Color(0xFF7B2FBE)),
            TrackPalette(Color(0xFF1A0018), Color(0xFF2D0030), Color(0xFFFF3D8A), Color(0xFFFF006E)),
            TrackPalette(Color(0xFF001219), Color(0xFF012A36), Color(0xFF00D4FF), Color(0xFF00B4D8)),
            TrackPalette(Color(0xFF0A1500), Color(0xFF162700), Color(0xFFA8E312), Color(0xFF80B918)),
            TrackPalette(Color(0xFF1A0A00), Color(0xFF2E1500), Color(0xFFFFB84D), Color(0xFFFF8500)),
            TrackPalette(Color(0xFF000B1A), Color(0xFF001833), Color(0xFF4D9DFF), Color(0xFF1E66D6)),
        )

        fun forIndex(index: Int) = Fallbacks[((index % Fallbacks.size) + Fallbacks.size) % Fallbacks.size]
    }
}

internal fun darken(color: Color, factor: Float) = Color(
    red = (color.red * (1f - factor)).coerceIn(0f, 1f),
    green = (color.green * (1f - factor)).coerceIn(0f, 1f),
    blue = (color.blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = color.alpha,
)

internal fun Long.toTimestamp(): String {
    val totalSec = coerceAtLeast(0L) / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
