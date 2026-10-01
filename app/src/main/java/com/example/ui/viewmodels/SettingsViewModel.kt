package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SettingsRepository
import com.example.data.repository.NumberReplyRule
import com.example.data.repository.numberReplyRulesToJson
import com.example.data.repository.parseNumberReplyRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.Immutable

@Immutable
data class SettingsState(
    val assistantName: String = "Assistant",
    val assistantAvatar: String = "",
    val targetNumbers: String = "",
    val forwardPhone: String = "",
    val allowExternalAutomation: Boolean = true,
    val autoRespondMissedCall: Boolean = false,
    val autoReplyRestrictedNumbers: String = "",
    val autoRespondSms: Boolean = false,
    val autoRespondWhatsapp: Boolean = false,
    val silentSwallow: Boolean = true,
    val masterKillSwitch: Boolean = false,
    val appTheme: String = "system",
    val sleepModeEnabled: Boolean = false,
    val sleepStartHour: Int = 22,
    val sleepStartMinute: Int = 0,
    val sleepEndHour: Int = 7,
    val sleepEndMinute: Int = 0,
    val senders: String = "",
    val keywordFilter: String = "",
    val merchantKeywords: String = "bank,alert,txn,otp,code",
    val vipCallers: String = "",
    val autoForwardCalls: Boolean = false,
    val callForwardTarget: String = "",
    val autoForwardDuration: String = "5",
    val alertForwardTarget: Boolean = false,
    val vipDivertNumber: String = "",
    val overrideDnd: Boolean = false,
    val dndTimeframeMinutes: String = "5",
    val dndThresholdCalls: String = "2",
    val hasSeenShieldTooltip: Boolean = false,
    val detectBusyAndReply: Boolean = false,
    val busyReplyMessage: String = "I'm currently busy. Please leave a message.",
    val vipReplyMsg: String = "",
    val standardReplyMsg: String = "",
    val unknownReplyMsg: String = "",
    val numberReplyRules: List<NumberReplyRule> = emptyList(),
    val selectedSimId: String? = null,
    val showKjCompanion: Boolean = true,
    val showCalls: Boolean = true,
    val showShield: Boolean = true,
    val widgetRecentLogs: Boolean = true,
    val widgetQuickChat: Boolean = true,
    val blockSpamCalls: Boolean = true,
    val ghostMode: Boolean = false,
    val calendarSync: Boolean = false,
    val calendarGhostModeActive: Boolean = false,
    val calendarSyncIds: Set<String> = emptySet(),
    val calendarTriggerKeywords: String = "meeting,interview,focus",
    val ghostModePauseEndTime: Long = 0L,
    val smartSpamReader: Boolean = false,
    val smsForwardingEnabled: Boolean = false,
    val smsForwardTarget: String = "",
    val dndBypassRingtoneUri: String = "",
    val extractOtps: Boolean = false,
    val forwardServiceSmsOnly: Boolean = false,
    val spamBlockedCount: Int = 0,
    val customSmsRules: String = "",
    val locationAutoShareEnabled: Boolean = false,
    val savedLocationLink: String = "",
    val savedLocationName: String = "",
    val photoLinkUrl: String = "",
    val photoLinkLabel: String = "Catalog / Media",
    val quickLinks: String = "",
    val appendLocationToVip: Boolean = false,
    val appendLocationToEmergency: Boolean = true,
    val autoReplyEnabled: Boolean = false,
    val autoReplyAttachedFileName: String = "",
    val autoReplyAttachedFileUrl: String = "",
    val autoReplyAttachLocation: Boolean = false,
    val autoReplyAttachedFileType: String = "DOCUMENT"
)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState

    init {
        viewModelScope.launch {
            settingsRepository.preferencesFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        spamBlockedCount = prefs[SettingsRepository.SPAM_BLOCKED_COUNT] ?: 0,
                        customSmsRules = prefs[SettingsRepository.CUSTOM_SMS_RULES] ?: "",
                        assistantName = prefs[SettingsRepository.ASSISTANT_NAME] ?: "Assistant",
                        assistantAvatar = prefs[SettingsRepository.ASSISTANT_AVATAR] ?: "",
                        targetNumbers = prefs[SettingsRepository.TARGET_NUMBERS] ?: "",
                        forwardPhone = prefs[SettingsRepository.FORWARD_PHONE] ?: "",
                        allowExternalAutomation = prefs[SettingsRepository.ALLOW_EXTERNAL_AUTOMATION] ?: true,
                        autoRespondMissedCall = prefs[SettingsRepository.AUTO_RESPOND_MISSED_CALL] ?: false,
                        autoReplyRestrictedNumbers = prefs[SettingsRepository.AUTO_REPLY_RESTRICTED_NUMBERS] ?: "",
                        autoRespondSms = prefs[SettingsRepository.AUTO_RESPOND_SMS] ?: false,
                        autoRespondWhatsapp = prefs[SettingsRepository.AUTO_RESPOND_WHATSAPP] ?: false,
                        silentSwallow = prefs[SettingsRepository.SILENT_SWALLOW] ?: true,
                        masterKillSwitch = prefs[SettingsRepository.MASTER_KILL_SWITCH] ?: false,
                        ghostModePauseEndTime = prefs[SettingsRepository.GHOST_MODE_PAUSE_END_TIME] ?: 0L,
                        senders = prefs[SettingsRepository.SENDERS] ?: "",
                        keywordFilter = prefs[SettingsRepository.KEYWORD_FILTER] ?: "",
                        merchantKeywords = prefs[SettingsRepository.MERCHANT_KEYWORDS] ?: "bank,alert,txn,otp,code",
                        vipCallers = prefs[SettingsRepository.VIP_CALLERS] ?: "",
                        autoForwardCalls = prefs[SettingsRepository.AUTO_FORWARD_CALLS] ?: false,
                        callForwardTarget = prefs[SettingsRepository.CALL_FORWARD_TARGET] ?: "",
                        autoForwardDuration = (prefs[SettingsRepository.AUTO_FORWARD_DURATION] ?: 5).toString(),
                        alertForwardTarget = prefs[SettingsRepository.ALERT_FORWARD_TARGET] ?: false,
                        vipDivertNumber = prefs[SettingsRepository.VIP_DIVERT_NUMBER] ?: "",
                        overrideDnd = prefs[SettingsRepository.OVERRIDE_DND] ?: false,
                        hasSeenShieldTooltip = prefs[SettingsRepository.HAS_SEEN_SHIELD_TOOLTIP] ?: false,
                        dndTimeframeMinutes = (prefs[SettingsRepository.DND_TIMEFRAME_MINUTES] ?: 5).toString(),
                        dndThresholdCalls = (prefs[SettingsRepository.DND_THRESHOLD_CALLS] ?: 2).toString(),
                        detectBusyAndReply = prefs[SettingsRepository.DETECT_BUSY] ?: false,
                        busyReplyMessage = prefs[SettingsRepository.BUSY_REPLY_MSG] ?: "I'm currently busy. Please leave a message.",
                        vipReplyMsg = prefs[SettingsRepository.VIP_REPLY_MSG] ?: "",
                        standardReplyMsg = prefs[SettingsRepository.STANDARD_REPLY_MSG] ?: "",
                        unknownReplyMsg = prefs[SettingsRepository.UNKNOWN_REPLY_MSG] ?: "",
                        numberReplyRules = parseNumberReplyRules(prefs[SettingsRepository.WHATSAPP_NUMBER_REPLIES] ?: ""),
                        selectedSimId = prefs[SettingsRepository.SELECTED_SIM_ID],
                        showKjCompanion = prefs[SettingsRepository.SHOW_KJ_COMPANION] ?: true,
                        showCalls = prefs[SettingsRepository.SHOW_CALLS] ?: true,
                        showShield = prefs[SettingsRepository.SHOW_SHIELD] ?: true,
                        widgetRecentLogs = prefs[SettingsRepository.WIDGET_RECENT_LOGS] ?: true,
                        widgetQuickChat = prefs[SettingsRepository.WIDGET_QUICK_CHAT] ?: true,
                        blockSpamCalls = prefs[SettingsRepository.BLOCK_SPAM_CALLS] ?: true,
                        ghostMode = prefs[SettingsRepository.GHOST_MODE] ?: false,
                        calendarSync = prefs[SettingsRepository.CALENDAR_SYNC] ?: false,
                        calendarGhostModeActive = prefs[SettingsRepository.CALENDAR_GHOST_MODE_ACTIVE] ?: false,
                        calendarSyncIds = prefs[SettingsRepository.CALENDAR_SYNC_IDS] ?: emptySet(),
                        calendarTriggerKeywords = prefs[SettingsRepository.CALENDAR_TRIGGER_KEYWORDS] ?: "meeting,interview,focus",
                        smartSpamReader = prefs[SettingsRepository.SMART_SPAM_READER] ?: false,
                        smsForwardingEnabled = prefs[SettingsRepository.SMS_FORWARDING_ENABLED] ?: false,
                        smsForwardTarget = prefs[SettingsRepository.SMS_FORWARD_TARGET] ?: "",
                        dndBypassRingtoneUri = prefs[SettingsRepository.DND_BYPASS_RINGTONE_URI] ?: "",
                        extractOtps = prefs[SettingsRepository.EXTRACT_OTPS] ?: false,
                        appTheme = prefs[SettingsRepository.APP_THEME] ?: "system",
                        sleepModeEnabled = prefs[SettingsRepository.SLEEP_MODE_ENABLED] ?: false,
                        sleepStartHour = prefs[SettingsRepository.SLEEP_START_HOUR] ?: 22,
                        sleepStartMinute = prefs[SettingsRepository.SLEEP_START_MINUTE] ?: 0,
                        sleepEndHour = prefs[SettingsRepository.SLEEP_END_HOUR] ?: 7,
                        sleepEndMinute = prefs[SettingsRepository.SLEEP_END_MINUTE] ?: 0,
                        forwardServiceSmsOnly = prefs[SettingsRepository.FORWARD_SERVICE_SMS_ONLY] ?: false,
                        locationAutoShareEnabled = prefs[SettingsRepository.LOCATION_AUTO_SHARE_ENABLED] ?: false,
                        savedLocationLink = prefs[SettingsRepository.SAVED_LOCATION_LINK] ?: "",
                        savedLocationName = prefs[SettingsRepository.SAVED_LOCATION_NAME] ?: "",
                        photoLinkUrl = prefs[SettingsRepository.PHOTO_LINK_URL] ?: "",
                        photoLinkLabel = prefs[SettingsRepository.PHOTO_LINK_LABEL] ?: "Catalog / Media",
                        quickLinks = prefs[SettingsRepository.QUICK_LINKS] ?: "",
                        appendLocationToVip = prefs[SettingsRepository.APPEND_LOCATION_TO_VIP] ?: false,
                        appendLocationToEmergency = prefs[SettingsRepository.APPEND_LOCATION_TO_EMERGENCY] ?: true,
                        autoReplyEnabled = prefs[SettingsRepository.AUTO_REPLY_ENABLED] ?: false,
                        autoReplyAttachedFileName = prefs[SettingsRepository.AUTO_REPLY_ATTACHED_FILE_NAME] ?: "",
                        autoReplyAttachedFileUrl = prefs[SettingsRepository.AUTO_REPLY_ATTACHED_FILE_URL] ?: "",
                        autoReplyAttachLocation = prefs[SettingsRepository.AUTO_REPLY_ATTACH_LOCATION] ?: false,
                        autoReplyAttachedFileType = prefs[SettingsRepository.AUTO_REPLY_ATTACHED_FILE_TYPE] ?: "DOCUMENT"
                    )
                }
            }
        }
    }
    
    fun updateAutoReplyMaster(enabled: Boolean) {
        _uiState.update { it.copy(autoReplyEnabled = enabled, autoRespondWhatsapp = enabled, autoRespondMissedCall = enabled) }
        viewModelScope.launch {
            settingsRepository.updateBoolean(SettingsRepository.AUTO_REPLY_ENABLED, enabled)
            settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_WHATSAPP, enabled)
            settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_MISSED_CALL, enabled)
        }
    }

    fun updateAutoReplyAttachedFileName(value: String) {
        _uiState.update { it.copy(autoReplyAttachedFileName = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.AUTO_REPLY_ATTACHED_FILE_NAME, value) }
    }

    fun updateAutoReplyAttachedFileUrl(value: String) {
        _uiState.update { it.copy(autoReplyAttachedFileUrl = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.AUTO_REPLY_ATTACHED_FILE_URL, value) }
    }

    fun updateAutoReplyAttachLocation(value: Boolean) {
        _uiState.update { it.copy(autoReplyAttachLocation = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_REPLY_ATTACH_LOCATION, value) }
    }

    fun updateAutoReplyAttachedFileType(value: String) {
        _uiState.update { it.copy(autoReplyAttachedFileType = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.AUTO_REPLY_ATTACHED_FILE_TYPE, value) }
    }
    
    fun updateAssistantName(value: String) { _uiState.update { it.copy(assistantName = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.ASSISTANT_NAME, value) } }
    fun updateAssistantAvatar(value: String) { _uiState.update { it.copy(assistantAvatar = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.ASSISTANT_AVATAR, value) } }
    fun updateTargetNumbers(value: String) { _uiState.update { it.copy(targetNumbers = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.TARGET_NUMBERS, value) } }
    fun updateForwardPhone(value: String) { _uiState.update { it.copy(forwardPhone = value, targetNumbers = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.FORWARD_PHONE, value); settingsRepository.updateString(SettingsRepository.TARGET_NUMBERS, value) } }
    fun updateAllowExternalAutomation(value: Boolean) { _uiState.update { it.copy(allowExternalAutomation = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.ALLOW_EXTERNAL_AUTOMATION, value) } }
    fun updateAutoRespondMissedCall(value: Boolean) { _uiState.update { it.copy(autoRespondMissedCall = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_MISSED_CALL, value) } }
    fun updateAutoReplyRestrictedNumbers(value: String) { _uiState.update { it.copy(autoReplyRestrictedNumbers = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.AUTO_REPLY_RESTRICTED_NUMBERS, value) } }
    fun updateAutoRespondSms(value: Boolean) { _uiState.update { it.copy(autoRespondSms = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_SMS, value) } }
    fun updateAutoRespondWhatsapp(value: Boolean) { _uiState.update { it.copy(autoRespondWhatsapp = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_RESPOND_WHATSAPP, value) } }
    fun updateSilentSwallow(value: Boolean) { _uiState.update { it.copy(silentSwallow = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SILENT_SWALLOW, value) } }
    fun updateMasterKillSwitch(value: Boolean) { _uiState.update { it.copy(masterKillSwitch = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.MASTER_KILL_SWITCH, value) } }
    fun updateSenders(value: String) { _uiState.update { it.copy(senders = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.SENDERS, value) } }
    fun updateKeywordFilter(value: String) { _uiState.update { it.copy(keywordFilter = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.KEYWORD_FILTER, value) } }
    fun updateMerchantKeywords(value: String) { _uiState.update { it.copy(merchantKeywords = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.MERCHANT_KEYWORDS, value) } }
    fun updateVipCallers(value: String) { _uiState.update { it.copy(vipCallers = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.VIP_CALLERS, value) } }
    fun updateAutoForwardCalls(value: Boolean) { _uiState.update { it.copy(autoForwardCalls = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.AUTO_FORWARD_CALLS, value) } }
    fun updateCallForwardTarget(value: String) { _uiState.update { it.copy(callForwardTarget = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.CALL_FORWARD_TARGET, value) } }
    fun updateAutoForwardDuration(value: String) { _uiState.update { it.copy(autoForwardDuration = value) }; viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.AUTO_FORWARD_DURATION, value.toIntOrNull() ?: 5) } }
    fun updateAlertForwardTarget(value: Boolean) { _uiState.update { it.copy(alertForwardTarget = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.ALERT_FORWARD_TARGET, value) } }
    fun updateVipDivertNumber(value: String) { _uiState.update { it.copy(vipDivertNumber = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.VIP_DIVERT_NUMBER, value) } }
    fun updateOverrideDnd(value: Boolean) { _uiState.update { it.copy(overrideDnd = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.OVERRIDE_DND, value) } }
    fun updateHasSeenShieldTooltip(value: Boolean) { _uiState.update { it.copy(hasSeenShieldTooltip = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.HAS_SEEN_SHIELD_TOOLTIP, value) } }
    fun updateDndTimeframeMinutes(value: String) { _uiState.update { it.copy(dndTimeframeMinutes = value) }; viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.DND_TIMEFRAME_MINUTES, value.toIntOrNull() ?: 5) } }
    fun updateDndThresholdCalls(value: String) { _uiState.update { it.copy(dndThresholdCalls = value) }; viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.DND_THRESHOLD_CALLS, value.toIntOrNull() ?: 2) } }
    fun updateDetectBusyAndReply(value: Boolean) { _uiState.update { it.copy(detectBusyAndReply = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.DETECT_BUSY, value) } }
    fun updateBusyReplyMessage(value: String) { _uiState.update { it.copy(busyReplyMessage = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.BUSY_REPLY_MSG, value) } }
    fun updateVipReplyMsg(value: String) { _uiState.update { it.copy(vipReplyMsg = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.VIP_REPLY_MSG, value) } }
    fun updateStandardReplyMsg(value: String) { _uiState.update { it.copy(standardReplyMsg = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.STANDARD_REPLY_MSG, value) } }
    fun updateUnknownReplyMsg(value: String) { _uiState.update { it.copy(unknownReplyMsg = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.UNKNOWN_REPLY_MSG, value) } }
    fun setNumberReplyRules(rules: List<NumberReplyRule>) {
        _uiState.update { it.copy(numberReplyRules = rules) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.WHATSAPP_NUMBER_REPLIES, numberReplyRulesToJson(rules)) }
    }
    fun updateSelectedSimId(value: String?) { _uiState.update { it.copy(selectedSimId = value) }; viewModelScope.launch { if (value == null) settingsRepository.removeKey(SettingsRepository.SELECTED_SIM_ID) else settingsRepository.updateString(SettingsRepository.SELECTED_SIM_ID, value) } }
    fun updateShowKjCompanion(value: Boolean) { _uiState.update { it.copy(showKjCompanion = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_KJ_COMPANION, value) } }
    fun updateShowCalls(value: Boolean) { _uiState.update { it.copy(showCalls = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_CALLS, value) } }
    fun updateShowShield(value: Boolean) { _uiState.update { it.copy(showShield = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SHOW_SHIELD, value) } }
    fun updateWidgetRecentLogs(value: Boolean) { _uiState.update { it.copy(widgetRecentLogs = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.WIDGET_RECENT_LOGS, value) } }
    fun updateWidgetQuickChat(value: Boolean) { _uiState.update { it.copy(widgetQuickChat = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.WIDGET_QUICK_CHAT, value) } }
    fun updateBlockSpamCalls(value: Boolean) { _uiState.update { it.copy(blockSpamCalls = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.BLOCK_SPAM_CALLS, value) } }
    fun updateCalendarSync(value: Boolean) { _uiState.update { it.copy(calendarSync = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.CALENDAR_SYNC, value) } }
    fun updateGhostMode(value: Boolean) { _uiState.update { it.copy(ghostMode = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.GHOST_MODE, value) } }
    fun pauseGhostMode(durationMs: Long = 60 * 60 * 1000L) {
        val pauseEndTime = System.currentTimeMillis() + durationMs
        _uiState.update { it.copy(ghostModePauseEndTime = pauseEndTime) }
        viewModelScope.launch { settingsRepository.updateLong(SettingsRepository.GHOST_MODE_PAUSE_END_TIME, pauseEndTime) }
    }
    fun updateSmartSpamReader(value: Boolean) { _uiState.update { it.copy(smartSpamReader = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SMART_SPAM_READER, value) } }
    fun updateSmsForwardingEnabled(value: Boolean) { _uiState.update { it.copy(smsForwardingEnabled = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SMS_FORWARDING_ENABLED, value) } }
    fun updateSmsForwardTarget(value: String) { _uiState.update { it.copy(smsForwardTarget = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.SMS_FORWARD_TARGET, value) } }
    fun updateExtractOtps(value: Boolean) { _uiState.update { it.copy(extractOtps = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.EXTRACT_OTPS, value) } }
        fun updateAppTheme(value: String) { _uiState.update { it.copy(appTheme = value) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.APP_THEME, value) } }
    
    fun updateSleepModeEnabled(value: Boolean) { _uiState.update { it.copy(sleepModeEnabled = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.SLEEP_MODE_ENABLED, value) } }
    fun updateSleepStart(hour: Int, minute: Int) { _uiState.update { it.copy(sleepStartHour = hour, sleepStartMinute = minute) }; viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.SLEEP_START_HOUR, hour); settingsRepository.updateInt(SettingsRepository.SLEEP_START_MINUTE, minute) } }
    fun updateSleepEnd(hour: Int, minute: Int) { _uiState.update { it.copy(sleepEndHour = hour, sleepEndMinute = minute) }; viewModelScope.launch { settingsRepository.updateInt(SettingsRepository.SLEEP_END_HOUR, hour); settingsRepository.updateInt(SettingsRepository.SLEEP_END_MINUTE, minute) } }
    fun updateForwardServiceSmsOnly(value: Boolean) { _uiState.update { it.copy(forwardServiceSmsOnly = value) }; viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.FORWARD_SERVICE_SMS_ONLY, value) } }
    fun updateDndBypassRingtoneUri(uri: String) { _uiState.update { it.copy(dndBypassRingtoneUri = uri) }; viewModelScope.launch { settingsRepository.updateString(SettingsRepository.DND_BYPASS_RINGTONE_URI, uri) } }

    fun updateCustomSmsRules(rules: String) {
        viewModelScope.launch {
            settingsRepository.updateString(SettingsRepository.CUSTOM_SMS_RULES, rules)
            _uiState.update { it.copy(customSmsRules = rules) }
        }
    }

    fun updateCalendarSyncIds(value: Set<String>) {
        _uiState.update { it.copy(calendarSyncIds = value) }
        viewModelScope.launch { settingsRepository.updateStringSet(SettingsRepository.CALENDAR_SYNC_IDS, value) }
    }

    fun updateCalendarTriggerKeywords(value: String) {
        _uiState.update { it.copy(calendarTriggerKeywords = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.CALENDAR_TRIGGER_KEYWORDS, value) }
    }

    fun updateLocationAutoShareEnabled(value: Boolean) {
        _uiState.update { it.copy(locationAutoShareEnabled = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.LOCATION_AUTO_SHARE_ENABLED, value) }
    }

    fun updateSavedLocationLink(value: String) {
        _uiState.update { it.copy(savedLocationLink = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.SAVED_LOCATION_LINK, value) }
    }

    fun updateSavedLocationName(value: String) {
        _uiState.update { it.copy(savedLocationName = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.SAVED_LOCATION_NAME, value) }
    }

    fun updatePhotoLinkUrl(value: String) {
        _uiState.update { it.copy(photoLinkUrl = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.PHOTO_LINK_URL, value) }
    }

    fun updatePhotoLinkLabel(value: String) {
        _uiState.update { it.copy(photoLinkLabel = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.PHOTO_LINK_LABEL, value) }
    }

    fun updateQuickLinks(value: String) {
        _uiState.update { it.copy(quickLinks = value) }
        viewModelScope.launch { settingsRepository.updateString(SettingsRepository.QUICK_LINKS, value) }
    }

    fun updateAppendLocationToVip(value: Boolean) {
        _uiState.update { it.copy(appendLocationToVip = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.APPEND_LOCATION_TO_VIP, value) }
    }

    fun updateAppendLocationToEmergency(value: Boolean) {
        _uiState.update { it.copy(appendLocationToEmergency = value) }
        viewModelScope.launch { settingsRepository.updateBoolean(SettingsRepository.APPEND_LOCATION_TO_EMERGENCY, value) }
    }

    class Factory(private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

