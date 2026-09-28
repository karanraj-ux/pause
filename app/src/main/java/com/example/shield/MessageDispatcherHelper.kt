package com.example.shield

import android.content.Context
import com.example.data.repository.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MessageDispatcherHelper {

    fun resolveDynamicPlaceholders(
        context: Context,
        template: String,
        senderName: String,
        senderNumber: String,
        settingsRepo: SettingsRepository
    ): String {
        var result = template
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val now = Date()

        result = result.replace("{name}", senderName)
            .replace("{sender}", senderName)
            .replace("{number}", senderNumber)
            .replace("{time}", timeFormat.format(now))
            .replace("{date}", dateFormat.format(now))

        val assistantName = settingsRepo.getStringSync(SettingsRepository.ASSISTANT_NAME, "Pause")
        result = result.replace("{app}", assistantName)
            .replace("{assistant}", assistantName)

        return result
    }

    fun getLocationLink(context: Context, settingsRepo: SettingsRepository): String {
        val link = settingsRepo.getStringSync(SettingsRepository.SAVED_LOCATION_LINK, "")
        return if (link.isNotBlank()) link else "https://maps.google.com"
    }
}
