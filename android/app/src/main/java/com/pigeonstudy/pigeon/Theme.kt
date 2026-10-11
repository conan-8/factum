package com.pigeonstudy.pigeon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val gloucester = FontFamily(
    Font(R.font.gloucester_os, FontWeight.Normal),
    Font(R.font.gloucester_condensed, FontWeight.Bold),
)

data class PigeonColors(
    val background: Color, val cardBackground: Color, val surfaceBackground: Color,
    val elevated: Color, val primaryText: Color, val secondaryText: Color,
    val tertiaryText: Color, val accent: Color, val accentText: Color,
    val separator: Color, val destructive: Color,
)

object PigeonTheme {
    val light = PigeonColors(
        background = Color(0.98f, 0.96f, 0.92f),
        cardBackground = Color(0.91f, 0.87f, 0.82f),
        surfaceBackground = Color(0.92f, 0.89f, 0.84f),
        elevated = Color(0.93f, 0.90f, 0.85f),
        primaryText = Color(0.13f, 0.10f, 0.08f),
        secondaryText = Color(0.44f, 0.39f, 0.34f),
        tertiaryText = Color(0.56f, 0.51f, 0.46f),
        accent = Color(0.84f, 0.79f, 0.72f),
        accentText = Color(0.13f, 0.10f, 0.08f),
        separator = Color(0.80f, 0.75f, 0.68f),
        destructive = Color(0.78f, 0.28f, 0.24f),
    )
    val dark = PigeonColors(
        background = Color(0.06f, 0.06f, 0.05f),
        cardBackground = Color(0.13f, 0.12f, 0.11f),
        surfaceBackground = Color(0.09f, 0.08f, 0.07f),
        elevated = Color(0.19f, 0.18f, 0.16f),
        primaryText = Color(0.93f, 0.91f, 0.88f),
        secondaryText = Color(0.60f, 0.58f, 0.54f),
        tertiaryText = Color(0.43f, 0.41f, 0.38f),
        accent = Color(0.28f, 0.26f, 0.23f),
        accentText = Color(0.93f, 0.91f, 0.88f),
        separator = Color(0.22f, 0.20f, 0.18f),
        destructive = Color(0.78f, 0.28f, 0.24f),
    )

    val spacing4 = 4.dp; val spacing8 = 8.dp; val spacing12 = 12.dp
    val spacing16 = 16.dp; val spacing24 = 24.dp; val spacing32 = 32.dp

    val cornerBadge = 8.dp; val cornerField = 12.dp; val cornerCard = 16.dp; val cornerSheet = 24.dp

    val titleFont = 32.sp; val headlineFont = 24.sp; val subheadlineFont = 19.sp
    val bodyFont = 17.sp; val captionFont = 15.sp; val smallFont = 13.sp
}

val LocalPigeonColors = staticCompositionLocalOf { PigeonTheme.light }

@Composable
fun pigeonColors(dark: Boolean = isSystemInDarkTheme()) = if (dark) PigeonTheme.dark else PigeonTheme.light

fun rarityColor(r: PunRarity, dark: Boolean): Color = when (r) {
    PunRarity.COMMON -> Color(0.60f, 0.58f, 0.54f)
    PunRarity.UNCOMMON -> Color(0.40f, 0.72f, 0.44f)
    PunRarity.RARE -> Color(0.30f, 0.70f, 0.78f)
    PunRarity.EPIC -> Color(0.62f, 0.40f, 0.82f)
    PunRarity.LEGENDARY -> Color(0.85f, 0.72f, 0.30f)
    PunRarity.MYTHIC -> Color(0.88f, 0.28f, 0.28f)
}

fun parseHex(hex: String?): Color? {
    if (hex == null) return null
    val h = hex.removePrefix("#").trim()
    if (h.length != 6) return null
    return try {
        val rgb = h.toLong(16)
        Color(((rgb shr 16) and 0xFF).toInt(), ((rgb shr 8) and 0xFF).toInt(), (rgb and 0xFF).toInt())
    } catch (_: Exception) { null }
}

fun subjectColor(name: String, subjects: List<StudySubject>): Color {
    val s = subjects.firstOrNull { it.name.equals(name, ignoreCase = true) }
    return parseHex(s?.colorHex) ?: Color(0.4f, 0.4f, 0.4f)
}

fun Modifier.glassCard(
    corner: Dp = PigeonTheme.cornerCard,
    tint: Color,
    border: Color,
): Modifier = this
    .clip(RoundedCornerShape(corner))
    .background(tint)
    .border(0.5.dp, border.copy(alpha = 0.6f), RoundedCornerShape(corner))

// MARK: - Wallpapers

data class GradientPreset(val id: String, val name: String, val colors: List<Color>, val diagonal: Boolean = false)
data class SolidPreset(val id: String, val name: String, val hex: String)

object Wallpapers {
    private fun c(r: Float, g: Float, b: Float) = Color(r, g, b)

    val gradients = listOf(
        GradientPreset("gradient_sunset", "Sunset", listOf(c(0.96f, 0.42f, 0.26f), c(0.85f, 0.24f, 0.45f), c(0.55f, 0.18f, 0.56f))),
        GradientPreset("gradient_ocean", "Ocean", listOf(c(0.05f, 0.20f, 0.45f), c(0.08f, 0.45f, 0.55f), c(0.10f, 0.58f, 0.52f))),
        GradientPreset("gradient_forest", "Forest", listOf(c(0.10f, 0.25f, 0.12f), c(0.15f, 0.42f, 0.18f), c(0.22f, 0.55f, 0.25f))),
        GradientPreset("gradient_lavender", "Lavender", listOf(c(0.55f, 0.35f, 0.75f), c(0.75f, 0.50f, 0.80f), c(0.90f, 0.70f, 0.85f)), diagonal = true),
        GradientPreset("gradient_midnight", "Midnight", listOf(c(0.05f, 0.05f, 0.15f), c(0.10f, 0.10f, 0.25f), c(0.15f, 0.08f, 0.20f))),
        GradientPreset("gradient_ember", "Ember", listOf(c(0.35f, 0.08f, 0.05f), c(0.65f, 0.18f, 0.08f), c(0.85f, 0.35f, 0.10f)), diagonal = true),
        GradientPreset("gradient_arctic", "Arctic", listOf(c(0.78f, 0.88f, 0.95f), c(0.55f, 0.75f, 0.90f), c(0.40f, 0.60f, 0.85f))),
        GradientPreset("gradient_golden", "Golden Hour", listOf(c(0.95f, 0.75f, 0.30f), c(0.90f, 0.55f, 0.25f), c(0.80f, 0.40f, 0.20f)), diagonal = true),
        GradientPreset("gradient_moss", "Moss", listOf(c(0.35f, 0.38f, 0.22f), c(0.48f, 0.50f, 0.30f), c(0.60f, 0.58f, 0.40f))),
        GradientPreset("gradient_storm", "Storm", listOf(c(0.15f, 0.15f, 0.18f), c(0.25f, 0.25f, 0.30f), c(0.35f, 0.35f, 0.38f))),
        GradientPreset("gradient_blush", "Blush", listOf(c(0.95f, 0.75f, 0.75f), c(0.92f, 0.60f, 0.65f), c(0.88f, 0.50f, 0.58f)), diagonal = true),
        GradientPreset("gradient_dusk", "Dusk", listOf(c(0.15f, 0.08f, 0.30f), c(0.25f, 0.12f, 0.45f), c(0.10f, 0.10f, 0.30f))),
        GradientPreset("gradient_parchment", "Parchment+", listOf(c(0.96f, 0.90f, 0.82f), c(0.92f, 0.85f, 0.76f), c(0.88f, 0.80f, 0.70f))),
        GradientPreset("gradient_noir", "Noir", listOf(Color(0f, 0f, 0f), Color(0.08f, 0.08f, 0.08f), Color(0f, 0f, 0f)), diagonal = true),
    )

    val patterns = listOf("Dots" to "pattern_dots", "Lines" to "pattern_lines", "Grid" to "pattern_grid", "Waves" to "pattern_waves", "Feathers" to "pattern_feathers")

    val solids = listOf(
        SolidPreset("color_black", "Black", "0A0A0A"),
        SolidPreset("color_white", "White", "F5F5F0"),
        SolidPreset("color_warmgray", "Warm Gray", "8C857A"),
        SolidPreset("color_navy", "Navy", "1B2838"),
        SolidPreset("color_forest", "Forest", "1A3A2A"),
        SolidPreset("color_burgundy", "Burgundy", "4A1525"),
        SolidPreset("color_slate", "Slate", "3A3F47"),
        SolidPreset("color_cream", "Cream", "F0E6D4"),
    )

    fun wallpaperBrush(id: String): Brush? {
        val g = gradients.firstOrNull { it.id == id } ?: return null
        return if (g.diagonal) Brush.linearGradient(g.colors)
        else Brush.verticalGradient(g.colors)
    }
}

// MARK: - Formatters

object Fmt {
    fun timeAgo(then: Long, now: Long = System.currentTimeMillis()): String {
        val s = (now - then) / 1000
        return when {
            s < 60 -> "just now"
            s < 3600 -> "${s / 60}m ago"
            s < 86400 -> "${s / 3600}h ago"
            s < 7 * 86400 -> "${s / 86400}d ago"
            else -> "${s / (7 * 86400)}w ago"
        }
    }

    fun durationShort(totalSeconds: Int): String {
        val h = totalSeconds / 3600; val m = (totalSeconds % 3600) / 60; val s = totalSeconds % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            m > 0 && s > 0 -> "${m}m ${s}s"
            m > 0 -> "${m}m"
            else -> "${s}s"
        }
    }

    fun durationHm(totalSeconds: Int): String {
        val h = totalSeconds / 3600; val m = (totalSeconds % 3600) / 60
        return when {
            h > 0 -> "${h}h ${m}m"
            else -> "${m}m"
        }
    }

    fun clock(totalSeconds: Int): String {
        val h = totalSeconds / 3600; val m = (totalSeconds % 3600) / 60; val s = totalSeconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }
}

object Haptics {
    fun light(view: android.view.View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }
    fun medium(view: android.view.View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
