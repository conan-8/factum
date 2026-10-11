package com.pigeonstudy.pigeon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class AuthSession(val uid: String, val email: String?, val accessToken: String, val refreshToken: String)

object Supabase {
    const val URL_BASE = "https://ijtyoxqaaopvnxvblhdf.supabase.co"
    const val ANON_KEY = "sb_publishable_6zWOMkf4_2P_ueLsrLg2-A_eKhIJg0e"

    private var session: AuthSession? = null
    fun currentSession(): AuthSession? = session
    fun setSession(s: AuthSession?) { session = s }

    private fun authHeader(): Pair<String, String> =
        "Authorization" to ("Bearer " + (session?.accessToken ?: ANON_KEY))

    private fun errStream(conn: HttpURLConnection): String = try {
        conn.errorStream?.bufferedReader()?.readText() ?: ""
    } catch (_: Exception) { "" }

    class SupabaseException(msg: String) : Exception(msg)

    private fun requestChecked(method: String, path: String, body: JSONObject? = null,
                                extraHeaders: List<Pair<String, String>> = emptyList(), query: String = ""): String {
        val conn = URL("$URL_BASE$path$query").openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.requestMethod = method
            conn.setRequestProperty("apikey", ANON_KEY)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer " + (session?.accessToken ?: ANON_KEY))
            extraHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            }
            val code = conn.responseCode
            if (code in 200..299) return conn.inputStream.bufferedReader().readText()
            throw SupabaseException("HTTP $code: ${errStream(conn).take(300)}")
        } finally {
            conn.disconnect()
        }
    }

    // MARK: - Auth

    suspend fun signUpWithEmail(email: String, password: String, fullName: String): AuthSession = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email).put("password", password)
            .put("data", JSONObject().put("full_name", fullName))
        val resp = JSONObject(requestChecked("POST", "/auth/v1/signup", body))
        parseAuthResponse(resp)
    }

    suspend fun signInWithEmail(email: String, password: String): AuthSession = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email).put("password", password)
        val resp = JSONObject(requestChecked("POST", "/auth/v1/token?grant_type=password", body))
        parseAuthResponse(resp)
    }

    suspend fun refreshSession(refreshToken: String): AuthSession = withContext(Dispatchers.IO) {
        val body = JSONObject().put("refresh_token", refreshToken)
        val resp = JSONObject(requestChecked("POST", "/auth/v1/token?grant_type=refresh_token", body))
        parseAuthResponse(resp)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try { requestChecked("POST", "/auth/v1/logout") } catch (_: Exception) {}
    }

    private fun parseAuthResponse(resp: JSONObject): AuthSession {
        val token = resp.optString("access_token")
        if (token.isNullOrEmpty()) throw SupabaseException("No access token in response")
        val user = resp.optJSONObject("user")
        val uid = user?.optString("id") ?: resp.optString("uid")
        val email = user?.optString("email")?.takeIf { it.isNotEmpty() && it != "null" }
        return AuthSession(uid, email, token, resp.optString("refresh_token"))
    }

    // MARK: - Date helpers

    fun isoNow(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())
    fun parseIso(s: String?): Long? {
        if (s == null) return null
        return try { OffsetDateTime.parse(s).toInstant().toEpochMilli() } catch (_: Exception) { null }
    }

    // MARK: - Users

    private val userFields = "uid,display_name,username,avatar_url,bio,join_date,total_study_minutes,streak_days,friend_uids,pending_friend_request_uids,subjects,last_seen_at,is_studying,current_subject,is_private"

    fun userToRow(p: UserProfile): JSONObject {
        val uid = p.firebaseUID ?: throw SupabaseException("profile has no uid")
        return JSONObject()
            .put("uid", normalizeUUID(uid))
            .put("display_name", p.displayName)
            .put("username", p.username ?: JSONObject.NULL)
            .put("avatar_url", p.avatarURL ?: JSONObject.NULL)
            .put("bio", p.bio)
            .put("join_date", isoNow())
            .put("total_study_minutes", p.totalStudyMinutes)
            .put("streak_days", p.streakDays)
            .put("friend_uids", JSONArray(p.friendUIDs.map { normalizeUUID(it) }))
            .put("pending_friend_request_uids", JSONArray(p.pendingFriendRequestUIDs.map { normalizeUUID(it) }))
            .put("is_private", p.isPrivate)
    }

    fun rowToProfile(o: JSONObject): UserProfile {
        val uid = o.optString("uid")
        return UserProfile(
            firebaseUID = uid,
            displayName = o.optString("display_name", "Pigeon"),
            username = o.optString("username").takeIf { it.isNotEmpty() && it != "null" },
            avatarURL = o.optString("avatar_url").takeIf { it.isNotEmpty() && it != "null" },
            bio = o.optString("bio", ""),
            joinDate = parseIso(o.optString("join_date")) ?: System.currentTimeMillis(),
            totalStudyMinutes = o.optInt("total_study_minutes"),
            streakDays = o.optInt("streak_days"),
            isPrivate = o.optBoolean("is_private", true),
        ).apply {
            friendUIDs = o.optJSONArray("friend_uids")?.let { 0.until(it.length()).map { i -> it.getString(i) } }?.toMutableList() ?: mutableListOf()
            pendingFriendRequestUIDs = o.optJSONArray("pending_friend_request_uids")?.let { 0.until(it.length()).map { i -> it.getString(i) } }?.toMutableList() ?: mutableListOf()
        }
    }

    suspend fun upsertUser(p: UserProfile) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/users", userToRow(p),
            listOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
    }

    suspend fun fetchUser(uid: String): UserProfile? = withContext(Dispatchers.IO) {
        val q = "?select=" + urlEncode(userFields) + "&uid=eq." + urlEncode(normalizeUUID(uid))
        val arr = JSONArray(requestChecked("GET", "/rest/v1/users", query = q))
        if (arr.length() == 0) null else rowToProfile(arr.getJSONObject(0))
    }

    suspend fun searchUsers(query: String, excludeUid: String): List<UserProfile> = withContext(Dispatchers.IO) {
        val q = ("?select=" + urlEncode(userFields)
            + "&or=(username.ilike.*" + urlEncode(query) + "*,display_name.ilike.*" + urlEncode(query) + "*)"
            + "&limit=20")
        val arr = JSONArray(requestChecked("GET", "/rest/v1/users", query = q))
        0.until(arr.length()).map { rowToProfile(arr.getJSONObject(it)) }.filter { it.firebaseUID != excludeUid }
    }

    suspend fun isUsernameAvailable(username: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().put("p_username", username.lowercase())
        requestChecked("POST", "/rest/v1/rpc/is_username_available", body).trim('"') == "true"
    }

    suspend fun claimUsername(uid: String, username: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().put("p_uid", normalizeUUID(uid)).put("p_username", username.lowercase())
        try { requestChecked("POST", "/rest/v1/rpc/claim_username", body).trim('"') != "false" } catch (_: Exception) { false }
    }

    suspend fun updatePresence(uid: String, isStudying: Boolean, subject: String?) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("last_seen_at", isoNow()).put("is_studying", isStudying)
            .put("current_subject", subject ?: JSONObject.NULL)
        val q = "?uid=eq." + urlEncode(normalizeUUID(uid))
        try { requestChecked("PATCH", "/rest/v1/users", body, query = q) } catch (_: Exception) {}
    }

    // MARK: - RPCs

    suspend fun addFriend(uid: String, friendUid: String) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/rpc/add_friend",
            JSONObject().put("current_uid", normalizeUUID(uid)).put("friend_uid", normalizeUUID(friendUid)))
    }

    suspend fun removeFriend(uid: String, friendUid: String) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/rpc/remove_friend",
            JSONObject().put("current_uid", normalizeUUID(uid)).put("friend_uid", normalizeUUID(friendUid)))
    }

    suspend fun acceptFriendRequest(requestId: String) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/rpc/accept_friend_request", JSONObject().put("request_id", normalizeUUID(requestId)))
    }

    suspend fun toggleLike(timelapseId: String, userUid: String, isLiked: Boolean) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/rpc/toggle_like",
            JSONObject().put("p_timelapse_id", normalizeUUID(timelapseId))
                .put("p_user_uid", normalizeUUID(userUid)).put("p_is_liked", isLiked))
    }

    suspend fun joinGroup(groupId: String, uid: String) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/rpc/join_group",
            JSONObject().put("p_group_id", normalizeUUID(groupId)).put("p_user_uid", normalizeUUID(uid)))
    }

    suspend fun leaveGroup(groupId: String, uid: String) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/rpc/leave_group",
            JSONObject().put("p_group_id", normalizeUUID(groupId)).put("p_user_uid", normalizeUUID(uid)))
    }

    // MARK: - Friend requests

    suspend fun sendFriendRequest(from: String, to: String) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/friend_requests",
            JSONObject().put("id", UUID.randomUUID().toString())
                .put("sender_uid", normalizeUUID(from)).put("receiver_uid", normalizeUUID(to))
                .put("status", "pending").put("created_at", isoNow()),
            listOf("Prefer" to "return=minimal"))
    }

    suspend fun fetchFriendRequests(uid: String): JSONArray = withContext(Dispatchers.IO) {
        val q = ("?select=id,sender_uid,receiver_uid,status,created_at&receiver_uid=eq."
            + urlEncode(normalizeUUID(uid)) + "&status=eq.pending")
        JSONArray(requestChecked("GET", "/rest/v1/friend_requests", query = q))
    }

    // MARK: - Timelapses

    fun timelapseToRow(t: StudyTimelapse): JSONObject {
        fun ns(v: String?) = v ?: JSONObject.NULL
        return JSONObject()
            .put("id", t.id).put("author_id", normalizeUUID(t.authorID))
            .put("author_name", t.authorName).put("author_avatar_url", ns(t.authorAvatarURL))
            .put("caption", t.caption).put("study_description", t.studyDescription)
            .put("subject", t.subject).put("duration_seconds", t.durationSeconds)
            .put("created_at", DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(t.createdAt)))
            .put("is_landscape", t.isLandscape).put("like_count", t.likeCount)
            .put("liked_by_uids", JSONArray(t.likedByUIDs.map { normalizeUUID(it) }))
            .put("comment_count", t.commentCount)
            .put("app_leave_count", t.appLeaveCount).put("off_task_seconds", t.offTaskSeconds)
            .put("subject_segments_json", ns(t.subjectSegmentsJSON))
            .put("pigeon_origin", ns(t.pigeonOrigin)).put("pigeon_destination", ns(t.pigeonDestination))
            .put("description_rarity", ns(t.descriptionRarity))
    }

    fun rowToTimelapse(o: JSONObject): StudyTimelapse {
        fun opt(k: String) = o.optString(k).takeIf { it.isNotEmpty() && it != "null" }
        val t = StudyTimelapse(
            id = o.getString("id"),
            authorID = o.getString("author_id"),
            authorName = o.optString("author_name", "Pigeon"),
            authorAvatarURL = opt("author_avatar_url"),
            caption = o.optString("caption", ""),
            studyDescription = o.optString("study_description", ""),
            subject = o.optString("subject", "General"),
            durationSeconds = o.optInt("duration_seconds"),
            createdAt = parseIso(o.optString("created_at")) ?: System.currentTimeMillis(),
            isLandscape = o.optBoolean("is_landscape"),
            likeCount = o.optInt("like_count"),
            commentCount = o.optInt("comment_count"),
            appLeaveCount = o.optInt("app_leave_count"),
            offTaskSeconds = o.optInt("off_task_seconds"),
            subjectSegmentsJSON = opt("subject_segments_json"),
            pigeonOrigin = opt("pigeon_origin"),
            pigeonDestination = opt("pigeon_destination"),
            descriptionRarity = opt("description_rarity"),
        )
        t.likedByUIDs = o.optJSONArray("liked_by_uids")?.let { 0.until(it.length()).map { i -> it.getString(i) } }?.toMutableList() ?: mutableListOf()
        return t
    }

    suspend fun saveTimelapse(t: StudyTimelapse) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/timelapses", timelapseToRow(t),
            listOf("Prefer" to "resolution=merge-duplicates,return=minimal"))
    }

    suspend fun deleteTimelapse(id: String) = withContext(Dispatchers.IO) {
        requestChecked("DELETE", "/rest/v1/timelapses", query = "?id=eq." + urlEncode(id))
    }

    private val tlFields = "id,author_id,author_name,author_avatar_url,caption,study_description,subject,duration_seconds,created_at,is_landscape,like_count,liked_by_uids,comment_count,app_leave_count,off_task_seconds,subject_segments_json,pigeon_origin,pigeon_destination,description_rarity"

    suspend fun fetchTimelapsesFor(uid: String): List<StudyTimelapse> = withContext(Dispatchers.IO) {
        val q = ("?select=" + urlEncode(tlFields) + "&author_id=eq." + urlEncode(normalizeUUID(uid))
            + "&order=created_at.desc&limit=500")
        val arr = JSONArray(requestChecked("GET", "/rest/v1/timelapses", query = q))
        0.until(arr.length()).map { rowToTimelapse(arr.getJSONObject(it)) }
    }

    suspend fun fetchFeed(authorUids: List<String>): List<StudyTimelapse> = withContext(Dispatchers.IO) {
        if (authorUids.isEmpty()) return@withContext emptyList()
        val inList = authorUids.joinToString(",") { normalizeUUID(it) }
        val q = ("?select=" + urlEncode(tlFields) + "&author_id=in.(" + inList + ")"
            + "&order=created_at.desc&limit=200")
        val arr = JSONArray(requestChecked("GET", "/rest/v1/timelapses", query = q))
        0.until(arr.length()).map { rowToTimelapse(arr.getJSONObject(it)) }
    }

    // MARK: - Groups

    suspend fun createGroup(g: StudyGroup) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/study_groups",
            JSONObject().put("id", g.id).put("name", g.name).put("description", g.groupDescription)
                .put("creator_uid", normalizeUUID(g.creatorID))
                .put("member_uids", JSONArray(g.memberIDs.map { normalizeUUID(it) }))
                .put("icon_name", g.iconName).put("created_at", isoNow()),
            listOf("Prefer" to "return=minimal"))
    }

    suspend fun fetchGroups(uid: String): List<StudyGroup> = withContext(Dispatchers.IO) {
        val q = ("?select=id,name,description,creator_uid,member_uids,icon_name,created_at&member_uids=cs.{" + urlEncode(normalizeUUID(uid)) + "}")
        val arr = JSONArray(requestChecked("GET", "/rest/v1/study_groups", query = q))
        0.until(arr.length()).map {
            val o = arr.getJSONObject(it)
            StudyGroup(
                id = o.getString("id"), name = o.getString("name"),
                groupDescription = o.optString("description", ""),
                creatorID = o.optString("creator_uid"), createdAt = parseIso(o.optString("created_at")) ?: 0L,
                iconName = o.optString("icon_name", "book"),
            ).apply {
                memberIDs = o.optJSONArray("member_uids")?.let { j -> 0.until(j.length()).map { i -> j.getString(i) } }?.toMutableList() ?: mutableListOf()
            }
        }
    }

    suspend fun inviteToGroup(groupId: String, inviter: String, invitee: String) = withContext(Dispatchers.IO) {
        requestChecked("POST", "/rest/v1/group_invites",
            JSONObject().put("id", UUID.randomUUID().toString()).put("group_id", normalizeUUID(groupId))
                .put("inviter_uid", normalizeUUID(inviter)).put("invitee_uid", normalizeUUID(invitee))
                .put("status", "pending").put("created_at", isoNow()),
            listOf("Prefer" to "return=minimal"))
    }

    suspend fun fetchGroupInvites(uid: String): JSONArray = withContext(Dispatchers.IO) {
        val q = ("?select=id,group_id,inviter_uid,invitee_uid,status,created_at&invitee_uid=eq."
            + urlEncode(normalizeUUID(uid)) + "&status=eq.pending")
        JSONArray(requestChecked("GET", "/rest/v1/group_invites", query = q))
    }

    suspend fun deleteGroup(groupId: String) = withContext(Dispatchers.IO) {
        requestChecked("DELETE", "/rest/v1/study_groups", query = "?id=eq." + urlEncode(groupId))
    }

    fun normalizeUUID(s: String): String = try {
        UUID.fromString(s.trim()).toString()
    } catch (_: Exception) { s }

    fun urlEncode(s: String): String = URLEncoder.encode(s, "UTF-8")
}
