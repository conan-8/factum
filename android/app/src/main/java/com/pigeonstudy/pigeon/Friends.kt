package com.pigeonstudy.pigeon

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun FriendsView() {
    val c = pigeonColors()
    var tab by remember { mutableIntStateOf(0) }
    var friends by remember { mutableStateOf(listOf<UserProfile>()) }
    var requests by remember { mutableStateOf(listOf<Triple<String, UserProfile, String>>()) } // requestId, sender, createdAt
    var searchResults by remember { mutableStateOf(listOf<UserProfile>()) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val uid = Store.activeUid() ?: ""

    fun loadFriends() {
        scope.launch {
            try {
                val friendUids = Store.currentProfile?.friendUIDs ?: return@launch
                friends = friendUids.mapNotNull { runCatching { Supabase.fetchUser(it) }.getOrNull() }
            } catch (_: Exception) {}
        }
    }
    fun loadRequests() {
        scope.launch {
            try {
                val arr = Supabase.fetchFriendRequests(uid)
                val list = mutableListOf<Triple<String, UserProfile, String>>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val sender = runCatching { Supabase.fetchUser(o.getString("sender_uid")) }.getOrNull()
                    if (sender != null) list.add(Triple(o.getString("id"), sender, o.optString("created_at")))
                }
                requests = list
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) { loadFriends(); loadRequests() }
    LaunchedEffect(query) {
        if (query.length < 2) { searchResults = emptyList(); return@LaunchedEffect }
        delay(300)
        scope.launch {
            try { searchResults = Supabase.searchUsers(query, uid) } catch (_: Exception) {}
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Friends", fontFamily = gloucester, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.primaryText, modifier = Modifier.weight(1f))
            if (requests.isNotEmpty()) {
                Box(Modifier.clip(CircleShape).background(c.destructive).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("${requests.size}", color = Color.White, fontSize = 11.sp)
                }
            }
        }
        UnderlinePicker(listOf("Friends", "Requests", "Search"), tab) { tab = it }
        Spacer(Modifier.height(8.dp))

        when (tab) {
            0 -> {
                if (friends.isEmpty() && !Store.isCloudUser) {
                    EmptyState("\uD83D\uDC8A", "no flock yet", "Sign in with an account to\nconnect with friends")
                } else if (friends.isEmpty()) {
                    EmptyState("\uD83D\uDC8A", "no flock yet", "Search for friends by @username", "Search") { tab = 2 }
                } else {
                    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(friends, key = { it.firebaseUID ?: it.id }) { f ->
                            var showProfile by remember { mutableStateOf(false) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(PigeonTheme.cornerField))
                                    .background(c.cardBackground.copy(alpha = 0.6f))
                                    .clickable { showProfile = true }
                                    .padding(12.dp),
                            ) {
                                Box {
                                    Avatar(f.displayName, size = 44)
                                    Box(Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(Color(0.3f, 0.8f, 0.35f)))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(f.displayName, fontFamily = gloucester, fontSize = 16.sp, color = c.primaryText)
                                    Text("@${f.username}", fontSize = 12.sp, color = c.tertiaryText)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${f.totalStudyMinutes / 60}h", fontFamily = gloucester, fontSize = 15.sp, color = c.primaryText)
                                    Text("studied", fontSize = 10.sp, color = c.tertiaryText)
                                }
                            }
                            if (showProfile) {
                                FriendProfileSheet(f) { showProfile = false; loadFriends() }
                            }
                        }
                    }
                }
            }
            1 -> {
                if (requests.isEmpty()) {
                    EmptyState("\uD83D\uDD14", "No pending requests", "")
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        requests.forEach { (reqId, sender, _) ->
                            GlassCard(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(sender.displayName, size = 40)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(sender.displayName, fontFamily = gloucester, fontSize = 15.sp, color = c.primaryText)
                                        Text("@${sender.username}", fontSize = 12.sp, color = c.tertiaryText)
                                    }
                                    PigeonButton("Accept", {
                                        scope.launch {
                                            try {
                                                Supabase.acceptFriendRequest(reqId)
                                                loadRequests(); loadFriends()
                                            } catch (_: Exception) {}
                                        }
                                    })
                                    Spacer(Modifier.width(6.dp))
                                    IconButton(onClick = { /* decline = delete row */ }) {
                                        Icon(Icons.Outlined.Close, null, tint = c.tertiaryText, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Search by @username or name", fontSize = 14.sp, color = c.tertiaryText) },
                        singleLine = true, shape = RoundedCornerShape(PigeonTheme.cornerField), colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    if (query.length >= 2 && searchResults.isEmpty()) {
                        Text("no pigeons spotted", fontSize = 13.sp, color = c.tertiaryText)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchResults.forEach { u ->
                            var status by remember { mutableStateOf("Add") }
                            GlassCard(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Avatar(u.displayName, size = 40)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(u.displayName, fontFamily = gloucester, fontSize = 15.sp, color = c.primaryText)
                                        Text("@${u.username}", fontSize = 12.sp, color = c.tertiaryText)
                                    }
                                    if (status == "Add") {
                                        PigeonButton("Add", {
                                            status = "Pending"
                                            scope.launch {
                                                try { Supabase.sendFriendRequest(uid, u.firebaseUID ?: "") } catch (_: Exception) {}
                                            }
                                        }, filled = false)
                                    } else {
                                        Text(status, fontSize = 12.sp, color = c.tertiaryText)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendProfileSheet(f: UserProfile, onDismiss: () -> Unit) {
    val c = pigeonColors()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(f.displayName, size = 88)
            Spacer(Modifier.height(8.dp))
            Text(f.displayName, fontFamily = gloucester, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Text("@${f.username}", fontSize = 13.sp, color = c.tertiaryText)
            if (f.bio.isNotEmpty()) Text(f.bio, fontSize = 13.sp, color = c.secondaryText, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCell(Fmt.durationHm(f.totalStudyMinutes * 60), "Total Study", Modifier.weight(1f))
                StatCell("${f.streakDays}", "Day Streak", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Joined ${java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.US).format(java.util.Date(f.joinDate))}",
                fontSize = 11.sp, color = c.tertiaryText,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, subtitle: String, button: String? = null, onButton: () -> Unit = {}) {
    val c = pigeonColors()
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontFamily = gloucester, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, fontSize = 13.sp, color = c.tertiaryText, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
        if (button != null) {
            Spacer(Modifier.height(16.dp))
            PigeonButton(button, onButton)
        }
    }
}
