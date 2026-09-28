package com.example.shield

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AutoResponder {
    fun handleMissedCall(context: Context, phoneNumber: String, contactName: String) {
        (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
            try {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val masterAutoReply = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_REPLY_ENABLED, false)
            val autoRespondMissedCall = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_RESPOND_MISSED_CALL, false) || masterAutoReply
            
            if (autoRespondMissedCall && phoneNumber.isNotBlank() && isSafeToReply(context, phoneNumber)) {
                val replyMessage = generateReply(context, "MISSED_CALL", phoneNumber)
                sendSms(context, phoneNumber, replyMessage)
            }
            } catch (e: Exception) {
                Log.e("AutoResponder", "Crash prevented in handleMissedCall", e)
            }
        }
    }

    fun handleIncomingSms(context: Context, sender: String, message: String) {
        (context.applicationContext as com.example.ShieldApplication).applicationScope.launch(Dispatchers.IO) {
            try {
            val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
            val autoRespondSms = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_RESPOND_SMS, false)
            
            if (autoRespondSms && sender.isNotBlank() && isSafeToReply(context, sender)) {
                val replyMessage = generateReply(context, "SMS", sender)
                sendSms(context, sender, replyMessage)
            }
            } catch (e: Exception) {
                Log.e("AutoResponder", "Crash prevented in handleIncomingSms", e)
            }
        }
    }

    private fun isSafeToReply(context: Context, phoneNumber: String): Boolean {
        // Check Relationship Tier
        val tier = com.example.calls.CallHandlingManager.getRelationshipTier(context, phoneNumber)
        if (tier == "Blocked" || tier == "Muted") {
            Log.d("AutoResponder", "Loop Prevention: Caller is $tier. No auto-reply sent to $phoneNumber.")
            return false
        }

        // Basic loop/bot avoidance: Ignore short codes or empty numbers
        val strippedNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (strippedNumber.length < 7) {
            Log.w("AutoResponder", "Loop Prevention: Ignoring short code or invalid number: $phoneNumber")
            return false
        }
        val prefs = context.getSharedPreferences("auto_reply_history", Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        val lastReplyTime = prefs.getLong("time_$strippedNumber", 0L)
        val replyCount = prefs.getInt("count_$strippedNumber", 0)
        // Reset rate limit window every 1 hour (3600000 ms)
        if (currentTime - lastReplyTime > 3600000) {
            prefs.edit()
                .putLong("time_$strippedNumber", currentTime)
                .putInt("count_$strippedNumber", 1)
                .apply()
            return true
        }
        // Limit to 3 auto-replies per hour to same number to avoid loops (zero credit loss)
        if (replyCount >= 3) {
            Log.w("AutoResponder", "Loop Prevention: Rate limit exceeded for auto-replies to $strippedNumber")
            return false
        }
        
        prefs.edit()
            .putInt("count_$strippedNumber", replyCount + 1)
            .putLong("time_$strippedNumber", currentTime)
            .apply()
        return true
    }
    private suspend fun generateReply(context: Context, eventType: String, phoneNumber: String): String {
        val settingsRepo = (context.applicationContext as com.example.ShieldApplication).container.settingsRepository
        val tier = com.example.calls.CallHandlingManager.getRelationshipTier(context, phoneNumber)
        
        val rawTemplate = when (tier) {
            "Inner Circle" -> settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.VIP_REPLY_MSG, "Hey, my phone is on silent. If this is an emergency (or if you are helping me find my phone), reply with the exact word URGENT and it will sound an alarm.")
            "Standard" -> settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.STANDARD_REPLY_MSG, "Hi, I am currently focused or away. I will get back to you as soon as I can.")
            else -> settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.UNKNOWN_REPLY_MSG, "I do not accept direct calls from unknown numbers to prevent spam. If this is important, please message me.")
        }

        var resolved = MessageDispatcherHelper.resolveDynamicPlaceholders(
            context = context,
            template = rawTemplate,
            senderName = phoneNumber,
            senderNumber = phoneNumber,
            settingsRepo = settingsRepo
        )

        // Attach configured file (document, PDF, photo, catalog link)
        val attachedFileName = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.AUTO_REPLY_ATTACHED_FILE_NAME, "")
        val attachedFileUrl = settingsRepo.getStringSync(com.example.data.repository.SettingsRepository.AUTO_REPLY_ATTACHED_FILE_URL, "")
        if (attachedFileUrl.isNotBlank()) {
            val label = if (attachedFileName.isNotBlank()) "📎 Attached ($attachedFileName)" else "📎 Attached File"
            resolved = "$resolved\n$label: $attachedFileUrl"
        }

        val attachLoc = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.AUTO_REPLY_ATTACH_LOCATION, false)
        val appendVipLoc = settingsRepo.getBooleanSync(com.example.data.repository.SettingsRepository.APPEND_LOCATION_TO_VIP, false)
        if ((attachLoc || (tier == "Inner Circle" && appendVipLoc)) && !resolved.contains("maps.google.com")) {
            val locLink = MessageDispatcherHelper.getLocationLink(context, settingsRepo)
            resolved = "$resolved\n📍 Location: $locLink"
        }

        return resolved
    }
    private fun sendSms(context: Context, phoneNumber: String, content: String) {
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.e("AutoResponder", "Permission denied for SEND_SMS, silently exiting.")
                return
            }
            val sm: android.telephony.SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            val parts = sm.divideMessage(content)
            if (parts.size > 1) {
                sm.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                sm.sendTextMessage(phoneNumber, null, content, null, null)
            }
            Log.d("AutoResponder", "Auto-responded natively via SMS to $phoneNumber")
        } catch (e: Exception) {
            Log.e("AutoResponder", "Failed to auto-respond natively via SMS", e)
        }
    }
}
