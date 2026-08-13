package com.notify.core.di

import android.app.Application
import android.content.Context
import com.notify.core.data.InstancesRepository
import com.notify.core.data.NotifyApi
import com.notify.core.data.OfflineDownloadManager
import com.notify.core.data.OfflineStore
import com.notify.core.data.SessionManager
import com.notify.core.player.PlayerEngine

/** Lightweight manual DI: a single container held by the Application. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val instancesRepository = InstancesRepository(appContext)
    val session = SessionManager(instancesRepository)
    val api = NotifyApi(session)
    val offlineStore = OfflineStore(appContext)
    val offlineDownloads = OfflineDownloadManager(appContext, api, session, offlineStore)

    /** Single shared player engine, owned by the Application process (not the
     *  service or the UI). [PlayerService] wraps it in a MediaSession so playback
     *  continues in the background; the engine must never be released while the
     *  process is alive or the whole app's playback breaks. */
    val playerEngine: PlayerEngine by lazy {
        PlayerEngine(appContext, api, session, offlineStore)
    }
}

class NotifyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
