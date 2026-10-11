package com.pigeonstudy.pigeon

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun RecordSetupScreen(engine: SessionEngine, onClose: () -> Unit, onStart: () -> Unit) {
    val c = pigeonColors()
    androidx.activity.compose.BackHandler { onClose() }
    var showSubjectPicker by remember { mutableStateOf(false) }
    var showFlights by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, null, tint = c.primaryText)
            }
            Spacer(Modifier.weight(1f))
            Text("new session", fontFamily = gloucester, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.width(40.dp))
        }
        Spacer(Modifier.height(12.dp))

        // Timelapse / Timer toggle
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp)) {
                listOf("Timelapse" to true, "Timer" to false).forEach { (label, cam) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(PigeonTheme.cornerField))
                            .background(if (engine.useCamera == cam) c.primaryText else Color.Transparent)
                            .clickable { engine.useCamera = cam }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, fontFamily = gloucester, fontSize = 15.sp,
                            color = if (engine.useCamera == cam) c.background else c.secondaryText)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Mode cards
        val modes = listOf(
            TimerMode.CONTINUOUS to ("Continuous" to "study until you stop"),
            TimerMode.POMODORO to ("Pomodoro" to "focused sprints with breaks"),
            TimerMode.SET_TIME to ("Set Time" to "pick your target and go"),
            TimerMode.CARRIER_PIGEON to ("Carrier Pigeon" to "race a pigeon across town"),
        )
        modes.forEach { (m, info) ->
            val selected = engine.mode == m
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(PigeonTheme.cornerCard))
                    .background(c.cardBackground.copy(alpha = if (selected) 0.95f else 0.6f))
                    .border(if (selected) 1.5.dp else 0.dp, c.primaryText, RoundedCornerShape(PigeonTheme.cornerCard))
                    .clickable { engine.mode = m }
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected, onClick = { engine.mode = m },
                        colors = RadioButtonDefaults.colors(selectedColor = c.primaryText))
                    Column {
                        Text(info.first, fontFamily = gloucester, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
                        Text(info.second, fontSize = 12.sp, color = c.tertiaryText)
                    }
                }
                // Mode settings
                if (selected) {
                    Spacer(Modifier.height(8.dp))
                    when (m) {
                        TimerMode.POMODORO -> {
                            StepperRow("study", engine.pomodoro.studyMinutes, 5..90) { engine.pomodoro.studyMinutes = it }
                            StepperRow("break", engine.pomodoro.breakMinutes, 1..30) { engine.pomodoro.breakMinutes = it }
                            StepperRow("cycles (0 = forever)", engine.pomodoro.cycles, 0..12) { engine.pomodoro.cycles = it }
                        }
                        TimerMode.SET_TIME -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(30, 60, 120, 180).forEach { mins ->
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(if (engine.setTargetMinutes == mins) c.primaryText else c.surfaceBackground)
                                            .clickable { engine.setTargetMinutes = mins }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    ) {
                                        Text("${mins}m", fontSize = 13.sp, color = if (engine.setTargetMinutes == mins) c.background else c.secondaryText)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            StepperRow("minutes", engine.setTargetMinutes, 5..480) { engine.setTargetMinutes = it }
                        }
                        TimerMode.CARRIER_PIGEON -> {
                            Text(
                                engine.pigeonFlight?.let { "${it.origin.name} → ${it.destination.name}" } ?: "pick a pigeon flight",
                                fontSize = 13.sp, color = c.secondaryText,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(PigeonTheme.cornerField))
                                    .background(c.surfaceBackground)
                                    .clickable { showFlights = true }
                                    .padding(12.dp),
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        // Subject row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PigeonTheme.cornerCard))
                .background(c.cardBackground.copy(alpha = 0.6f))
                .clickable { showSubjectPicker = true }
                .padding(16.dp),
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(subjectColor(engine.currentSubject, Store.subjects)))
            Text("  ${engine.currentSubject}", fontFamily = gloucester, fontSize = 16.sp, color = c.primaryText, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, null, tint = c.tertiaryText)
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PigeonButton("start session", {
                engine.start(); onStart()
            }, Modifier.fillMaxWidth(0.7f))
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showSubjectPicker) {
        SubjectPickerSheet(engine.currentSubject) { engine.currentSubject = it; showSubjectPicker = false }
    }
    if (showFlights) {
        FlightPickerSheet { engine.pigeonFlight = it; showFlights = false }
    }
}

@Composable
private fun StepperRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val c = pigeonColors()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Text("$label  ", fontSize = 13.sp, color = c.secondaryText)
        IconButton(onClick = { if (value > range.first) onChange(value - 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Remove, null, tint = c.primaryText, modifier = Modifier.size(16.dp))
        }
        Text(
            when (label) { "study", "break", "minutes" -> "${value}m"; else -> "$value" },
            fontFamily = gloucester, fontSize = 15.sp, color = c.primaryText,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(onClick = { if (value < range.last) onChange(value + 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Add, null, tint = c.primaryText, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SubjectPickerSheet(current: String, onDismiss: () -> Unit = {}, onPick: (String) -> Unit) {
    val c = pigeonColors()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("subject", fontFamily = gloucester, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Spacer(Modifier.height(8.dp))
            Store.subjects.sortedBy { it.sortOrder }.forEach { s ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(PigeonTheme.cornerField))
                        .background(if (s.name == current) c.accent.copy(alpha = 0.4f) else Color.Transparent)
                        .clickable { onPick(s.name) }
                        .padding(14.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(subjectColor(s.name, Store.subjects)))
                    Text("  ${s.name}", fontSize = 15.sp, color = c.primaryText, modifier = Modifier.weight(1f))
                    if (s.name == current) Icon(Icons.Filled.Check, null, tint = c.primaryText, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun FlightPickerSheet(onPick: (CarrierPigeonService.Flight) -> Unit) {
    val c = pigeonColors()
    val flights = remember { CarrierPigeonService.generateFlights(5) }
    ModalBottomSheet(onDismissRequest = {}, containerColor = c.background) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("departures", fontFamily = gloucester, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.primaryText)
            Text("estimated pigeon arrival times", fontSize = 12.sp, color = c.tertiaryText)
            Spacer(Modifier.height(12.dp))
            flights.forEach { f ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(PigeonTheme.cornerField))
                        .background(c.cardBackground.copy(alpha = 0.7f))
                        .clickable { onPick(f) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(f.origin.name, fontSize = 15.sp, color = c.primaryText)
                        Text("→ ${f.destination.name}", fontSize = 13.sp, color = c.secondaryText)
                    }
                    Text(Fmt.durationHm(f.actualSeconds), fontFamily = gloucester, fontSize = 15.sp, color = c.primaryText)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// MARK: - Recording screen

@Composable
fun RecordingScreen(engine: SessionEngine, onFinished: () -> Unit) {
    val c = pigeonColors()
    androidx.activity.compose.BackHandler(enabled = engine.phase == SessionEngine.Phase.RUNNING && !engine.locked) { engine.pause() }
    val context = LocalContext.current
    var showSubjectSwitch by remember { mutableStateOf(false) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var showLeaveBadge by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
    }
    LaunchedEffect(Unit) { if (cameraGranted) launcher.launch(Manifest.permission.CAMERA) }

    // finished watcher
    LaunchedEffect(engine.phase) {
        if (engine.phase == SessionEngine.Phase.DONE) onFinished()
    }
    // leave badge
    LaunchedEffect(engine.appLeaveCount) { showLeaveBadge = true; delay(2000); showLeaveBadge = false }

    Box(Modifier.fillMaxSize().background(Color(0.04f, 0.04f, 0.03f))) {
        if (engine.useCamera && cameraGranted) {
            CameraTimelapseLayer(engine)
        } else {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(14.dp).clip(CircleShape).background(Color(0.9f, 0.25f, 0.2f)),
                )
                Spacer(Modifier.height(16.dp))
                Text("recording study time", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }

        // Center stack
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (engine.useCamera && cameraGranted) Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0.9f, 0.25f, 0.2f)))
            Spacer(Modifier.height(12.dp))
            Text(
                engine.elapsedDisplay(),
                fontFamily = gloucester, fontSize = 52.sp, fontWeight = FontWeight.Normal,
                color = Color.White, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            when (engine.mode) {
                TimerMode.POMODORO -> Text(
                    if (engine.pomoPhase == "study") "focus — cycle ${engine.pomoCycle + 1}" else "break",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f),
                )
                TimerMode.SET_TIME -> Text(
                    "${Fmt.durationHm((engine.setTargetMinutes * 60 - engine.studySeconds).coerceAtLeast(0))} left",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f),
                )
                TimerMode.CARRIER_PIGEON -> {
                    val f = engine.pigeonFlight
                    if (f != null) {
                        val remaining = (f.actualSeconds - engine.studySeconds).coerceAtLeast(0)
                        Text("${f.origin.name} → ${f.destination.name}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Box(
                            Modifier.padding(top = 6.dp).fillMaxWidth(0.5f).height(4.dp)
                                .clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.25f))
                        ) {
                            Box(
                                Modifier.fillMaxWidth(
                                    (engine.studySeconds.toFloat() / f.actualSeconds).coerceIn(0f, 1f)
                                ).fillMaxHeight().background(Color(0.4f, 0.72f, 0.44f))
                            )
                        }
                        Text("pigeon lands in ${Fmt.durationHm(remaining)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
                else -> {}
            }
            Spacer(Modifier.height(14.dp))
            // Subject pill
            if (!engine.locked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showSubjectSwitch = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(subjectColor(engine.currentSubject, Store.subjects)))
                    Text("  ${engine.currentSubject}", fontSize = 14.sp, color = Color.White)
                    Icon(Icons.Outlined.ExpandMore, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            if (engine.appLeaveCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "left ${engine.appLeaveCount}× · ${Fmt.durationShort(engine.offTaskSeconds)} off-task",
                    fontSize = 11.sp, color = Color(1f, 0.7f, 0.4f),
                )
            }
        }

        // Top bar
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!engine.locked) {
                Text(
                    "Lock",
                    color = Color.White, fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { engine.lock() }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }

        // Bottom controls
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (engine.locked) {
                var hold by remember { mutableStateOf(false) }
                LaunchedEffect(hold) { if (hold) { delay(1200); engine.unlock(); hold = false } }
                Text(
                    "hold to unlock",
                    color = Color.White, fontSize = 14.sp, fontFamily = gloucester,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (hold) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.5f))
                        .clickable { hold = true }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                )
            } else {
                IconButton(
                    onClick = { engine.pause() },
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                ) { Icon(Icons.Filled.Pause, null, tint = Color.White) }
                Spacer(Modifier.width(28.dp))
                IconButton(
                    onClick = { engine.finish() },
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
                ) { Icon(Icons.Filled.Stop, null, tint = Color(0.8f, 0.2f, 0.2f), modifier = Modifier.size(32.dp)) }
            }
        }
    }

    if (showSubjectSwitch) {
        SubjectPickerSheet(engine.currentSubject) { engine.switchSubject(it); showSubjectSwitch = false }
    }
}

// MARK: - Paused overlay

@Composable
fun PausedOverlay(engine: SessionEngine, onDismiss: () -> Unit) {
    val c = pigeonColors()
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(engine.elapsedDisplay(), fontFamily = gloucester, fontSize = 44.sp, color = Color.White)
            Spacer(Modifier.height(24.dp))
            PigeonButton("Resume", { engine.resume(); onDismiss() })
            Spacer(Modifier.height(10.dp))
            PigeonButton("End Session", { engine.finish(); onDismiss() }, filled = false)
            Spacer(Modifier.height(10.dp))
            var confirm by remember { mutableStateOf(false) }
            Text(
                "Discard",
                fontSize = 13.sp, color = Color(0.8f, 0.5f, 0.5f),
                modifier = Modifier.clickable { confirm = true }.padding(8.dp),
            )
            if (confirm) {
                AlertDialog(
                    onDismissRequest = { confirm = false },
                    title = { Text("Discard session?") },
                    text = { Text("Your study time will still be saved to your history.") },
                    confirmButton = {
                        TextButton(onClick = { engine.discard(); onDismiss() }) { Text("Discard", color = c.destructive) }
                    },
                    dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
                )
            }
        }
    }
}

// MARK: - Camera timelapse layer

@Composable
private fun CameraTimelapseLayer(engine: SessionEngine) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize(),
    )

    // periodic frame capture
    LaunchedEffect(engine.phase) {
        while (engine.phase == SessionEngine.Phase.RUNNING) {
            delay(engine.frameIntervalSeconds() * 1000L)
            if (engine.phase != SessionEngine.Phase.RUNNING) break
            val dir = engine.frameDirName?.let { File(context.filesDir, it) } ?: continue
            if (!dir.exists()) continue
            val file = File(dir, "frame_%04d.jpg".format(engine.frameCount))
            val opts = ImageCapture.OutputFileOptions.Builder(file).build()
            imageCapture.takePicture(opts, ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        engine.captureFrame(file)
                    }
                    override fun onError(exception: ImageCaptureException) {}
                })
        }
    }
}
