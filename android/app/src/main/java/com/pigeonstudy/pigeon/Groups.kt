package com.pigeonstudy.pigeon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

val groupIcons: List<Pair<String, ImageVector>> = listOf(
    "book" to Icons.Filled.MenuBook, "pencil" to Icons.Filled.Edit, "brain" to Icons.Filled.Psychology,
    "graduationcap" to Icons.Filled.School, "lightbulb" to Icons.Filled.Lightbulb, "star" to Icons.Filled.Star,
    "flame" to Icons.Filled.LocalFireDepartment, "bolt" to Icons.Filled.Bolt, "trophy" to Icons.Filled.EmojiEvents,
    "target" to Icons.Filled.TrackChanges, "flag" to Icons.Filled.Flag, "heart" to Icons.Filled.Favorite,
)

fun iconFor(name: String): ImageVector = groupIcons.firstOrNull { it.first == name }?.second ?: Icons.Filled.MenuBook

@Composable
fun GroupsView() {
    val c = pigeonColors()
    var tab by remember { mutableIntStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var openGroup by remember { mutableStateOf<StudyGroup?>(null) }
    val scope = rememberCoroutineScope()
    val uid = Store.activeUid() ?: ""

    LaunchedEffect(Unit) {
        if (Store.isCloudUser) scope.launch {
            try { Store.groups = Supabase.fetchGroups(uid) } catch (_: Exception) {}
        }
    }

    val group = openGroup
    if (group != null) {
        GroupDetailScreen(group) { openGroup = null }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Groups", fontFamily = gloucester, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.primaryText, modifier = Modifier.weight(1f))
            IconButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, null, tint = c.primaryText) }
        }
        UnderlinePicker(listOf("My Groups", "Invites"), tab) { tab = it }

        if (tab == 0) {
            if (Store.groups.isEmpty()) {
                EmptyState("\uD83D\uDC8A", "no flocks yet",
                    "Create a study group to compete\nwith friends on leaderboards",
                    "Create Group") { showCreate = true }
            } else {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(Store.groups, key = { it.id }) { g ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(PigeonTheme.cornerCard))
                                .background(c.cardBackground.copy(alpha = 0.6f))
                                .clickable { openGroup = g }
                                .padding(14.dp),
                        ) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                    .background(c.accent.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(iconFor(g.iconName), null, tint = c.primaryText, modifier = Modifier.size(24.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.name, fontFamily = gloucester, fontSize = 17.sp, color = c.primaryText)
                                Text("${g.memberIDs.size} member${if (g.memberIDs.size != 1) "s" else ""}", fontSize = 12.sp, color = c.tertiaryText)
                            }
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = c.tertiaryText)
                        }
                    }
                }
            }
        } else {
            EmptyState("\u2709\uFE0F", "no carrier pigeons", "Group invitations will appear here")
        }
    }

    if (showCreate) {
        CreateGroupSheet { showCreate = false }
    }
}

@Composable
private fun CreateGroupSheet(onDismiss: () -> Unit) {
    val c = pigeonColors()
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("book") }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Text("New Group", fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(c.accent.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Icon(iconFor(icon), null, tint = c.primaryText, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(12.dp))
            // icon grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groupIcons.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (id, iconVec) ->
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (icon == id) c.primaryText else c.surfaceBackground)
                                    .clickable { icon = id },
                                contentAlignment = Alignment.Center,
                            ) { Icon(iconVec, null, tint = if (icon == id) c.background else c.secondaryText, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") },
                singleLine = true, shape = RoundedCornerShape(PigeonTheme.cornerField), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (optional)") },
                singleLine = true, shape = RoundedCornerShape(PigeonTheme.cornerField), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PigeonButton("Create Group", {
                    val uid = Store.activeUid() ?: return@PigeonButton
                    val g = StudyGroup(name = name, groupDescription = desc, creatorID = uid, iconName = icon)
                    g.memberIDs.add(uid)
                    Store.groups = Store.groups + g
                    Store.saveGroups()
                    scope.launch {
                        try { Supabase.createGroup(g) } catch (_: Exception) {}
                    }
                    onDismiss()
                }, enabled = name.isNotBlank())
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
fun GroupDetailScreen(g: StudyGroup, onBack: () -> Unit) {
    val c = pigeonColors()
    var tab by remember { mutableIntStateOf(0) }
    var period by remember { mutableIntStateOf(4) }
    val periods = listOf("Today", "Week", "Month", "Year", "All Time")

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = c.primaryText) }
            Text("group", fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText, modifier = Modifier.weight(1f))
            var menu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreHoriz, null, tint = c.secondaryText) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    val uid = Store.activeUid() ?: ""
                    if (g.creatorID == uid) {
                        DropdownMenuItem(text = { Text("Delete Group", color = c.destructive) }, onClick = {
                            menu = false
                            Store.groups = Store.groups.filter { it.id != g.id }
                            Store.saveGroups()
                            onBack()
                        })
                    } else {
                        DropdownMenuItem(text = { Text("Leave Group", color = c.destructive) }, onClick = {
                            menu = false
                            g.memberIDs.remove(uid)
                            Store.saveGroups()
                            onBack()
                        })
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(c.accent.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Icon(iconFor(g.iconName), null, tint = c.primaryText, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(g.name, fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            if (g.groupDescription.isNotEmpty()) Text(g.groupDescription, fontSize = 12.sp, color = c.tertiaryText)
            Text("${g.memberIDs.size} member${if (g.memberIDs.size != 1) "s" else ""}", fontSize = 11.sp, color = c.tertiaryText)
        }
        Spacer(Modifier.height(8.dp))
        UnderlinePicker(listOf("Leaderboard", "Feed"), tab) { tab = it }
        Spacer(Modifier.height(8.dp))

        if (tab == 0) {
            // leaderboard over member timelapses
            val cutoffMs = when (period) {
                0 -> System.currentTimeMillis() - 86400000L
                1 -> System.currentTimeMillis() - 7 * 86400000L
                2 -> System.currentTimeMillis() - 30L * 86400000L
                3 -> System.currentTimeMillis() - 365L * 86400000L
                else -> 0L
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                periods.forEachIndexed { i, p ->
                    Text(
                        p, fontSize = 11.sp, color = if (period == i) c.primaryText else c.tertiaryText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (period == i) c.accent.copy(alpha = 0.5f) else Color.Transparent)
                            .clickable { period = i }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            val rows = g.memberIDs.map { mUid ->
                val name = Store.profiles.firstOrNull { it.firebaseUID == mUid }?.displayName
                    ?: Store.timelapses.firstOrNull { it.authorID == mUid }?.authorName ?: "Pigeon"
                val secs = Store.timelapses.filter { it.authorID == mUid && it.createdAt >= cutoffMs }.sumOf { it.durationSeconds }
                Triple(mUid, name, secs)
            }.sortedByDescending { it.third }
            if (rows.isEmpty()) {
                EmptyState("\uD83D\uDC8A", "the flock hasn't arrived", "")
            } else {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEachIndexed { i, (_, name, secs) ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${i + 1}", fontFamily = gloucester, fontSize = 17.sp,
                                    color = when (i) { 0 -> Color(0.85f, 0.72f, 0.3f); 1 -> Color(0.7f, 0.7f, 0.7f); 2 -> Color(0.78f, 0.58f, 0.42f); else -> c.tertiaryText },
                                    modifier = Modifier.width(28.dp),
                                )
                                Avatar(name, size = 36)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) { Text(name, fontSize = 14.sp, color = c.primaryText) }
                                Text(Fmt.durationHm(secs), fontFamily = gloucester, fontSize = 15.sp, color = c.primaryText)
                            }
                        }
                    }
                }
            }
        } else {
            val feed = Store.timelapses.filter { it.authorID in g.memberIDs }.sortedByDescending { it.createdAt }
            if (feed.isEmpty()) {
                EmptyState("\uD83D\uDD25", "no feathers ruffled yet", "")
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(feed, key = { it.id }) { t ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(t.authorName, size = 36)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.authorName, fontSize = 14.sp, color = c.primaryText)
                                    Text("${t.subject} · ${Fmt.durationHm(t.durationSeconds)}", fontSize = 11.sp, color = c.tertiaryText)
                                }
                                Text(Fmt.timeAgo(t.createdAt), fontSize = 11.sp, color = c.tertiaryText)
                            }
                        }
                    }
                }
            }
        }
    }
}
