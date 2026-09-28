package com.example.calls

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.util.Log
import com.example.ShieldApplication
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TruecallerNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
        val packageName = sbn.packageName ?: ""

        // 1. OMNI-SHIELD: Threat Matrix Analysis (Ghost Wipe)
        val shouldGhostWipe = com.example.shield.ThreatMatrixEngine.checkNotificationForThreat(
            this, title, text, packageName
        )
        if (shouldGhostWipe) {
            cancelNotification(sbn.key)
            Log.w("TruecallerNL", "Ghost Wipe executed for high-threat notification from $packageName")
            return
        }

        // 2. OMNI-SHIELD: WhatsApp Call Detection & DND Handling + Chat Auto-Reply
        if (packageName.contains("whatsapp", ignoreCase = true)) {
            if (isWhatsAppCall(sbn)) {
                com.example.shield.ThreatMatrixEngine.onWhatsAppCallStarted(this, title)
                handleWhatsAppCall(sbn, title)
            } else {
                handleWhatsAppMessage(sbn, title, text)
            }
        }

        // Check if Smart Spam Reader is enabled
        val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
        val smartSpamEnabled = settingsRepo.getBooleanSync(SettingsRepository.SMART_SPAM_READER, false)
        
        if (!smartSpamEnabled) return

        if (packageName.contains("truecaller", ignoreCase = true)) {
            val lowerTitle = title.lowercase()
            val lowerText = text.lowercase()
            
            Log.d("TruecallerNL", "Truecaller notification posted: Title=$title, Text=$text")
            val isSpam = lowerTitle.contains("spam") || lowerText.contains("spam") || lowerTitle.contains("spammer") || lowerText.contains("spammer")
            
            if (isSpam) {
                Log.d("TruecallerNL", "Spam signature detected via Notification Listener! Firing endCall().")
                rejectCall(settingsRepo)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val packageName = sbn.packageName ?: ""
        if (packageName.contains("whatsapp", ignoreCase = true) && isWhatsAppCall(sbn)) {
            CallHandlingManager.restoreAudioState(this)
        }
    }

    private fun isWhatsAppCall(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        if (notification.category == android.app.Notification.CATEGORY_CALL) return true

        val extras = notification.extras
        val text = (extras.getString(android.app.Notification.EXTRA_TEXT) ?: "").lowercase()
        val title = (extras.getString(android.app.Notification.EXTRA_TITLE) ?: "").lowercase()
        val subText = (extras.getString(android.app.Notification.EXTRA_SUB_TEXT) ?: "").lowercase()

        val callKeywords = listOf("call", "incoming", "ringing", "audio", "video", "calling")
        val matchesKeyword = callKeywords.any { text.contains(it) || title.contains(it) || subText.contains(it) }

        val actions = notification.actions
        val hasCallActions = actions != null && actions.any { action ->
            val label = action.title?.toString()?.lowercase() ?: ""
            label.contains("decline") || label.contains("reject") || label.contains("dismiss") || label.contains("answer")
        }

        return matchesKeyword || hasCallActions
    }

    private fun isFocusModeActive(settingsRepo: SettingsRepository): Boolean {
        val masterKill = settingsRepo.getBooleanSync(SettingsRepository.MASTER_KILL_SWITCH, false)
        if (masterKill) return false

        val pauseEndTime = settingsRepo.getLongSync(SettingsRepository.GHOST_MODE_PAUSE_END_TIME, 0L)
        if (pauseEndTime > System.currentTimeMillis()) return false

        val ghostMode = settingsRepo.getBooleanSync(SettingsRepository.GHOST_MODE, false)
        val calendarGhost = settingsRepo.getBooleanSync(SettingsRepository.CALENDAR_GHOST_MODE_ACTIVE, false)
        val sleepGhost = settingsRepo.getBooleanSync(SettingsRepository.SLEEP_GHOST_MODE_ACTIVE, false)
        val overrideDnd = settingsRepo.getBooleanSync(SettingsRepository.OVERRIDE_DND, false)

        return ghostMode || calendarGhost || sleepGhost || overrideDnd
    }

    private fun isStarredOrChosenContact(callerTitle: String, settingsRepo: SettingsRepository): Boolean {
        if (callerTitle.isBlank()) return false

        // 1. Check user's selected VIP callers in settings
        val vipString = settingsRepo.getStringSync(SettingsRepository.VIP_CALLERS, "")
        if (vipString.isNotBlank()) {
            val vips = vipString.split(",")
            for (vip in vips) {
                val cleanVip = vip.trim()
                if (cleanVip.isBlank()) continue
                val namePart = if (cleanVip.contains("(")) cleanVip.substringBefore("(").trim() else cleanVip
                val numPart = if (cleanVip.contains("(") && cleanVip.contains(")")) 
                    cleanVip.substringAfter("(").substringBefore(")").replace(Regex("[^0-9+]"), "")
                else cleanVip.replace(Regex("[^0-9+]"), "")

                if (namePart.isNotBlank() && callerTitle.contains(namePart, ignoreCase = true)) {
                    return true
                }
                val callerDigits = callerTitle.replace(Regex("[^0-9]"), "")
                val numDigits = numPart.replace(Regex("[^0-9]"), "")
                if (numDigits.length >= 7 && callerDigits.length >= 7) {
                    if (callerDigits.endsWith(numDigits) || numDigits.endsWith(callerDigits)) {
                        return true
                    }
                }
            }
        }

        // 2. Query Android Contacts database for STARRED contact
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                val cleanDigits = callerTitle.replace(Regex("[^0-9]"), "")
                if (cleanDigits.length >= 6) {
                    val uri = android.net.Uri.withAppendedPath(
                        android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        android.net.Uri.encode(callerTitle)
                    )
                    contentResolver.query(
                        uri,
                        arrayOf(android.provider.ContactsContract.PhoneLookup.STARRED),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.STARRED)
                            if (idx >= 0 && cursor.getInt(idx) == 1) {
                                return true
                            }
                        }
                    }
                }

                contentResolver.query(
                    android.provider.ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(android.provider.ContactsContract.Contacts.STARRED),
                    "${android.provider.ContactsContract.Contacts.DISPLAY_NAME} = ? OR ${android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ?",
                    arrayOf(callerTitle, callerTitle),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts.STARRED)
                        if (idx >= 0 && cursor.getInt(idx) == 1) {
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("TruecallerNL", "Error checking starred contact for $callerTitle", e)
            }
        }

        return false
    }

    private fun handleWhatsAppCall(sbn: StatusBarNotification, callerTitle: String) {
        val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
        if (!isFocusModeActive(settingsRepo)) {
            Log.d("TruecallerNL", "Focus Mode is inactive; WhatsApp call allowed through: $callerTitle")
            return
        }

        val isStarred = isStarredOrChosenContact(callerTitle, settingsRepo)

        if (isStarred) {
            Log.d("TruecallerNL", "WhatsApp call from Starred Contact: $callerTitle. Bypassing DND and silent mode!")
            CallHandlingManager.bypassSilentForWhatsAppVip(this, callerTitle)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val appDb = (applicationContext as ShieldApplication).container.database
                    appDb.smsLogDao().insert(
                        com.example.data.SmsLogEntity(
                            timestamp = System.currentTimeMillis(),
                            sender = callerTitle.ifBlank { "Starred Contact" },
                            message = "WhatsApp Call Allowed (Starred VIP DND Bypass)",
                            targetNumber = "",
                            status = "DND_BYPASS"
                        )
                    )
                } catch (e: Exception) {
                    Log.e("TruecallerNL", "Failed to log WhatsApp VIP bypass", e)
                }
            }
            return
        }

        // Unknown Number / Unstarred Call during Focus Mode: Reject the call immediately
        try {
            val actions = sbn.notification.actions
            var declined = false
            if (actions != null) {
                for (action in actions) {
                    val actionLabel = action.title?.toString()?.lowercase() ?: ""
                    if (actionLabel.contains("decline") || actionLabel.contains("reject") || actionLabel.contains("dismiss") || actionLabel.contains("hang")) {
                        action.actionIntent.send()
                        declined = true
                        Log.d("TruecallerNL", "WhatsApp call rejected via notification action for: $callerTitle")
                        break
                    }
                }
            }
            cancelNotification(sbn.key)
            Log.d("TruecallerNL", "WhatsApp call notification dismissed for non-starred: $callerTitle (declined=$declined)")

            CoroutineScope(Dispatchers.IO).launch {
                settingsRepo.incrementSpamBlockedCount()
                try {
                    val appDb = (applicationContext as ShieldApplication).container.database
                    appDb.smsLogDao().insert(
                        com.example.data.SmsLogEntity(
                            timestamp = System.currentTimeMillis(),
                            sender = callerTitle.ifBlank { "Unknown WhatsApp Number" },
                            message = if (declined) "WhatsApp Call Rejected (Unknown Number - Focus Mode)" else "WhatsApp Call Silenced (Focus Mode)",
                            targetNumber = "",
                            status = "CALL_DEFLECTED"
                        )
                    )
                } catch (e: Exception) {
                    Log.e("TruecallerNL", "Failed to log WhatsApp deflection", e)
                }
            }
        } catch (e: Exception) {
            Log.e("TruecallerNL", "Error handling WhatsApp call", e)
        }
    }

    private fun rejectCall(settingsRepo: SettingsRepository) {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION") telecomManager.endCall()
                Log.d("TruecallerNL", "Call successfully rejected via TelecomManager.")
                
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { 
                    settingsRepo.incrementSpamBlockedCount() 
                    try {
                        val appDb = (applicationContext as ShieldApplication).container.database
                        appDb.smsLogDao().insert(com.example.data.SmsLogEntity(
                            timestamp = System.currentTimeMillis(),
                            sender = "Unknown Truecaller Caller",
                            message = "Spam Call Blocked (Notification)",
                            targetNumber = "",
                            status = "SPAM_BLOCKED"
                        ))
                    } catch (e: Exception) {
                        Log.e("TruecallerNL", "Failed to log spam block", e)
                    }
                }
            } else {
                Log.e("TruecallerNL", "Missing ANSWER_PHONE_CALLS permission.")
            }
        } catch (e: Exception) {
            Log.e("TruecallerNL", "Failed to reject call", e)
        }
    }

    private fun isKnownSavedContact(callerTitle: String): Boolean {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return false
        }
        return try {
            val cleanDigits = callerTitle.replace(Regex("[^0-9]"), "")
            if (cleanDigits.length >= 6) {
                val uri = android.net.Uri.withAppendedPath(
                    android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    android.net.Uri.encode(callerTitle)
                )
                contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) return true
                }
            }
            val cursor = contentResolver.query(
                android.provider.ContactsContract.Contacts.CONTENT_URI,
                arrayOf(android.provider.ContactsContract.Contacts._ID),
                "${android.provider.ContactsContract.Contacts.DISPLAY_NAME} = ? OR ${android.provider.ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ?",
                arrayOf(callerTitle, callerTitle),
                null
            )
            val exists = cursor?.moveToFirst() == true
            cursor?.close()
            exists
        } catch (e: Exception) {
            false
        }
    }

    private fun handleWhatsAppMessage(sbn: StatusBarNotification, senderTitle: String, messageText: String) {
        val settingsRepo = (applicationContext as ShieldApplication).container.settingsRepository
        val masterKill = settingsRepo.getBooleanSync(SettingsRepository.MASTER_KILL_SWITCH, false)
        if (masterKill) return

        val focusActive = isFocusModeActive(settingsRepo)
        val autoReplyWhatsapp = settingsRepo.getBooleanSync(SettingsRepository.AUTO_RESPOND_WHATSAPP, false)

        if (!focusActive && !autoReplyWhatsapp) return
        if (senderTitle.isBlank() || messageText.isBlank()) return
        if (messageText.startsWith("You:", ignoreCase = true)) return

        val cleanSenderKey = senderTitle.replace(Regex("[^a-zA-Z0-9+]"), "").lowercase()
        val prefs = getSharedPreferences("whatsapp_reply_history", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastTime = prefs.getLong("time_$cleanSenderKey", 0L)
        val count = prefs.getInt("count_$cleanSenderKey", 0)

        // Emergency Keyword Override
        val trimmed = messageText.trim()
        val lower = trimmed.lowercase()
        val isEmergency = lower == "#urgent" || lower.startsWith("#urgent ") ||
                lower == "#emergency" || lower.startsWith("#emergency ") ||
                trimmed.equals("URGENT", ignoreCase = false) ||
                trimmed.equals("EMERGENCY", ignoreCase = false)

        if (isEmergency) {
            com.example.shield.WhatsAppBotInterpreter.interpretMessage(this, senderTitle, messageText, settingsRepo)
        }

        // Loop prevention: Max 3 auto-replies per contact per hour
        if (!isEmergency) {
            if (now - lastTime > 3600000) {
                prefs.edit().putLong("time_$cleanSenderKey", now).putInt("count_$cleanSenderKey", 1).apply()
            } else {
                if (count >= 3) {
                    Log.d("TruecallerNL", "WhatsApp rate limit reached for $senderTitle")
                    return
                }
                prefs.edit().putInt("count_$cleanSenderKey", count + 1).putLong("time_$cleanSenderKey", now).apply()
            }
        }

        // Select chosen auto-reply message based on recipient relationship
        val isStarred = isStarredOrChosenContact(senderTitle, settingsRepo)
        val isKnownContact = isStarred || isKnownSavedContact(senderTitle)

        val replyText = if (isEmergency) {
            "⚠️ [EMERGENCY ALERT DELIVERED]\nYour urgent message bypassed silent mode and sounded the phone alarm. The user has been alerted."
        } else if (isStarred) {
            settingsRepo.getStringSync(SettingsRepository.VIP_REPLY_MSG, "Hey, my phone is on silent. If this is an emergency, message URGENT.")
        } else if (!isKnownContact) {
            settingsRepo.getStringSync(SettingsRepository.UNKNOWN_REPLY_MSG, "I am currently in Focus Mode and do not take unsaved incoming messages right away. I will get back to you shortly.")
        } else {
            settingsRepo.getStringSync(SettingsRepository.STANDARD_REPLY_MSG, "Hi, I am currently focused or away. I will get back to you as soon as I can.")
        }

        val logStatus = if (isEmergency) "WHATSAPP_EMERGENCY" else if (isStarred) "WHATSAPP_VIP_REPLY" else "WHATSAPP_AUTO_REPLY"

        // Send chosen reply directly over internet via WhatsApp RemoteInput
        val notification = sbn.notification
        val actions = notification.actions
        if (actions != null) {
            for (action in actions) {
                val remoteInputs = action.remoteInputs
                if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                    for (remoteInput in remoteInputs) {
                        val replyBundle = android.os.Bundle()
                        replyBundle.putCharSequence(remoteInput.resultKey, replyText)
                        val replyIntent = android.content.Intent()
                        android.app.RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, replyBundle)
                        try {
                            action.actionIntent.send(this, 0, replyIntent)
                            Log.d("TruecallerNL", "WhatsApp auto-reply sent to $senderTitle over internet: $replyText")

                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val appDb = (applicationContext as ShieldApplication).container.database
                                    appDb.smsLogDao().insert(
                                        com.example.data.SmsLogEntity(
                                            timestamp = System.currentTimeMillis(),
                                            sender = senderTitle,
                                            message = "WhatsApp Auto-Reply: $replyText",
                                            targetNumber = "",
                                            status = logStatus
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e("TruecallerNL", "Error logging WhatsApp reply", e)
                                }
                            }
                            return
                        } catch (e: Exception) {
                            Log.e("TruecallerNL", "Failed to send WhatsApp RemoteInput reply", e)
                        }
                    }
                }
            }
        }
    }
}
