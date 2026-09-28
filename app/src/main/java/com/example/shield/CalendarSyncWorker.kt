package com.example.shield

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ShieldApplication
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CalendarSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        /** Default trigger keywords when the user has not customized them. */
        const val DEFAULT_KEYWORDS = "meeting,interview,focus"

        /**
         * Shared trigger rule: an event activates Focus Mode when it is not an
         * all-day event AND (it is marked Busy OR its title contains one of the
         * trigger keywords). Used by both the worker and the Schedule & Focus UI.
         */
        fun eventTriggersFocus(
            isAllDay: Boolean,
            availability: Int,
            title: String?,
            keywords: List<String>
        ): Boolean {
            if (isAllDay) return false
            if (availability == CalendarContract.Instances.AVAILABILITY_BUSY) return true
            val lowerTitle = title?.lowercase().orEmpty()
            return keywords.any { kw -> kw.isNotBlank() && lowerTitle.contains(kw) }
        }

        fun parseKeywords(raw: String): List<String> =
            raw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
        val isSyncEnabled = settingsRepo.calendarSync.first()

        if (!isSyncEnabled) {
            return@withContext Result.success()
        }

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CalendarSyncWorker", "READ_CALENDAR permission not granted.")
            return@withContext Result.success() // Can't do anything without permission
        }

        try {
            val now = System.currentTimeMillis()
            val selectedIds = settingsRepo.calendarSyncIds.first()
            val keywords = parseKeywords(settingsRepo.calendarTriggerKeywords.first().ifBlank { DEFAULT_KEYWORDS })

            // Build URI for instances happening RIGHT NOW (between now-1min and now+1min)
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            android.content.ContentUris.appendId(builder, now - 60000)
            android.content.ContentUris.appendId(builder, now + 60000)

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.AVAILABILITY
            )

            val cursor = applicationContext.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                null
            )

            var hasActiveMeeting = false

            cursor?.use {
                val calIdIdx = it.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val availIdx = it.getColumnIndex(CalendarContract.Instances.AVAILABILITY)

                while (it.moveToNext()) {
                    // Skip calendars the user did not select (empty selection = all calendars)
                    if (selectedIds.isNotEmpty() && calIdIdx >= 0) {
                        val calId = it.getLong(calIdIdx).toString()
                        if (!selectedIds.contains(calId)) continue
                    }

                    val begin = if (beginIdx >= 0) it.getLong(beginIdx) else 0L
                    val end = if (endIdx >= 0) it.getLong(endIdx) else 0L
                    val title = if (titleIdx >= 0) it.getString(titleIdx) else null
                    val isAllDay = if (allDayIdx >= 0) it.getInt(allDayIdx) == 1 else false
                    val availability = if (availIdx >= 0) it.getInt(availIdx) else CalendarContract.Instances.AVAILABILITY_BUSY

                    // Check if it's actually happening right now
                    if (now in begin..end) {
                        if (eventTriggersFocus(isAllDay, availability, title, keywords)) {
                            hasActiveMeeting = true
                            break
                        }
                    }
                }
            }

            val isGhostModeActive = settingsRepo.ghostMode.first()
            val wasActivatedByCalendar = settingsRepo.calendarGhostModeActive.first()

            if (hasActiveMeeting) {
                if (!isGhostModeActive) {
                    Log.d("CalendarSyncWorker", "Triggering calendar event found. Activating Ghost Mode.")
                    settingsRepo.updateBoolean(SettingsRepository.GHOST_MODE, true)
                    settingsRepo.updateBoolean(SettingsRepository.CALENDAR_GHOST_MODE_ACTIVE, true)
                }
            } else {
                // No triggering event right now. If WE turned it on, turn it off.
                if (wasActivatedByCalendar) {
                    Log.d("CalendarSyncWorker", "Triggering event ended. Deactivating Ghost Mode.")
                    settingsRepo.updateBoolean(SettingsRepository.GHOST_MODE, false)
                    settingsRepo.updateBoolean(SettingsRepository.CALENDAR_GHOST_MODE_ACTIVE, false)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CalendarSyncWorker", "Error syncing calendar", e)
            Result.failure()
        }
    }
}
