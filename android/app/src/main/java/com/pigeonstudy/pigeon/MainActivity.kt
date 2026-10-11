package com.pigeonstudy.pigeon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { PigeonRoot() }
    }
}

enum class MainTab(val icon: ImageVector, val label: String) {
    HOME(Icons.Outlined.Home, "Home"),
    FRIENDS(Icons.Outlined.People, "Friends"),
    RECORD(Icons.Filled.Videocam, "Record"),
    GROUPS(Icons.Outlined.Workspaces, "Groups"),
    YOU(Icons.Outlined.Person, "You"),
}

@Composable
fun PigeonRoot() {
    val c = pigeonColors()
    val context = androidx.compose.ui.platform.LocalContext.current
    val engine = remember { SessionState.get(context) }

    var tab by remember { mutableStateOf(MainTab.HOME) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showRecaps by remember { mutableStateOf(false) }
    var showRecording by remember { mutableStateOf(false) }
    var showSetup by remember { mutableStateOf(false) }
    var showPostCaption by remember { mutableStateOf(false) }
    var detailPost by remember { mutableStateOf<StudyTimelapse?>(null) }
    var recoveredMsg by remember { mutableStateOf<String?>(null) }

    // Auth gate
    LaunchedEffect(Unit) {
        while (Store.authLoading) kotlinx.coroutines.delay(50)
        if (!Store.signedIn) showOnboarding = true
        else {
            // session recovery
            val snap = engine.loadSnapshotInfo()
            if (snap != null && snap.studySeconds > 5) {
                if (engine.recoverFromSnapshot()) {
                    recoveredMsg = "Your last study session was interrupted but we saved it."
                    showRecording = true
                }
            }
        }
    }

    // App lifecycle → session engine
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> engine.onAppHide()
                Lifecycle.Event.ON_START -> {
                    engine.onAppShow()
                    if (Store.signedIn && Store.isCloudUser) Store.retryUnsynced()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    WallpaperBackground {
        when {
            showOnboarding -> OnboardingView { showOnboarding = false }
            else -> {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            MainTab.HOME -> FeedView(
                                onRecord = { showSetup = true },
                                onOpenDetail = { detailPost = it },
                            )
                            MainTab.FRIENDS -> FriendsView()
                            MainTab.GROUPS -> GroupsView()
                            MainTab.YOU -> ProfileView(
                                onOpenStats = { showStats = true },
                                onOpenFriends = { tab = MainTab.FRIENDS },
                                onOpenRecaps = { showRecaps = true },
                                onOpenDetail = { detailPost = it },
                            )
                            MainTab.RECORD -> {}
                        }
                    }
                    PigeonTabBar(selected = tab, onSelect = {
                        if (it == MainTab.RECORD) showSetup = true else tab = it
                    })
                }

                // overlays
                if (showSetup) {
                    Box(Modifier.fillMaxSize()) {
                        Surface(
                            color = c.background,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            RecordSetupScreen(
                                engine,
                                onClose = { showSetup = false },
                                onStart = { showSetup = false; showRecording = true },
                            )
                        }
                    }
                }
                if (showRecording) {
                    Surface(color = Color(0.04f, 0.04f, 0.03f), modifier = Modifier.fillMaxSize()) {
                        RecordingScreen(engine, onFinished = {
                            if (engine.phase == SessionEngine.Phase.DONE) {
                                showRecording = false
                                showPostCaption = true
                            }
                        })
                        if (engine.phase == SessionEngine.Phase.PAUSED) {
                            PausedOverlay(engine) {
                                if (engine.phase == SessionEngine.Phase.SETUP) showRecording = false
                            }
                        }
                    }
                }
                if (showPostCaption) {
                    Surface(color = c.background, modifier = Modifier.fillMaxSize()) {
                        PostCaptionScreen(engine) { showPostCaption = false }
                    }
                }
                if (showStats) {
                    Surface(color = c.background, modifier = Modifier.fillMaxSize()) {
                        StatsScreen { showStats = false }
                    }
                }
                if (showRecaps) {
                    Surface(color = c.background, modifier = Modifier.fillMaxSize()) {
                        RecapsView { showRecaps = false }
                    }
                }
                detailPost?.let { post ->
                    val live = Store.timelapses.firstOrNull { it.id == post.id } ?: post
                    PostDetailSheet(live) { detailPost = null }
                }
                recoveredMsg?.let { msg ->
                    AlertDialog(
                        onDismissRequest = { recoveredMsg = null },
                        title = { Text("Session Recovered") },
                        text = { Text("$msg It has been added to your history.") },
                        confirmButton = {
                            TextButton(onClick = { recoveredMsg = null }) { Text("OK") }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun PigeonTabBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val c = pigeonColors()
    val profile = Store.currentProfile
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .padding(bottom = 6.dp)
            .navigationBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.cardBackground.copy(alpha = 0.85f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { t ->
                val isSelected = t == selected
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (isSelected && t != MainTab.RECORD) {
                        Box(
                            Modifier.matchParentSize().padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(c.accent.copy(alpha = 0.35f))
                        )
                    }
                    Column(
                        Modifier
                            .clickable { onSelect(t) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (t == MainTab.RECORD) {
                            Icon(
                                Icons.Filled.Videocam, null,
                                tint = c.primaryText,
                                modifier = Modifier.size(26.dp),
                            )
                        } else if (t == MainTab.YOU) {
                            Box(Modifier.size(26.dp).clip(CircleShape).background(c.separator),
                                contentAlignment = Alignment.Center) {
                                Text(
                                    profile?.displayName?.firstOrNull()?.uppercase() ?: "?",
                                    fontFamily = gloucester, fontSize = 12.sp, color = c.primaryText,
                                )
                            }
                        } else {
                            Icon(
                                t.icon,
                                null, tint = if (isSelected) c.primaryText else c.tertiaryText,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            t.label, fontSize = 9.sp,
                            color = if (isSelected) c.primaryText else c.tertiaryText,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostDetailSheet(t: StudyTimelapse, onDismiss: () -> Unit) {
    val c = pigeonColors()
    val uid = Store.activeUid()
    val liked = uid != null && uid in t.likedByUIDs
    val rarity = PunRarity.fromRaw(t.descriptionRarity)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(t.authorName, size = 44)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.authorName, fontFamily = gloucester, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                    Text(
                        java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.US).format(java.util.Date(t.createdAt)),
                        fontSize = 11.sp, color = c.tertiaryText,
                    )
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            Spacer(Modifier.height(12.dp))
            if (t.frameDir != null || t.thumbnailPath != null) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(4f / 5f)
                        .clip(RoundedCornerShape(12.dp)).background(c.surfaceBackground),
                ) { PostMedia(t) }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCell(Fmt.durationHm(t.durationSeconds), "Duration", Modifier.weight(1f))
                    StatCell(t.subject, "Subject", Modifier.weight(1f))
                    if (t.appLeaveCount > 0) StatCell("${t.appLeaveCount}", "Left App", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(t.caption, fontFamily = gloucester, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.studyDescription, fontSize = 14.sp,
                    color = rarity?.let { rarityColor(it, false) } ?: c.secondaryText,
                    modifier = Modifier.weight(1f))
                rarity?.let {
                    Text(" ${it.displayName}", fontSize = 11.sp, color = rarityColor(it, false))
                }
            }
            if (t.hasMultipleSubjects) {
                Spacer(Modifier.height(8.dp))
                t.subjectSegments.sortedByDescending { it.seconds }.forEach { seg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(subjectColor(seg.subject, Store.subjects)))
                        Text("  ${seg.subject}", fontSize = 13.sp, color = c.secondaryText, modifier = Modifier.weight(1f))
                        Text(Fmt.durationShort(seg.seconds), fontSize = 13.sp, color = c.tertiaryText)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { Store.toggleLike(t) }) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null,
                        tint = if (liked) c.destructive else c.tertiaryText,
                    )
                }
                Text("${t.likeCount}", fontSize = 14.sp, color = c.secondaryText)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
