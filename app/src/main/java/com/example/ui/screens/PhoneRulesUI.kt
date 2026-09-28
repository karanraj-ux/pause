package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PhoneRuleEntity
import com.example.ui.viewmodels.PhoneRuleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PhoneRulesUI(ruleViewModel: PhoneRuleViewModel) {
    val rules by ruleViewModel.rules.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rule Numbers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Text("Add Number")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        if (rules.isEmpty()) {
            Text("No rules configured. Add a number to set their Relationship Tier.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            rules.forEach { rule ->
                key(rule.id) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if(rule.contactName.isNotBlank()) "${rule.contactName} (${rule.phoneNumber})" else rule.phoneNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                val tierStr = rule.relationshipTier
                                val badges = mutableListOf<String>()
                                if (tierStr == "Inner Circle" || rule.isVip) badges.add("Inner Circle (Always Rings)")
                                else if (tierStr == "Standard") badges.add("Standard (Auto-reply in DND)")
                                else if (tierStr == "Muted") badges.add("Muted (Always Silenced)")
                                else if (tierStr == "Blocked") badges.add("Blocked (Always Rejected)")
                                else badges.add(tierStr)
                                
                                if (rule.isDivert) badges.add("Forward to Secondary")
                                
                                Text(badges.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { ruleViewModel.removeRule(rule) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        var phoneNumber by remember { mutableStateOf("") }
        var contactName by remember { mutableStateOf("") }
        var selectedTier by remember { mutableStateOf("Standard") }
        var isDivert by remember { mutableStateOf(false) }
        
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        
        val contactPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickContact()
        ) { uri ->
            if (uri != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.query(
                            uri,
                            arrayOf(ContactsContract.Contacts.HAS_PHONE_NUMBER, ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                                
                                val hasPhone = if (hasPhoneIndex >= 0) cursor.getString(hasPhoneIndex) else "0"
                                val id = if (idIndex >= 0) cursor.getString(idIndex) else ""
                                val cName = if (nameIndex >= 0) cursor.getString(nameIndex) else ""
                                
                                if (hasPhone == "1" && id.isNotEmpty()) {
                                    context.contentResolver.query(
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                        null,
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                        arrayOf(id),
                                        null
                                    )?.use { phones ->
                                        if (phones.moveToFirst()) {
                                            val numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                            if (numberIndex >= 0) {
                                                val number = phones.getString(numberIndex)
                                                withContext(Dispatchers.Main) {
                                                    phoneNumber = number ?: ""
                                                    contactName = cName ?: ""
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

        val requestContactsPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                contactPickerLauncher.launch(null)
            }
        }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Categorize Contact", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "How should this contact be handled?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                    contactPickerLauncher.launch(null)
                                } else {
                                    requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            }) {
                                Icon(Icons.Default.Contacts, contentDescription = "Pick Contact")
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Relationship Tier:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    
                    val tiers = listOf(
                        "Inner Circle" to "Always rings, even when sleeping or in meetings.",
                        "Standard" to "Silenced during DND. Receives auto-reply.",
                        "Muted" to "Ignored. Phone won't ring, but they can leave voicemail.",
                        "Blocked" to "Call is immediately rejected (hung up)."
                    )
                    
                    tiers.forEach { (tier, desc) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (selectedTier == tier) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            onClick = { selectedTier = tier }
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedTier == tier, onClick = { selectedTier = tier })
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(tier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDivert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { isDivert = !isDivert }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isDivert, onCheckedChange = { isDivert = it })
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text("Forward to Secondary Phone", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text("Instantly forward this caller to your secondary phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val snackbarHostState = com.example.LocalSnackbarHostState.current
                val scope = rememberCoroutineScope()
                FilledTonalButton(onClick = {
                    if (phoneNumber.isNotBlank()) {
                        ruleViewModel.addRule(phoneNumber, contactName, selectedTier, isDivert)
                        scope.launch { snackbarHostState.showSnackbar("Rule Saved Successfully!") }
                        showAddDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
