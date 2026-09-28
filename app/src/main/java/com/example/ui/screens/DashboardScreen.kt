package com.example.ui.screens

import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.net.Uri
import android.provider.Settings
import android.os.PowerManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.role.RoleManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.MainViewModel
import com.example.Screen
import com.example.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel, 
    navController: NavHostController, 
    windowSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val context = LocalContext.current
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        )
    )
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val snackbarHostState = com.example.LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var showAdvanced by remember { mutableStateOf(false) }
    var showContactPermissionRationale by remember { mutableStateOf(false) }

    val roleManagerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(android.content.Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                settingsViewModel.updateGhostMode(true)
            } else {
                scope.launch { snackbarHostState.showSnackbar("Ghost Mode needs Call Screening permission to work perfectly.") }
            }
        }
    }

    val notificationPolicyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            settingsViewModel.updateOverrideDnd(true)
            val policy = android.app.NotificationManager.Policy(
                android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED
            )
            nm.notificationPolicy = policy
        }
    }
    
    // Also apply it periodically if enabled
    LaunchedEffect(settingsState.overrideDnd) {
        if (settingsState.overrideDnd) {
            val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                val policy = android.app.NotificationManager.Policy(
                    android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS or android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                    android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED,
                    android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED
                )
                nm.notificationPolicy = policy
            }
        }
    }
    
    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                settingsViewModel.updateDndBypassRingtoneUri(uri.toString())
            }
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            
                            if (hasPhoneIndex >= 0 && idIndex >= 0) {
                                val hasPhone = cursor.getInt(hasPhoneIndex)
                                val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                                
                                if (hasPhone > 0) {
                                    val id = cursor.getString(idIndex)
                                    context.contentResolver.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
                                        null, 
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", 
                                        arrayOf(id), 
                                        null
                                    )?.use { phones ->
                                        if (phones.moveToFirst()) {
                                            val numIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                            if (numIndex >= 0) {
                                                val currentVips = settingsState.vipCallers
                                                val numStr = phones.getString(numIndex)
                                                val newVips = if (currentVips.isEmpty()) "$name ($numStr)" else "$currentVips,$name ($numStr)"
                                                settingsViewModel.updateVipCallers(newVips)
                                                
                                                // Also set the STARRED status in the Android Contacts Database
                                                try {
                                                    val values = android.content.ContentValues()
                                                    values.put(android.provider.ContactsContract.Contacts.STARRED, 1)
                                                    context.contentResolver.update(
                                                        android.provider.ContactsContract.Contacts.CONTENT_URI,
                                                        values,
                                                        android.provider.ContactsContract.Contacts._ID + " = ?",
                                                        arrayOf(id)
                                                    )
                                                    scope.launch { snackbarHostState.showSnackbar("VIP Saved & Starred to bypass DND!") }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    scope.launch { snackbarHostState.showSnackbar("Added VIP, but could not star contact.") }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val currentVips = settingsState.vipCallers
                                    val newVips = if (currentVips.isEmpty()) name else "$currentVips,$name"
                                    settingsViewModel.updateVipCallers(newVips)
                                    scope.launch { snackbarHostState.showSnackbar("Important Contact Saved Successfully!") }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results[android.Manifest.permission.READ_CONTACTS] == true) {
            contactPickerLauncher.launch(null)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(true) }
    var isCallScreeningRoleHeld by remember { mutableStateOf(true) }
    var hasTelephonyPermissions by remember { mutableStateOf(true) }
    var showRestrictedSettingsGuide by remember { mutableStateOf(false) }

    fun refreshDiagnostics() {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
        isIgnoringBatteryOptimizations = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(android.content.Context.ROLE_SERVICE) as? RoleManager
            isCallScreeningRoleHeld = rm?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            isCallScreeningRoleHeld = true
        }

        val hasSms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val hasCallLog = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        hasTelephonyPermissions = hasSms && hasCallLog && hasContacts
    }

    val systemPermissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshDiagnostics()
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshDiagnostics()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showContactPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showContactPermissionRationale = false },
            title = { Text("Permission Required") },
            text = { Text("We need permission to read and write your contacts to automatically mark VIPs as 'Starred' so they bypass the Do Not Disturb policy.") },
            confirmButton = {
                TextButton(onClick = {
                    showContactPermissionRationale = false
                    contactPermissionLauncher.launch(arrayOf(android.Manifest.permission.READ_CONTACTS, android.Manifest.permission.WRITE_CONTACTS))
                }) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContactPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRestrictedSettingsGuide) {
        AlertDialog(
            onDismissRequest = { showRestrictedSettingsGuide = false },
            icon = { Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Android 13–16 Sideload Setup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "On modern Android (13 to 16), sideloaded apps from outside Google Play have their Accessibility and Notification settings restricted by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "To allow Pause to screen calls & auto-reply:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("1. Tap 'Open App Info' below.", style = MaterialTheme.typography.bodySmall)
                    Text("2. In the top-right corner, tap the Three Dots (⋮) menu.", style = MaterialTheme.typography.bodySmall)
                    Text("3. Tap 'Allow restricted settings' and confirm your PIN or fingerprint.", style = MaterialTheme.typography.bodySmall)
                    Text("4. Return here to enjoy fully automated background protection.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showRestrictedSettingsGuide = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open App Info")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedSettingsGuide = false }) {
                    Text("Got It")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Developer Info State
        var showDeveloperInfo by remember { mutableStateOf(false) }
        
        if (showDeveloperInfo) {
            AlertDialog(
                onDismissRequest = { showDeveloperInfo = false },
                icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("About the Developer") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            com.example.config.AppConfig.getDeveloperBio(context),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider()
                        Text(
                            "Core Features:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            """• Ghost Mode & Shield
• Silent Bypass
• Auto-Responder""",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeveloperInfo = false
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(com.example.config.AppConfig.getRepoUrl(context)))
                        context.startActivity(intent)
                    }) {
                        Text("View on GitHub")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeveloperInfo = false }) {
                        Text("Close")
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = { showDeveloperInfo = true }) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "About Developer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            
            // System Readiness & Modern Android Diagnostics Hub
            item {
                if (isIgnoringBatteryOptimizations && isCallScreeningRoleHeld && hasTelephonyPermissions) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("System Protection: Optimal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Call Screener active • Background unrestricted • Android 16 ready", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showRestrictedSettingsGuide = true }) {
                                Icon(Icons.Rounded.HelpOutline, contentDescription = "Sideload Help", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Action Required for Android 14–16", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Modern Android requires explicit permissions to stop system kills and enable silent call screening.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f))
                                }
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))

                            // 1. Call Screening Role
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isCallScreeningRoleHeld) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Call Screening Role", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text("Required to drop incoming spam before phone rings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            val roleManager = context.getSystemService(android.content.Context.ROLE_SERVICE) as? RoleManager
                                            val intent = roleManager?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                            if (intent != null) roleManagerLauncher.launch(intent)
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Text("Set Role", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            // 2. Battery Optimization
                            if (!isIgnoringBatteryOptimizations) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Unrestricted Battery", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text("Prevents Android Doze from killing Silent Guard", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Text("Allow", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            // 3. Telephony & SMS
                            if (!hasTelephonyPermissions) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Phone & SMS Access", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text("Needed for missed call detection & auto-reply", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            val perms = mutableListOf(
                                                Manifest.permission.RECEIVE_SMS,
                                                Manifest.permission.SEND_SMS,
                                                Manifest.permission.READ_SMS,
                                                Manifest.permission.READ_CALL_LOG,
                                                Manifest.permission.READ_CONTACTS,
                                                Manifest.permission.READ_PHONE_STATE
                                            )
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) perms.add(Manifest.permission.ANSWER_PHONE_CALLS)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                            systemPermissionsLauncher.launch(perms.toTypedArray())
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Text("Grant", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            // Android 13-16 Sideload Helper link
                            TextButton(
                                onClick = { showRestrictedSettingsGuide = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sideloaded APK Setup Guide", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // 1. Command Center / AI Status Hero
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pause Active",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (settingsState.ghostMode) "Silent Guard Active. Silently deflecting non-VIPs." else "Silent Guard Inactive. Normal operation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                if (!settingsState.ghostMode) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        val roleManager = context.getSystemService(android.content.Context.ROLE_SERVICE) as RoleManager
                                        if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                                            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                            roleManagerLauncher.launch(intent)
                                        } else {
                                            settingsViewModel.updateGhostMode(true)
                                        }
                                    } else {
                                        settingsViewModel.updateGhostMode(true)
                                    }
                                } else {
                                    settingsViewModel.updateGhostMode(false)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settingsState.ghostMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                contentColor = if (settingsState.ghostMode) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                if (settingsState.ghostMode) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive, 
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (settingsState.ghostMode) "Turn Off Silent Guard" else "Activate Silent Guard", 
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            // 4. The Vault (VIPs & Rules) Title
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The Vault (VIPs & Rules)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                
            }
            // 3. Smart DND Section (Structured, No big toggle)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.DoNotDisturbOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Smart Silent Mode", 
                                     style = MaterialTheme.typography.titleMedium, 
                                     fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = settingsState.overrideDnd,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        if (!nm.isNotificationPolicyAccessGranted) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                            try {
                                                notificationPolicyLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        } else {
                                            settingsViewModel.updateOverrideDnd(true)
                                        }
                                    } else {
                                        settingsViewModel.updateOverrideDnd(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Silences all incoming calls except your important contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // VIPs List
                        val vips = remember(settingsState.vipCallers) {
                            settingsState.vipCallers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                        
                        Text(
                            "Important Contacts (Always Ring)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (vips.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                vips.forEach { vip ->
                                    InputChip(
                                        selected = true,
                                        onClick = { },
                                        label = { Text(vip) },
                                        colors = InputChipDefaults.inputChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove Contact",
                                                modifier = Modifier.size(16.dp).clickable {
                                                    val newList = vips.filter { it != vip }.joinToString(",")
                                                    settingsViewModel.updateVipCallers(newList)
                                                }
                                            )
                                        },
                                        border = null,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No important contacts added yet. All calls will be silenced.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    contactPickerLauncher.launch(null)
                                } else {
                                    showContactPermissionRationale = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add VIP Number (Bypasses All)")
                        }
                    }
                }
            }
            
            // Advanced settings section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                        .clickable { showAdvanced = !showAdvanced },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Persistent Caller Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(
                                if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = showAdvanced,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    "Allow unknown numbers to bypass silent mode if they call multiple times in a row.", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Bypass after", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    var expandedCalls by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).clickable {
                                            expandedCalls = true
                                        }.padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("${settingsState.dndThresholdCalls} calls", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        DropdownMenu(expanded = expandedCalls, onDismissRequest = { expandedCalls = false }) {
                                            listOf(2, 3, 4, 5).forEach { callCount ->
                                                DropdownMenuItem(
                                                    text = { Text("$callCount calls") },
                                                    onClick = {
                                                        settingsViewModel.updateDndThresholdCalls(callCount.toString())
                                                        expandedCalls = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Within timeframe of", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.weight(1f))
                                    var expandedMins by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).clickable {
                                            expandedMins = true
                                        }.padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("${settingsState.dndTimeframeMinutes} mins", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        DropdownMenu(expanded = expandedMins, onDismissRequest = { expandedMins = false }) {
                                            listOf(1, 2, 3, 5, 10, 15).forEach { mins ->
                                                DropdownMenuItem(
                                                    text = { Text("$mins mins") },
                                                    onClick = {
                                                        settingsViewModel.updateDndTimeframeMinutes(mins.toString())
                                                        expandedMins = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
                        
                        // 2. Activity Overview / Reports
            // 2. Activity Overview / Reports (Connected to Real Database Metrics)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Calls Deflected",
                        value = settingsState.spamBlockedCount.toString(),
                        icon = Icons.Rounded.Shield,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Protect.route) }
                    )
                    StatCard(
                        title = "Tasks Today",
                        value = mainUiState.tasksToday.toString(),
                        icon = Icons.Rounded.Schedule,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Schedule.route) }
                    )
                    StatCard(
                        title = "Total Events",
                        value = mainUiState.totalForwarded.toString(),
                        icon = Icons.Rounded.ReceiptLong,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Connect.route) }
                    )
                }
            }

            // 3. Real Interactive Telephony & Protection Timeline
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Activity Stream",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (mainUiState.recentLogs.isNotEmpty()) {
                        TextButton(onClick = { navController.navigate(Screen.Connect.route) }) {
                            Text("View All", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (mainUiState.recentLogs.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        mainUiState.recentLogs.take(5).forEach { log ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val isSpam = log.status.contains("SPAM", ignoreCase = true) || log.status.contains("BLOCKED", ignoreCase = true)
                                    val isAutoReply = log.status.contains("REPLY", ignoreCase = true) || log.status.contains("AUTO", ignoreCase = true)
                                    val nodeColor = if (isSpam) MaterialTheme.colorScheme.error else if (isAutoReply) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    val nodeIcon = if (isSpam) Icons.Rounded.Block else if (isAutoReply) Icons.Rounded.Quickreply else Icons.Rounded.Notifications

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(nodeColor.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(nodeIcon, contentDescription = null, tint = nodeColor, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = log.sender.ifBlank { "Unknown Caller" },
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val timeFormatted = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
                                            Text(
                                                text = timeFormatted,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = log.message.ifBlank { log.status },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("No Recent Events", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Calls screened, auto-replies sent, and scheduled tasks will stream here live.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun StatCard(
    title: String, 
    value: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    color: Color, 
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .semantics { contentDescription = "$title is $value" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
