package com.pigeonstudy.pigeon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WallpaperBackground(content: @Composable BoxScope.() -> Unit) {
    val dark = when (Store.appearanceMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val c = pigeonColors(dark)
    val brush = Wallpapers.wallpaperBrush(Store.wallpaperId)
    Box(Modifier.fillMaxSize().background(c.background)) {
        if (brush != null) Box(Modifier.fillMaxSize().background(brush))
        content()
    }
}

@Composable
fun PigeonHeading(text: String, size: Int = 32, color: Color? = null) {
    val c = pigeonColors()
    Text(
        text, fontFamily = gloucester, fontSize = size.sp,
        fontWeight = FontWeight.Bold, color = color ?: c.primaryText,
        letterSpacing = (size / 16f).sp,
    )
}

@Composable
fun UnderlinePicker(tabs: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val c = pigeonColors()
    Row(horizontalArrangement = Arrangement.spacedBy(PigeonTheme.spacing24)) {
        tabs.forEachIndexed { i, tab ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                Text(
                    tab, fontFamily = gloucester, fontSize = 17.sp,
                    color = if (selected == i) c.primaryText else c.tertiaryText,
                    fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Normal,
                )
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(if (selected == i) 24.dp else 0.dp)
                        .height(2.dp)
                        .background(if (selected == i) c.primaryText else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun PigeonButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    filled: Boolean = true, enabled: Boolean = true,
) {
    val c = pigeonColors()
    val bg = if (filled) c.primaryText else Color.Transparent
    val fg = if (filled) c.background else c.primaryText
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.textButtonColors(
            contentColor = fg, containerColor = bg,
            disabledContainerColor = c.separator, disabledContentColor = c.tertiaryText,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier,
    ) {
        Text(text, fontFamily = gloucester, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = pigeonColors()
    Column(
        modifier = modifier.glassCard(
            PigeonTheme.cornerCard,
            tint = c.cardBackground.copy(alpha = 0.72f),
            border = c.separator,
        ),
    ) { content() }
}

@Composable
fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    val c = pigeonColors()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = gloucester, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
        Text(label, fontSize = 12.sp, color = c.tertiaryText)
    }
}

@Composable
fun Avatar(name: String?, size: Int = 40, ring: Color? = null) {
    val c = pigeonColors()
    var m = Modifier.size(size.dp).clip(CircleShape).background(c.separator)
    if (ring != null) m = m.border(2.dp, ring, CircleShape)
    Box(m, contentAlignment = Alignment.Center) {
        val initial = name?.trim()?.firstOrNull()?.uppercase() ?: "?"
        Text(initial, fontFamily = gloucester, fontSize = (size * 0.42).sp, color = c.primaryText, fontWeight = FontWeight.Bold)
    }
}
