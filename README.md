# ⏸️ Pause
> **The autonomous, privacy-first call gatekeeper, DND VIP bypass, and focus auto-responder for Android.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Zero Telemetry](https://img.shields.io/badge/Telemetry-Zero%20%2F%20None-brightgreen.svg)](#-privacy--security-model)
[![100% Offline Core](https://img.shields.io/badge/Core-100%25%20On--Device-orange.svg)](#-privacy--security-model)
[![Android Compatibility](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-green.svg)](#)

---

## 🛑 Why Pause?

Modern communication apps and stock phone dialers present an all-or-nothing dilemma:

1. **The Do Not Disturb Dilemma**: If you mute everything, your family or closest contacts cannot reach you in genuine emergencies. If you keep the ringer active, robocalls, unsaved callers, and group chat notifications shatter your focus and sleep.
2. **Missing Calls While Busy**: When you are in meetings, driving, sleeping, or doing deep focus work, people keep calling repeatedly because they do not know why you cannot answer.
3. **The Cloud Surveillance Trap**: Traditional commercial call blockers demand access to your entire address book, upload your private call logs, read your OTPs, and sell your network graph to data brokers.

**Pause** gives you complete, sovereign control over your incoming calls and messages. It operates entirely as an **on-device personal gatekeeper**.

---

## 🎯 Real-Life Everyday Scenarios

| Real-Life Scenario | What Normally Happens | How Pause Solves It |
| :--- | :--- | :--- |
| **Deep Work / Coding Sprint** | Phone is on silent, but you're constantly glancing at the screen worrying about an urgent family call. | **Ghost Mode + Starred DND Bypass**: All spam and unsaved callers are instantly rejected with zero screen distraction, but a call from a **Starred family contact** punches through silent mode and rings immediately. |
| **Driving / In a Meeting** | Client or colleague calls three times in a row, thinking you're intentionally ignoring them. | **Instant Multi-Tier Auto-Responder**: Pause immediately replies via SMS or WhatsApp with your custom message: *"In a meeting until 3 PM. If urgent, reply #emergency to alert me."* |
| **Secondary Phone / Work Device** | You leave your work phone at your desk or home while heading out, missing urgent client texts or bank OTPs. | **Smart Multi-Channel Forwarder**: Automatically mirrors critical SMS or missed call alerts to your Telegram bot, Discord webhook, or secondary phone number securely. |
| **Sound Sleep at Night** | Muted phone prevents you from hearing a critical 3 AM emergency from a loved one or on-call teammate. | **Guardian Protocol & Emergency Siren**: If a VIP repeats a call or texts `URGENT`, Pause sounds an audible bypass alarm tone so you wake up when it truly counts. |

---

## ⚡ Core Capabilities

### 🛡️ 1. Focus Mode & Unknown Call Rejection
* **WhatsApp Call Shielding**: When Focus Mode (Silent Guard) is active, incoming WhatsApp audio and video calls from unknown or unsaved numbers are automatically declined and silenced before your device rings.
* **Cellular Call Screening**: Automatically deflects unknown, spam, or telemarketing phone calls during focus sessions.
* **Audit Log**: Every deflection and automated action is recorded locally in your private on-device history.

### 🌟 2. Starred & VIP Contact DND Silent Bypass
* **Never Miss What Matters**: Keep your phone in Do Not Disturb or Silent Mode with peace of mind.
* **Ringer Override for Starred Contacts**: When a contact marked as **Starred** in your Android contacts (or listed in your VIP settings) calls via cellular or WhatsApp, Pause temporarily overrides silent mode, raises the alert ringer, and sounds the ringtone so the call breaks through.
* **Auto-Restoration**: Once the call concludes, your original volume and ringer mode are immediately restored.

### 💬 3. Instant Auto-Responder & Forwarder (SMS, WhatsApp, Telegram, Discord)
* **Chosen Tiered Responses**: Send immediate, personalized responses when you are occupied:
  * **Starred / VIP Contacts**: Warm notes informing them your phone is on silent, with emergency keyword overrides.
  * **Known Contacts**: Courteous status updates (e.g., *"In focus mode until 4 PM; will get back to you shortly"*).
  * **Unknown Senders**: Clear boundary notices filtering out unsolicited communication.
* **Smart Forwarding to Telegram & Discord**: Forward incoming notifications, SMS, or missed-call alerts directly to your private Telegram Bot chat or Discord channel via secure HTTPS webhooks.
* **WhatsApp Quick-Reply**: Replies to incoming WhatsApp messages directly over the internet via native notification actions without requiring third-party bot servers.
* **Emergency Keyword Trigger**: If anyone texts emergency triggers (such as `URGENT` or `#emergency`), Pause triggers an audible alert tone on your device.

### ⏰ 4. Local Scheduled SMS & Sleep Sync
* **Scheduled SMS Tasks**: Schedule important messages or follow-ups to be sent at specific dates, times, or recurring intervals. Dispatches directly from your device's SIM card without cloud dependencies.
* **Sleep & Calendar Sync**: Automatically engages Ghost Mode and DND protections during your scheduled bedtime or active calendar events, then deactivates them when you wake up.

### 🎛️ 5. Quick Action Desktop Widgets
* **Master Kill Switch**: Instantly disable all automation rules with a single tap.
* **Ghost Mode Toggle**: Quickly silence unsaved callers before entering meetings, classes, or quiet environments.
* **1-Hour Temporary Pause**: Temporarily suspend blocking rules (ideal when expecting a food delivery or courier).
* **DND Bypass Toggle**: Rapidly toggle whether Starred VIPs can break through silent mode.

---

## 🔒 Privacy & Security Model

* **Hardware-Backed AES-256 Encryption**: All local rules, configurations, and communication logs are stored in an encrypted SQLite database using **SQLCipher**, with cryptographic keys secured by the **Android Hardware Keystore**.
* **Zero Telemetry & Zero Trackers**: No Google Analytics, no Crashlytics, no advertising SDKs, and no tracking libraries.
* **Zero Cloud Dependence**: All filtering decisions, starred contact lookups, and scheduled tasks execute 100% on your device silicon.

---

## 📋 Transparent Permissions Breakdown

To deliver reliable, autonomous on-device protection, Pause uses the following Android permissions strictly for local execution:

| Permission | Purpose & Justification |
| :--- | :--- |
| `READ_PHONE_STATE` & `READ_CALL_LOG` | Detects incoming cellular calls and call terminations to trigger missed-call automation. |
| `READ_CONTACTS` | Inspects local contacts on-device to determine if an incoming caller is **Starred** or a saved VIP contact. Contacts are never transmitted anywhere. |
| `ACCESS_NOTIFICATION_POLICY` | Enables temporary DND bypass so Starred/VIP emergency calls can ring while your device is muted. |
| `SEND_SMS` & `RECEIVE_SMS` | Dispatches missed-call auto-replies via cellular SIM and sends locally scheduled SMS tasks. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Detects incoming WhatsApp calls to decline unknown numbers and sends instant quick-replies over the internet. |
| `RECEIVE_BOOT_COMPLETED` & `SCHEDULE_EXACT_ALARM` | Ensures your scheduled SMS tasks and quiet hours persist reliably after your device reboots. |
| `USE_BIOMETRIC` | Provides optional fingerprint or biometric security to protect app settings. |

---

## 🛠️ Building From Source

### Prerequisites
* Java Development Kit (JDK 17 or higher)
* Android SDK (Compile SDK 34, Min SDK 26)

### Build Commands

```bash
# Run unit tests
gradle :app:testDebugUnitTest

# Build release APK
gradle :app:assembleRelease
```

The compiled release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## ⚙️ Configuration & Customization

All repository links, donation endpoints, and project metadata are centralized in:
`app/src/main/res/values/strings.xml`

You can update `repo_url`, `github_sponsor_url`, and related values directly in that file without modifying any Kotlin source code.

---

## 📄 License

Pause is distributed under the terms of the [MIT License](LICENSE).
Copyright (c) 2026.
