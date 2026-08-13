package com.notify.core.ui.auth

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notify.core.data.Instance
import com.notify.core.di.AppContainer
import com.notify.core.model.User
import com.notify.core.player.PlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RootState {
    data object Loading : RootState
    data object LoggedOut : RootState
    data class LoggedIn(val user: User) : RootState
}

/** App-level gate: figures out whether the active instance has a valid
 *  session and drives the auth vs. main UI switch. */
class RootViewModel(private val container: AppContainer) : AndroidViewModel(container.appContext as Application) {

    private val api = container.api
    private val session = container.session
    private val repo = container.instancesRepository

    private val _state = MutableStateFlow<RootState>(RootState.Loading)
    val state: StateFlow<RootState> = _state.asStateFlow()

    private val _instances = MutableStateFlow<List<Instance>>(emptyList())
    val instances: StateFlow<List<Instance>> = _instances.asStateFlow()

    init {
        viewModelScope.launch {
            session.instances.collect { _instances.value = it }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = RootState.Loading
            if (!session.isConfigured()) {
                _state.value = RootState.LoggedOut
                return@launch
            }
            if (!session.hasSession()) {
                _state.value = RootState.LoggedOut
                return@launch
            }
            // Optimistic login: a stored token means the user is signed in.
            // We try to validate with the server, but offline playback must
            // still work, so a network failure keeps the session.
            val optimistic = RootState.LoggedIn(
                User(id = "", username = session.username ?: "", settings = null)
            )
            runCatching { api.me() }
                .onSuccess { res ->
                    _state.value = RootState.LoggedIn(res.user)
                }
                .onFailure {
                    _state.value = optimistic
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { api.logout() }
            session.logout()
            // Stop playback explicitly: the service no longer releases the
            // shared engine player, so without this audio would keep playing
            // after the service is torn down.
            container.playerEngine.stop()
            container.appContext.stopService(Intent(container.appContext, PlayerService::class.java))
            _state.value = RootState.LoggedOut
        }
    }

    fun selectInstance(id: String) {
        viewModelScope.launch {
            session.setActive(id)
            refresh()
        }
    }
}
