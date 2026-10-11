package com.pigeonstudy.pigeon

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class TimerMode { CONTINUOUS, POMODORO, SET_TIME, CARRIER_PIGEON }

data class PomodoroConfig(var studyMinutes: Int = 25, var breakMinutes: Int = 5, var cycles: Int = 0)

class SessionEngine(private val context: Context) {
    var phase by mutableStateOf(Phase.SETUP) // SETUP, RUNNING, PAUSED, DONE
    var mode by mutableStateOf(TimerMode.CONTINUOUS)
    var useCamera by mutableStateOf(true)

    var currentSubject by mutableStateOf("General")
    var segments by mutableStateOf(listOf<SubjectSegment>())

    // clock state
    var studySeconds by mutableIntStateOf(0)
    var breakSeconds by mutableIntStateOf(0)
    var runningWallStart by mutableLongStateOf(0L)
    var pausedAccumMs by mutableLongStateOf(0L)

    // pomodoro
    var pomodoro = PomodoroConfig()
    var pomoPhase by mutableStateOf("study") // study / break
    var pomoCycle by mutableIntStateOf(0)

    // set time target (study seconds)
    var setTargetMinutes by mutableIntStateOf(30)

    // carrier pigeon
    var pigeonFlight by mutableStateOf<CarrierPigeonService.Flight?>(null)

    // tracking
    var appLeaveCount by mutableIntStateOf(0)
    var offTaskSeconds by mutableIntStateOf(0)
    var locked by mutableStateOf(false)
    var lastResignMs by mutableLongStateOf(0L)

    // media
    var sessionId: String = UUID.randomUUID().toString()
    var frameDirName: String? = null
    var frameCount = 0

    private var tickJob: Job? = null
    private var ticker = 0
    private val scope = CoroutineScope(Dispatchers.Main)
    private var segmentStartWallMs = 0L

    val snapshotFile: File get() = File(context.filesDir, "pigeon_active_session.json")

    enum class Phase { SETUP, RUNNING, PAUSED, DONE }

    fun start() {
        sessionId = UUID.randomUUID().toString()
        studySeconds = 0; breakSeconds = 0; pausedAccumMs = 0
        pomoPhase = "study"; pomoCycle = 0
        appLeaveCount = 0; offTaskSeconds = 0
        segments = listOf(SubjectSegment(currentSubject, 0))
        runningWallStart = System.currentTimeMillis()
        segmentStartWallMs = runningWallStart
        if (useCamera) {
            frameDirName = "frames/$sessionId"
            File(context.filesDir, frameDirName!!).mkdirs()
            frameCount = 0
        } else frameDirName = null
        phase = Phase.RUNNING
        if (mode == TimerMode.CARRIER_PIGEON && pigeonFlight == null) {
            pigeonFlight = CarrierPigeonService.generateFlights(5).firstOrNull()
        }
        saveSnapshot(backgrounded = false)
        startTicking()
        showNotification()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && phase == Phase.RUNNING) {
                delay(1000)
                if (phase != Phase.RUNNING) break
                ticker++
                when (mode) {
                    TimerMode.CONTINUOUS -> studySeconds++
                    TimerMode.POMODORO -> {
                        if (pomoPhase == "study") {
                            studySeconds++
                            val withinCycle = studySeconds - pomoCycle * pomodoro.studyMinutes * 60
                            if (withinCycle >= pomodoro.studyMinutes * 60) maybeEndPomoPhase()
                        } else {
                            breakSeconds++
                            if (breakSeconds >= pomodoro.breakMinutes * 60) maybeEndPomoPhase()
                        }
                    }
                    TimerMode.SET_TIME -> {
                        studySeconds++
                        if (studySeconds >= setTargetMinutes * 60) { finish(); break }
                    }
                    TimerMode.CARRIER_PIGEON -> {
                        studySeconds++
                        val flight = pigeonFlight
                        if (flight != null && studySeconds >= flight.actualSeconds) { finish(); break }
                    }
                }
                if (ticker % 30 == 0) saveSnapshot(backgrounded = false)
            }
        }
    }

    private fun totalStudyTarget(): Int {
        val cycles = if (pomodoro.cycles == 0) Int.MAX_VALUE else pomodoro.cycles
        return cycles * pomodoro.studyMinutes * 60
    }

    private fun maybeEndPomoPhase() {
        val cycles = if (pomodoro.cycles == 0) Int.MAX_VALUE else pomodoro.cycles
        if (pomoPhase == "study") {
            if (pomoCycle + 1 >= cycles) {
                finish(); return
            }
            if (pomodoro.breakMinutes == 0) {
                pomoCycle++
                return
            }
            pomoPhase = "break"; breakSeconds = 0
        } else {
            pomoPhase = "study"; pomoCycle++
        }
    }

    fun pause() {
        if (phase != Phase.RUNNING) return
        phase = Phase.PAUSED
        pausedAccumMs += System.currentTimeMillis() - runningWallStart
        tickJob?.cancel()
        saveSnapshot(backgrounded = false)
        cancelNotification()
    }

    fun resume() {
        if (phase != Phase.PAUSED) return
        runningWallStart = System.currentTimeMillis()
        segmentStartWallMs = runningWallStart
        phase = Phase.RUNNING
        startTicking()
        showNotification()
    }

    fun switchSubject(to: String) {
        finalizeSegment()
        currentSubject = to
        segments = segments + SubjectSegment(to, 0)
        segmentStartWallMs = System.currentTimeMillis()
        saveSnapshot(backgrounded = false)
    }

    private fun finalizeSegment() {
        val now = System.currentTimeMillis()
        val extraSecs = ((now - segmentStartWallMs) / 1000L).toInt()
        if (extraSecs >= 1 && segments.isNotEmpty()) {
            val idx = segments.indexOfLast { it.subject == currentSubject }
            val seg = segments[idx]
            segments = segments.toMutableList().also { it[idx] = seg.copy(seconds = seg.seconds + extraSecs) }
        }
    }

    fun finalizedSegments(): List<SubjectSegment> =
        segments.filter { it.seconds > 0 }.sortedByDescending { it.seconds }

    fun lock() { locked = true }
    fun unlock() { locked = false }

    // MARK: App leave

    fun onAppHide() {
        if (phase != Phase.RUNNING || locked) return
        lastResignMs = System.currentTimeMillis()
        saveSnapshot(backgrounded = true)
    }

    fun onAppShow() {
        if (phase != Phase.RUNNING || lastResignMs == 0L) return
        val awayMs = System.currentTimeMillis() - lastResignMs
        lastResignMs = 0L
        if (awayMs >= 2000) {
            appLeaveCount++
            offTaskSeconds += (awayMs / 1000L).toInt()
            // fast-forward elapsed by wall-clock time away
            val awaySecs = (awayMs / 1000L).toInt()
            if (mode == TimerMode.CONTINUOUS) studySeconds += awaySecs
        }
        saveSnapshot(backgrounded = false)
    }

    fun captureFrame(file: File) { frameCount++ }

    fun frameIntervalSeconds(): Int {
        val m = studySeconds / 60
        return when {
            m < 2 -> 5
            m < 10 -> 10
            m < 30 -> 20
            else -> 30
        }
    }

    // MARK: Finish

    fun finish() {
        tickJob?.cancel()
        finalizeSegment()
        phase = Phase.DONE
        cancelNotification()
    }

    fun discard() {
        tickJob?.cancel()
        phase = Phase.SETUP
        cancelNotification()
        // keep snapshot so time is recoverable next launch (matches iOS)
        saveSnapshot(backgrounded = true)
        frameDirName?.let { File(context.filesDir, it).deleteRecursively() }
        frameDirName = null
    }

    fun cleanupAfterPost() {
        snapshotFile.delete()
        phase = Phase.SETUP
        pigeonFlight = null
    }

    fun buildTimelapse(caption: String, description: String, rarity: String?): StudyTimelapse {
        val uid = Store.activeUid() ?: "local"
        val profile = Store.currentProfile
        val segs = finalizedSegments()
        val primary = segs.maxByOrNull { it.seconds }?.subject ?: currentSubject
        val segmentsJson = if (segs.size > 1) {
            JSONArray().apply { segs.forEach { put(it.toJson()) } }.toString()
        } else null
        return StudyTimelapse(
            authorID = uid,
            authorName = profile?.displayName ?: "Pigeon",
            authorAvatarURL = profile?.avatarURL,
            caption = caption,
            studyDescription = description,
            subject = primary,
            durationSeconds = studySeconds,
            frameDir = frameDirName,
            appLeaveCount = appLeaveCount,
            offTaskSeconds = offTaskSeconds,
            subjectSegmentsJSON = segmentsJson,
            pigeonOrigin = pigeonFlight?.origin?.name,
            pigeonDestination = pigeonFlight?.destination?.name,
            descriptionRarity = rarity,
        )
    }

    // MARK: Snapshot

    fun saveSnapshot(backgrounded: Boolean) {
        try {
            val segs = finalizedSegments().let { list ->
                JSONArray().apply { list.forEach { put(it.toJson()) } }
            }
            val o = JSONObject()
                .put("sessionId", sessionId)
                .put("studySeconds", studySeconds)
                .put("breakSeconds", breakSeconds)
                .put("currentSubject", currentSubject)
                .put("segments", segs)
                .put("mode", mode.name)
                .put("appLeaveCount", appLeaveCount)
                .put("offTaskSeconds", offTaskSeconds)
                .put("frameDir", frameDirName ?: JSONObject.NULL)
                .put("savedAt", System.currentTimeMillis())
                .put("wasBackgrounded", backgrounded)
            if (mode == TimerMode.CARRIER_PIGEON && pigeonFlight != null) {
                o.put("pigeonOrigin", pigeonFlight!!.origin.name)
                o.put("pigeonDestination", pigeonFlight!!.destination.name)
                o.put("pigeonActualSeconds", pigeonFlight!!.actualSeconds)
            }
            snapshotFile.writeText(o.toString())
        } catch (_: Exception) {}
    }

    data class SnapshotInfo(val studySeconds: Int, val subject: String, val savedAt: Long, val wasBackgrounded: Boolean)

    fun loadSnapshotInfo(): SnapshotInfo? = try {
        if (!snapshotFile.exists()) null
        else {
            val o = JSONObject(snapshotFile.readText())
            SnapshotInfo(
                o.optInt("studySeconds"),
                o.optString("currentSubject", "General"),
                o.optLong("savedAt"),
                o.optBoolean("wasBackgrounded", false),
            )
        }
    } catch (_: Exception) { null }

    fun recoverFromSnapshot(): Boolean {
        val info = loadSnapshotInfo() ?: return false
        return try {
            val o = JSONObject(snapshotFile.readText())
            val awaySecs = ((System.currentTimeMillis() - info.savedAt) / 1000L).toInt()
            studySeconds = info.studySeconds + awaySecs.coerceAtLeast(0)
            currentSubject = info.subject
            appLeaveCount = o.optInt("appLeaveCount")
            offTaskSeconds = o.optInt("offTaskSeconds")
            mode = runCatching { TimerMode.valueOf(o.optString("mode")) }.getOrDefault(TimerMode.CONTINUOUS)
            frameDirName = null
            segments = 0.until(o.optJSONArray("segments").length()).map {
                SubjectSegment.fromJson(o.optJSONArray("segments").getJSONObject(it))
            }
            phase = Phase.RUNNING
            runningWallStart = System.currentTimeMillis()
            segmentStartWallMs = runningWallStart
            snapshotFile.delete()
            startTicking()
            showNotification()
            true
        } catch (_: Exception) { false }
    }

    // MARK: Notification

    private fun showNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val note = Notification.Builder(context, "session")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("study session in progress")
            .setContentText("$currentSubject — keep going, pigeon")
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        try { nm.notify(1, note) } catch (_: Exception) {}
    }

    private fun cancelNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(1)
    }

    fun elapsedDisplay(): String = Fmt.clock(studySeconds)
}

object SessionState {
    var engine: SessionEngine? = null
    fun get(context: Context): SessionEngine {
        if (engine == null) engine = SessionEngine(context.applicationContext)
        return engine!!
    }
}
