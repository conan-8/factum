package com.pigeonstudy.pigeon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

private val comparisonPalette = listOf(
    0x3478F6, 0xFF3B30, 0x34C759, 0xFF9500, 0xAF52DE, 0x5AC8FA,
    0xFF2D55, 0xA2845E, 0xFFCC00, 0x00C7BE, 0xBF5AF2, 0x64D2FF,
).map { Color(it) }

data class Bucket(val label: String, val subjectSeconds: Map<String, Int>)

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val c = pigeonColors()
    androidx.activity.compose.BackHandler { onBack() }
    var mode by remember { mutableIntStateOf(0) } // Day Week Month Year Lifetime
    val modes = listOf("Day", "Week", "Month", "Year", "Lifetime")
    var ref by remember { mutableStateOf(System.currentTimeMillis()) }

    val user = remember(Store.timelapses) { Store.userTimelapses() }

    // period bounds
    val cal = Calendar.getInstance()
    val (startMs, endMs, navLabel) = remember(ref, mode) {
        val k = Calendar.getInstance()
        k.timeInMillis = ref
        k.set(Calendar.HOUR_OF_DAY, 0); k.set(Calendar.MINUTE, 0); k.set(Calendar.SECOND, 0); k.set(Calendar.MILLISECOND, 0)
        when (mode) {
            0 -> { val s = k.timeInMillis; Triple(s, s + 86400000L, "Today") }
            1 -> {
                k.firstDayOfWeek = Calendar.SUNDAY
                k.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val s = k.timeInMillis
                Triple(s, s + 7 * 86400000L, "this week")
            }
            2 -> {
                k.set(Calendar.DAY_OF_MONTH, 1)
                val s = k.timeInMillis
                val e = Calendar.getInstance().apply { timeInMillis = s; add(Calendar.MONTH, 1) }.timeInMillis
                Triple(s, e, "this month")
            }
            3 -> {
                k.set(Calendar.DAY_OF_YEAR, 1)
                val s = k.timeInMillis
                val e = Calendar.getInstance().apply { timeInMillis = s; add(Calendar.YEAR, 1) }.timeInMillis
                Triple(s, e, "this year")
            }
            else -> Triple(0L, Long.MAX_VALUE, "All Time")
        }
    }

    val inPeriod = user.filter { it.createdAt >= startMs && it.createdAt < endMs }
    val totalSecs = inPeriod.sumOf { it.durationSeconds }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        // header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = c.primaryText, modifier = Modifier.size(28.dp)) }
            Text("Study Stats", fontFamily = gloucester, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
        }
        Spacer(Modifier.height(8.dp))
        UnderlinePicker(modes, mode) { mode = it; ref = System.currentTimeMillis() }
        // date navigator
        if (mode < 4) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                IconButton(onClick = { ref -= periodStepMs(mode) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = c.secondaryText)
                }
                Text(periodLabel(mode, ref), fontSize = 14.sp, color = c.secondaryText, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { if (ref + periodStepMs(mode) < System.currentTimeMillis()) ref += periodStepMs(mode) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = c.tertiaryText)
                }
            }
        }

        // Chart card
        GlassCard(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.durationHm(totalSecs), fontFamily = gloucester, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                Text(navLabel, fontSize = 12.sp, color = c.tertiaryText)
                Spacer(Modifier.height(14.dp))
                when (mode) {
                    0 -> DonutChart(subjectSlices(inPeriod), "today")
                    1 -> WeekStackedChart(inPeriod, startMs)
                    2 -> MonthHeatmap(inPeriod, startMs, c)
                    3 -> YearBars(inPeriod)
                    else -> DonutChart(subjectSlices(user), "all time")
                }
            }
        }

        // Summary
        val sessions = inPeriod.size
        val subjectsCount = inPeriod.flatMap { it.subjectSegments.map { s -> s.subject } }.distinct().size
        val leaves = inPeriod.sumOf { it.appLeaveCount }
        val avgSession = if (sessions > 0) totalSecs.toFloat() / sessions else 0f
        val avgHour = if (totalSecs > 0) leaves.toFloat() / (totalSecs / 3600f) else 0f
        val longest = inPeriod.maxOfOrNull { it.durationSeconds } ?: 0
        val mostLeaves = inPeriod.maxOfOrNull { it.appLeaveCount } ?: 0
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Total Time", Fmt.durationHm(totalSecs), Modifier.weight(1f))
            SummaryCard("Sessions", "$sessions", Modifier.weight(1f))
            SummaryCard("Subjects", "$subjectsCount", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("App Leaves", "$leaves", Modifier.weight(1f), accent = if (leaves == 0) Color(0.3f, 0.7f, 0.35f) else c.destructive)
            SummaryCard("Avg/Session", Fmt.durationHm(avgSession.toInt()), Modifier.weight(1f))
            SummaryCard("Avg/Hour", String.format("%.1f", avgHour), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard("Longest Session", Fmt.durationHm(longest), Modifier.weight(1f))
            SummaryCard("Most Leaves", "$mostLeaves", Modifier.weight(1f))
        }

        // Most studied records
        Spacer(Modifier.height(20.dp))
        Text("Most Studied", fontFamily = gloucester, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
        Spacer(Modifier.height(8.dp))
        listOf("Day" to 86400000L, "Week" to 7 * 86400000L, "Month" to 30L * 86400000L, "Year" to 365L * 86400000L).forEach { (label, ms) ->
            val best = bestWindow(user, ms)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 13.sp, color = c.tertiaryText, modifier = Modifier.width(56.dp))
                Text(best.first, fontSize = 12.sp, color = c.secondaryText, modifier = Modifier.weight(1f))
                Text(Fmt.durationHm(best.second), fontFamily = gloucester, fontSize = 14.sp, color = c.primaryText)
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

private fun bestWindow(user: List<StudyTimelapse>, windowMs: Long): Pair<String, Int> {
    if (user.isEmpty()) return "—" to 0
    val sorted = user.sortedBy { it.createdAt }
    var best = 0
    var bestStart = 0L
    var j = 0
    var sum = 0
    for (i in sorted.indices) {
        sum += sorted[i].durationSeconds
        while (sorted[i].createdAt - sorted[j].createdAt >= windowMs) { sum -= sorted[j].durationSeconds; j++ }
        if (sum > best) { best = sum; bestStart = sorted[i].createdAt }
    }
    val fmt = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
    return (fmt.format(java.util.Date(bestStart)) to best)
}

private fun periodStepMs(mode: Int): Long = when (mode) {
    0 -> 86400000L; 1 -> 7 * 86400000L; 2 -> 30L * 86400000L; else -> 365L * 86400000L
}

private fun periodLabel(mode: Int, ref: Long): String {
    val fmtLong = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US)
    val fmtShort = java.text.SimpleDateFormat("MMM d", java.util.Locale.US)
    val cal = Calendar.getInstance().apply { timeInMillis = ref }
    return when (mode) {
        0 -> if (fmtLong.format(cal.time) == fmtLong.format(java.util.Date())) "Today"
             else fmtLong.format(cal.time)
        1 -> {
            val end = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
            "${fmtShort.format(cal.time)} – ${fmtShort.format(end.time)}"
        }
        2 -> java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US).format(cal.time)
        else -> java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(cal.time)
    }
}

private fun subjectSlices(timelapses: List<StudyTimelapse>): List<Triple<String, Int, Color>> {
    val map = HashMap<String, Int>()
    timelapses.forEach { t -> t.subjectSegments.forEach { map[it.subject] = (map[it.subject] ?: 0) + it.seconds } }
    return map.entries.sortedByDescending { it.value }.map { (s, secs) ->
        Triple(s, secs, subjectColor(s, Store.subjects))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier, accent: Color? = null) {
    val c = pigeonColors()
    Column(
        modifier.clip(RoundedCornerShape(PigeonTheme.cornerField)).background(c.cardBackground.copy(alpha = 0.6f)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontFamily = gloucester, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accent ?: c.primaryText)
        Text(label, fontSize = 10.sp, color = c.tertiaryText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DonutChart(slices: List<Triple<String, Int, Color>>, centerLabel: String) {
    val c = pigeonColors()
    val total = slices.sumOf { it.second }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            if (total == 0) {
                Text("the nest is quiet", fontFamily = gloucester, fontSize = 16.sp, color = c.tertiaryText)
            } else {
                Canvas(Modifier.fillMaxSize()) {
                    var start = -90f
                    val diameter = size.minDimension * 0.8f
                    val inset = (size.minDimension - diameter) / 2
                    slices.forEach { (_, secs, col) ->
                        val sweep = secs.toFloat() / total * 360f
                        drawArc(
                            color = col, startAngle = start, sweepAngle = sweep - 2,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(diameter, diameter),
                            style = Stroke(width = diameter * 0.28f),
                        )
                        start += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Fmt.durationHm(total), fontFamily = gloucester, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                    Text(centerLabel, fontSize = 11.sp, color = c.tertiaryText)
                }
            }
        }
        if (total > 0) {
            Spacer(Modifier.height(10.dp))
            slices.take(5).forEach { (name, secs, col) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(col))
                    Text("  $name", fontSize = 12.sp, color = c.secondaryText, modifier = Modifier.weight(1f))
                    val pct = secs * 100 / total
                    Text("${Fmt.durationHm(secs)} ($pct%)", fontSize = 12.sp, color = c.tertiaryText)
                }
            }
        }
    }
}

@Composable
private fun WeekStackedChart(inPeriod: List<StudyTimelapse>, weekStart: Long) {
    val c = pigeonColors()
    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")
    val buckets = (0..6).map { d ->
        val s = weekStart + d * 86400000L
        val e = s + 86400000L
        val dayTl = inPeriod.filter { it.createdAt >= s && it.createdAt < e }
        val map = HashMap<String, Int>()
        dayTl.forEach { t -> t.subjectSegments.forEach { map[it.subject] = (map[it.subject] ?: 0) + it.seconds } }
        Bucket(dayNames[d], map)
    }
    val maxMins = buckets.maxOfOrNull { b -> b.subjectSeconds.values.sum() / 60 } ?: 0
    Column {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val barMax = size.height * 0.9f
            val slot = size.width / 7f
            buckets.forEachIndexed { i, b ->
                val totalMin = b.subjectSeconds.values.sum() / 60f
                var yBottom = size.height
                b.subjectSeconds.entries.sortedByDescending { it.value }.forEach { (subject, secs) ->
                    val h = (secs / 60f) / maxMins * barMax
                    drawRect(
                        color = subjectColor(subject, Store.subjects),
                        topLeft = Offset(i * slot + slot * 0.2f, yBottom - h),
                        size = Size(slot * 0.6f, h),
                    )
                    yBottom -= h
                }
                if (totalMin == 0f) {
                    drawRect(
                        color = c.separator.copy(alpha = 0.35f),
                        topLeft = Offset(i * slot + slot * 0.2f, size.height - 3f),
                        size = Size(slot * 0.6f, 3f),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            buckets.forEach {
                Text(it.label, fontSize = 11.sp, color = c.tertiaryText, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Text("Tap bar for details", fontSize = 10.sp, color = c.tertiaryText, modifier = Modifier.padding(top = 4.dp))
        // legend
        buckets.forEachIndexed { i, b ->
            val total = b.subjectSeconds.values.sum()
            if (total > 0) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(b.label, fontSize = 11.sp, color = c.tertiaryText, modifier = Modifier.width(16.dp))
                    b.subjectSegmentsLegend(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Bucket.subjectSegmentsLegend(modifier: Modifier) {
    val c = pigeonColors()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        subjectSeconds.entries.sortedByDescending { it.value }.take(3).forEach { (subject, secs) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(subjectColor(subject, Store.subjects)))
                Text(" ${Fmt.durationHm(secs)}", fontSize = 11.sp, color = c.secondaryText)
            }
        }
    }
}

@Composable
private fun MonthHeatmap(inPeriod: List<StudyTimelapse>, monthStart: Long, c: com.pigeonstudy.pigeon.PigeonColors) {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) - 1
    val byDay = HashMap<Int, Int>()
    inPeriod.forEach { t ->
        val d = ((t.createdAt - monthStart) / 86400000L).toInt()
        if (d in 0 until daysInMonth) byDay[d + 1] = (byDay[d + 1] ?: 0) + t.durationSeconds
    }
    val maxMins = (byDay.values.maxOrNull() ?: 0) / 60
    val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val isCurrentMonth = monthStart <= System.currentTimeMillis() &&
        System.currentTimeMillis() < monthStart + 30L * 86400000L

    Column {
        Row(Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, fontSize = 10.sp, color = c.tertiaryText, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        // grid: rows of 7
        val cells = firstDow + daysInMonth
        val rows = (cells + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            var day = 1 - firstDow
            repeat(rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) {
                        if (day in 1..daysInMonth) {
                            val secs = byDay[day] ?: 0
                            val intensity = if (maxMins > 0) secs / 60f / maxMins else 0f
                            val bg = if (secs > 0) c.accent.copy(alpha = 0.3f + intensity * 0.7f) else c.surfaceBackground
                            val isToday = isCurrentMonth && day == today
                            Box(
                                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(6.dp))
                                    .background(if (isToday) c.primaryText else bg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$day", fontSize = 11.sp,
                                        color = if (isToday) c.background else c.primaryText,
                                    )
                                    if (secs > 0) Text(
                                        if (secs >= 3600) Fmt.durationHm(secs) else "${secs / 60}m",
                                        fontSize = 8.sp,
                                        color = if (isToday) c.background else c.secondaryText,
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f).height(44.dp))
                        }
                        day++
                    }
                }
            }
        }
    }
}

@Composable
private fun YearBars(inPeriod: List<StudyTimelapse>) {
    val c = pigeonColors()
    val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    val cal = Calendar.getInstance()
    val sums = IntArray(12)
    inPeriod.forEach { t ->
        cal.timeInMillis = t.createdAt
        sums[cal.get(Calendar.MONTH)] += t.durationSeconds
    }
    val maxHrs = (sums.maxOrNull() ?: 0) / 3600f
    Column {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val barMax = size.height * 0.9f
            val slot = size.width / 12f
            sums.forEachIndexed { i, secs ->
                val h = if (maxHrs > 0) (secs / 3600f) / maxHrs * barMax else 0f
                drawRect(
                    color = comparisonPalette[i % comparisonPalette.size],
                    topLeft = Offset(i * slot + slot * 0.2f, size.height - h),
                    size = Size(slot * 0.6f, max(h, 3f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            months.forEach {
                Text(it, fontSize = 10.sp, color = c.tertiaryText, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}
