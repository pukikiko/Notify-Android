package com.notify.android.mobile.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notify.core.ui.auth.RootViewModel
import com.notify.core.ui.authViewModel
import com.notify.android.mobile.ui.theme.NotifyPurple
import com.notify.android.mobile.ui.theme.SpotifyBlack
import com.notify.android.mobile.ui.theme.SpotifySurfaceHigh
import com.notify.core.data.Instance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(rootVm: RootViewModel) {
    val authVm = authViewModel()
    val instances by authVm.instances.collectAsState()
    val activeId by authVm.activeId.collectAsState()
    val busy by authVm.busy.collectAsState()
    val error by authVm.error.collectAsState()
    val loggedIn by authVm.loggedIn.collectAsState()

    var mode by rememberSaveable { mutableStateOf("login") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showAddInstance by remember { mutableStateOf(false) }
    var instanceExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            rootVm.refresh()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SpotifyBlack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Notify",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = NotifyPurple
            )
            Text(
                "Your private music, streamed from your own server.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // ---- instance selector ----
            OutlinedTextField(
                value = activeId?.let { id -> instances.find { it.id == id }?.name ?: "Select instance" } ?: "Select instance",
                onValueChange = {},
                readOnly = true,
                label = { Text("Instance") },
                trailingIcon = {
                    IconButton(onClick = { instanceExpanded = !instanceExpanded }) {
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = instances.isNotEmpty()
            )
            if (instanceExpanded && instances.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SpotifySurfaceHigh)
                ) {
                    Column {
                        instances.forEach { inst ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        authVm.selectInstance(inst.id)
                                        instanceExpanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(inst.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        inst.baseUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (inst.token != null) {
                                    Text("Logged in", color = NotifyPurple, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(color = SpotifyBlack)
                        }
                    }
                }
            }

            TextButton(onClick = { showAddInstance = true }, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Text("Add instance", modifier = Modifier.padding(start = 4.dp))
            }

            // ---- auth form ----
            if (instances.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    if (mode == "login") "Log in to Notify" else "Sign up for Notify",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Button(
                    onClick = {
                        if (mode == "login") {
                            authVm.login(username, password) {}
                        } else {
                            authVm.register(username, password) {}
                        }
                    },
                    enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NotifyPurple, contentColor = SpotifyBlack),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(if (mode == "login") "Log In" else "Sign Up", fontWeight = FontWeight.Bold)
                }

                Text(
                    if (mode == "login") "Don't have an account?" else "Already have an account?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = { mode = if (mode == "login") "register" else "login"; authVm.clearError() }) {
                    Text(if (mode == "login") "Sign up for Notify" else "Log in")
                }
            } else {
                Text(
                    "Add a Notify instance to get started — e.g. https://notify.mfc.pw",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }

    if (showAddInstance) {
        AddInstanceDialog(
            onDismiss = { showAddInstance = false },
            onAdd = { name, url ->
                authVm.addInstance(name, url)
                showAddInstance = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddInstanceDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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
            Button(onClick = { onAdd(name, url) }, enabled = url.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = NotifyPurple, contentColor = SpotifyBlack)) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
