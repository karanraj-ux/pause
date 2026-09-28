package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodels.SettingsState
import com.example.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GhostModeCard(
    settingsState: SettingsState,
    settingsViewModel: SettingsViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showPauseMenu by remember { mutableStateOf(false) }
    
    val ghostModePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        val phoneGranted = permissions[Manifest.permission.ANSWER_PHONE_CALLS] == true
        if (contactsGranted && phoneGranted) {
            settingsViewModel.updateGhostMode(true)
            scope.launch { snackbarHostState.showSnackbar("Silent Guard Active") }
        } else {
            settingsViewModel.updateGhostMode(false)
        }
    }

    var timeLeft by remember { mutableStateOf(0L) }
    
    LaunchedEffect(settingsState.ghostModePauseEndTime) {
        while (true) {
            val now = System.currentTimeMillis()
            timeLeft = settingsState.ghostModePauseEndTime - now
            if (timeLeft <= 0) break
            delay(1000)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Silent Guard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Silently filters unknown calls before ringing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (timeLeft > 0) {
                        val minutes = (timeLeft / 1000) / 60
                        val seconds = (timeLeft / 1000) % 60
                        Text(
                            text = "Paused: ${minutes}m ${seconds}s left",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (settingsState.ghostMode) {
                    Box {
                        IconButton(onClick = { showPauseMenu = true }) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(
                            expanded = showPauseMenu,
                            onDismissRequest = { showPauseMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Pause 15 mins") },
                                onClick = {
                                    settingsViewModel.pauseGhostMode(15 * 60 * 1000L)
                                    showPauseMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pause 30 mins") },
                                onClick = {
                                    settingsViewModel.pauseGhostMode(30 * 60 * 1000L)
                                    showPauseMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pause 1 Hour") },
                                onClick = {
                                    settingsViewModel.pauseGhostMode(60 * 60 * 1000L)
                                    showPauseMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pause 2 Hours") },
                                onClick = {
                                    settingsViewModel.pauseGhostMode(2 * 60 * 60 * 1000L)
                                    showPauseMenu = false
                                }
                            )
                            if (timeLeft > 0) {
                                DropdownMenuItem(
                                    text = { Text("Resume Now") },
                                    onClick = {
                                        settingsViewModel.pauseGhostMode(0L)
                                        showPauseMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Switch(
                    checked = settingsState.ghostMode,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            ghostModePermissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.ANSWER_PHONE_CALLS
                                )
                            )
                        } else {
                            settingsViewModel.updateGhostMode(false)
                        }
                    }
                )
            }
        }
    }
}
