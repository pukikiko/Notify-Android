package com.notify.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.offlineDataStore by preferencesDataStore(name = "offline_index")

@Serializable
data class OfflineCollection(
    val kind: String,
    val id: String,
    val name: String
) {
    val key: String get() = "$kind:$id"
}

@Serializable
data class OfflineTrack(
    val trackId: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val artUrl: String? = null,
    val duration: Double? = null,
    val filePath: String,
    val instanceId: String,
    val format: String? = null,
    val collection: OfflineCollection? = null,
    val downloadedAt: Long = System.currentTimeMillis()
)

/** Manages locally downloaded tracks for offline listening. Audio files live
 *  under filesDir/offline/<instanceId>/, metadata in a JSON index. */
class OfflineStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val index = stringPreferencesKey("offline_index")
    }

    private fun offlineDir(instanceId: String): java.io.File =
        java.io.File(context.getDir("offline", Context.MODE_PRIVATE), instanceId).apply { mkdirs() }

    val offlineTracks: Flow<List<OfflineTrack>> = context.offlineDataStore.data.map { prefs ->
        runCatching { json.decodeFromString<List<OfflineTrack>>(prefs[Keys.index] ?: "[]") }
            .getOrDefault(emptyList())
            .sortedByDescending { it.downloadedAt }
    }

    suspend fun all(): List<OfflineTrack> = offlineTracks.first()

    suspend fun tracksFor(instanceId: String): List<OfflineTrack> =
        all().filter { it.instanceId == instanceId }

    suspend fun isDownloaded(instanceId: String, trackId: String): Boolean =
        all().any { it.instanceId == instanceId && it.trackId == trackId }

    suspend fun tracksForCollection(collection: OfflineCollection): List<OfflineTrack> =
        all().filter { it.collection?.key == collection.key }

    /** True if the local file for this track still exists on disk. */
    suspend fun isPlayable(instanceId: String, trackId: String): Boolean {
        val track = all().firstOrNull { it.instanceId == instanceId && it.trackId == trackId } ?: return false
        return java.io.File(track.filePath).exists()
    }

    suspend fun localFileFor(instanceId: String, trackId: String): java.io.File? {
        val track = all().firstOrNull { it.instanceId == instanceId && it.trackId == trackId } ?: return null
        val f = java.io.File(track.filePath)
        return if (f.exists()) f else null
    }

    suspend fun add(track: OfflineTrack) {
        context.offlineDataStore.edit { prefs ->
            val list = runCatching {
                json.decodeFromString<List<OfflineTrack>>(prefs[Keys.index] ?: "[]")
            }.getOrDefault(emptyList())
            val next = list.filter { !(it.instanceId == track.instanceId && it.trackId == track.trackId) } + track
            prefs[Keys.index] = json.encodeToString(next)
        }
    }

    suspend fun remove(instanceId: String, trackId: String) {
        val existing = all().firstOrNull { it.instanceId == instanceId && it.trackId == trackId }
        existing?.let { runCatching { java.io.File(it.filePath).delete() } }
        context.offlineDataStore.edit { prefs ->
            val list = runCatching {
                json.decodeFromString<List<OfflineTrack>>(prefs[Keys.index] ?: "[]")
            }.getOrDefault(emptyList())
            val next = list.filterNot { it.instanceId == instanceId && it.trackId == trackId }
            prefs[Keys.index] = json.encodeToString(next)
        }
    }

    suspend fun removeAllForInstance(instanceId: String) {
        val list = all().filter { it.instanceId == instanceId }
        list.forEach { runCatching { java.io.File(it.filePath).delete() } }
        context.offlineDataStore.edit { prefs ->
            val all = runCatching {
                json.decodeFromString<List<OfflineTrack>>(prefs[Keys.index] ?: "[]")
            }.getOrDefault(emptyList())
            prefs[Keys.index] = json.encodeToString(all.filterNot { it.instanceId == instanceId })
        }
    }

    suspend fun removeCollection(collection: OfflineCollection) {
        val list = all().filter { it.collection?.key == collection.key }
        list.forEach { runCatching { java.io.File(it.filePath).delete() } }
        context.offlineDataStore.edit { prefs ->
            val all = runCatching {
                json.decodeFromString<List<OfflineTrack>>(prefs[Keys.index] ?: "[]")
            }.getOrDefault(emptyList())
            prefs[Keys.index] = json.encodeToString(all.filterNot { it.collection?.key == collection.key })
        }
    }

    fun diskUsageBytes(instanceId: String): Long {
        val dir = offlineDir(instanceId)
        if (!dir.exists()) return 0
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
