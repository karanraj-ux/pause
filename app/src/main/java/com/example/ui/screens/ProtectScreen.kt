package com.example.ui.screens

import kotlinx.coroutines.launch

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsAccessibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.MoneyOff
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainViewModel
import com.example.ui.viewmodels.SettingsViewModel

@Composable
fun ProtectScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        )
    )
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = com.example.LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    val ghostModePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val contactsGranted = permissions[Manifest.permission.READ_CONTACTS] == true
        val phoneGranted = permissions[Manifest.permission.ANSWER_PHONE_CALLS] == true
        if (contactsGranted && phoneGranted) {
            settingsViewModel.updateGhostMode(true)
            // Toast will be shown from the switch toggle
        } else {
            // Permission denied
            settingsViewModel.updateGhostMode(false)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!settingsState.masterKillSwitch) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (!settingsState.masterKillSwitch) Icons.Rounded.GppGood else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = if (!settingsState.masterKillSwitch) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (!settingsState.masterKillSwitch) "Shield is Active" else "App is Disabled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (!settingsState.masterKillSwitch) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (!settingsState.masterKillSwitch) "Master Switch is ON" else "Master Switch is OFF (Dumb state)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!settingsState.masterKillSwitch) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = !settingsState.masterKillSwitch,
                        onCheckedChange = { isChecked ->
                            settingsViewModel.updateMasterKillSwitch(!isChecked)
                            scope.launch { snackbarHostState.showSnackbar(if (isChecked) "Shield Activated" else "App Disabled") }
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.GppGood, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Shield Defense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Advanced spam & caller blocking",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
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
                            Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Silent Guard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
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
                                    scope.launch { snackbarHostState.showSnackbar("Silent Guard Active") }
                                } else {
                                    settingsViewModel.updateGhostMode(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Ultimate Privacy: Instantly screens any caller not saved in your contacts before your phone rings. Perfect for deep focus and peace of mind.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    
                    if (settingsState.ghostMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { 
                                settingsViewModel.pauseGhostMode(60 * 60 * 1000L)
                                scope.launch { snackbarHostState.showSnackbar("Silent Guard paused for 1 hour") }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pause for 1 Hour (Deliveries/Rides)")
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFF7043).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF7043))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Truecaller Smart Spam",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = settingsState.smartSpamReader,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    ghostModePermissionsLauncher.launch(
                                        arrayOf(Manifest.permission.ANSWER_PHONE_CALLS)
                                    )
                                    val nlIntent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    try {
                                        context.startActivity(nlIntent)
                                        scope.launch { snackbarHostState.showSnackbar("Please allow Notification Access for Shield.") }
                                    } catch (e: Exception) {
                                        // Ignore if activity not found
                                    }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Smart Spam Reader Disabled") }
                                }
                                settingsViewModel.updateSmartSpamReader(isChecked)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFF7043))
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Acts like Premium Truecaller. Reads the screen to instantly cut known red-flag spam before it even rings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { 
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SettingsAccessibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Grant Accessibility Access", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Why use Shield?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BenefitRow(
                        icon = Icons.Rounded.MoneyOff,
                        title = "Save on Subscriptions",
                        description = "Get premium spam blocking for free. No Truecaller Premium needed."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "100% On-Device",
                        description = "We don't upload your contacts to cloud servers, unlike other apps."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.Star,
                        title = "No Ads, No Clutter",
                        description = "A clean experience focused strictly on protecting your peace."
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}
