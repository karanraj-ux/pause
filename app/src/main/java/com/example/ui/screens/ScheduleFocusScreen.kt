package com.example.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ShieldApplication
import com.example.shield.CalendarSyncWorker
import com.example.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class DeviceCalendar(val id: String, val name: String, val account: String)

private data class UpcomingEvent(
    val title: String,
    val begin: Long,
    val end: Long,
    val calendarName: String,
    val willTrigger: Boolean
)

private fun enqueueCalendarWork(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "CalendarSync",
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
    )
}

private fun queryCalendars(context: Context): List<DeviceCalendar> {
    val result = mutableListOf<DeviceCalendar>()
    try {
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME
            ),
            null,
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
            val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                result.add(
                    DeviceCalendar(
                        id = cursor.getLong(idIdx).toString(),
                        name = cursor.getString(nameIdx) ?: "Calendar",
                        account = cursor.getString(accIdx) ?: ""
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e("ScheduleFocus", "Error reading calendars", e)
    }
    return result
}

private fun queryUpcomingEvents(
    context: Context,
    calendars: List<DeviceCalendar>,
    selectedIds: Set<String>,
    keywords: List<String>
): List<UpcomingEvent> {
    val result = mutableListOf<UpcomingEvent>()
    val now = System.currentTimeMillis()
    val windowEnd = now + 12 * 60 * 60 * 1000L
    val nameById = calendars.associate { it.id to it.name }
    try {
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, windowEnd)
        context.contentResolver.query(
            builder.build(),
            arrayOf(
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.AVAILABILITY
            ),
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val calIdIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
            val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val availIdx = cursor.getColumnIndex(CalendarContract.Instances.AVAILABILITY)
            while (cursor.moveToNext()) {
                val calId = cursor.getLong(calIdIdx).toString()
                if (selectedIds.isNotEmpty() && !selectedIds.contains(calId)) continue
                val title = cursor.getString(titleIdx) ?: "(No title)"
                val begin = cursor.getLong(beginIdx)
                val end = cursor.getLong(endIdx)
                val isAllDay = cursor.getInt(allDayIdx) == 1
                val availability = cursor.getInt(availIdx)
                result.add(
                    UpcomingEvent(
                        title = title,
                        begin = begin,
                        end = end,
                        calendarName = nameById[calId] ?: "Calendar",
                        willTrigger = CalendarSyncWorker.eventTriggersFocus(isAllDay, availability, title, keywords)
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e("ScheduleFocus", "Error reading upcoming events", e)
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleFocusScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsRepository = (context.applicationContext as ShieldApplication).container.settingsRepository
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(settingsRepository)
    )
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var hasCalendarPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }
    var calendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var upcoming by remember { mutableStateOf<List<UpcomingEvent>>(emptyList()) }

    val keywords = remember(uiState.calendarTriggerKeywords) {
        CalendarSyncWorker.parseKeywords(
            uiState.calendarTriggerKeywords.ifBlank { CalendarSyncWorker.DEFAULT_KEYWORDS }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCalendarPerm = granted
        if (granted) {
            settingsViewModel.updateCalendarSync(true)
            enqueueCalendarWork(context)
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        if (enabled) {
            if (hasCalendarPerm) {
                settingsViewModel.updateCalendarSync(true)
                enqueueCalendarWork(context)
            } else {
                permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        } else {
            settingsViewModel.updateCalendarSync(false)
            WorkManager.getInstance(context).cancelUniqueWork("CalendarSync")
        }
    }

    LaunchedEffect(hasCalendarPerm, uiState.calendarSyncIds, uiState.calendarTriggerKeywords) {
        if (hasCalendarPerm) {
            withContext(Dispatchers.IO) {
                val loaded = queryCalendars(context)
                calendars = loaded
                upcoming = queryUpcomingEvents(context, loaded, uiState.calendarSyncIds, keywords)
            }
        }
    }

    val timeFormat = remember { SimpleDateFormat("EEE, h:mm a", Locale.getDefault()) }
    val suggestions = listOf("Meeting", "Interview", "Focus", "Standup", "Review", "Call")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Master toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { setSyncEnabled(!uiState.calendarSync) }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Calendar Focus Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Automatically activate Focus Mode during your busy events, and switch it off when they end.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.calendarSync,
                        onCheckedChange = { setSyncEnabled(it) }
                    )
                }
            }
        }

        // 2. Calendar selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calendars to sync", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "No selection means all calendars. Pick specific ones like Work or Personal to be precise.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (!hasCalendarPerm) {
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                            Text("Grant calendar access")
                        }
                    } else if (calendars.isEmpty()) {
                        Text(
                            "No calendars found on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        calendars.forEach { cal ->
                            val selected = uiState.calendarSyncIds.contains(cal.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newSet = uiState.calendarSyncIds.toMutableSet()
                                        if (selected) newSet.remove(cal.id) else newSet.add(cal.id)
                                        settingsViewModel.updateCalendarSyncIds(newSet)
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = {
                                        val newSet = uiState.calendarSyncIds.toMutableSet()
                                        if (it) newSet.add(cal.id) else newSet.remove(cal.id)
                                        settingsViewModel.updateCalendarSyncIds(newSet)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cal.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (cal.account.isNotBlank()) {
                                        Text(cal.account, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Trigger rules
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("What triggers Focus Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Focus activates when an event is marked Busy, or when its title contains one of your keywords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { suggestion ->
                            val active = keywords.contains(suggestion.lowercase())
                            FilterChip(
                                selected = active,
                                onClick = {
                                    val newKeywords = if (active) keywords - suggestion.lowercase() else keywords + suggestion.lowercase()
                                    settingsViewModel.updateCalendarTriggerKeywords(newKeywords.joinToString(", "))
                                },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = uiState.calendarTriggerKeywords,
                        onValueChange = { settingsViewModel.updateCalendarTriggerKeywords(it) },
                        label = { Text("Trigger keywords") },
                        placeholder = { Text("meeting, interview, focus") },
                        supportingText = { Text("Comma-separated. Matching is case-insensitive.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }

        // 4. Upcoming events live preview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF25D366), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live preview — next 12 hours", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    if (!hasCalendarPerm) {
                        Text(
                            "Grant calendar access to see which upcoming events will trigger Focus Mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (upcoming.isEmpty()) {
                        Text(
                            "No events in the next 12 hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        upcoming.forEach { event ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(event.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${timeFormat.format(event.begin)} – ${timeFormat.format(event.end)} • ${event.calendarName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (event.willTrigger) {
                                    Surface(
                                        color = Color(0xFF25D366).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "Will trigger Focus",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0B7A3E),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "Skipped",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    Text(
                        "Auto-reply sent while Focus is on:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val vipMsg = uiState.vipReplyMsg.ifBlank { "Hey, my phone is on silent. If this is an emergency, reply URGENT." }
                    val stdMsg = uiState.standardReplyMsg.ifBlank { "Hi, I am currently focused or away. I will get back to you as soon as I can." }
                    val unkMsg = uiState.unknownReplyMsg.ifBlank { "I am currently in Focus Mode. I will get back to you shortly." }
                    Text("VIP: $vipMsg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Saved: $stdMsg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Unknown: $unkMsg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 5. Back link
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.popBackStack() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Back to Settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
