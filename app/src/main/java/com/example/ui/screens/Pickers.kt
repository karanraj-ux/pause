package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun contactPickerLauncher(onNumberPicked: (String) -> Unit): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val number = cursor.getString(0)
                        onNumberPicked(number.replace(Regex("[^0-9+]"), ""))
                    }
                }
            }
        }
    }

    return {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        launcher.launch(intent)
    }
}

fun getRecentSmsMessages(context: Context): List<Pair<String, String>> {
    val messages = mutableListOf<Pair<String, String>>()
    try {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
            null, null, Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT 20"
        )
        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            while (it.moveToNext()) {
                messages.add(Pair(it.getString(addressIndex) ?: "Unknown", it.getString(bodyIndex) ?: ""))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return messages
}

@Composable
fun SmsPickerDialog(onDismiss: () -> Unit, onMessageSelected: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val messages = remember { getRecentSmsMessages(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a Recent Message") },
        text = {
            if (messages.isEmpty()) {
                Text("No messages found or permission denied.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(messages) { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onMessageSelected(msg.second) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.first, style = MaterialTheme.typography.labelMedium)
                                Text(msg.second, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
