# Privacy Policy for Pause

**Effective Date: September 2026**

Your privacy and digital sovereignty are our highest priorities. This policy outlines how Pause operates and handles your data.

## 1. Zero Cloud Data Collection & Telemetry
**Pause does not collect, record, harvest, upload, or sell your data.**
There are no servers, no cloud databases, and no developer telemetry endpoints. All analytics, advertising, and crash-reporting trackers are strictly excluded from the application.

## 2. On-Device Execution & Hardware-Backed Encryption
All processing performed by Pause—including missed call filtering, WhatsApp focus screening, scheduled tasks, and event logging—takes place 100% locally on your device:
* The internal database storing your preferences, rules, and communication logs is encrypted at rest using **SQLCipher (AES-256)** with encryption keys tied to the **Android Hardware Keystore**.
* Logs are retained locally on your device and can be cleared by the user at any time.

## 3. WhatsApp Focus Mode & Internet Messaging
When Focus Mode is active:
* WhatsApp notification payloads are analyzed on-device to detect incoming calls or messages.
* When auto-replies are enabled, responses are transmitted directly through WhatsApp's native quick-reply notification action over your existing internet connection.
* No message content or contact identifiers are sent to any external server or third-party service.

## 4. Permissions & Transparent Usage
Pause requests only the permissions strictly required to perform automated boundary protection:
* **READ_PHONE_STATE & READ_CALL_LOG**: Used to detect incoming phone calls, determine if the caller is unsaved, and trigger missed-call automation.
* **READ_CONTACTS**: Used locally to identify Starred and VIP contacts so they can bypass Do Not Disturb or receive specialized auto-replies. Your contacts never leave your device.
* **SEND_SMS & RECEIVE_SMS**: Used to dispatch your configured automated replies via your cellular carrier SIM and send locally scheduled SMS messages.
* **ACCESS_NOTIFICATION_POLICY**: Used to temporarily adjust ringer volume so Starred/VIP calls can break through Do Not Disturb in emergencies.
* **BIND_NOTIFICATION_LISTENER_SERVICE**: Used to inspect incoming notifications from WhatsApp to reject unknown calls and dispatch instant quick-replies.
* **USE_BIOMETRIC**: Used optionally to lock and protect app configuration screens.

## 5. Open Source Verification
Pause is fully open source under the MIT License. Anyone can inspect, build, and verify the source code independently.

## 6. Contact
If you have questions or wish to report an issue, please open an issue in the official GitHub repository.
