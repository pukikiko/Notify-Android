package com.notify.android.tv.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.android.tv.ui.components.TvInputButton
import com.notify.android.tv.ui.components.formatBytes
import com.notify.core.ui.settingsViewModel

@Composable
fun TvSettingsScreen(nav: TvNavState, onLogout: () -> Unit) {
    val vm = settingsViewModel()
    val status by vm.status.collectAsState()
    val counts by vm.counts.collectAsState()
    val instances by vm.instances.collectAsState()
    val activeId by vm.activeId.collectAsState()

    var format by remember { mutableStateOf(vm.currentFormat) }
    var formatFocused by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text("Settings", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 40.dp, top = 36.dp))
        }

        item {
            SettingBlock("Account", "${vm.currentUser} @ ${vm.currentBaseUrl}") {
                Row {
                    SettingChip("${counts?.likedTracks ?: 0} songs")
                    Spacer(Modifier.width(10.dp))
                    SettingChip("${counts?.likedAlbums ?: 0} albums")
                    Spacer(Modifier.width(10.dp))
                    SettingChip("${counts?.likedArtists ?: 0} artists")
                }
            }
        }

        item {
            SettingBlock("Streaming format", "Preferred codec, transcoded on the server.") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val formats = status?.formats.orEmpty()
                    if (formats.isNotEmpty()) {
                        TvInputButton(formats[format] ?: format, onClick = {
                            val keys = formats.keys.toList()
                            val next = keys[(keys.indexOf(format) + 1) % keys.size]
                            format = next
                            vm.saveFormat(next)
                        })
                    }
                }
            }
        }

        item {
            SettingBlock("Instance", "Self-hosted Notify servers") {
                instances.forEach { inst ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(inst.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(inst.baseUrl, color = Color(0xFFB3B3B3), fontSize = 14.sp)
                        }
                        Text(
                            if (inst.id == activeId) "ACTIVE" else "Switch",
                            color = if (inst.id == activeId) Color(0xFF8F5CFF) else Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier
                        )
                        if (inst.id != activeId) {
                            Spacer(Modifier.width(12.dp))
                            TvInputButton("Select", onClick = { vm.selectInstance(inst.id) })
                        }
                    }
                }
            }
        }

        item {
            SettingBlock("Music sources", "") {
                Row {
                    SettingChip("Soulseek: ${status?.soulseek?.mode ?: "…"}")
                    Spacer(Modifier.width(10.dp))
                    SettingChip("YouTube Music: ${if (status?.sources?.get("youtube")?.enabled == true) "enabled" else "disabled"}")
                    Spacer(Modifier.width(10.dp))
                    SettingChip("SoundCloud: ${if (status?.sources?.get("soundcloud")?.enabled == true) "enabled" else "disabled"}")
                }
            }
        }

        item {
            SettingBlock("Shared cache pool", "") {
                Row {
                    SettingChip("${status?.cache?.availableTracks ?: 0} tracks available")
                    Spacer(Modifier.width(10.dp))
                    SettingChip("original: ${formatBytes(status?.cache?.originalBytes ?: 0)}")
                    Spacer(Modifier.width(10.dp))
                    SettingChip("transcoded: ${formatBytes(status?.cache?.transcodedBytes ?: 0)}")
                }
            }
        }

        item {
            Row(Modifier.padding(40.dp)) {
                TvInputButton("Log out (${vm.currentUser ?: ""})", onClick = onLogout)
            }
        }
    }
}

@Composable
private fun SettingBlock(title: String, description: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, top = 24.dp, end = 40.dp)
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (description.isNotEmpty()) {
            Text(description, color = Color(0xFFB3B3B3), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingChip(text: String) {
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        color = Color(0xFF2A2A2A)
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
    }
}
