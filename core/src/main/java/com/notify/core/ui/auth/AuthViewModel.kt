package com.notify.core.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notify.core.data.Instance
import com.notify.core.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Login/register form + instance management (add, select, remove). */
class AuthViewModel(container: AppContainer) : AndroidViewModel(container.appContext as Application) {

    private val api = container.api
    private val session = container.session
    private val repo = container.instancesRepository

    private val _instances = MutableStateFlow<List<Instance>>(emptyList())
    val instances: StateFlow<List<Instance>> = _instances.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    init {
        viewModelScope.launch {
            session.instances.collect {
                _instances.value = it
                _activeId.value = session.currentInstance?.id
            }
            _loggedIn.value = session.hasSession()
        }
    }

    fun clearError() { _error.value = null }

    fun addInstance(name: String, baseUrl: String) {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val id = "inst_${System.currentTimeMillis()}"
            repo.upsert(Instance(id = id, name = name.ifBlank { trimmed }, baseUrl = trimmed))
            session.setActive(id)
            _activeId.value = id
        }
    }

    fun removeInstance(id: String) {
        viewModelScope.launch {
            repo.remove(id)
            val current = session.currentInstance
            _activeId.value = current?.id
        }
    }

    fun selectInstance(id: String) {
        viewModelScope.launch {
            session.setActive(id)
            _activeId.value = id
            _loggedIn.value = session.hasSession()
        }
    }

    fun login(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val res = api.login(username, password)
                session.update { it.copy(token = res.token, username = res.user.username, preferredFormat = res.user.settings?.preferredFormat ?: it.preferredFormat) }
                _loggedIn.value = true
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
            } finally {
                _busy.value = false
            }
        }
    }

    fun register(username: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            try {
                val res = api.register(username, password)
                session.update { it.copy(token = res.token, username = res.user.username, preferredFormat = res.user.settings?.preferredFormat ?: it.preferredFormat) }
                _loggedIn.value = true
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
            } finally {
                _busy.value = false
            }
        }
    }
}
