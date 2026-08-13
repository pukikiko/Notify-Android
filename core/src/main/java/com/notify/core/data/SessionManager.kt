package com.notify.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import com.notify.core.model.User
import com.notify.core.model.UserSettings

/** Holds the currently selected instance and the logged-in account.
 *  Both the API client and the media data sources read from here. */
class SessionManager(private val repo: InstancesRepository) {

    val instances: Flow<List<Instance>> = repo.instances
    val activeInstance: Flow<Instance?> = repo.activeInstance

    val currentInstance: Instance?
        get() = runBlocking { repo.current() }

    val baseUrl: String
        get() = currentInstance?.baseUrl?.trimEnd('/') ?: ""

    val token: String?
        get() = currentInstance?.token

    val username: String?
        get() = currentInstance?.username

    val preferredFormat: String
        get() = currentInstance?.preferredFormat ?: "opus-160"

    fun hasSession(): Boolean = token != null

    fun isConfigured(): Boolean = baseUrl.isNotEmpty()

    fun imageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file://")) {
            path
        } else {
            baseUrl + path
        }
    }

    suspend fun setActive(id: String) {
        repo.setActive(id)
    }

    suspend fun update(transform: (Instance) -> Instance) {
        val current = repo.current() ?: return
        repo.updateInstance(current.id, transform)
    }

    suspend fun logout() {
        val current = repo.current() ?: return
        repo.updateInstance(current.id) { it.copy(token = null, username = null) }
    }
}
