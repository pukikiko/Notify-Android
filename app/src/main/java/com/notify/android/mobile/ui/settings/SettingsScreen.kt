package com.notify.android.mobile.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notify.android.mobile.ui.components.formatBytes
import com.notify.core.ui.settingsViewModel
import com.notify.core.data.Instance
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenOffline: () -> Unit,
    onLogout: () -> Unit,
) {
    val vm = settingsViewModel()
    val status by vm.status.collectAsState()
    val counts by vm.counts.collectAsState()
    val instances by vm.instances.collectAsState()
    val activeId by vm.activeId.collectAsState()
    val offlineCount by vm.offlineCount.collectAsState()
    val saved by vm.saved.collectAsState()

    var format by remember { mutableStateOf(vm.currentFormat) }
    var showAddInstance by remember { mutableStateOf(false) }
    val appContainer = com.notify.core.ui.appContainer()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF1F1F1F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ---- instance ----
            SettingsBlock(title = "Instance", description = "Your self-hosted Notify server. Switch accounts by switching instances.") {
                instances.forEach { inst ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.selectInstance(inst.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(inst.name, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                if (activeId == inst.id) {
                                    Text("  ACTIVE", color = Color(0xFF8F5CFF), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(inst.baseUrl, color = Color(0xFFB3B3B3), style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (inst.username != null) "Logged in as ${inst.username}" else "Not logged in",
                                color = if (inst.username != null) Color(0xFF8F5CFF) else Color(0xFFE91429),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (instances.size > 1) {
                            IconButton(onClick = { vm.removeInstance(inst.id) }) {
                                Icon(Icons.Default.Delete, "Remove", tint = Color(0xFFB3B3B3))
                            }
                        }
                    }
                }
                TextButton(onClick = { showAddInstance = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text(" Add instance")
                }
            }

            // ---- account ----
            SettingsBlock(title = "Account", description = vm.currentUser ?: "") {
                Row(Modifier.padding(bottom = 8.dp)) {
                    StatsChip("${counts?.likedTracks ?: 0} songs")
                    Spacer(Modifier.width(6.dp))
                    StatsChip("${counts?.likedArtists ?: 0} artists")
                    Spacer(Modifier.width(6.dp))
                    StatsChip("${counts?.likedAlbums ?: 0} albums")
                }
                Row {
                    StatsChip("${counts?.playlists ?: 0} playlists")
                }
            }

            // ---- streaming format ----
            SettingsBlock(title = "Streaming format", description = "Your preferred codec. It's transcoded once on the server and cached.") {
                val formats = status?.formats.orEmpty()
                if (formats.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var expanded by remember { mutableStateOf(false) }
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(formats[format] ?: format, color = Color.White)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                formats.forEach { (key, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = {
                                        expanded = false
                                        format = key
                                        vm.saveFormat(key)
                                    })
                                }
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (saved) "✓ Saved" else "", color = Color(0xFF8F5CFF))
                    }
                }
            }

            // ---- offline ----
            SettingsBlock(title = "Offline", description = "Downloaded tracks are stored on this device.") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenOffline) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Text(" Manage downloads ($offlineCount)")
                    }
                }
            }

            // ---- music sources ----
            SettingsBlock(title = "Music sources", description = "Tracks are pulled from Soulseek first; YouTube Music and SoundCloud cover the rest.") {
                Row {
                    SourceChip(
                        label = "Soulseek: ${status?.soulseek?.mode ?: "…"}${if (status?.soulseek?.connected == true) " · connected" else ""}",
                        ok = status?.soulseek?.connected == true || status?.soulseek?.mode == "mock"
                    )
                }
                Row {
                    SourceChip("YouTube Music: ${if (status?.sources?.get("youtube")?.enabled == true) "enabled" else "disabled"}")
                    Spacer(Modifier.width(6.dp))
                    SourceChip("SoundCloud: ${if (status?.sources?.get("soundcloud")?.enabled == true) "enabled" else "disabled"}")
                }
                TextButton(onClick = { vm.refreshStatus() }) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Text(" Refresh status")
                }
            }

            // ---- shared cache ----
            SettingsBlock(title = "Shared cache pool", description = "Music from any source lives in one server-side pool shared by all users.") {
                val cache = status?.cache
                Row {
                    StatsChip("${cache?.availableTracks ?: 0} tracks available")
                    Spacer(Modifier.width(6.dp))
                    StatsChip("${cache?.downloading ?: 0} downloading")
                }
                Row(Modifier.padding(top = 6.dp)) {
                    StatsChip("original: ${formatBytes(cache?.originalBytes ?: 0)}")
                    Spacer(Modifier.width(6.dp))
                    StatsChip("transcoded: ${formatBytes(cache?.transcodedBytes ?: 0)}")
                }
            }

            // ---- logout ----
            Button(
                onClick = onLogout,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A), contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Log out (${vm.currentUser ?: ""})")
            }
        }
    }

    if (showAddInstance) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddInstance = false },
            shape = RoundedCornerShape(12.dp),
            title = { Text("Add instance") },
            text = {
                Column {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Server URL") },
                        placeholder = { Text("https://notify.mfc.pw") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        scope.launch {
                            val instance = Instance(
                                id = "inst_${System.currentTimeMillis()}",
                                name = name.ifBlank { url.trim().trimEnd('/') },
                                baseUrl = url.trim().trimEnd('/')
                            )
                            appContainer.instancesRepository.upsert(instance)
                            appContainer.instancesRepository.setActive(instance.id)
                        }
                        showAddInstance = false
                    },
                    enabled = url.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddInstance = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsBlock(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB3B3B3), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun StatsChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF2A2A2A),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SourceChip(label: String, ok: Boolean = true) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFF2A2A2A),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
