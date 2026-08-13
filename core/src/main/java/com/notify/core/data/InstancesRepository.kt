package com.notify.core.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "notify_instances")

@Serializable
data class Instance(
    val id: String,
    val name: String,
    val baseUrl: String,
    val token: String? = null,
    val username: String? = null,
    val preferredFormat: String? = null
)

/** Persists self-hosted instances and the active one. Tokens are kept per
 *  instance so switching instances swaps accounts too. */
class InstancesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val instances = stringPreferencesKey("instances")
        val active = stringPreferencesKey("active_instance")
    }

    val instances: Flow<List<Instance>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.instances] ?: "[]"
        runCatching { json.decodeFromString<List<Instance>>(raw) }.getOrDefault(emptyList())
    }

    val activeInstance: Flow<Instance?> = context.dataStore.data.map { prefs ->
        val list = runCatching {
            json.decodeFromString<List<Instance>>(prefs[Keys.instances] ?: "[]")
        }.getOrDefault(emptyList())
        val activeId = prefs[Keys.active]
        list.firstOrNull { it.id == activeId } ?: list.firstOrNull()
    }

    suspend fun current(): Instance? = activeInstance.first()

    suspend fun all(): List<Instance> = instances.first()

    suspend fun upsert(instance: Instance) {
        context.dataStore.edit { prefs ->
            val list = runCatching {
                json.decodeFromString<List<Instance>>(prefs[Keys.instances] ?: "[]")
            }.getOrDefault(emptyList())
            val next = list.filter { it.id != instance.id } + instance
            prefs[Keys.instances] = json.encodeToString(next)
            if (prefs[Keys.active] == null) prefs[Keys.active] = instance.id
        }
    }

    suspend fun setActive(id: String) {
        context.dataStore.edit { prefs -> prefs[Keys.active] = id }
    }

    suspend fun remove(id: String) {
        context.dataStore.edit { prefs ->
            val list = runCatching {
                json.decodeFromString<List<Instance>>(prefs[Keys.instances] ?: "[]")
            }.getOrDefault(emptyList())
            val next = list.filter { it.id != id }
            prefs[Keys.instances] = json.encodeToString(next)
            if (prefs[Keys.active] == id) {
                prefs[Keys.active] = next.firstOrNull()?.id ?: ""
            }
        }
    }

    suspend fun updateInstance(id: String, transform: (Instance) -> Instance) {
        context.dataStore.edit { prefs ->
            val list = runCatching {
                json.decodeFromString<List<Instance>>(prefs[Keys.instances] ?: "[]")
            }.getOrDefault(emptyList())
            val next = list.map { if (it.id == id) transform(it) else it }
            prefs[Keys.instances] = json.encodeToString(next)
        }
    }
}
