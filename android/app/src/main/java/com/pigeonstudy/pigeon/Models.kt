package com.pigeonstudy.pigeon

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SubjectSegment(val subject: String, val seconds: Int) {
    fun toJson(): JSONObject = JSONObject().put("subject", subject).put("seconds", seconds)
    companion object {
        fun fromJson(o: JSONObject) = SubjectSegment(o.getString("subject"), o.getInt("seconds"))
    }
}

data class StudySubject(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var colorHex: String,
    var isUserCreated: Boolean = false,
    var sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("colorHex", colorHex)
        .put("isUserCreated", isUserCreated).put("sortOrder", sortOrder).put("createdAt", createdAt)

    companion object {
        val defaults = listOf(
            "Math" to "#3478F6", "English" to "#FF3B30", "Science" to "#34C759",
            "History" to "#A2845E", "Computer Science" to "#AF52DE", "Literature" to "#FF9500",
            "Physics" to "#5AC8FA", "Chemistry" to "#A8CC3C", "Economics" to "#FF2D55",
            "Psychology" to "#BF5AF2", "Biology" to "#00C7BE", "Spanish" to "#FFCC00",
            "Art" to "#64D2FF", "Geography" to "#8E8E93",
        )
        fun fromJson(o: JSONObject) = StudySubject(
            id = o.getString("id"), name = o.getString("name"), colorHex = o.getString("colorHex"),
            isUserCreated = o.optBoolean("isUserCreated"), sortOrder = o.optInt("sortOrder"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        )
        fun seedDefaults() = defaults.mapIndexed { i, (n, c) -> StudySubject(name = n, colorHex = c, sortOrder = i) }
    }
}

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    var firebaseUID: String? = null,
    var displayName: String,
    var username: String? = null,
    var avatarURL: String? = null,
    var bio: String = "",
    val joinDate: Long = System.currentTimeMillis(),
    var totalStudyMinutes: Int = 0,
    var streakDays: Int = 0,
    var friendUIDs: MutableList<String> = mutableListOf(),
    var pendingFriendRequestUIDs: MutableList<String> = mutableListOf(),
    var groupIDs: MutableList<String> = mutableListOf(),
    var isPrivate: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("firebaseUID", firebaseUID ?: JSONObject.NULL)
        .put("displayName", displayName).put("username", username ?: JSONObject.NULL)
        .put("avatarURL", avatarURL ?: JSONObject.NULL).put("bio", bio)
        .put("joinDate", joinDate).put("totalStudyMinutes", totalStudyMinutes)
        .put("streakDays", streakDays).put("friendUIDs", JSONArray(friendUIDs))
        .put("pendingFriendRequestUIDs", JSONArray(pendingFriendRequestUIDs))
        .put("groupIDs", JSONArray(groupIDs)).put("isPrivate", isPrivate)

    companion object {
        fun fromJson(o: JSONObject) = UserProfile(
            id = o.getString("id"),
            firebaseUID = o.optString("firebaseUID").takeIf { it.isNotEmpty() && it != "null" },
            displayName = o.getString("displayName"),
            username = o.optString("username").takeIf { it.isNotEmpty() && it != "null" },
            avatarURL = o.optString("avatarURL").takeIf { it.isNotEmpty() && it != "null" },
            bio = o.optString("bio", ""),
            joinDate = o.optLong("joinDate", System.currentTimeMillis()),
            totalStudyMinutes = o.optInt("totalStudyMinutes"),
            streakDays = o.optInt("streakDays"),
            isPrivate = o.optBoolean("isPrivate", true),
        ).apply {
            friendUIDs = o.optJSONArray("friendUIDs")?.let { 0.until(it.length()).map { i -> it.getString(i) } }?.toMutableList() ?: mutableListOf()
            pendingFriendRequestUIDs = o.optJSONArray("pendingFriendRequestUIDs")?.let { 0.until(it.length()).map { i -> it.getString(i) } }?.toMutableList() ?: mutableListOf()
            groupIDs = o.optJSONArray("groupIDs")?.let { 0.until(it.length()).map { i -> it.getString(i) } }?.toMutableList() ?: mutableListOf()
        }
    }
}

data class StudyTimelapse(
    val id: String = UUID.randomUUID().toString(),
    var authorID: String,
    var authorName: String,
    var authorAvatarURL: String? = null,
    var caption: String,
    var studyDescription: String,
    var subject: String,
    var durationSeconds: Int,
    var createdAt: Long = System.currentTimeMillis(),
    var videoFileName: String? = null,
    var thumbnailPath: String? = null,
    var afterPhotoPath: String? = null,
    var frameDir: String? = null,
    var isLandscape: Boolean = false,
    var likeCount: Int = 0,
    var likedByUIDs: MutableList<String> = mutableListOf(),
    var commentCount: Int = 0,
    var appLeaveCount: Int = 0,
    var offTaskSeconds: Int = 0,
    var subjectSegmentsJSON: String? = null,
    var cloudSynced: Boolean = false,
    var syncRetryCount: Int = 0,
    var lastSyncAttempt: Long? = null,
    var pigeonOrigin: String? = null,
    var pigeonDestination: String? = null,
    var descriptionRarity: String? = null,
) {
    val subjectSegments: List<SubjectSegment>
        get() {
            val json = subjectSegmentsJSON ?: return listOf(SubjectSegment(subject, durationSeconds))
            return try {
                val arr = JSONArray(json)
                if (arr.length() == 0) listOf(SubjectSegment(subject, durationSeconds))
                else 0.until(arr.length()).map { SubjectSegment.fromJson(arr.getJSONObject(it)) }
            } catch (_: Exception) { listOf(SubjectSegment(subject, durationSeconds)) }
        }

    val hasMultipleSubjects: Boolean get() = subjectSegments.size > 1

    val formattedDuration: String
        get() {
            val h = durationSeconds / 3600; val m = (durationSeconds % 3600) / 60; val s = durationSeconds % 60
            return when {
                h > 0 -> "${h}h ${m}m ${s}s"
                m > 0 -> "${m}m ${s}s"
                else -> "${s}s"
            }
        }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("authorID", authorID).put("authorName", authorName)
        .put("authorAvatarURL", authorAvatarURL ?: JSONObject.NULL)
        .put("caption", caption).put("studyDescription", studyDescription)
        .put("subject", subject).put("durationSeconds", durationSeconds)
        .put("createdAt", createdAt)
        .put("videoFileName", videoFileName ?: JSONObject.NULL)
        .put("thumbnailPath", thumbnailPath ?: JSONObject.NULL)
        .put("afterPhotoPath", afterPhotoPath ?: JSONObject.NULL)
        .put("frameDir", frameDir ?: JSONObject.NULL)
        .put("isLandscape", isLandscape).put("likeCount", likeCount)
        .put("likedByUIDs", JSONArray(likedByUIDs)).put("commentCount", commentCount)
        .put("appLeaveCount", appLeaveCount).put("offTaskSeconds", offTaskSeconds)
        .put("subjectSegmentsJSON", subjectSegmentsJSON ?: JSONObject.NULL)
        .put("cloudSynced", cloudSynced).put("syncRetryCount", syncRetryCount)
        .put("lastSyncAttempt", lastSyncAttempt ?: JSONObject.NULL)
        .put("pigeonOrigin", pigeonOrigin ?: JSONObject.NULL)
        .put("pigeonDestination", pigeonDestination ?: JSONObject.NULL)
        .put("descriptionRarity", descriptionRarity ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject): StudyTimelapse {
            fun optStr(k: String) = o.optString(k).takeIf { it.isNotEmpty() && it != "null" }
            return StudyTimelapse(
                id = o.getString("id"), authorID = o.getString("authorID"),
                authorName = o.getString("authorName"), authorAvatarURL = optStr("authorAvatarURL"),
                caption = o.getString("caption"), studyDescription = o.optString("studyDescription", ""),
                subject = o.optString("subject", "General"),
                durationSeconds = o.optInt("durationSeconds"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                videoFileName = optStr("videoFileName"), thumbnailPath = optStr("thumbnailPath"),
                afterPhotoPath = optStr("afterPhotoPath"), frameDir = optStr("frameDir"),
                isLandscape = o.optBoolean("isLandscape"),
                likeCount = o.optInt("likeCount"), commentCount = o.optInt("commentCount"),
                appLeaveCount = o.optInt("appLeaveCount"), offTaskSeconds = o.optInt("offTaskSeconds"),
                subjectSegmentsJSON = optStr("subjectSegmentsJSON"),
                cloudSynced = o.optBoolean("cloudSynced"),
                syncRetryCount = o.optInt("syncRetryCount"),
                lastSyncAttempt = if (o.has("lastSyncAttempt") && !o.isNull("lastSyncAttempt")) o.optLong("lastSyncAttempt") else null,
                pigeonOrigin = optStr("pigeonOrigin"), pigeonDestination = optStr("pigeonDestination"),
                descriptionRarity = optStr("descriptionRarity"),
            ).apply {
                likedByUIDs = o.optJSONArray("likedByUIDs")?.let { 0.until(it.length()).map { i -> it.getString(i) } }?.toMutableList() ?: mutableListOf()
            }
        }
    }
}

data class StudyGroup(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var groupDescription: String = "",
    var creatorID: String,
    var memberIDs: MutableList<String> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var iconName: String = "book",
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("groupDescription", groupDescription)
        .put("creatorID", creatorID).put("memberIDs", JSONArray(memberIDs))
        .put("createdAt", createdAt).put("iconName", iconName)

    companion object {
        fun fromJson(o: JSONObject) = StudyGroup(
            id = o.getString("id"), name = o.getString("name"),
            groupDescription = o.optString("groupDescription", ""),
            creatorID = o.getString("creatorID"), createdAt = o.optLong("createdAt"),
            iconName = o.optString("iconName", "book"),
        ).apply {
            memberIDs = o.optJSONArray("memberIDs")?.let { 0.until(it.length()).map { i -> it.getString(i) } }?.toMutableList() ?: mutableListOf()
        }
    }
}

enum class PunRarity(val displayName: String) {
    COMMON("common"), UNCOMMON("uncommon"), RARE("rare"), EPIC("epic"), LEGENDARY("legendary"), MYTHIC("mythic");

    val feedBoostSeconds: Long get() = when (this) {
        COMMON -> 0; UNCOMMON -> 30 * 60; RARE -> 60 * 60
        EPIC -> 2 * 60 * 60; LEGENDARY -> 4 * 60 * 60; MYTHIC -> 8 * 60 * 60
    }

    companion object {
        fun roll(): PunRarity {
            val v = Math.random() * 100
            return when {
                v < 60 -> COMMON; v < 85 -> UNCOMMON; v < 95 -> RARE
                v < 98.5 -> EPIC; v < 99.5 -> LEGENDARY; else -> MYTHIC
            }
        }
        fun fromRaw(s: String?) = entries.firstOrNull { it.displayName == s }
    }
}

object PunLibrary {
    fun rollPun(rarity: PunRarity? = null): Puns.TaggedPun {
        val tier = rarity ?: PunRarity.roll()
        val pool = Puns.all.filter { it.rarity == tier.displayName }
        return if (pool.isEmpty()) Puns.TaggedPun("just winging it", "common") else pool.random()
    }
}

object PendingDeleteStore {
    private val key = "pigeon_pending_deletes"
    private val cached = mutableSetOf<String>()

    fun ids(prefs: android.content.SharedPreferences): Set<String> {
        if (cached.isEmpty()) cached.addAll(prefs.getStringSet(key, emptySet()) ?: emptySet())
        return cached
    }

    fun add(prefs: android.content.SharedPreferences, id: String) {
        cached.add(id)
        prefs.edit().putStringSet(key, cached.toSet()).apply()
    }

    fun remove(prefs: android.content.SharedPreferences, id: String) {
        cached.remove(id)
        prefs.edit().putStringSet(key, cached.toSet()).apply()
    }
}

object MediaStore {
    fun frameFiles(dir: File): List<File> =
        dir.listFiles { f -> f.extension == "jpg" }?.sortedBy { it.name } ?: emptyList()
}
