package com.pigeonstudy.pigeon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun RecapsView(onClose: () -> Unit) {
    val c = pigeonColors()
    androidx.activity.compose.BackHandler { onClose() }
    var period by remember { mutableIntStateOf(1) }
    val periods = listOf("Week", "Month", "Year", "All Time")
    var offset by remember { mutableIntStateOf(0) }

    val user = remember(Store.timelapses) { Store.userTimelapses() }
    val cal = Calendar.getInstance()

    data class RecPeriod(val start: Long, val end: Long, val label: String, val accent: Color)

    val period0 = remember(period, offset) {
        val k = Calendar.getInstance()
        when (period) {
            0 -> {
                k.firstDayOfWeek = Calendar.SUNDAY
                k.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); k.add(Calendar.WEEK_OF_YEAR, offset)
                val s = k.timeInMillis
                val fmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.US)
                RecPeriod(s, s + 7 * 86400000L, "Week of ${fmt.format(java.util.Date(s))}", Color(0.35f, 0.65f, 0.55f))
            }
            1 -> {
                k.set(Calendar.DAY_OF_MONTH, 1); k.add(Calendar.MONTH, offset)
                val s = k.timeInMillis
                RecPeriod(s, s + 30L * 86400000L, java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US).format(java.util.Date(s)), Color(0.72f, 0.52f, 0.35f))
            }
            2 -> {
                k.set(Calendar.DAY_OF_YEAR, 1); k.add(Calendar.YEAR, offset)
                val s = k.timeInMillis
                RecPeriod(s, s + 365L * 86400000L, "${k.get(Calendar.YEAR)}", Color(0.55f, 0.42f, 0.65f))
            }
            else -> RecPeriod(0L, Long.MAX_VALUE, "All Time", Color(0.78f, 0.62f, 0.40f))
        }
    }
    val (startMs, endMs, label, accent) = period0

    val inPeriod = user.filter { it.createdAt >= startMs && it.createdAt < endMs }
    val totalSecs = inPeriod.sumOf { it.durationSeconds }
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val leaves = inPeriod.sumOf { it.appLeaveCount }
    val focus = (100 - 2 * leaves).coerceAtLeast(0)
    val longest = inPeriod.maxOfOrNull { it.durationSeconds } ?: 0
    val subjectMap = HashMap<String, Int>()
    inPeriod.forEach { t -> t.subjectSegments.forEach { subjectMap[it.subject] = (subjectMap[it.subject] ?: 0) + it.seconds } }
    val topSubjects = subjectMap.entries.sortedByDescending { it.value }.take(3)
    val maxSubject = topSubjects.firstOrNull()?.value ?: 1

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("Recaps", fontFamily = gloucester, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.primaryText, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(10.dp))
            UnderlinePicker(periods, period) { period = it; offset = 0 }
            Spacer(Modifier.height(8.dp))
            if (period < 3) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = c.secondaryText) }
                    Text(label.ifEmpty { " " }, fontSize = 13.sp, color = c.secondaryText, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    IconButton(onClick = { if (offset < 0) offset++ }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = c.tertiaryText) }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Recap card
            Column(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.62f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.25f), c.background)
                        )
                    )
                    .padding(24.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("pigeon.", fontFamily = gloucester, fontSize = 16.sp, color = c.background)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "pigeon. " + when (period) { 0 -> "weekly"; 1 -> "monthly"; 2 -> "yearly"; else -> "wrapped" } + " recap",
                        fontSize = 10.sp, color = c.background.copy(alpha = 0.8f), letterSpacing = 2.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(if (totalSecs > 0) "$hours" else "no sessions this period",
                    fontFamily = gloucester, fontSize = if (totalSecs > 0) 92.sp else 22.sp,
                    fontWeight = FontWeight.Bold, color = if (totalSecs > 0) c.background else accent)
                if (totalSecs > 0) {
                    Text("hours studied", fontSize = 15.sp, color = c.background.copy(alpha = 0.85f))
                    if (minutes > 0) Text("and $minutes minutes", fontSize = 12.sp, color = c.background.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecapPill("${inPeriod.size} sessions", c.background, accent)
                    if (period == 3) RecapPill("${Store.streakDays()} streak", c.background, accent)
                    else RecapPill("$focus% focus", c.background, accent)
                    RecapPill("${longest / 60}m longest", c.background, accent)
                }
                Spacer(Modifier.height(22.dp))
                if (topSubjects.isNotEmpty()) {
                    Text("TOP SUBJECTS", fontSize = 10.sp, color = c.background.copy(alpha = 0.8f), letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    topSubjects.forEach { (subject, secs) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(subject, fontSize = 12.sp, color = c.background, modifier = Modifier.width(110.dp))
                            Box(
                                Modifier.weight(1f).height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(c.background.copy(alpha = 0.2f)),
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(secs.toFloat() / maxSubject)
                                        .fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(c.background)
                                )
                            }
                            Text(" ${Fmt.durationHm(secs)}", fontSize = 11.sp, color = c.background)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = c.background.copy(alpha = 0.3f))
                Text(
                    "${(Store.currentProfile?.displayName ?: "pigeon").lowercase()} · pigeon",
                    fontSize = 11.sp, color = c.background.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(40.dp))
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            Icon(Icons.Filled.Close, null, tint = c.primaryText)
        }
    }
}

@Composable
private fun RecapPill(text: String, fg: Color, bg: Color) {
    Text(
        text, fontSize = 11.sp, color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
