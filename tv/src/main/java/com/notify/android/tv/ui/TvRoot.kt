package com.notify.android.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.notify.android.tv.theme.SpotifyBlack
import com.notify.core.ui.auth.RootState
import com.notify.core.ui.auth.RootViewModel
import com.notify.core.ui.playerViewModel
import com.notify.core.ui.rootViewModel

@Composable
fun TvRoot() {
    val rootVm = rootViewModel()
    val state by rootVm.state.collectAsState()

    when (val s = state) {
        is RootState.Loading -> {
            Box(Modifier.fillMaxSize().background(SpotifyBlack), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = androidx.compose.ui.graphics.Color(0xFF2A2A2A))
            }
        }
        is RootState.LoggedOut -> {
            TvAuthScreen(rootVm)
        }
        is RootState.LoggedIn -> {
            val playerVm = playerViewModel()
            TvMainScreen(
                playerVm = playerVm,
                onLogout = { rootVm.logout() }
            )
        }
    }
}
