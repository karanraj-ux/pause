# Shield & Forward: The Master Artisan's Blueprint

## 1. Vision & Core Concept
**Shield & Forward** is conceived as a fortress for personal communication. It is a privacy-first, offline-capable Android utility designed to give users absolute control over their inbound communications (SMS and Phone Calls). Rather than relying on opaque cloud-based spam filters or black-box AI models that require internet access, it uses a deterministic, transparent local rules engine to filter, forward, reject, or webhook inbound data.

### The Artisan's Philosophy
* **Offline-First Sanctity:** The device is the vault. All rules, logs, and configurations are stored locally using a Room Database. Data never leaves the device unless explicitly commanded by the user via Webhooks or SMS forwarding rules.
* **Deterministic Transparency:** No black-box AI. Users define exact keyword triggers for SMS and specific number matching for calls. If a rule triggers, it is because the user explicitly forged that rule.
* **Granular Automation:** The app acts as a local server for personal communication, routing data through custom pipelines seamlessly in the background.

---

## 2. Technical Stack & Foundation
* **Language:** Pure Kotlin, emphasizing Coroutines and Flow for asynchronous data streams.
* **UI Toolkit:** Jetpack Compose (Material Design 3). The aesthetic relies on Edge-to-Edge window handling, deep surface colors, and high-contrast primary accents to convey security and control.
* **Architecture:** Strict MVVM (Model-View-ViewModel) with Clean Architecture principles.
* **Local Persistence:** Room Database (SQLite abstraction) acting as the single source of truth.
* **Background Processing:** WorkManager (for guaranteed execution of Webhooks/SMS dispatch), BroadcastReceivers (for real-time event interception), and Android Telecom/Telephony APIs.

---

## 3. Core Modules & Feature Implementation Details

### A. SMS Interception & Routing Engine
**The Goal:** Silently catch incoming SMS, evaluate them against user-defined rules, and act immediately without requiring the app to be open.
* **`SmsReceiver`:** A `BroadcastReceiver` listening for `android.provider.Telephony.SMS_RECEIVED` with high priority. It acts as the gatekeeper.
* **`SmsProcessor`:** The brain of the SMS engine. When a message arrives, this class:
  1. Extracts the sender and the raw text body.
  2. Queries the Room Database for active `CustomRule` entries.
  3. Uses Regex/Keyword matching to determine if the message body contains the trigger string.
* **The Actions:** If a match occurs, the processor can execute multiple distinct actions:
  * **AutoResponder:** Automatically replies to the sender using `SmsManager`.
  * **Forwarding:** Uses `SmsWorker` (a WorkManager task) to reliably forward the SMS content to a different phone number.
  * **Webhooks:** Dispatches a JSON payload containing the SMS data to a configured URL, bridging the offline SMS to the online world (e.g., Discord, Slack, IFTTT).

### B. Call Shielding & VIP Bypass Engine
**The Goal:** Protect the user from unwanted interruptions while ensuring critical (VIP) calls always break through, even in Do Not Disturb (DND) mode.
* **`PhoneStateReceiver` & `CallHandlingManager`:** 
  * Binds to `TelephonyManager.EXTRA_STATE_RINGING` to detect the exact moment a call comes in.
  * Captures the incoming Caller ID.
* **The Decision Matrix:** The incoming number is compared against `PhoneRuleEntity` records in the database.
  * **Block Rule:** If flagged as spam/blocked, the app uses Android's `TelecomManager` to programmatically end the call before the user's phone even rings.
  * **VIP Bypass:** If the number is flagged as VIP, the system overrides the device's silent/DND state. It utilizes the `NotificationManager` and `AudioManager` to trigger a loud, undeniable alert, ensuring emergency contacts always get through.

### C. Automation & Webhook Dispatch System
**The Goal:** Allow advanced users to hook their offline communication events into custom web servers or automation pipelines.
* **`WebhookWorker`:** Built on Android's `WorkManager` for guaranteed execution. Even if the device loses internet when an SMS arrives, the WorkManager will queue the HTTP POST request and fire it the moment connectivity is restored.
* **Payload Construction:** Formats intercepted data into clean JSON, ensuring compatibility with standard REST APIs.

### D. Data Persistence Layer (Room Architecture)
**The Goal:** A robust, relational, and fully local data store. `AppDatabase` is the immutable core.
* **`CustomRuleDao`:** Stores SMS triggers and their associated actions (Keyword -> Action mapping).
* **`PhoneRuleDao`:** Maintains the roster of Blocked and VIP phone numbers.
* **`WebhookConfigDao`:** Securely stores endpoint URLs and payload configurations.
* **`SmsLogDao`:** The ledger. Records the history of processed, forwarded, or webhooked messages, powering the real-time UI dashboard.

### E. User Interface (The Visual Identity)
**The Goal:** A dashboard that feels like a command center. Dark, secure, and incredibly fast, built entirely in Jetpack Compose.
* **Adaptive Navigation:** Uses a standard `NavigationBar` (bottom bar) for mobile screens, seamlessly morphing into a `NavigationRail` (side bar) on expanded screens (tablets/foldables).
* **`MessagesScreen` (The Rulesmith):** A focused interface to craft SMS forwarding rules. Clean text fields for triggers and target numbers, backed by instant validation.
* **`PhoneScreen` (The Shield):** A dual-list view managing the VIP roster and the Blocklist. Uses swipe-to-delete gestures for fluid management.
* **`WebhookScreen` (The Bridge):** A technical interface for managing external HTTP hooks.
* **`LogsScreen` (The Ledger):** A high-performance `LazyColumn` rendering the history of all intercepted events. Uses visual status indicators (Success/Ignored) and timestamps to provide a clear audit trail.
* **Privacy Trust Banner:** A permanent visual fixture reminding the user that "No data leaves this device."

---

## 4. Evolution & Purge (The Clean Architecture Shift)

**The AI Purge:** In its nascent stages, the app experimented with offline AI classifiers (Gemini SDKs) to "guess" if a message was spam or important. This proved antithetical to the core philosophy: it was slow, unpredictable, and bloated the app. 
**The Refactor:** We executed a complete purge of all AI dependencies, the `DeclutterScreen`, and complex inference engines. We returned to a strict, deterministic regex/keyword system. This resulted in an app that is vastly faster, completely crash-free, and adheres perfectly to the "Privacy By Default" mandate.

---

## 5. Execution Context & Guidelines for Future Edits

* **The Codebase is Fresh:** The app was recently imported from GitHub, representing the clean, post-purge state.
* **The Preview Fix:** The `FLAG_SECURE` attribute was manually stripped from `MainActivity.kt` to allow the AI Studio streaming emulator to render the preview. Do NOT re-add `FLAG_SECURE` unless preparing for a final production build, as it blinds the preview window.
* **Strict Mandate:** Maintain the streamlined UI. Do NOT reintroduce any external cloud SDKs, AI inference models, or complex visual clutter unless explicitly commanded by the user. Keep it offline, keep it fast, keep it secure.
