package com.example.ui.screens


import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.SmsProcessor
import com.example.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsRepository = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
    val viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModel.Factory(settingsRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val calendarPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            viewModel.updateCalendarSync(true)
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.shield.CalendarSyncWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                .build()
            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CalendarSync",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    val uriHandler = LocalUriHandler.current
    
    
    
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let {
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val jsonString = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                    if (jsonString != null) {
                        val rootObj = org.json.JSONObject(jsonString)
                        
                        val container = (context.applicationContext as com.example.ShieldApplication).container
                        val db = container.database
                        
                        val logsArray = rootObj.optJSONArray("logs")
                        if (logsArray != null) {
                            val smsRepo = container.smsRepository
                            for (i in 0 until logsArray.length()) {
                                val obj = logsArray.getJSONObject(i)
                                smsRepo.insertLog(com.example.data.SmsLogEntity(
                                    id = obj.optLong("id", 0),
                                    timestamp = obj.getLong("timestamp"),
                                    sender = obj.getString("sender"),
                                    message = obj.getString("message"),
                                    targetNumber = obj.getString("targetNumber"),
                                    status = obj.getString("status")
                                ))
                            }
                        }
                        
                        
                        val rulesArray = rootObj.optJSONArray("custom_rules")
                        if (rulesArray != null) {
                            val ruleRepo = container.ruleRepository
                            for (i in 0 until rulesArray.length()) {
                                val obj = rulesArray.getJSONObject(i)
                                ruleRepo.insertRule(com.example.db.CustomRule(
                                    id = obj.getString("id"),
                                    trigger = obj.getString("trigger"),
                                    action = obj.getString("action")
                                ))
                            }
                        }
                        
                        val phoneRulesArray = rootObj.optJSONArray("phone_rules")
                        if (phoneRulesArray != null) {
                            val phoneRuleRepo = container.phoneRuleRepository
                            for (i in 0 until phoneRulesArray.length()) {
                                val obj = phoneRulesArray.getJSONObject(i)
                                phoneRuleRepo.insert(com.example.data.PhoneRuleEntity(
                                    phoneNumber = obj.getString("phoneNumber"),
                                    isVip = obj.getBoolean("isVip"),
                                    isDivert = obj.getBoolean("isDivert")
                                ))
                            }
                        }
                        
                        val expensesArray = rootObj.optJSONArray("expenses")
                        if (expensesArray != null) {
                            val finRepo = container.financialRepository
                            for (i in 0 until expensesArray.length()) {
                                val obj = expensesArray.getJSONObject(i)
                                finRepo.insertExpense(com.example.data.ExpenseEntity(
                                    id = obj.optLong("id", 0),
                                    merchant = obj.getString("merchant"),
                                    amountStr = obj.getString("amountStr"),
                                    amountVal = obj.getDouble("amountVal"),
                                    dateDetected = obj.getLong("dateDetected"),
                                    source = obj.getString("source"),
                                    originalMessage = obj.optString("originalMessage", "")
                                ))
                            }
                        }
                        
                        val chatMessagesArray = rootObj.optJSONArray("chat_messages")
                        if (chatMessagesArray != null) {
                            val chatRepo = container.chatRepository
                            for (i in 0 until chatMessagesArray.length()) {
                                val obj = chatMessagesArray.getJSONObject(i)
                                chatRepo.insertMessage(com.example.data.ChatMessageEntity(
                                    id = obj.optInt("id", 0),
                                    text = obj.getString("text"),
                                    isUser = obj.getBoolean("isUser"),
                                    timestamp = obj.getLong("timestamp")
                                ))
                            }
                        }
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Data successfully imported", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    // Legacy settings removed
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {


                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "Emergency Safety Nets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ListItem(
                headlineContent = { Text("Repeat Caller DND Bypass") },
                supportingContent = { Text("Optional: Allow persistent callers to ring loudly if they call 3 times in 5 mins") },
                leadingContent = { Icon(Icons.Default.Repeat, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiState.overrideDnd,
                        onCheckedChange = { isChecked ->
                            viewModel.updateOverrideDnd(isChecked)
                        }
                    )
                }
            )
            
            ListItem(
                headlineContent = { Text("URGENT Keyword Alarm") },
                supportingContent = { Text("SMS or WhatsApp with 'URGENT' sounds a loud 15-second alarm") },
                leadingContent = { Icon(Icons.Default.Warning, contentDescription = null) }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "Calendar Focus Sync",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            ListItem(
                headlineContent = { Text("Local Busy Shield (Calendar Sync)") },
                supportingContent = { Text("When you have a 'Busy' calendar event/meeting, Pause automatically enables Silent Guard to reject unknown calls and auto-reply until the meeting ends.") },
                leadingContent = { Icon(Icons.Default.Event, contentDescription = null) },
                trailingContent = { 
                    Switch(checked = uiState.calendarSync, onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.updateCalendarSync(true)
                                val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.shield.CalendarSyncWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                                    .build()
                                androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                    "CalendarSync",
                                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                                    workRequest
                                )
                            } else {
                                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                        } else {
                            viewModel.updateCalendarSync(false)
                            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("CalendarSync")
                        }
                    })
                }
            )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "Privacy & Data Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "All logs, preferences, and rules are protected with hardware AES-256 SQLCipher encryption.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Network & Data Transparency Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Transparency: What Uses Internet vs. Offline?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "• Call Filtering & Ghost Mode: 100% Offline (Local chip)\n" +
                        "• Starred VIP DND Bypass: 100% Offline (Local notification policy)\n" +
                        "• SMS Auto-Replies: Carrier SIM Only (No cloud server)\n" +
                        "• WhatsApp Auto-Reply: Uses your phone's internet directly via WhatsApp notification quick-reply (Pause has no server)\n" +
                        "• Database & Logs: 100% Local (Encrypted with Hardware Keystore)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val scope = rememberCoroutineScope()
            var privacyMessage by remember { mutableStateOf("") }
            var showExportWarning by remember { mutableStateOf(false) }
            
            if (privacyMessage.isNotBlank()) {
                Text(privacyMessage, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            
            if (showExportWarning) {
                AlertDialog(
                    onDismissRequest = { showExportWarning = false },
                    title = { Text("Export Warning", color = MaterialTheme.colorScheme.error) },
                    text = { Text("WARNING: Exporting your data creates a plain-text JSON file containing all your SMS logs and automation configurations. Anyone with access to this file can read your sensitive data. Keep it safe.") },
                    confirmButton = {
                        FilledTonalButton(
                            onClick = {
                                showExportWarning = false
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val container = (context.applicationContext as com.example.ShieldApplication).container
                            val logs = container.smsRepository.getAllLogsSync()
                            val customRules = container.ruleRepository.getAllRulesSync()
                            val phoneRulesFlow = container.phoneRuleRepository.getAllRules()
                            val phoneRules = phoneRulesFlow.first()
                                                        
                            val rootObj = org.json.JSONObject()
                            
                            val logsArray = org.json.JSONArray()
                            logs.forEach { log ->
                                val obj = org.json.JSONObject()
                                obj.put("id", log.id)
                                obj.put("sender", log.sender)
                                obj.put("message", log.message)
                                obj.put("timestamp", log.timestamp)
                                obj.put("targetNumber", log.targetNumber)
                                obj.put("status", log.status)
                                logsArray.put(obj)
                            }
                            rootObj.put("logs", logsArray)
                            
                            val rulesArray = org.json.JSONArray()
                            customRules.forEach { rule ->
                                val obj = org.json.JSONObject()
                                obj.put("id", rule.id)
                                obj.put("trigger", rule.trigger)
                                obj.put("action", rule.action)
                                rulesArray.put(obj)
                            }
                            rootObj.put("custom_rules", rulesArray)
                            
                            val phoneRulesArray = org.json.JSONArray()
                            phoneRules.forEach { rule ->
                                val obj = org.json.JSONObject()
                                obj.put("phoneNumber", rule.phoneNumber)
                                obj.put("isVip", rule.isVip)
                                obj.put("isDivert", rule.isDivert)
                                phoneRulesArray.put(obj)
                            }
                            rootObj.put("phone_rules", phoneRulesArray)
                            
                            
                            val subscriptions = container.financialRepository.getAllSubscriptionsSync()
                            val expenses = container.financialRepository.getAllExpensesSync()
                            val chatMessages = container.chatRepository.getAllMessagesSync()
                            val subscriptionsArray = org.json.JSONArray()
                            subscriptions.forEach { sub ->
                                val obj = org.json.JSONObject()
                                obj.put("id", sub.id)
                                obj.put("name", sub.name)
                                obj.put("amount", sub.amount)
                                obj.put("dateDetected", sub.dateDetected)
                                obj.put("source", sub.source)
                                obj.put("isNewsletter", sub.isNewsletter)
                                obj.put("originalMessage", sub.originalMessage)
                                subscriptionsArray.put(obj)
                            }
                            rootObj.put("subscriptions", subscriptionsArray)
                            
                            val expensesArray = org.json.JSONArray()
                            expenses.forEach { exp ->
                                val obj = org.json.JSONObject()
                                obj.put("id", exp.id)
                                obj.put("merchant", exp.merchant)
                                obj.put("amountStr", exp.amountStr)
                                obj.put("amountVal", exp.amountVal)
                                obj.put("dateDetected", exp.dateDetected)
                                obj.put("source", exp.source)
                                obj.put("originalMessage", exp.originalMessage)
                                expensesArray.put(obj)
                            }
                            rootObj.put("expenses", expensesArray)
                            
                            val chatMessagesArray = org.json.JSONArray()
                            chatMessages.forEach { msg ->
                                val obj = org.json.JSONObject()
                                obj.put("id", msg.id)
                                obj.put("text", msg.text)
                                obj.put("isUser", msg.isUser)
                                obj.put("timestamp", msg.timestamp)
                                chatMessagesArray.put(obj)
                            }
                            rootObj.put("chat_messages", chatMessagesArray)
                            
                            val jsonString = rootObj.toString(4)
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, jsonString)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Local Data"))
                                privacyMessage = "Data ready for export. No cloud transmission."
                            }
                        }
                    }) { Text("Export Anyway") }
                    },
                    dismissButton = { TextButton(onClick = { showExportWarning = false }) { Text("Cancel") } }
                )
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { showExportWarning = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Export")
                }
                
                FilledTonalButton(
                    onClick = { importLauncher.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Import")
                }
                
                FilledTonalButton(
                    onClick = { 
                        scope.launch {
                            val db = (context.applicationContext as com.example.ShieldApplication).container.database
                            db.clearAllTables()
                            privacyMessage = "All local data permanently deleted."
                        }
                    },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text("Delete All")
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                text = "Open Source Community",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "Support the project. No ads, no telemetry, entirely open source.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { uriHandler.openUri(com.example.config.AppConfig.getRepoUrl(context)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Source")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("GitHub")
                }
                
                FilledTonalButton(
                    onClick = { uriHandler.openUri(com.example.config.AppConfig.getSponsorUrl(context)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Donate")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Donate")
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { uriHandler.openUri(com.example.config.AppConfig.getCoffeeUrl(context)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Coffee")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buy me a coffee", maxLines = 1)
                }
                
                FilledTonalButton(
                    onClick = { uriHandler.openUri(com.example.config.AppConfig.getLiberapayUrl(context)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.Star, contentDescription = "LiberaPay")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LiberaPay", maxLines = 1)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "About & Legal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { uriHandler.openUri(com.example.config.AppConfig.getPrivacyPolicyUrl(context)) }) {
                    Text("Privacy")
                }
                TextButton(onClick = { uriHandler.openUri(com.example.config.AppConfig.getTermsUrl(context)) }) {
                    Text("Terms")
                }
                TextButton(onClick = { uriHandler.openUri(com.example.config.AppConfig.getLicenseUrl(context)) }) {
                    Text("License")
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
    



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxPickerModal(onDismiss: () -> Unit, onSenderSelected: (String) -> Unit) {
    val context = LocalContext.current
    var recentSenders by remember { mutableStateOf<List<String>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val list = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val senders = mutableSetOf<String>()
            try {
                val cursor = context.contentResolver.query(
                    android.provider.Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf("address"),
                    null,
                    null,
                    "date DESC LIMIT 100"
                )
                cursor?.use {
                    val addressIndex = it.getColumnIndex("address")
                    while (it.moveToNext()) {
                        val s = it.getString(addressIndex)
                        if (!s.isNullOrBlank()) {
                            senders.add(s)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            senders.toList()
        }
        recentSenders = list
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recent Inbound Senders") },
        text = {
            if (recentSenders.isEmpty()) {
                Text("No recent SMS found or permission denied.")
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    recentSenders.forEach { s ->
                        TextButton(
                            onClick = { onSenderSelected(s) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(s, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel") }
        },
        text = { content() }
    )
}
