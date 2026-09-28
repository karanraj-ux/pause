package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.repository.WebhookRepository
import com.example.shield.WebhookConfig
import com.example.shield.WebhookWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhookScreen() {
    val context = LocalContext.current
    val webhookRepo = (context.applicationContext as com.example.ShieldApplication).container.webhookRepository
    val webhooks by webhookRepo.allWebhooks.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    
    var showEditDialog by remember { mutableStateOf<WebhookConfig?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showEditDialog = WebhookConfig(name = "New Webhook", url = "")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Webhook")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Webhook Integrations", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Forward events to your own servers, Discord, Slack, or home automation systems.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (webhooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No webhooks configured.\nTap + to create one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(webhooks) { webhook ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(webhook.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${webhook.method} ${webhook.url}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = {
                                        val data = Data.Builder()
                                            .putString("url", webhook.url)
                                            .putString("title", "Test Sender")
                                            .putString("message", "This is a test message from KjAiShield")
                                            .putString("type", "TEST")
                                            .putString("method", webhook.method)
                                            .putString("headersJson", webhook.headersJson)
                                            .putString("customPayload", webhook.customPayload)
                                            .build()
                                        val request = OneTimeWorkRequestBuilder<WebhookWorker>().setInputData(data).build()
                                        WorkManager.getInstance(context).enqueue(request)
                                        android.widget.Toast.makeText(context, "Test triggered", android.widget.Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Test", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = {
                                        showEditDialog = webhook
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                webhookRepo.deleteWebhook(webhook)
                                            }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showEditDialog != null) {
        EditWebhookDialog(
            webhook = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onSave = { updatedWebhook ->
                coroutineScope.launch {
                    webhookRepo.insertWebhook(updatedWebhook)
                }
                showEditDialog = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWebhookDialog(webhook: WebhookConfig, onDismiss: () -> Unit, onSave: (WebhookConfig) -> Unit) {
    var name by remember { mutableStateOf(webhook.name) }
    var url by remember { mutableStateOf(webhook.url) }
    var method by remember { mutableStateOf(webhook.method) }
    var headersJson by remember { mutableStateOf(webhook.headersJson) }
    var customPayload by remember { mutableStateOf(webhook.customPayload) }

    val applyPreset = { pName: String, pMethod: String, pPayload: String, pHeaders: String ->
        name = pName
        method = pMethod
        customPayload = pPayload
        headersJson = pHeaders
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
        title = { Text(if (webhook.url.isEmpty()) "New Webhook" else "Edit Webhook") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text("Quick Presets", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { applyPreset("Discord", "POST", "{\"content\": \"**{type}**\\n**From:** {sender}\\n**Message:** {message}\"}", "") },
                            label = { Text("Discord") }
                        )
                        SuggestionChip(
                            onClick = { applyPreset("Slack", "POST", "{\"text\": \"*{type}*\\n*From:* {sender}\\n*Message:* {message}\"}", "") },
                            label = { Text("Slack") }
                        )
                        SuggestionChip(
                            onClick = { applyPreset("Telegram", "POST", "{\"chat_id\": \"YOUR_CHAT_ID\", \"text\": \"<b>{type}</b>\\n<b>From:</b> {sender}\\n{message}\", \"parse_mode\": \"HTML\"}", "") },
                            label = { Text("Telegram") }
                        )
                        SuggestionChip(
                            onClick = { applyPreset("Home Assistant", "POST", "{\"title\": \"{sender}\", \"message\": \"{message}\"}", "{\"Authorization\": \"Bearer YOUR_TOKEN\", \"Content-Type\": \"application/json\"}") },
                            label = { Text("Home Assistant") }
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Configuration Name") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        label = { Text("Webhook URL") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                
                item {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = method, onValueChange = {}, readOnly = true,
                            label = { Text("HTTP Method") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("POST", "GET", "PUT", "PATCH").forEach { m ->
                                DropdownMenuItem(text = { Text(m) }, onClick = { method = m; expanded = false })
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = headersJson, onValueChange = { headersJson = it },
                        label = { Text("Custom Headers (JSON Dictionary)") },
                        placeholder = { Text("{\"Authorization\": \"Bearer token\"}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = customPayload, onValueChange = { customPayload = it },
                        label = { Text("Custom Payload / Request Body") },
                        placeholder = { Text("{\"content\": \"{sender}: {message}\"}") },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        supportingText = { Text("Available Variables:\n{sender} - Sender Name/Number\n{message} - Message content\n{type} - Event Type (SMS/CALL/RULE)\nLeave blank for default JSON.") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(webhook.copy(
                    name = name, url = url, method = method, headersJson = headersJson, customPayload = customPayload
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
