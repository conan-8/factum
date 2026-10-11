package com.pigeonstudy.pigeon

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar

object Greetings {
    val general = listOf(
        "coo coo", "ready when you are", "the nest awaits", "books are warm",
        "head down, feathers up", "another day, another crumb of knowledge",
    )
    val morning = listOf("early bird gets the A", "morning perch secured", "sunrise study session", "fresh crumbs this morning")
    val noon = listOf("midday grind", "lunch break? more like study break", "no rest for the studious")
    val evening = listOf("evening flap", "golden hour grind", "sunset over the desk")
    val night = listOf("burning the midnight feathers", "night owl pigeon", "the flock is asleep but you're up")

    fun pick(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val pool = when {
            hour in 5..11 -> morning; hour in 12..16 -> noon
            hour in 17..20 -> evening; else -> night
        }
        val all = general + pool
        return all.random()
    }
}

@Composable
fun FeedView(onRecord: () -> Unit, onOpenDetail: (StudyTimelapse) -> Unit) {
    val c = pigeonColors()
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val greeting = remember { Greetings.pick() }
    val firstName = Store.currentProfile?.displayName?.split(" ")?.firstOrNull() ?: "pigeon"
    val uid = Store.activeUid()
    val streak = remember(Store.timelapses) { Store.streakDays() }

    val visible = remember(Store.timelapses, query) {
        val base = Store.timelapses.sortedByDescending { t ->
            t.createdAt + (PunRarity.fromRaw(t.descriptionRarity)?.feedBoostSeconds ?: 0L) * 1000
        }
        if (query.isBlank()) base
        else base.filter {
            it.subject.contains(query, true) || it.caption.contains(query, true) ||
                it.studyDescription.contains(query, true) || it.authorName.contains(query, true)
        }
    }
    val unsynced = remember(Store.timelapses) { Store.timelapses.count { !it.cloudSynced && it.authorID == uid } }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PigeonHeading("pigeon.", 28)
            }
            IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                Icon(if (searchOpen) Icons.Filled.Close else Icons.Outlined.Search, null, tint = c.primaryText)
            }
        }
        if (searchOpen) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search by subject, caption...", fontFamily = gloucester, fontSize = 15.sp, color = c.tertiaryText) },
                singleLine = true,
                shape = RoundedCornerShape(PigeonTheme.cornerField),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.primaryText, unfocusedBorderColor = c.separator,
                    focusedTextColor = c.primaryText, unfocusedTextColor = c.primaryText,
                    cursorColor = c.primaryText,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (!searchOpen) {
                item {
                    Text(
                        "$greeting, $firstName.",
                        fontFamily = gloucester, fontSize = 18.sp, color = c.secondaryText,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(1f, 0.58f, 0.16f), modifier = Modifier.size(24.dp))
                            Text(" $streak", fontFamily = gloucester, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                            Text(" streak", fontSize = 13.sp, color = c.tertiaryText)
                            Spacer(Modifier.weight(1f))
                            PigeonButton("Record", onRecord)
                        }
                    }
                }
                if (unsynced > 0) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.CloudOff, null, tint = c.tertiaryText, modifier = Modifier.size(16.dp))
                            Text("  $unsynced session${if (unsynced > 1) "s" else ""} not yet backed up", fontSize = 13.sp, color = c.tertiaryText)
                        }
                    }
                }
            }

            if (visible.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("the nest is empty", fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                        Text(
                            if (query.isNotBlank()) "Try a different search term"
                            else "Record a study session and\nwatch your nest fill up.",
                            fontSize = 14.sp, color = c.tertiaryText, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (query.isBlank()) {
                            Spacer(Modifier.height(16.dp))
                            PigeonButton("Record a Session", onRecord)
                        }
                    }
                }
            } else {
                items(visible, key = { it.id }) { t -> TimelapseCard(t, onOpenDetail) }
            }
        }
    }
}

@Composable
fun TimelapseCard(t: StudyTimelapse, onOpenDetail: (StudyTimelapse) -> Unit) {
    val c = pigeonColors()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var heartBurst by remember { mutableStateOf(false) }
    val uid = Store.activeUid()
    val liked = uid != null && uid in t.likedByUIDs
    val rarity = PunRarity.fromRaw(t.descriptionRarity)
    val rarityCol = rarity?.let { rarityColor(it, false) }

    val rarityTint = rarityCol?.copy(alpha = 0.10f) ?: Color.Transparent
    Column(
        Modifier
            .fillMaxWidth()
            .glassCard(PigeonTheme.cornerCard, tint = c.cardBackground.copy(alpha = 0.72f), border = rarityCol ?: c.separator)
            .clickable { onOpenDetail(t) }
            .padding(16.dp),
    ) {
        // Author row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(t.authorName, size = 40)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.authorName, fontFamily = gloucester, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.timeAgo(t.createdAt), fontSize = 12.sp, color = c.tertiaryText)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).clip(CircleShape).background(subjectColor(t.subject, Store.subjects)))
                    Text("  ${t.subject}", fontSize = 12.sp, color = c.secondaryText)
                    if (t.hasMultipleSubjects) {
                        Text(" +${t.subjectSegments.size - 1}", fontSize = 12.sp, color = c.tertiaryText)
                    }
                }
            }
            if (t.authorID == uid) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.MoreHoriz, null, tint = c.tertiaryText, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = {
                            menu = false; onOpenDetail(t)
                        })
                        DropdownMenuItem(text = { Text("Delete", color = c.destructive) }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = c.destructive) }, onClick = {
                            menu = false
                            Store.deleteTimelapse(t)
                        })
                    }
                }
            }
        }

        if (expanded && t.hasMultipleSubjects) {
            Column(Modifier.padding(top = 8.dp)) {
                t.subjectSegments.sortedByDescending { it.seconds }.forEach { seg ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(subjectColor(seg.subject, Store.subjects)))
                        Text("  ${seg.subject}", fontSize = 13.sp, color = c.secondaryText, modifier = Modifier.weight(1f))
                        Text(Fmt.durationShort(seg.seconds), fontSize = 13.sp, color = c.tertiaryText)
                    }
                }
            }
        }

        if (t.caption.isNotEmpty()) {
            Text(
                t.caption, fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = c.primaryText, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
        }
        if (t.studyDescription.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t.studyDescription, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = rarityCol ?: c.secondaryText, modifier = Modifier.weight(1f, fill = false),
                )
                if (rarity != null && rarity != PunRarity.COMMON) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background((rarityCol ?: c.secondaryText).copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(rarityCol ?: c.secondaryText))
                        Text(" ${rarity.displayName}", fontSize = 11.sp, color = rarityCol ?: c.secondaryText)
                    }
                }
            }
        }
        if (t.pigeonOrigin != null && t.pigeonDestination != null) {
            Text(
                "\uD83D\uDC26 ${t.pigeonOrigin} → ${t.pigeonDestination}",
                fontSize = 12.sp, fontStyle = FontStyle.Italic, color = c.tertiaryText,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        // Media
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (t.isLandscape) 16f / 9f else if (t.frameDir != null || t.thumbnailPath != null) 4f / 5f else 2f)
                .clip(RoundedCornerShape(PigeonTheme.cornerField))
                .background(c.surfaceBackground)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (uid != null && uid !in t.likedByUIDs) { Store.toggleLike(t); heartBurst = true }
                        },
                        onTap = { onOpenDetail(t) },
                    )
                },
        ) {
            PostMedia(t)
            Text(
                t.formattedDuration,
                color = Color.White, fontSize = 12.sp, fontFamily = gloucester, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            val burstAlpha by animateFloatAsState(if (heartBurst) 1f else 0f, label = "burst")
            if (heartBurst) {
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(800); heartBurst = false }
                Icon(
                    Icons.Filled.Favorite, null, tint = Color.White.copy(alpha = burstAlpha),
                    modifier = Modifier.align(Alignment.Center).size(80.dp),
                )
            }
        }

        // Stats row
        Spacer(Modifier.height(10.dp))
        val showLeaves = t.appLeaveCount > 0
        Row(Modifier.fillMaxWidth()) {
            StatCell(Fmt.durationHm(t.durationSeconds), "Duration", Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(28.dp).background(c.separator))
            StatCell(t.subject, "Subject", Modifier.weight(1f))
            if (showLeaves) {
                Box(Modifier.width(1.dp).height(28.dp).background(c.separator))
                StatCell("${t.appLeaveCount}", "Left App", Modifier.weight(1f))
            }
        }

        HorizontalDivider(color = c.separator.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

        // Action bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { Store.toggleLike(t) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    null, tint = if (liked) c.destructive else c.tertiaryText, modifier = Modifier.size(22.dp),
                )
            }
            Text("${t.likeCount}", fontSize = 14.sp, color = c.secondaryText)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "I studied ${Fmt.durationHm(t.durationSeconds)} of ${t.subject} on Pigeon\nhttps://pigeonstudy.ing/nest?post=${t.id}")
                }
                context.startActivity(Intent.createChooser(send, "Share Session"))
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Share, null, tint = c.tertiaryText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun PostMedia(t: StudyTimelapse) {
    val c = pigeonColors()
    // flipbook of frames if present
    val frames = remember(t.frameDir) {
        t.frameDir?.let { MediaStore.frameFiles(File(Store.appContext.filesDir, it)) } ?: emptyList()
    }
    if (frames.isNotEmpty()) {
        var idx by remember { mutableIntStateOf(0) }
        var visible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(600)
                if (visible) idx = (idx + 1) % frames.size
            }
        }
        val bmp = remember(idx) {
            try { BitmapFactory.decodeFile(frames[idx.coerceIn(frames.indices)].absolutePath) } catch (_: Exception) { null }
        }
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    } else if (t.thumbnailPath != null) {
        val bmp = remember(t.thumbnailPath) {
            try { BitmapFactory.decodeFile(File(Store.appContext.filesDir, t.thumbnailPath).absolutePath) } catch (_: Exception) { null }
        }
        if (bmp != null) {
            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else { SubjectGradientPlaceholder(t) }
    } else {
        SubjectGradientPlaceholder(t)
    }
}

@Composable
fun SubjectGradientPlaceholder(t: StudyTimelapse) {
    val col = subjectColor(t.subject, Store.subjects)
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(col.copy(alpha = 0.35f), col.copy(alpha = 0.12f)))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text("study time", fontFamily = gloucester, fontSize = 20.sp, color = col)
    }
}
