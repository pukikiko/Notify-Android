package com.notify.android.mobile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.notify.android.mobile.ui.auth.AuthScreen
import com.notify.core.ui.auth.RootState
import com.notify.core.ui.auth.RootViewModel
import com.notify.android.mobile.ui.main.MainScreen
import com.notify.core.ui.player.PlayerViewModel
import com.notify.core.ui.playerViewModel
import com.notify.core.ui.rootViewModel

@Composable
fun AppRoot() {
    val rootVm = rootViewModel()
    val state by rootVm.state.collectAsState()

    when (val s = state) {
        is RootState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is RootState.LoggedOut -> {
            AuthScreen(rootVm)
        }
        is RootState.LoggedIn -> {
            val playerVm = playerViewModel()
            MainScreen(
                user = s.user,
                playerVm = playerVm,
                onLogout = { rootVm.logout() }
            )
        }
    }
}
