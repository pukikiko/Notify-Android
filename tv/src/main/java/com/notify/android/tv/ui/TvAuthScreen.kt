package com.notify.android.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.tv.theme.NotifyPurple
import com.notify.android.tv.ui.components.TvInputButton
import com.notify.android.tv.ui.components.TvPrimaryButton
import com.notify.android.tv.ui.components.TvTextField
import com.notify.android.tv.theme.SpotifyBlack
import com.notify.android.tv.theme.SpotifySurfaceHigh
import com.notify.core.ui.auth.RootViewModel
import com.notify.core.ui.authViewModel

@Composable
fun TvAuthScreen(rootVm: RootViewModel) {
    val authVm = authViewModel()
    val instances by authVm.instances.collectAsState()
    val activeId by authVm.activeId.collectAsState()
    val busy by authVm.busy.collectAsState()
    val error by authVm.error.collectAsState()
    val loggedIn by authVm.loggedIn.collectAsState()

    var mode by remember { mutableStateOf("login") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(loggedIn) {
        if (loggedIn) rootVm.refresh()
    }

    Box(Modifier.fillMaxSize().background(SpotifyBlack)) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Notify", color = NotifyPurple, fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(
                "Your private music, streamed from your own server.",
                color = Color(0xFFB3B3B3),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Instance selector
            if (instances.isNotEmpty()) {
                Column {
                    Text("Instance", color = Color(0xFFB3B3B3), fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    instances.forEach { inst ->
                        val active = inst.id == activeId
                        var focused by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .border(
                                    width = if (focused || active) 2.dp else 0.dp,
                                    color = if (active) NotifyPurple else Color.White,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .background(SpotifySurfaceHigh, RoundedCornerShape(10.dp))
                                .clickable { authVm.selectInstance(inst.id) }
                                .onFocusChanged { focused = it.hasFocus }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(inst.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(inst.baseUrl, color = Color(0xFFB3B3B3), fontSize = 13.sp)
                            }
                            Text(
                                if (inst.username != null) "Logged in" else if (active) "Selected" else "",
                                color = NotifyPurple,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            TvInputButton(text = "Add instance", onClick = { showAdd = true }, modifier = Modifier.padding(top = 16.dp))

            // Auth form
            Spacer(Modifier.height(32.dp))
            Text(
                if (mode == "login") "Log in to Notify" else "Sign up for Notify",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))

            TvTextField(value = username, onValueChange = { username = it }, label = "Username")
            Spacer(Modifier.height(16.dp))
            TvTextField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)

            error?.let {
                Text(it, color = Color(0xFFE91429), fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
            }

            TvPrimaryButton(
                text = if (mode == "login") "Log In" else "Sign Up",
                enabled = username.isNotBlank() && password.isNotBlank() && !busy,
                onClick = {
                    if (mode == "login") authVm.login(username, password) {}
                    else authVm.register(username, password) {}
                },
                modifier = Modifier.padding(top = 24.dp)
            )

            TvInputButton(
                text = if (mode == "login") "Sign up for Notify" else "Log in",
                onClick = { mode = if (mode == "login") "register" else "login"; authVm.clearError() },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }

    if (showAdd) {
        BackHandler { showAdd = false }
        var url by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        Dialog {
            Column(
                Modifier
                    .background(SpotifyBlack, RoundedCornerShape(16.dp))
                    .padding(32.dp)
                    .widthIn(max = 520.dp)
            ) {
                Text("Add instance", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                TvTextField(value = url, onValueChange = { url = it }, label = "Server URL (https://notify.mfc.pw)")
                Spacer(Modifier.height(16.dp))
                TvTextField(value = name, onValueChange = { name = it }, label = "Name (optional)")
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    TvPrimaryButton(text = "Add", enabled = url.isNotBlank(), onClick = {
                        authVm.addInstance(name, url)
                        showAdd = false
                    })
                    TvInputButton(text = "Cancel", onClick = { showAdd = false })
                }
            }
        }
    }
}

@Composable
private fun Dialog(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun TvPrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .background(if (enabled) NotifyPurple else Color(0xFF4D4D4D), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .onFocusChanged { focused = it.hasFocus }
            .padding(horizontal = 40.dp, vertical = 12.dp)
    ) {
        Text(text, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

