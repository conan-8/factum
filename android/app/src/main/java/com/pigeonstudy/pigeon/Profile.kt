package com.pigeonstudy.pigeon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun ProfileView(onOpenStats: () -> Unit, onOpenFriends: () -> Unit, onOpenRecaps: () -> Unit, onOpenDetail: (StudyTimelapse) -> Unit) {
    val c = pigeonColors()
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showWallpaper by remember { mutableStateOf(false) }
    var showAddSubject by remember { mutableStateOf(false) }
    val profile = Store.currentProfile
    val scope = rememberCoroutineScope()

    val userTl = remember(Store.timelapses) { Store.userTimelapses() }
    val totalMin = remember(Store.timelapses) { Store.totalMinutes() }
    val streak = remember(Store.timelapses) { Store.streakDays() }
    val top = remember(Store.timelapses) { Store.topSubjects(3) }
    val longest = userTl.maxOfOrNull { it.durationSeconds } ?: 0
    val mostInDay = userTl.groupBy { it.createdAt / 86400000L }.maxOfOrNull { (_, list) -> list.sumOf { it.durationSeconds } } ?: 0

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("You", fontFamily = gloucester, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.primaryText, modifier = Modifier.weight(1f))
            IconButton(onClick = onOpenRecaps) { Icon(Icons.Outlined.IosShare, null, tint = c.secondaryText) }
            IconButton(onClick = { showSettings = true }) { Icon(Icons.Outlined.Settings, null, tint = c.secondaryText) }
        }

        if (profile == null) {
            Text("Loading profile…", color = c.tertiaryText, modifier = Modifier.padding(40.dp))
        } else {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box {
                    Avatar(profile.displayName, size = 64)
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(22.dp).clip(CircleShape).background(c.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(18.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Edit, null, tint = c.accentText, modifier = Modifier.size(10.dp))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.displayName, fontFamily = gloucester, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                    Text("@${profile.username}", fontSize = 13.sp, color = c.secondaryText)
                }
            }
            if (profile.bio.isNotEmpty()) {
                Text(profile.bio, fontSize = 13.sp, color = c.secondaryText, modifier = Modifier.padding(top = 6.dp))
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${profile.friendUIDs.size} friend${if (profile.friendUIDs.size != 1) "s" else ""}",
                    fontSize = 12.sp, color = c.tertiaryText,
                    modifier = Modifier.clickable { onOpenFriends() },
                )
            }
            Spacer(Modifier.height(14.dp))
            // Stat row
            GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                    StatCell(Fmt.durationHm(totalMin * 60), "Total Time", Modifier.weight(1f))
                    StatCell("${userTl.size}", "Sessions", Modifier.weight(1f))
                    StatCell("$streak", "Streak", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            UnderlinePicker(listOf("Progress", "Sessions"), tab) { tab = it }

            if (tab == 0) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NavCard("Stats", "charts & trends", Icons.Outlined.QueryStats, Modifier.weight(1f)) { onOpenStats() }
                    NavCard("Your Flock", "manage friends", Icons.Outlined.People, Modifier.weight(1f)) { onOpenFriends() }
                }
                Spacer(Modifier.height(20.dp))
                Text("TOP SUBJECTS", fontSize = 11.sp, color = c.tertiaryText, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                top.forEach { (subject, secs) ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(subjectColor(subject, Store.subjects)))
                        Text("  $subject", fontSize = 14.sp, color = c.primaryText, modifier = Modifier.weight(1f))
                        Text(Fmt.durationHm(secs), fontFamily = gloucester, fontSize = 14.sp, color = c.secondaryText)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("PERSONAL RECORDS", fontSize = 11.sp, color = c.tertiaryText, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassCard(Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(Fmt.durationHm(longest), fontFamily = gloucester, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                            Text("Longest Session", fontSize = 11.sp, color = c.tertiaryText)
                        }
                    }
                    GlassCard(Modifier.weight(1f)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(Fmt.durationHm(mostInDay), fontFamily = gloucester, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                            Text("Most in a Day", fontSize = 11.sp, color = c.tertiaryText)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("YOUR SUBJECTS", fontSize = 11.sp, color = c.tertiaryText, letterSpacing = 1.5.sp, modifier = Modifier.weight(1f))
                    Text("Edit", fontSize = 12.sp, color = c.secondaryText)
                }
                Spacer(Modifier.height(8.dp))
                // wrapping subject pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Store.subjects.sortedBy { it.sortOrder }.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { s ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .background(subjectColor(s.name, Store.subjects).copy(alpha = 0.2f))
                                        .clickable { showAddSubject = true }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                ) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(subjectColor(s.name, Store.subjects)))
                                    Text(" ${s.name}", fontSize = 11.sp, color = c.primaryText, maxLines = 1)
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Text(
                    "+ Add Subject",
                    fontSize = 13.sp, fontFamily = gloucester, color = c.secondaryText,
                    modifier = Modifier.padding(top = 10.dp).clickable { showAddSubject = true },
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Text("My Sessions", fontFamily = gloucester, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                Spacer(Modifier.height(8.dp))
                if (userTl.isEmpty()) {
                    Text("Your recorded sessions will appear here.", fontSize = 13.sp, color = c.tertiaryText, modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    userTl.forEach { t ->
                        Spacer(Modifier.height(10.dp))
                        TimelapseCard(t, onOpenDetail)
                    }
                }
            }
            Spacer(Modifier.height(110.dp))
        }
    }

    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            onWallpaper = { showSettings = false; showWallpaper = true },
            onSignOut = {
                showSettings = false
                scope.launch { Store.signOut() }
            },
        )
    }
    if (showWallpaper) {
        WallpaperPickerSheet { showWallpaper = false }
    }
    if (showAddSubject) {
        AddSubjectSheet { showAddSubject = false }
    }
    if (showEdit && profile != null) {
        EditProfileSheet(profile) { showEdit = false }
    }
}

@Composable
private fun NavCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = pigeonColors()
    Column(
        modifier
            .clip(RoundedCornerShape(PigeonTheme.cornerCard))
            .background(c.cardBackground.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Icon(icon, null, tint = c.secondaryText, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, fontFamily = gloucester, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
        Text(subtitle, fontSize = 11.sp, color = c.tertiaryText)
    }
}

@Composable
private fun SettingsSheet(onDismiss: () -> Unit, onWallpaper: () -> Unit, onSignOut: () -> Unit) {
    val c = pigeonColors()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Settings", fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Spacer(Modifier.height(16.dp))
            SettingsRow("Wallpaper", Icons.Outlined.Wallpaper) { onWallpaper() }
            SettingsRow("Manage Subjects", Icons.Outlined.Category) {}
            HorizontalDivider(color = c.separator.copy(alpha = 0.4f))
            SettingsRow("Appearance", Icons.Outlined.DarkMode) {}
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("System" to 0, "Light" to 1, "Dark" to 2).forEach { (label, mode) ->
                    Text(
                        label, fontSize = 12.sp,
                        color = if (Store.appearanceMode == mode) c.background else c.secondaryText,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (Store.appearanceMode == mode) c.primaryText else c.surfaceBackground)
                            .clickable { Store.setAppearance(mode) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            HorizontalDivider(color = c.separator.copy(alpha = 0.4f))
            SettingsRow("Help & Support", Icons.Outlined.Help) {}
            SettingsRow("Privacy Policy", Icons.Outlined.PrivacyTip) {}
            SettingsRow("About Pigeon", Icons.Outlined.Info) {}
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onSignOut) {
                    Text("Sign Out", color = c.destructive, fontFamily = gloucester, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val c = pigeonColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
    ) {
        Icon(icon, null, tint = c.secondaryText, modifier = Modifier.size(20.dp))
        Text("  $label", fontSize = 15.sp, color = c.primaryText)
    }
}

@Composable
fun WallpaperPickerSheet(onDismiss: () -> Unit) {
    val c = pigeonColors()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Text("Wallpaper", fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Spacer(Modifier.height(4.dp))
            Text("None", fontSize = 12.sp, color = if (Store.wallpaperId.isEmpty()) c.primaryText else c.tertiaryText,
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable { Store.setWallpaper(""); onDismiss() })
            Spacer(Modifier.height(8.dp))
            Text("Gradients", fontSize = 11.sp, color = c.tertiaryText)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Wallpapers.gradients.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { g ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(PigeonTheme.cornerField))
                                    .background(
                                        if (g.diagonal) androidx.compose.ui.graphics.Brush.linearGradient(g.colors)
                                        else androidx.compose.ui.graphics.Brush.verticalGradient(g.colors)
                                    )
                                    .border(
                                        if (Store.wallpaperId == g.id) 2.5.dp else 0.dp, c.primaryText,
                                        RoundedCornerShape(PigeonTheme.cornerField),
                                    )
                                    .clickable { Store.setWallpaper(g.id); onDismiss() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(g.name, fontSize = 12.sp, color = Color.White, fontFamily = gloucester)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Text("Solid Colors", fontSize = 11.sp, color = c.tertiaryText)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Wallpapers.solids.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { s ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(PigeonTheme.cornerField))
                                    .background(parseHex(s.hex) ?: c.separator)
                                    .border(
                                        if (Store.wallpaperId == s.id) 2.5.dp else 0.dp, c.primaryText,
                                        RoundedCornerShape(PigeonTheme.cornerField),
                                    )
                                    .clickable { Store.setWallpaper(s.id); onDismiss() },
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun AddSubjectSheet(onDismiss: () -> Unit) {
    val c = pigeonColors()
    var name by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#3478F6") }
    val palette = listOf("#3478F6", "#FF3B30", "#34C759", "#A2845E", "#AF52DE", "#FF9500", "#5AC8FA", "#A8CC3C", "#FF2D55", "#BF5AF2", "#00C7BE", "#FFCC00")

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Add Subject", fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Subject name") }, singleLine = true,
                shape = RoundedCornerShape(PigeonTheme.cornerField), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { hex ->
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(parseHex(hex) ?: c.separator)
                                    .border(if (colorHex == hex) 3.dp else 0.dp, c.primaryText, CircleShape)
                                    .clickable { colorHex = hex },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.weight(1f))
                PigeonButton("Cancel", onDismiss, filled = false)
                PigeonButton("Add", {
                    if (name.isNotBlank()) {
                        Store.subjects = Store.subjects + StudySubject(
                            name = name.trim(), colorHex = colorHex,
                            isUserCreated = true, sortOrder = Store.subjects.size,
                        )
                        Store.saveSubjects()
                    }
                    onDismiss()
                })
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EditProfileSheet(profile: UserProfile, onDismiss: () -> Unit) {
    val c = pigeonColors()
    var name by remember { mutableStateOf(profile.displayName) }
    var username by remember { mutableStateOf(profile.username ?: "") }
    var bio by remember { mutableStateOf(profile.bio) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Text("Edit Profile", fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") },
                singleLine = true, shape = RoundedCornerShape(PigeonTheme.cornerField), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = username, onValueChange = { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                label = { Text("Username") }, prefix = { Text("@") },
                singleLine = true, shape = RoundedCornerShape(PigeonTheme.cornerField), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") },
                shape = RoundedCornerShape(PigeonTheme.cornerField), colors = fieldColors(), modifier = Modifier.fillMaxWidth(), minLines = 2)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.weight(1f))
                PigeonButton("Cancel", onDismiss, filled = false)
                PigeonButton("Save", {
                    profile.displayName = name.trim().ifEmpty { "Pigeon" }
                    profile.username = username.ifBlank { null }
                    profile.bio = bio
                    Store.saveProfiles()
                    if (Store.isCloudUser) scope.launch {
                        try { Supabase.upsertUser(profile) } catch (_: Exception) {}
                    }
                    onDismiss()
                })
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
