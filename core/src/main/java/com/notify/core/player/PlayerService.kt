package com.notify.core.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.notify.core.di.NotifyApplication

/**
 * Foreground media session service for the shared [PlayerEngine]. Exposing the
 * player through a MediaSession gives us:
 *  - the media notification (notification shade, Android 13+ lock screen card),
 *  - transport controls from the lock screen / notification / headset,
 *  - hardware media keys and TV remote play/pause/next/prev,
 *  - playback that keeps running while the app is backgrounded.
 */
class PlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val engine = (application as NotifyApplication).container.playerEngine
        mediaSession = MediaSession.Builder(this, engine.player).build()
        // The app drives the shared engine directly and never connects a
        // MediaController, so onGetSession() is never called and Media3 would
        // never learn about this session. Register it explicitly: only sessions
        // added to the service get a media notification and get promoted to a
        // foreground service while playing. Without this the service stays a
        // plain background service and the system kills it (and playback) the
        // moment the app leaves the foreground.
        mediaSession?.let { addSession(it) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /** Keep playing when the user swipes the app away from recents. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // Release only the session, never the shared engine player. The engine
        // lives for the whole process (see AppContainer) and must survive the
        // service being torn down — logout, idle timeout, task removal while
        // paused — so playback can resume once the service is restarted.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
