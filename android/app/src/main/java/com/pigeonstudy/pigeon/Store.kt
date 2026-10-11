package com.pigeonstudy.pigeon

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID

object Store {
    lateinit var appContext: Context
    lateinit var prefs: SharedPreferences
    val scope = CoroutineScope(Dispatchers.Main)
    val ioScope = CoroutineScope(Dispatchers.IO)

    var timelapses by mutableStateOf(listOf<StudyTimelapse>())
    var subjects by mutableStateOf(listOf<StudySubject>())
    var groups by mutableStateOf(listOf<StudyGroup>())
    var profiles by mutableStateOf(listOf<UserProfile>())
    var currentProfile by mutableStateOf<UserProfile?>(null)
    var authLoading by mutableStateOf(true)
    var signedIn by mutableStateOf(false)
    var isCloudUser by mutableStateOf(false)
    var syncInProgress by mutableStateOf(false)

    var wallpaperId by mutableStateOf("")
    var appearanceMode by mutableStateOf(0) // 0 system, 1 light, 2 dark
    var hasSeenTutorial by mutableStateOf(false)

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("pigeon", Context.MODE_PRIVATE)
        wallpaperId = prefs.getString("wallpaperId", "") ?: ""
        appearanceMode = prefs.getInt("appearanceMode", 0)
        hasSeenTutorial = prefs.getBoolean("hasSeenTutorial", false)
        load()
        restoreSession()
    }

    // MARK: - Persistence

    private fun file(name: String) = File(appContext.filesDir, name)

    private fun load() {
        timelapses = loadList("timelapses.json") { StudyTimelapse.fromJson(it) }
        subjects = loadList("subjects.json") { StudySubject.fromJson(it) }
        groups = loadList("groups.json") { StudyGroup.fromJson(it) }
        profiles = loadList("profiles.json") { UserProfile.fromJson(it) }
        if (subjects.isEmpty()) {
            subjects = StudySubject.seedDefaults()
            saveSubjects()
        }
        currentProfile = profiles.firstOrNull { it.firebaseUID == activeUid() }
    }

    private fun <T> loadList(name: String, parse: (JSONObject) -> T): List<T> = try {
        val txt = file(name).readText()
        val arr = JSONArray(txt)
        0.until(arr.length()).map { parse(arr.getJSONObject(it)) }
    } catch (_: Exception) { emptyList() }

    private fun <T> saveList(name: String, list: List<T>, toJson: (T) -> JSONObject) {
        ioScope.launch {
            try {
                val arr = JSONArray()
                list.forEach { arr.put(toJson(it)) }
                File(appContext.filesDir, name).writeText(arr.toString())
            } catch (_: Exception) {}
        }
    }

    fun saveTimelapses() = saveList("timelapses.json", timelapses) { it.toJson() }
    fun saveSubjects() = saveList("subjects.json", subjects) { it.toJson() }
    fun saveGroups() = saveList("groups.json", groups) { it.toJson() }
    fun saveProfiles() = saveList("profiles.json", profiles) { it.toJson() }

    fun setWallpaper(id: String) {
        wallpaperId = id
        prefs.edit().putString("wallpaperId", id).apply()
    }

    fun setAppearance(mode: Int) {
        appearanceMode = mode
        prefs.edit().putInt("appearanceMode", mode).apply()
    }

    fun setTutorialSeen() {
        hasSeenTutorial = true
        prefs.edit().putBoolean("hasSeenTutorial", true).apply()
    }

    // MARK: - Auth

    fun activeUid(): String? {
        val session = Supabase.currentSession()
        if (session != null) return session.uid
        return prefs.getString("localUid", null)
    }

    fun isLocalAccount(): Boolean = Supabase.currentSession() == null && prefs.contains("localUid")

    private fun restoreSession() {
        val refresh = prefs.getString("refreshToken", null)
        val uid = prefs.getString("authUid", null)
        scope.launch {
            if (refresh != null && uid != null) {
                try {
                    val session = Supabase.refreshSession(refresh)
                    applySession(session)
                } catch (_: Exception) {
                    clearSession()
                }
            } else if (uid != null) {
                // local account
                signedIn = true
            }
            authLoading = false
            if (signedIn) syncFromCloud()
        }
    }

    fun applySession(session: AuthSession) {
        Supabase.setSession(session)
        prefs.edit()
            .putString("refreshToken", session.refreshToken)
            .putString("authUid", session.uid)
            .remove("localUid")
            .apply()
        isCloudUser = true
        signedIn = true
        ensureProfile(session.uid)
        scope.launch { syncFromCloud() }
    }

    fun signInLocal(displayName: String, username: String?) {
        val uid = prefs.getString("localUid", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("localUid", it).putString("authUid", it).apply()
        }
        isCloudUser = false
        signedIn = true
        ensureProfile(uid).let {
            it.displayName = displayName
            if (username != null && it.username == null) it.username = username
            saveProfiles()
        }
        currentProfile = profiles.firstOrNull { it.firebaseUID == uid }
    }

    private fun ensureProfile(uid: String): UserProfile {
        val existing = profiles.firstOrNull { it.firebaseUID == uid }
        if (existing != null) { currentProfile = existing; return existing }
        val p = UserProfile(firebaseUID = uid, displayName = "Pigeon", isPrivate = true)
        profiles = profiles + p
        currentProfile = p
        saveProfiles()
        return p
    }

    fun clearSession() {
        prefs.edit().remove("refreshToken").remove("authUid").remove("localUid").apply()
        Supabase.setSession(null)
        signedIn = false
        isCloudUser = false
        currentProfile = null
    }

    suspend fun signOut() {
        Supabase.signOut()
        clearSession()
    }

    // MARK: - Stats

    fun userTimelapses(): List<StudyTimelapse> {
        val uid = activeUid() ?: return emptyList()
        return timelapses.filter { it.authorID == uid }
    }

    fun dayBucket(ms: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun streakDays(): Int {
        val dayMs = 86400000L
        val today = dayBucket(System.currentTimeMillis())
        val studyDays = userTimelapses().map { dayBucket(it.createdAt) }.toSet()
        var check = today
        if (check !in studyDays) {
            check -= dayMs
            if (check !in studyDays) return 0
        }
        var streak = 0
        while (check in studyDays) { streak++; check -= dayMs }
        return streak
    }

    fun totalMinutes(): Int = userTimelapses().sumOf { it.durationSeconds } / 60

    fun recomputeStats() {
        val p = currentProfile ?: return
        val minutes = totalMinutes()
        val streak = streakDays()
        if (p.totalStudyMinutes != minutes || p.streakDays != streak) {
            p.totalStudyMinutes = minutes
            p.streakDays = streak
            saveProfiles()
            if (isCloudUser) scope.launch { try { Supabase.upsertUser(p) } catch (_: Exception) {} }
        }
    }

    fun topSubjects(limit: Int = 3): List<Pair<String, Int>> {
        val map = HashMap<String, Int>()
        userTimelapses().forEach { tl ->
            tl.subjectSegments.forEach { seg ->
                map[seg.subject] = (map[seg.subject] ?: 0) + seg.seconds
            }
        }
        return map.entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }
    }

    // MARK: - Records

    fun addTimelapse(t: StudyTimelapse) {
        timelapses = listOf(t) + timelapses
        saveTimelapses()
        recomputeStats()
        if (isCloudUser) scope.launch {
            try {
                Supabase.saveTimelapse(t)
                t.cloudSynced = true
                saveTimelapses()
            } catch (_: Exception) {
                t.syncRetryCount++
                saveTimelapses()
            }
        }
    }

    fun updateTimelapse(t: StudyTimelapse) {
        timelapses = timelapses.map { if (it.id == t.id) t else it }
        saveTimelapses()
        if (isCloudUser) scope.launch { try { Supabase.saveTimelapse(t) } catch (_: Exception) {} }
    }

    fun deleteTimelapse(t: StudyTimelapse) {
        timelapses = timelapses.filter { it.id != t.id }
        saveTimelapses()
        recomputeStats()
        PendingDeleteStore.add(prefs, t.id)
        if (isCloudUser) scope.launch {
            try {
                Supabase.deleteTimelapse(t.id)
                PendingDeleteStore.remove(prefs, t.id)
            } catch (_: Exception) {}
        }
    }

    fun toggleLike(t: StudyTimelapse) {
        val uid = activeUid() ?: return
        val liked = uid in t.likedByUIDs
        if (liked) { t.likedByUIDs.remove(uid); t.likeCount-- } else { t.likedByUIDs.add(uid); t.likeCount++ }
        updateTimelapse(t)
        if (isCloudUser) scope.launch { try { Supabase.toggleLike(t.id, uid, !liked) } catch (_: Exception) {} }
    }

    // MARK: - Cloud sync

    fun syncFromCloud() {
        if (!isCloudUser) return
        val uid = activeUid() ?: return
        syncInProgress = true
        scope.launch {
            try {
                val cloudProfile = Supabase.fetchUser(uid)
                if (cloudProfile != null) {
                    val local = profiles.firstOrNull { it.firebaseUID == uid }
                    if (local == null) {
                        profiles = profiles + cloudProfile
                    } else {
                        local.displayName = cloudProfile.displayName
                        local.username = cloudProfile.username
                        local.bio = cloudProfile.bio
                        local.avatarURL = cloudProfile.avatarURL
                        local.friendUIDs = cloudProfile.friendUIDs
                        local.pendingFriendRequestUIDs = cloudProfile.pendingFriendRequestUIDs
                        local.isPrivate = cloudProfile.isPrivate
                    }
                    currentProfile = profiles.firstOrNull { it.firebaseUID == uid }
                    saveProfiles()
                } else {
                    currentProfile?.let { Supabase.upsertUser(it) }
                }
                Supabase.updatePresence(uid, false, null)

                val pendingDeletes = PendingDeleteStore.ids(prefs)
                val cloudTimelapses = Supabase.fetchTimelapsesFor(uid)
                val byId = HashMap<String, StudyTimelapse>()
                (timelapses + cloudTimelapses).forEach { byId[it.id] = it }
                timelapses = byId.values.filter { it.id !in pendingDeletes }
                    .sortedByDescending { it.createdAt }

                val friendUids = currentProfile?.friendUIDs ?: emptyList()
                if (friendUids.isNotEmpty()) {
                    try {
                        val feed = Supabase.fetchFeed(friendUids)
                        val existing = timelapses.map { it.id }.toSet()
                        val newOnes = feed.filter { it.id !in existing }
                        timelapses = (newOnes + timelapses).sortedByDescending { it.createdAt }
                    } catch (_: Exception) {}
                }

                try {
                    groups = Supabase.fetchGroups(uid)
                } catch (_: Exception) {}
                saveGroups()
                saveTimelapses()
                recomputeStats()

                // upload any unsynced
                timelapses.filter { !it.cloudSynced && it.authorID == uid }.forEach { t ->
                    try { Supabase.saveTimelapse(t); t.cloudSynced = true } catch (_: Exception) {}
                }
                saveTimelapses()
            } catch (_: Exception) {
            } finally {
                syncInProgress = false
            }
        }
    }

    fun retryUnsynced() {
        if (!isCloudUser) return
        val uid = activeUid() ?: return
        scope.launch {
            timelapses.filter { !it.cloudSynced && it.authorID == uid }.forEach { t ->
                try { Supabase.saveTimelapse(t); t.cloudSynced = true; saveTimelapses() } catch (_: Exception) {}
            }
        }
    }
}
