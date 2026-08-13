package com.notify.core.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.notify.core.data.AuthDataSourceFactory
import com.notify.core.data.NotifyApi
import com.notify.core.data.OfflineStore
import com.notify.core.data.SessionManager
import com.notify.core.model.DiscoverPlayRequest
import com.notify.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RepeatMode { OFF, ALL, ONE }

/** How many times a single track is re-prepared before the engine gives up
 *  and skips to the next one (Spotify-style). */
private const val MAX_TRACK_RETRIES = 3
private const val RETRY_BASE_DELAY_MS = 1500L
private const val DOWNLOAD_POLL_MS = 3000L

/**
 * Spotify-style player engine. Manages a queue of tracks (real library rows
 * and discover placeholders), resolves placeholders on demand, streams with
 * the instance token, and plays from the local cache when a track has been
 * downloaded for offline use.
 *
 * A track that hasn't finished downloading is streamed by the backend in real
 * time: the server holds the HTTP connection open while the download grows and
 * feeds decodable audio through as soon as it exists. Those streams can take
 * a while to produce their first byte (a Soulseek peer may not start writing
 * for many seconds) and can stall mid-track on slow peers, so every stage of
 * the pipeline here is deliberately patient:
 *  - the media source never fails a load outright (generous retry policy),
 *  - a player error retries the *same* track with backoff before skipping,
 *  - a live stream that ends before the download completed is treated as
 *    transient and re-attempted, and
 *  - seeking into not-yet-downloaded territory is clamped to what exists.
 */
class PlayerEngine(
    context: Context,
    private val api: NotifyApi,
    private val session: SessionManager,
    private val offlineStore: OfflineStore
) {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                androidx.media3.datasource.DefaultDataSource.Factory(
                    context,
                    AuthDataSourceFactory.create(context) { session.token }
                )
            )
            .setLiveTargetOffsetMs(0)
            .setLoadErrorHandlingPolicy(PatientLoadErrorHandlingPolicy())
        )
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var resolutionJob: Job? = null
    private var tickerJob: Job? = null
    private var pollJob: Job? = null
    private var retryJob: Job? = null
    private var currentResolving = false
    private var retryCount = 0

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _index = MutableStateFlow(-1)
    val index: StateFlow<Int> = _index.asStateFlow()

    private val _current = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _preparing = MutableStateFlow(false)
    val preparing: StateFlow<Boolean> = _preparing.asStateFlow()

    private val _buffering = MutableStateFlow(false)
    val buffering: StateFlow<Boolean> = _buffering.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeat = MutableStateFlow(RepeatMode.OFF)
    val repeat: StateFlow<RepeatMode> = _repeat.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _buffering.value = false
                        resetRetries()
                    }
                    Player.STATE_BUFFERING -> {
                        _buffering.value = true
                    }
                    Player.STATE_ENDED -> {
                        _buffering.value = false
                        onTrackEnded()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playing.value = isPlaying
                if (isPlaying) _buffering.value = false
            }

            override fun onPlayerError(error: PlaybackException) {
                _buffering.value = false
                // A track that can't stream is skipped Spotify-style, but only
                // after a few patient re-attempts: for tracks that are still
                // downloading, "can't stream right now" is usually just "the
                // download hasn't produced audio yet" and a retry succeeds.
                retryCurrentTrack("player error (${error.errorCodeName})")
            }
        })
        startTicker()
        startDownloadPoller()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                if (player.duration > 0) _duration.value = player.duration
                _position.value = player.currentPosition.coerceAtLeast(0)
                delay(250)
            }
        }
    }

    /** Keep the queue row fresh while a track is still downloading: once the
     *  server flips it to 'available', the row (and Now Playing metadata)
     *  reflects the completed cache, mirroring the web client. */
    private fun startDownloadPoller() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                delay(DOWNLOAD_POLL_MS)
                val track = _current.value ?: continue
                if (track.status != "downloading") continue
                val fresh = runCatching { api.track(track.id) }.getOrNull()?.track ?: continue
                if (_current.value?.id != track.id) continue
                if (fresh.status != track.status) {
                    updateQueueRow(fresh)
                }
            }
        }
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        if (_shuffle.value) {
            player.shuffleModeEnabled = true
        } else {
            player.shuffleModeEnabled = false
        }
    }

    fun cycleRepeat() {
        _repeat.value = when (_repeat.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        player.repeatMode = when (_repeat.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setVolume(v: Float) {
        _volume.value = v.coerceIn(0f, 1f)
        player.volume = _volume.value
    }

    fun toggle() {
        val currentTrack = _current.value ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(ms: Long) {
        val track = _current.value
        var target = ms.coerceIn(0, player.duration.coerceAtLeast(0))
        // For a still-downloading live stream, don't seek into territory the
        // server hasn't produced yet — the range request would 416 and the
        // player would skip the track.
        if (track?.status == "downloading") {
            val available = (player.duration - 3_000).coerceAtLeast(0)
            if (target > available) target = available
        }
        player.seekTo(target)
    }

    /** Start playback from a list of tracks, Spotify-style: the queue starts
     *  at the clicked track and drops everything before it. */
    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val start = startIndex.coerceIn(0, tracks.size - 1)
        val trimmed = tracks.drop(start)
        resolutionJob?.cancel()
        retryJob?.cancel()
        resetRetries()
        _queue.value = trimmed
        _index.value = 0
        _preparing.value = false
        loadCurrent(0)
    }

    fun loadIndex(idx: Int) {
        val q = _queue.value
        if (idx < 0 || idx >= q.size) return
        _index.value = idx
        _preparing.value = false
        loadCurrent(idx)
    }

    fun advance(delta: Int) {
        val q = _queue.value
        if (q.isEmpty()) return
        val i = _index.value

        if (_repeat.value == RepeatMode.ONE && delta > 0) {
            player.seekTo(0)
            player.play()
            return
        }

        var nextIdx: Int
        val n = q.size
        if (_shuffle.value && delta > 0) {
            nextIdx = (i + 1..i + n).map { it % n }.random()
        } else {
            nextIdx = i + delta
            if (nextIdx >= n) {
                if (_repeat.value == RepeatMode.ALL) nextIdx = 0
                else {
                    player.stop()
                    _playing.value = false
                    return
                }
            }
            if (nextIdx < 0) nextIdx = n - 1
        }
        loadIndex(nextIdx)
    }

    fun advanceForward() = advance(1)
    fun advanceBackward() = advance(-1)

    private fun loadCurrent(idx: Int) {
        val q = _queue.value
        if (idx >= q.size) return
        val track = q[idx]
        retryJob?.cancel()
        _current.value = track
        _buffering.value = false
        player.pause()
        player.clearMediaItems()

        val isResolved = track.isResolved
        if (isResolved) {
            scope.launch { playResolved(track) }
        } else {
            _preparing.value = true
            resolutionJob = scope.launch {
                runCatching {
                    api.discoverPlay(
                        DiscoverPlayRequest(
                            kind = "track",
                            artist = track.artist?.name,
                            album = track.album?.title,
                            title = track.title,
                            mbid = track.mbid,
                            image = track.image,
                            duration = track.duration
                        )
                    )
                }.onSuccess { result ->
                    val row = result.tracks.firstOrNull()
                    if (row != null) {
                        // replace the placeholder in the queue so the UI and
                        // subsequent next-tracks use the real library row
                        _queue.value = _queue.value.mapIndexed { index, t ->
                            if (index == idx) row else t
                        }
                        _current.value = row
                        _preparing.value = false
                        scope.launch { playResolved(row) }
                    } else {
                        _preparing.value = false
                        advance(1)
                    }
                }.onFailure {
                    _preparing.value = false
                    advance(1)
                }
            }
        }
    }

    private suspend fun playResolved(track: Track) {
        withContext(Dispatchers.Main) {
            val mediaItem = buildMediaItem(track)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    private suspend fun buildMediaItem(track: Track): MediaItem {
        val instanceId = session.currentInstance?.id ?: ""
        val local = offlineStore.localFileFor(instanceId, track.id)
        val uri = if (local != null && local.exists()) {
            Uri.fromFile(local)
        } else {
            Uri.parse(api.streamUrl(track.id))
        }
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadataFor(track))
            .build()
    }

    private fun metadataFor(track: Track): MediaMetadata {
        val artUri = session.imageUrl(track.displayArt)?.let { Uri.parse(it) }
        return MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.displayArtist)
            .setAlbumTitle(track.displayAlbum)
            .setArtworkUri(artUri)
            .build()
    }

    /* ------------------------- error handling ------------------------- */

    private fun resetRetries() {
        retryCount = 0
    }

    private fun onTrackEnded() {
        val track = _current.value ?: run { advance(1); return }
        // A live stream that is still downloading should never have ended: the
        // server keeps the connection open until the download completes. If it
        // ended anyway (e.g. the live transcode hiccuped), confirm against the
        // server and re-attempt rather than skipping to the next track.
        if (track.status == "downloading" && isPrematureEnd(track)) {
            scope.launch {
                val fresh = runCatching { api.track(track.id) }.getOrNull()?.track
                if (_current.value?.id != track.id) return@launch
                if (fresh != null && fresh.status != "downloading") {
                    updateQueueRow(fresh)
                    advance(1)
                } else {
                    retryCurrentTrack("live stream ended before download completed")
                }
            }
            return
        }
        advance(1)
    }

    /** True when playback ended far before the expected length, which for a
     *  still-downloading track means the live stream was cut short. */
    private fun isPrematureEnd(track: Track): Boolean {
        val expected = track.duration?.takeIf { it > 0 }?.times(1000) ?: return false
        val played = player.currentPosition.coerceAtLeast(0)
        val missing = expected - played
        return missing > 20_000 && played < expected * 0.8
    }

    /** Re-prepare the current track (a fresh stream request — more of the file
     *  will have downloaded by now) up to [MAX_TRACK_RETRIES] times with
     *  backoff before giving up and skipping it. */
    private fun retryCurrentTrack(reason: String) {
        val track = _current.value ?: run { advance(1); return }
        retryJob?.cancel()
        if (retryCount >= MAX_TRACK_RETRIES) {
            resetRetries()
            advance(1)
            return
        }
        retryCount++
        val wasPlayWhenReady = player.playWhenReady
        retryJob = scope.launch {
            delay(RETRY_BASE_DELAY_MS * retryCount)
            if (_current.value?.id != track.id) return@launch
            _buffering.value = true
            _preparing.value = false
            val mediaItem = buildMediaItem(track)
            player.pause()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.playWhenReady = wasPlayWhenReady
            player.prepare()
        }
    }

    /** Stop playback, clear the queue and reset all state (e.g. on logout).
     *  Unlike [release], the engine stays fully usable afterwards. */
    fun stop() {
        resolutionJob?.cancel()
        retryJob?.cancel()
        player.pause()
        player.stop()
        player.clearMediaItems()
        _queue.value = emptyList()
        _index.value = -1
        _current.value = null
        _playing.value = false
        _preparing.value = false
        _buffering.value = false
    }

    /** Replace the current track's metadata in place (e.g. after a like or
     *  when a download completes). */
    fun replaceCurrent(updated: Track) {
        updateQueueRow(updated)
    }

    private fun updateQueueRow(track: Track) {
        _current.value = track
        _queue.value = _queue.value.map { if (it.id == track.id) track else it }
    }

    /** Called when playback of the currently-loaded local file finished. */
    fun release() {
        tickerJob?.cancel()
        resolutionJob?.cancel()
        pollJob?.cancel()
        retryJob?.cancel()
        player.release()
    }
}

/**
 * A load-error policy that never gives up on a load outright. Growing live
 * streams stall (slow peers, no data yet while the download starts) and can
 * momentarily look like a parser/range failure; each error is retried with a
 * backoff so playback simply waits out the download instead of dying. The
 * engine's own retry budget (plus the user skipping manually) bounds this.
 */
private class PatientLoadErrorHandlingPolicy :
    DefaultLoadErrorHandlingPolicy(/* minimumLoadableRetryCount = */ 8) {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val errorCount = loadErrorInfo.errorCount.coerceAtLeast(1)
        return minOf(1000L * errorCount, 10_000L)
    }
}
