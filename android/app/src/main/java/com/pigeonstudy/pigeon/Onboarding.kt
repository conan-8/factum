package com.pigeonstudy.pigeon

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingView(onComplete: () -> Unit) {
    val c = pigeonColors()
    val pager = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> FeaturePage(
                    emoji = "\u23F1", title = "record the work",
                    body = "your study sessions become\nshort timelapses — proof\nthat the hours happened",
                )
                2 -> FeaturePage(
                    emoji = "\uD83E\uDD89", title = "study together",
                    body = "see what your friends are\nworking on — it's easier to\nshow up when others do too",
                )
                else -> AuthPage(onComplete)
            }
        }
        // page indicator
        if (pager.currentPage < 3) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == pager.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i <= pager.currentPage) c.primaryText else c.separator)
                    )
                }
            }
            val canNext = pager.currentPage < 3
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PigeonButton(if (pager.currentPage == 2) "get started" else "next", {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }, enabled = canNext)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomePage() {
    val c = pigeonColors()
    Column(
        Modifier.fillMaxSize().padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("\uD83D\uDC86", fontSize = 84.sp)
        Spacer(Modifier.height(20.dp))
        PigeonHeading("pigeon.", 48)
        Spacer(Modifier.height(10.dp))
        Text("a quiet place to study", fontFamily = gloucester, fontSize = 16.sp, color = c.tertiaryText)
    }
}

@Composable
private fun FeaturePage(emoji: String, title: String, body: String) {
    val c = pigeonColors()
    Column(
        Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 56.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(title, fontFamily = gloucester, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
        Spacer(Modifier.height(12.dp))
        Text(body, fontSize = 15.sp, color = c.secondaryText, textAlign = TextAlign.Center, lineHeight = 22.sp)
    }
}

@Composable
private fun AuthPage(onComplete: () -> Unit) {
    val c = pigeonColors()
    var mode by remember { mutableStateOf(0) } // 0 sign in, 1 sign up
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPw by remember { mutableStateOf(false) }
    var isPrivate by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var usernameState by remember { mutableStateOf(0) } // 0 unknown, 1 checking, 2 available, 3 taken
    val scope = rememberCoroutineScope()

    suspend fun checkUsername(u: String) {
        usernameState = 1
        try {
            usernameState = if (Supabase.isUsernameAvailable(u)) 2 else 3
        } catch (_: Exception) { usernameState = 0 }
    }
    LaunchedEffect(username) {
        val clean = username.lowercase().filter { it.isLetterOrDigit() || it == '_' }
        if (clean.length < 3) { usernameState = 0; return@LaunchedEffect }
        delay(400)
        checkUsername(clean)
    }

    fun attemptAuth() {
        if (busy) return
        error = null
        val cleanUser = username.lowercase().filter { it.isLetterOrDigit() || it == '_' }
        if (mode == 1) {
            if (name.isBlank() || email.isBlank() || password.length < 6 || password != confirm || cleanUser.length < 3) {
                error = "fill everything out to continue"
                return
            }
        } else if (email.isBlank() || password.isEmpty()) {
            error = "enter your email and password"
            return
        }
        busy = true
        scope.launch {
            try {
                if (mode == 1) {
                    Supabase.signUpWithEmail(email, password, name)
                } else {
                    Supabase.signInWithEmail(email, password)
                }
                val session = Supabase.currentSession()!!
                Store.applySession(session)
                if (mode == 1) {
                    Store.currentProfile?.let { p ->
                        p.displayName = name
                        p.username = cleanUser
                        p.isPrivate = isPrivate
                        Store.saveProfiles()
                        try { Supabase.claimUsername(p.firebaseUID ?: session.uid, cleanUser) } catch (_: Exception) {}
                        try { Supabase.upsertUser(p) } catch (_: Exception) {}
                    }
                }
                kotlinx.coroutines.delay(300)
                onComplete()
            } catch (e: Exception) {
                error = e.message?.take(120) ?: "something went wrong"
                busy = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("\uD83D\uDC86", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            if (mode == 0) "ready when you are" else "create an account",
            fontFamily = gloucester, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = c.primaryText,
        )
        Text(
            if (mode == 0) "sign in to keep your work safe" else "so your study sessions follow you",
            fontSize = 13.sp, color = c.tertiaryText,
        )
        Spacer(Modifier.height(20.dp))
        UnderlinePicker(listOf("Sign In", "Sign Up"), mode) { mode = it }
        Spacer(Modifier.height(20.dp))

        if (mode == 1) {
            AuthField("Display Name", "Your name", name) { name = it }
            Spacer(Modifier.height(10.dp))
            AuthField("@username", "handle", username,
                state = usernameState, stateText = when (usernameState) {
                    1 -> "Checking..."; 2 -> "this perch is yours"; 3 -> "another pigeon took it"; else -> null
                }) {
                username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }
            }
            Spacer(Modifier.height(10.dp))
        }
        AuthField("Email", "you@example.com", email) { email = it }
        Spacer(Modifier.height(10.dp))
        AuthField("Password", "\u2022\u2022\u2022\u2022\u2022\u2022", password, password = true, showPw = showPw, onTogglePw = { showPw = it }) { password = it }
        if (mode == 1) {
            Spacer(Modifier.height(10.dp))
            AuthField("Confirm Password", "\u2022\u2022\u2022\u2022\u2022\u2022", confirm, password = true, showPw = showPw, onTogglePw = { showPw = it }) { confirm = it }
            if (confirm.isNotEmpty() && password != confirm) {
                Text("those coos don't match", fontSize = 12.sp, color = c.destructive)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Private Account", fontSize = 14.sp, color = c.primaryText)
                    Text(
                        if (isPrivate) "Only friends can see your sessions" else "Anyone can see your sessions in the feed",
                        fontSize = 11.sp, color = c.tertiaryText,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (busy) {
            CircularProgressIndicator(color = c.primaryText, modifier = Modifier.size(28.dp))
        } else {
            PigeonButton(if (mode == 0) "Sign In" else "Create Account", { attemptAuth() }, Modifier.fillMaxWidth(0.7f))
        }
        Spacer(Modifier.height(14.dp))
        Text("or", fontSize = 12.sp, color = c.tertiaryText)
        Spacer(Modifier.height(14.dp))
        Text(
            "continue without an account",
            fontSize = 14.sp, fontFamily = gloucester, color = c.secondaryText,
            modifier = Modifier.clickable {
                val localName = "Pigeon " + (100..999).random()
                Store.signInLocal(localName, "pigeon${(1000..9999).random()}")
                onComplete()
            },
        )
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, fontSize = 12.sp, color = c.destructive, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AuthField(
    label: String, placeholder: String, value: String,
    password: Boolean = false, showPw: Boolean = false, onTogglePw: ((Boolean) -> Unit)? = null,
    state: Int = 0, stateText: String? = null,
    onChange: (String) -> Unit,
) {
    val c = pigeonColors()
    val borderColor = when {
        state == 2 -> Color(0.3f, 0.7f, 0.35f)
        state == 3 -> c.destructive
        else -> c.separator
    }
    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label, fontFamily = gloucester, fontSize = 13.sp) },
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            singleLine = true,
            visualTransformation = if (password && !showPw) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (password && onTogglePw != null) {
                {
                    IconButton(onClick = { onTogglePw(!showPw) }) {
                        Icon(if (showPw) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = c.tertiaryText, modifier = Modifier.size(18.dp))
                    }
                }
            } else null,
            shape = RoundedCornerShape(PigeonTheme.cornerField),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.primaryText, unfocusedBorderColor = borderColor,
                focusedTextColor = c.primaryText, unfocusedTextColor = c.primaryText,
                focusedLabelColor = c.secondaryText, unfocusedLabelColor = c.tertiaryText,
                cursorColor = c.primaryText,
            ),
            modifier = Modifier.fillMaxWidth().border(0.dp, Color.Transparent),
        )
        stateText?.let {
            Text(it, fontSize = 11.sp, color = when (state) { 2 -> Color(0.3f, 0.7f, 0.35f); 3 -> c.destructive; else -> c.tertiaryText })
        }
    }
}
