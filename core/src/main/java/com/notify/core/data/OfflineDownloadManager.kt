package com.notify.core.data

import android.content.Context
import com.notify.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ActiveDownload(
    val trackId: String,
    val progress: Float,
    val state: DownloadState
)

enum class DownloadState { DOWNLOADING, DONE, FAILED }

/** Downloads tracks (in the user's preferred format) to local storage so
 *  they can be played offline. Files land in context.getDir("offline"),
 *  the same directory the OfflineStore indexes. */
class OfflineDownloadManager(
    context: Context,
    private val api: NotifyApi,
    private val session: SessionManager,
    private val offlineStore: OfflineStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

    private val appContext = context.applicationContext

    private val _downloads = MutableStateFlow<Map<String, ActiveDownload>>(emptyMap())
    val downloads: StateFlow<Map<String, ActiveDownload>> = _downloads.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    /** Queue every track in a collection for download (idempotent: already
     *  downloaded or in-flight tracks are skipped). */
    fun downloadCollection(tracks: List<Track>, collection: OfflineCollection) {
        tracks.forEach { track ->
            scope.launch { startDownload(track, collection) }
        }
    }

    suspend fun startDownload(track: Track, collection: OfflineCollection? = null) {
        val instanceId = session.currentInstance?.id ?: return
        if (_downloads.value.containsKey(track.id)) return
        if (offlineStore.isDownloaded(instanceId, track.id)) return

        _downloads.value = _downloads.value + (track.id to ActiveDownload(track.id, 0f, DownloadState.DOWNLOADING))

        val job = scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val instDir = java.io.File(
                        appContext.getDir("offline", Context.MODE_PRIVATE),
                        instanceId
                    ).apply { mkdirs() }
                    val tmp = java.io.File(instDir, "${track.id}.part")
                    if (tmp.exists()) tmp.delete()
                    val contentType = api.downloadTrackToFile(track.id, tmp) { written, total ->
                        val p = if (total > 0) written.toFloat() / total.toFloat() else 0.5f
                        _downloads.value = _downloads.value + (track.id to ActiveDownload(track.id, p, DownloadState.DOWNLOADING))
                    }
                    val ext = extensionFor(contentType)
                    val final = java.io.File(instDir, "${track.id}.$ext")
                    if (final.exists()) final.delete()
                    tmp.renameTo(final)

                    offlineStore.add(
                        OfflineTrack(
                            trackId = track.id,
                            title = track.title,
                            artist = track.displayArtist,
                            album = track.displayAlbum,
                            artUrl = track.displayArt,
                            duration = track.duration,
                            filePath = final.absolutePath,
                            instanceId = instanceId,
                            format = session.preferredFormat,
                            collection = collection
                        )
                    )
                }
                _downloads.value = _downloads.value + (track.id to ActiveDownload(track.id, 1f, DownloadState.DONE))
            } catch (e: Exception) {
                _downloads.value = _downloads.value + (track.id to ActiveDownload(track.id, 0f, DownloadState.FAILED))
            }
        }
        jobs[track.id] = job
    }

    fun cancelDownload(trackId: String) {
        jobs.remove(trackId)?.cancel()
        _downloads.value = _downloads.value - trackId
    }

    suspend fun removeDownload(instanceId: String, trackId: String) {
        jobs.remove(trackId)?.cancel()
        offlineStore.remove(instanceId, trackId)
        _downloads.value = _downloads.value - trackId
    }

    suspend fun removeCollection(collection: OfflineCollection) {
        offlineStore.tracksForCollection(collection).forEach { ot ->
            jobs.remove(ot.trackId)?.cancel()
            _downloads.value = _downloads.value - ot.trackId
        }
        offlineStore.removeCollection(collection)
    }

    fun activeDownload(trackId: String): ActiveDownload? = _downloads.value[trackId]

    private fun extensionFor(contentType: String): String = when {
        contentType.contains("ogg") || contentType.contains("opus") -> "ogg"
        contentType.contains("flac") -> "flac"
        contentType.contains("mpeg") -> "mp3"
        contentType.contains("mp4") || contentType.contains("m4a") || contentType.contains("aac") -> "m4a"
        contentType.contains("webm") -> "webm"
        contentType.contains("wav") -> "wav"
        else -> "bin"
    }
}
