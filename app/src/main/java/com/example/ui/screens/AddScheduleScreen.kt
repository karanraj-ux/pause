package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleScreen(
    onDismiss: () -> Unit,
    onSave: (type: String, target: String, message: String?, timeMillis: Long, isRecurring: Boolean, intervalMillis: Long) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf("SMS") }
    var target by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    var showSmsPicker by remember { mutableStateOf(false) }
    
    var repeatOption by remember { mutableStateOf("None") }
    var expandedRepeat by remember { mutableStateOf(false) }
    val repeatOptions = listOf("None", "Daily", "Weekly")

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val timePickerState = rememberTimePickerState()

    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedMinute by remember { mutableStateOf<Int?>(null) }

    val dateStr = selectedDateMillis?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select Date"
    val timeStr = if (selectedHour != null && selectedMinute != null) {
        String.format("%02d:%02d", selectedHour, selectedMinute)
    } else {
        "Select Time"
    }

    val contactPicker = contactPickerLauncher { number -> target = number }
    
    fun buildFinalMessage(): String? {
        if (type == "Call") return null
        val fullMsg = message.trim()
        return if (fullMsg.isNotEmpty()) fullMsg else null
    }

    val sendSmsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            if ((type == "Silent Guard" || type == "Ghost Mode" || target.isNotBlank()) && selectedDateMillis != null && selectedHour != null) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis!!
                    set(Calendar.HOUR_OF_DAY, selectedHour!!)
                    set(Calendar.MINUTE, selectedMinute!!)
                    set(Calendar.SECOND, 0)
                }
                onSave(
                    if (type == "Silent Guard") "Silent Guard" else type,
                    target,
                    buildFinalMessage(),
                    cal.timeInMillis,
                    repeatOption != "None",
                    if(repeatOption == "Daily") 86400000L else if(repeatOption == "Weekly") 604800000L else 0L
                )
            }
        }
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showSmsPicker = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Schedule a Task", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    color = Color.Transparent
                ) {
                    FilledTonalButton(
                        onClick = {
                            if ((type == "Silent Guard" || type == "Ghost Mode" || target.isNotBlank()) && selectedDateMillis != null && selectedHour != null) {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateMillis!!
                                    set(Calendar.HOUR_OF_DAY, selectedHour!!)
                                    set(Calendar.MINUTE, selectedMinute!!)
                                    set(Calendar.SECOND, 0)
                                }
                                
                                val finalMsg = buildFinalMessage()
                                val saveType = if (type == "Silent Guard") "Silent Guard" else type
                                
                                if (type == "SMS") {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                                        sendSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                    } else {
                                        onSave(saveType, target, finalMsg, cal.timeInMillis, repeatOption != "None", if(repeatOption == "Daily") 86400000L else if(repeatOption == "Weekly") 604800000L else 0L)
                                    }
                                } else {
                                    onSave(saveType, target, finalMsg, cal.timeInMillis, repeatOption != "None", if(repeatOption == "Daily") 86400000L else if(repeatOption == "Weekly") 604800000L else 0L)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = (type == "Silent Guard" || type == "Ghost Mode" || target.isNotBlank()) && selectedDateMillis != null && selectedHour != null
                    ) {
                        Text("Schedule Task", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Select Task Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = type == "SMS",
                        onClick = { type = "SMS" },
                        label = { Text("SMS") },
                        leadingIcon = { if (type == "SMS") Icon(Icons.Default.Message, null) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "Call",
                        onClick = { type = "Call" },
                        label = { Text("Call") },
                        leadingIcon = { if (type == "Call") Icon(Icons.Default.Call, null) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "WhatsApp",
                        onClick = { type = "WhatsApp" },
                        label = { Text("WhatsApp") },
                        leadingIcon = { if (type == "WhatsApp") Icon(Icons.Default.ChatBubble, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = type == "Silent Guard" || type == "Ghost Mode",
                        onClick = { type = "Silent Guard" },
                        label = { Text("Silent Guard") },
                        leadingIcon = { if (type == "Silent Guard" || type == "Ghost Mode") Icon(Icons.Default.Lock, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (type != "Silent Guard" && type != "Ghost Mode") {
                    Text("Recipient Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { contactPicker() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = "Pick Contact")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contact")
                    }
                    
                    if (type != "Call") {
                        FilledTonalButton(
                            onClick = { 
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                                    showSmsPicker = true
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = "Pick SMS")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import SMS")
                        }
                    }
                }
                
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text(if (type == "Call") "Phone Number to Call" else "Target Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                
                if (type != "Call") {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text(if (type == "WhatsApp") "WhatsApp Message Caption" else "SMS Message") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5
                    )
                }
                
                }
                Text("Date & Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(dateStr, style = MaterialTheme.typography.bodyLarge)
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(timeStr, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                
                Text("Repeat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(
                    expanded = expandedRepeat,
                    onExpandedChange = { expandedRepeat = !expandedRepeat }
                ) {
                    OutlinedTextField(
                        value = repeatOption,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRepeat) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRepeat,
                        onDismissRequest = { expandedRepeat = false }
                    ) {
                        repeatOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    repeatOption = selectionOption
                                    expandedRepeat = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
    
    if (showSmsPicker) {
        SmsPickerDialog(
            onDismiss = { showSmsPicker = false },
            onMessageSelected = { msg ->
                message = msg
                showSmsPicker = false
            }
        )
    }
}
