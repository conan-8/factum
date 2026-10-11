package com.pigeonstudy.pigeon

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PostCaptionScreen(engine: SessionEngine, onComplete: () -> Unit) {
    val c = pigeonColors()
    var caption by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var rolledRarity by remember { mutableStateOf<PunRarity?>(null) }
    var hasRolled by remember { mutableStateOf(false) }
    var rerolled by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler { showSkipDialog = true }
    var revealScale by remember { mutableStateOf(1f) }
    val rarityColor = rolledRarity?.let { rarityColor(it, false) }

    fun savePost(post: Boolean) {
        busy = true
        val tl = engine.buildTimelapse(
            if (post && caption.isNotBlank()) caption else "study session",
            description,
            rolledRarity?.displayName,
        )
        Store.addTimelapse(tl)
        Store.recomputeStats()
        engine.cleanupAfterPost()
        onComplete()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("New Post", fontFamily = gloucester, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.primaryText, modifier = Modifier.weight(1f))
            IconButton(onClick = { showSkipDialog = true }) {
                Icon(Icons.Filled.Close, null, tint = c.secondaryText)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Summary card
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(72.dp).clip(RoundedCornerShape(PigeonTheme.cornerField))
                        .background(subjectColor(engine.currentSubject, Store.subjects).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Schedule, null, tint = subjectColor(engine.currentSubject, Store.subjects), modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(Fmt.durationHm(engine.studySeconds), fontFamily = gloucester, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                    Text("Study Session Complete", fontSize = 13.sp, color = c.secondaryText)
                    Text("Add details about your session", fontSize = 11.sp, color = c.tertiaryText)
                    if (engine.finalizedSegments().size > 1) {
                        Text(engine.finalizedSegments().joinToString(" · ") { "${it.subject} ${Fmt.durationShort(it.seconds)}" },
                            fontSize = 11.sp, color = c.tertiaryText, maxLines = 2)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Caption", fontFamily = gloucester, fontSize = 15.sp, color = c.secondaryText)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = caption, onValueChange = { caption = it },
            placeholder = { Text("e.g. late night grind session", fontSize = 14.sp, color = c.tertiaryText) },
            singleLine = true,
            shape = RoundedCornerShape(PigeonTheme.cornerField),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Description", fontFamily = gloucester, fontSize = 15.sp, color = c.secondaryText, modifier = Modifier.weight(1f))
            Text(
                if (!hasRolled) "pun me" else if (!rerolled) "reroll" else "rolled",
                fontSize = 13.sp, fontFamily = gloucester, color = if (!hasRolled || !rerolled) c.primaryText else c.tertiaryText,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.accent.copy(alpha = 0.4f))
                    .clickable(enabled = !hasRolled || !rerolled) {
                        val pun = PunLibrary.rollPun()
                        rolledRarity = PunRarity.fromRaw(pun.rarity)
                        description = pun.text
                        if (hasRolled) rerolled = true
                        hasRolled = true
                        revealScale = 0.6f
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = description, onValueChange = { description = it },
            placeholder = { Text("what did you study? any breakthroughs?", fontSize = 14.sp, color = c.tertiaryText) },
            minLines = 3,
            shape = RoundedCornerShape(PigeonTheme.cornerField),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        rolledRarity?.let { rarity ->
            val scale by animateFloatAsState(revealScale, animationSpec = tween(300), label = "reveal") { revealScale = 1f }
            Row(
                Modifier.padding(top = 8.dp).graphicsLayer {
                    scaleX = scale; scaleY = scale
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(rarityColor ?: c.secondaryText))
                Text("  ${rarity.displayName}", fontSize = 12.sp, color = rarityColor ?: c.secondaryText, fontWeight = FontWeight.Bold)
                if (rarity != PunRarity.COMMON) Text("  pun unlocked!", fontSize = 11.sp, color = c.tertiaryText)
            }
        }
        Spacer(Modifier.height(28.dp))

        if (busy) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.primaryText)
            }
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PigeonButton("Share Session", { savePost(true) }, Modifier.fillMaxWidth(0.7f),
                    enabled = caption.isNotBlank())
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Save without posting",
                fontSize = 13.sp, color = c.tertiaryText, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { savePost(false) },
            )
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showSkipDialog) {
        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip posting?") },
            text = { Text("Your study time will still be saved to your history.") },
            confirmButton = {
                TextButton(onClick = { showSkipDialog = false; savePost(false) }) {
                    Text("Save without posting", color = c.destructive)
                }
            },
            dismissButton = { TextButton(onClick = { showSkipDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = pigeonColors().primaryText,
    unfocusedBorderColor = pigeonColors().separator,
    focusedTextColor = pigeonColors().primaryText,
    unfocusedTextColor = pigeonColors().primaryText,
    cursorColor = pigeonColors().primaryText,
)
