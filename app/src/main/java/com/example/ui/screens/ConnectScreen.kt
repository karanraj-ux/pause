package com.example.ui.screens

import kotlinx.coroutines.launch


import android.Manifest
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.MainViewModel
import com.example.data.repository.NumberReplyRule
import com.example.ui.viewmodels.SettingsViewModel


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectScreen(viewModel: MainViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Auto-Reply", fontWeight = FontWeight.Bold) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Forwarding", fontWeight = FontWeight.Bold) },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (selectedTab == 0) {
            AutoReplyTab(viewModel)
        } else {
            ForwardingTab(viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoReplyTab(viewModel: MainViewModel) {

    val context = LocalContext.current
    val snackbarHostState = com.example.LocalSnackbarHostState.current
    var showPrivacyPledge by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        )
    )
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val hasSmsPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasCallLogPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasPhoneStatePerm = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] == true
        val callLogGranted = permissions[Manifest.permission.READ_CALL_LOG] == true
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] == true
        
        if (smsGranted && (callLogGranted || phoneStateGranted)) {
            settingsViewModel.updateAutoRespondMissedCall(true)
            settingsViewModel.updateAutoRespondSms(true)
        } else if (smsGranted) {
            settingsViewModel.updateAutoRespondSms(true)
            settingsViewModel.updateAutoRespondMissedCall(false)
        } else {
            settingsViewModel.updateAutoRespondMissedCall(false)
            settingsViewModel.updateAutoRespondSms(false)
        }
    }

    if (showPrivacyPledge) {
        PrivacyPledgeDialog(
            onConfirm = {
                showPrivacyPledge = false
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            },
            onDismiss = {
                showPrivacyPledge = false
            }
        )
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
                                                val number = phones.getString(numIndex)
                                                val currentList = settingsState.autoReplyRestrictedNumbers
                                                val newList = if (currentList.isEmpty()) name else "$currentList,$name"
                                                settingsViewModel.updateAutoReplyRestrictedNumbers(newList)
                                            }
                                        }
                                    }
                                } else {
                                    val currentList = settingsState.autoReplyRestrictedNumbers
                                    val newList = if (currentList.isEmpty()) name else "$currentList,$name"
                                    settingsViewModel.updateAutoReplyRestrictedNumbers(newList)
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

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Top Section: Triggers & Explanation
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "When to Auto-Reply",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Choose the triggers that prompt Pause to send an automated SMS reply.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Missed Calls Trigger
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
                            Icon(Icons.Rounded.PhoneMissed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "On Missed Phone Calls",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Sends an SMS when you miss a call",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settingsState.autoRespondMissedCall,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (hasSmsPerm && (hasCallLogPerm || hasPhoneStatePerm)) {
                                        settingsViewModel.updateAutoRespondMissedCall(true)
                                    } else {
                                        showPrivacyPledge = true
                                    }
                                } else {
                                    settingsViewModel.updateAutoRespondMissedCall(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                    
                    // Incoming SMS Trigger
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "On Incoming SMS Messages",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Sends an SMS reply when someone texts you",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settingsState.autoRespondSms,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (hasSmsPerm) {
                                        settingsViewModel.updateAutoRespondSms(true)
                                    } else {
                                        showPrivacyPledge = true
                                    }
                                } else {
                                    settingsViewModel.updateAutoRespondSms(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Incoming WhatsApp Trigger
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, tint = Color(0xFF25D366))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "On Incoming WhatsApp Chats",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Direct offline auto-reply via notification listener",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settingsState.autoRespondWhatsapp,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val notifEnabled = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                                    if (notifEnabled) {
                                        settingsViewModel.updateAutoRespondWhatsapp(true)
                                    } else {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                        context.startActivity(intent)
                                        settingsViewModel.updateAutoRespondWhatsapp(true)
                                    }
                                } else {
                                    settingsViewModel.updateAutoRespondWhatsapp(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF25D366))
                        )
                    }

                    if (!settingsState.autoRespondMissedCall && !settingsState.autoRespondSms && !settingsState.autoRespondWhatsapp) {
                        Text(
                            "Turn on one or more triggers above to activate smart auto-replies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // WhatsApp Offline Bot & Message Interpreter Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFF25D366).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Quickreply, contentDescription = null, tint = Color(0xFF25D366))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Enable WhatsApp Auto-Reply",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Instant replies over the internet • 3 recipient tiers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF25D366),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Switch(
                            checked = settingsState.autoRespondWhatsapp,
                            onCheckedChange = { isChecked ->
                                settingsViewModel.updateAutoRespondWhatsapp(isChecked)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF25D366))
                        )
                    }

                    Text(
                        "When Focus Mode is on, Pause rejects unknown WhatsApp calls, allows starred/VIP contacts through DND silent mode, and instantly sends your chosen auto-reply message over the internet to incoming chats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Showcase: Everyday WhatsApp Sleep / Morning Auto-Reply
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF075E54).copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ChatBubble, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Everyday Example: Peaceful Morning & Sleep Shield",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "When someone texts 'Good morning!' while you are asleep or in focus mode, Pause answers instantly over the internet with your pre-set response:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Simulated Incoming WhatsApp Bubble
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("Friend / Partner", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF25D366))
                                        Text("Good morning! Are you awake yet? ☕", style = MaterialTheme.typography.bodySmall)
                                        Text("07:15 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
                                    }
                                }
                            }

                            // Simulated Auto-Reply WhatsApp Bubble
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Surface(
                                    color = Color(0xFFDCF8C6),
                                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("Pause (Auto-Reply)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF075E54))
                                        Text("Still resting! Phone is on silent until 8:30 AM. Will catch up shortly ☀️", style = MaterialTheme.typography.bodySmall, color = Color.Black)
                                        Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                                            Text("07:15 AM", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Rounded.DoneAll, contentDescription = null, tint = Color(0xFF34B7F1), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        // 2. Customized Reply Messages by Recipient Tier
        item {
            AnimatedVisibility(
                visible = settingsState.autoRespondMissedCall || settingsState.autoRespondSms || settingsState.autoRespondWhatsapp,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Auto-Reply Messages by Recipient",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Pause sends different messages depending on who is contacting you:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // VIPs
                            OutlinedTextField(
                                value = settingsState.vipReplyMsg,
                                onValueChange = { settingsViewModel.updateVipReplyMsg(it) },
                                label = { Text("Inner Circle (VIP Contacts)") },
                                supportingText = { Text("Sent only to numbers you marked as VIP in Pause") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4
                            )

                            // Standard Contacts
                            OutlinedTextField(
                                value = settingsState.standardReplyMsg,
                                onValueChange = { 
                                    settingsViewModel.updateStandardReplyMsg(it)
                                    settingsViewModel.updateBusyReplyMessage(it)
                                },
                                label = { Text("Saved Contacts (Address Book)") },
                                supportingText = { Text("Sent to any known number saved in your phone") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4
                            )

                            // Unknown Strangers
                            OutlinedTextField(
                                value = settingsState.unknownReplyMsg,
                                onValueChange = { settingsViewModel.updateUnknownReplyMsg(it) },
                                label = { Text("Unknown Numbers (Strangers / Unsaved)") },
                                supportingText = { Text("Sent to callers who are not in your contacts") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                    }

                    // 3. Replies for Specific Numbers — own reply set per number (1, 2, 3…)
                    var showRuleDialog by remember { mutableStateOf(false) }
                    var editingRule by remember { mutableStateOf<NumberReplyRule?>(null) }

                    Card(
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Replies for Specific Numbers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                FilledTonalButton(onClick = { editingRule = null; showRuleDialog = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }
                            Text(
                                "Give any number its own set of replies. When they message you on WhatsApp, Pause sends each reply as its own message, in order — this overrides the VIP / contact / unknown messages above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val numberRules = settingsState.numberReplyRules
                            if (numberRules.isEmpty()) {
                                Text(
                                    "No number-specific replies yet. Tap Add to create one.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                numberRules.forEach { rule ->
                                    key(rule.number) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        if (rule.name.isNotBlank()) "${rule.name} • ${rule.number}" else rule.number,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    val preview = rule.replies.first()
                                                    Text(
                                                        "${rule.replies.size} ${if (rule.replies.size == 1) "reply" else "replies"}: ${preview.take(60)}${if (preview.length > 60) "…" else ""}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }
                                                IconButton(onClick = { editingRule = rule; showRuleDialog = true }) {
                                                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = {
                                                    settingsViewModel.setNumberReplyRules(numberRules.filterNot { it.number == rule.number })
                                                }) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showRuleDialog) {
                        val base = editingRule
                        var dlgName by remember(base) { mutableStateOf(base?.name ?: "") }
                        var dlgNumber by remember(base) { mutableStateOf(base?.number ?: "") }
                        var dlgReplies by remember(base) { mutableStateOf((base?.replies ?: emptyList()).ifEmpty { listOf("") }) }

                        AlertDialog(
                            onDismissRequest = { showRuleDialog = false },
                            title = { Text(if (base == null) "Add number replies" else "Edit number replies") },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = dlgName,
                                        onValueChange = { dlgName = it },
                                        label = { Text("Name (optional)") },
                                        placeholder = { Text("e.g. Mom") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = dlgNumber,
                                        onValueChange = { dlgNumber = it },
                                        label = { Text("Phone number") },
                                        placeholder = { Text("+91 98765 43210") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                    Text(
                                        "Replies — sent in order, one WhatsApp message each",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    dlgReplies.forEachIndexed { i, rep ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = rep,
                                                onValueChange = { v ->
                                                    dlgReplies = dlgReplies.toMutableList().also { it[i] = v }
                                                },
                                                label = { Text("Reply ${i + 1}") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(12.dp),
                                                minLines = 2,
                                                maxLines = 4
                                            )
                                            if (dlgReplies.size > 1) {
                                                IconButton(onClick = {
                                                    dlgReplies = dlgReplies.toMutableList().also { it.removeAt(i) }
                                                }) {
                                                    Icon(Icons.Filled.Close, contentDescription = "Remove reply")
                                                }
                                            }
                                        }
                                    }
                                    TextButton(onClick = { dlgReplies = dlgReplies + "" }) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add reply")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val cleanReplies = dlgReplies.map { it.trim() }.filter { it.isNotBlank() }
                                    if (dlgNumber.trim().isNotBlank() && cleanReplies.isNotEmpty()) {
                                        val newRule = NumberReplyRule(
                                            number = dlgNumber.trim(),
                                            name = dlgName.trim(),
                                            replies = cleanReplies
                                        )
                                        settingsViewModel.setNumberReplyRules(
                                            settingsState.numberReplyRules.filterNot {
                                                it.number == newRule.number || (base != null && it.number == base.number)
                                            } + newRule
                                        )
                                        showRuleDialog = false
                                    }
                                }) { Text("Save") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRuleDialog = false }) { Text("Cancel") }
                            }
                        )
                    }

                    // Loop & Cost Safety Guarantee
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Safety & Rate-Limiting Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "• Blocked or muted numbers will NEVER receive auto-replies.\n• Shortcodes (e.g. bank OTP alerts) are automatically ignored.\n• Rate limit: Max 3 auto-replies per phone number per hour to prevent infinite reply loops.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        // 3. Premium Explanation Card
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
                            "Why use Auto-Responder?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BenefitRow(
                        icon = Icons.Rounded.Favorite,
                        title = "Polite Professionalism",
                        description = "Never leave someone hanging. Automatically acknowledge important calls when you're busy."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.PrivacyTip,
                        title = "100% On-Device",
                        description = "Your contacts and messages are never sent to a cloud server."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.SettingsSuggest,
                        title = "Smart Context",
                        description = "Only replies to actual missed calls, not spam or blocked numbers."
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForwardingTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = com.example.LocalSnackbarHostState.current
    var showPrivacyPledge by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        )
    )
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    
    val ruleViewModel: com.example.ui.viewmodels.PhoneRuleViewModel = viewModel(
        factory = com.example.ui.viewmodels.PhoneRuleViewModel.Factory(
            (context.applicationContext as com.example.ShieldApplication).container.phoneRuleRepository
        )
    )
    val rules by ruleViewModel.rules.collectAsStateWithLifecycle(initialValue = emptyList())
    var showInboxPicker by remember { mutableStateOf(false) }

    


    val targetPhonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            
                            if (hasPhoneIndex >= 0 && idIndex >= 0) {
                                val hasPhone = cursor.getInt(hasPhoneIndex)
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
                                                val number = phones.getString(numIndex)
                                                settingsViewModel.updateSmsForwardTarget(number ?: "")
                                            }
                                        }
                                    }
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

            val callPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true && permissions[Manifest.permission.ANSWER_PHONE_CALLS] == true) {
            settingsViewModel.updateAutoForwardCalls(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Call permissions are required to forward calls") }
        }
    }


        val smsForwardPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val sendGranted = results[Manifest.permission.SEND_SMS] == true
        val receiveGranted = results[Manifest.permission.RECEIVE_SMS] == true
        val readGranted = results[Manifest.permission.READ_SMS] == true
        if (sendGranted && receiveGranted && readGranted) {
            settingsViewModel.updateSmsForwardingEnabled(true)
        } else {
            scope.launch { snackbarHostState.showSnackbar("SMS Send, Receive, and Read permissions are required") }
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            targetPhonePickerLauncher.launch(null)
        } else {
            scope.launch { snackbarHostState.showSnackbar("Contacts permission is required to pick a number") }
        }
    }
    

    
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            showInboxPicker = true
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        
        // Call Forwarding Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.PhoneForwarded, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Call Forwarding Rules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                            OutlinedTextField(
                                value = settingsState.forwardPhone,
                                onValueChange = { settingsViewModel.updateForwardPhone(it) },
                                label = { Text("Secondary Phone (Call Forwarding Target)") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { 
                                        if (!settingsState.autoForwardCalls) {
                                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS))
                                            } else {
                                                settingsViewModel.updateAutoForwardCalls(true)
                                            }
                                        } else {
                                            settingsViewModel.updateAutoForwardCalls(false)
                                        }
                                    }.padding(16.dp).fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto-Forward Unknown Calls", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                        Text("Forwards callers not in Important Contacts to your secondary phone and auto-replies to them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = settingsState.autoForwardCalls,
                                                                                                                        onCheckedChange = { isChecked -> 
                                            if (isChecked) {
                                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    callPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS))
                                                } else {
                                                    settingsViewModel.updateAutoForwardCalls(true)
                                                }
                                            } else {
                                                settingsViewModel.updateAutoForwardCalls(false)
                                            }
                                        }
                                    )
                                }
                            }

                            PhoneRulesUI(ruleViewModel)
                        }
                }
            }
        }
        
        // Contextual Integrations Card
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
                            "Contextual Integrations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BenefitRow(
                        icon = Icons.Rounded.Event,
                        title = "Calendar Sync (Silent Guard)",
                        description = "Automatically block all non-VIP calls when you have a busy event on your calendar."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.Videocam,
                        title = "Deep Work Detection",
                        description = "Instantly mute calls when you are in a Zoom, Teams, or Meet video conference."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BenefitRow(
                        icon = Icons.Rounded.DirectionsCar,
                        title = "Drive Mode",
                        description = "Activate automatic WhatsApp redirect replies when connected to your car's Bluetooth."
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyPledgeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Privacy Pledge & Permissions", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Shield operates 100% offline on your device. We respect your privacy completely.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "To enable the Auto-Responder and the 'URGENT' emergency keyword, we need SMS and Call Log access. Modern Android versions restrict these for security, but we need them specifically for these two features.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• We DO NOT read your personal OTPs.\n• We DO NOT harvest your contacts.\n• Shield makes ZERO network requests to external servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("I Understand & Agree")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
