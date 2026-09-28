package com.example.shield

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BotInterpretationResult(
    val replyText: String,
    val commandType: String,
    val triggeredEmergencyAlert: Boolean = false
)

object WhatsAppBotInterpreter {

    private const val EMERGENCY_CHANNEL_ID = "emergency_urgent_alerts"
    private const val TAG = "WhatsAppBotInterpreter"

    fun interpretMessage(
        context: Context,
        sender: String,
        messageText: String,
        settingsRepo: SettingsRepository
    ): BotInterpretationResult? {
        val trimmed = messageText.trim()
        val lower = trimmed.lowercase()

        // 1. Emergency Protocol: #urgent, URGENT, or EMERGENCY
        val isUrgentCommand = lower == "#urgent" || lower.startsWith("#urgent ") ||
                lower == "#emergency" || lower.startsWith("#emergency ") ||
                trimmed.equals("URGENT", ignoreCase = false) ||
                trimmed.equals("EMERGENCY", ignoreCase = false) ||
                trimmed.contains("URGENT!!")

        if (isUrgentCommand) {
            triggerUrgentAlarm(context, sender, trimmed)
            val attachLoc = settingsRepo.getBooleanSync(SettingsRepository.APPEND_LOCATION_TO_EMERGENCY, true)
            val locSnippet = if (attachLoc) {
                val locLink = MessageDispatcherHelper.getLocationLink(context, settingsRepo)
                "\n📍 User Location: $locLink"
            } else ""

            return BotInterpretationResult(
                replyText = "⚠️ [EMERGENCY ALERT DELIVERED]\nYour urgent message bypassed silent mode and sounded the phone alarm. The user has been notified with high priority.$locSnippet",
                commandType = "URGENT",
                triggeredEmergencyAlert = true
            )
        }

        // 2. Location Command: #location, #loc, or #where
        if (lower == "#location" || lower.startsWith("#location ") || lower == "#loc" || lower.startsWith("#loc ") || lower == "#where") {
            val locationSharingAllowed = settingsRepo.getBooleanSync(SettingsRepository.LOCATION_AUTO_SHARE_ENABLED, false)
            val reply = if (locationSharingAllowed) {
                val locLink = MessageDispatcherHelper.getLocationLink(context, settingsRepo)
                val locName = settingsRepo.getStringSync(SettingsRepository.SAVED_LOCATION_NAME, "")
                val nameHeader = if (locName.isNotBlank()) " ($locName)" else ""
                "📍 [Location Pin$nameHeader]\nGoogle Maps: $locLink\n(Shared securely on-device via Shield)"
            } else {
                "📍 Location auto-sharing is currently disabled in Shield. Please request the user directly."
            }
            return BotInterpretationResult(
                replyText = reply,
                commandType = "LOCATION"
            )
        }

        // 3. Photo / Image / Catalog Command: #photo, #image, #qr, #menu, #catalog
        if (lower == "#photo" || lower.startsWith("#photo ") || lower == "#image" || lower == "#qr" || lower == "#menu" || lower == "#catalog") {
            val photoUrl = settingsRepo.getStringSync(SettingsRepository.PHOTO_LINK_URL, "")
            val photoLabel = settingsRepo.getStringSync(SettingsRepository.PHOTO_LINK_LABEL, "Shared Media")
            val reply = if (photoUrl.isNotBlank()) {
                "🖼️ [$photoLabel]\nView / Download: $photoUrl"
            } else {
                "🖼️ No photo or catalog link configured yet. Check back soon!"
            }
            return BotInterpretationResult(
                replyText = reply,
                commandType = "PHOTO"
            )
        }

        // 4. Quick Links Command: #link, #links, #website, #site, #pay
        if (lower == "#link" || lower.startsWith("#link ") || lower == "#links" || lower == "#website" || lower == "#site" || lower == "#pay") {
            val quickLinks = settingsRepo.getStringSync(SettingsRepository.QUICK_LINKS, "")
            val reply = if (quickLinks.isNotBlank()) {
                val formatted = quickLinks.split(";").filter { it.isNotBlank() }.joinToString("\n") { rule ->
                    if (rule.contains("->")) {
                        val p = rule.split("->", limit = 2)
                        "• ${p[0].trim().replaceFirstChar { it.uppercase() }}: ${p[1].trim()}"
                    } else "• $rule"
                }
                "🔗 [Quick Links]\n$formatted"
            } else {
                "🔗 No quick links configured. Message the user directly for specific URLs."
            }
            return BotInterpretationResult(
                replyText = reply,
                commandType = "LINKS"
            )
        }

        // 5. Quiet Hours / DND Info: #dnd or #quiet
        if (lower == "#dnd" || lower.startsWith("#dnd ") || lower == "#quiet") {
            val isSleep = settingsRepo.getBooleanSync(SettingsRepository.SLEEP_MODE_ENABLED, false)
            val startHour = settingsRepo.getIntSync(SettingsRepository.SLEEP_START_HOUR, 22)
            val endHour = settingsRepo.getIntSync(SettingsRepository.SLEEP_END_HOUR, 7)
            val timeRange = String.format("%02d:00 - %02d:00", startHour, endHour)

            return BotInterpretationResult(
                replyText = "🔕 [Quiet Hours Info]\nQuiet schedule: $timeRange (${if (isSleep) "Enabled" else "Disabled"}).\nTo bypass silent mode in a true emergency, message #urgent.",
                commandType = "DND_INFO"
            )
        }

        // No command matched -> return null to allow normal auto-reply logic
        return null
    }

    private fun triggerUrgentAlarm(context: Context, sender: String, originalMessage: String) {
        Log.w(TAG, "URGENT keyword received from $sender! Triggering full emergency alarm & DND bypass.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Maximize Alarm Audio Stream Volume
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

                // 2. Play Alarm Sound
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                ringtone?.let {
                    it.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    it.play()

                    // Play for 15 seconds then stop
                    launch {
                        delay(15000)
                        try {
                            if (it.isPlaying) it.stop()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error stopping ringtone", e)
                        }
                    }
                }

                // 3. Trigger Emergency Vibration Pattern
                try {
                    val pattern = longArrayOf(0, 600, 200, 600, 200, 1000)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vib?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vib?.vibrate(pattern, -1)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Vibration failed", e)
                }

                // 4. Post High Priority Notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        EMERGENCY_CHANNEL_ID,
                        "Emergency Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "High-priority alarms from verified contacts & urgent keywords"
                        enableVibration(true)
                        enableLights(true)
                    }
                    nm.createNotificationChannel(channel)
                }

                val notif = NotificationCompat.Builder(context, EMERGENCY_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("🚨 EMERGENCY ALERT: $sender")
                    .setContentText(originalMessage)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("Emergency command received via WhatsApp from $sender:\n\n$originalMessage"))
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .build()

                nm.notify(sender.hashCode(), notif)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute emergency alarm protocol", e)
            }
        }
    }
}
