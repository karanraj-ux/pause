# Pause

<p align="center">
  <img src="assets/app-icon.png" alt="Pause app icon" width="160"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="MIT License"/>
  <img src="https://img.shields.io/badge/Platform-Android%2013%2B-blue.svg" alt="Android 13+"/>
  <img src="https://img.shields.io/badge/Privacy-100%25%20On--Device-purple.svg" alt="100% On-Device"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-orange.svg" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-09D3AC.svg" alt="Jetpack Compose"/>
</p>

**Pause** is a 100% on-device Android focus companion. It guards your attention by automatically handling calls and messages while you're busy — rejecting unknown callers, letting VIPs through silent mode, and sending instant auto-replies over SMS and WhatsApp.

Source: https://github.com/karanraj-ux/pause

---

## Why Pause Exists

Your phone has two failure modes: it either interrupts you at the worst moment, or it makes you unreachable to the people who actually matter. Pause fixes both. It never sends your data to a server — every decision happens on your phone, in milliseconds, with zero telemetry.

---

## Your Everyday Use Cases

### The Morning You Actually Get to Sleep In
You had a rough night. Set Pause to silent mode until 8:30 AM. When your partner messages *"Good morning! Are you awake yet?"* on WhatsApp, Pause replies instantly over the internet: *"Still resting! Phone is on silent until 8:30 AM. Will catch up shortly."* You wake up when you want to.

### The Deep Work Afternoon
You're in flow state with a deadline at 5 PM. Unknown numbers get silently rejected and receive your auto-reply. But when your mom — a starred contact — calls twice in five minutes, Pause recognizes the pattern and rings through. Emergencies never miss you; everything else waits.

### The Meeting You Can't Be Disturbed In
Pause syncs with your calendar. When your "Team Standup" or "Interview" starts, Focus Mode activates automatically — unknown calls are rejected, WhatsApp chats get your *"In a meeting, will reply after"* message. When the event ends, everything returns to normal on its own.

### The Lost Phone Panic
Your phone is on silent somewhere in the house. From any other phone, text **URGENT** to your number. Pause detects the keyword, overrides silent mode, and sounds a loud 15-second alarm so you can find it.

### The Important Delivery
Expecting a courier in the next hour? Tap the home-screen widget for a 1-hour Focus pause, or add the delivery number as a one-time VIP so their call always rings through.

### The Drive Home
Connect to your car's Bluetooth and Pause can activate Drive Mode — WhatsApp messages get an automatic *"Driving right now, will reply when I arrive"* while your navigation keeps working.

---

## Main Capabilities

| Feature | What It Does |
|---|---|
| **Focus Mode (Silent Guard)** | One-tap attention shield. Rejects unknown calls, silences notifications, and auto-replies to messages while active. |
| **WhatsApp Auto-Reply** | Replies instantly to incoming WhatsApp chats directly over the internet via notification quick-reply — no server involved. One toggle, three recipient tiers. |
| **SMS Auto-Reply** | Replies to missed calls and texts from your carrier SIM, with separate messages for VIPs, saved contacts, and unknown numbers. |
| **Emergency Keyword Override** | A message containing **URGENT** or **#urgent** bypasses silent mode and sounds a loud alarm — for lost phones and real emergencies. |
| **Starred / VIP DND Bypass** | Starred contacts ring through Do Not Disturb. Repeat callers (e.g. 3 calls in 5 minutes) can optionally break through too. |
| **Calendar Focus Sync** | Busy calendar events automatically activate Focus Mode and deactivate it when the event ends. You choose which calendars count and which keywords trigger it. |
| **Scheduled SMS** | Compose messages now, send them later from your SIM — reminders, birthday wishes, work updates. |
| **Call Forwarding Rules** | Forward unknown callers to a second phone while auto-replying to them. |
| **Drive Mode** | Auto-replies on WhatsApp when connected to your car's Bluetooth. |
| **Home-Screen Widgets** | Toggle Focus Mode, trigger a 1-hour pause, or hit the master kill switch from your launcher. |

---

## WhatsApp Auto-Reply

One toggle: **Enable WhatsApp Auto-Reply**. Pause watches WhatsApp notifications and replies instantly over your phone's internet connection using WhatsApp's own quick-reply action. There are no bot commands to learn and no simulator to fiddle with — it just sends your message.

**Three recipient tiers** — different messages for different relationships:

- **Inner Circle (VIP):** numbers you marked as VIP in Pause — your closest people get your warmest message.
- **Saved Contacts:** anyone in your address book gets your standard reply.
- **Unknown Numbers:** unsaved numbers get a polite boundary message.

**Safety rules are always on:** blocked or muted numbers never get replies, shortcodes (bank OTP alerts, etc.) are ignored, and each number gets at most 3 auto-replies per hour so bots can't loop with each other.

A small set of functional keywords still works over WhatsApp for people who know them: **#urgent** (emergency alarm), **#location** (shares your saved location), **#photo** (shares your photo link), **#links** (shares your quick links), and **#dnd** (checks your quiet hours).

---

## Starred / VIP Call Detection

Pause treats your starred contacts as your inner circle:

- Their calls bypass silent mode and Do Not Disturb.
- Optional **repeat-caller bypass**: if anyone calls 3 times within 5 minutes, the third call rings through — persistence usually means urgency.
- VIPs can have their own divert number and their own auto-reply message.

---

## Focus Mode (Silent Guard) & Calendar Sync

**Focus Mode** is the master switch: unknown calls are rejected before your ringer disturbs you, WhatsApp and SMS auto-replies go out, and your chosen VIPs can still reach you.

**Calendar Sync** (in the **Schedule & Focus** section) connects Focus Mode to your real life:

- **Choose which calendars count** — Work, Personal, Meetings, or all of them.
- **Smart triggers** — Focus activates only when an event is marked **Busy** or its title contains keywords you choose (e.g. *Meeting, Interview, Focus*).
- **Live preview** — see your upcoming events and exactly which ones will trigger Focus Mode and which auto-reply will go out.
- When the event ends, Focus Mode switches itself off. No manual toggling, no forgotten silent phones.

---

## Privacy by Design

- **100% on-device.** Call filtering, keyword detection, auto-replies, and calendar checks all run locally. Pause makes zero network requests to any Pause server — because there isn't one.
- **No telemetry.** No analytics SDKs, no crash reporters phoning home, no advertising.
- **Hardware-encrypted storage.** Your rules, logs, and preferences live in a SQLCipher database encrypted with AES-256, with keys in the Android hardware keystore.
- **Your contacts never leave the phone.** Starred/VIP matching and contact lookups use Android's on-device contacts provider only.
- **WhatsApp replies use your internet, not a cloud.** Replies are sent through WhatsApp's own notification quick-reply action on your device.

---

## Permissions Used

Every permission exists for one on-device feature — nothing is collected or transmitted:

| Permission | Why Pause Needs It |
|---|---|
| Phone / Call Log | Detect incoming calls, identify unknown vs. starred callers, reject spam |
| SMS (Send / Receive / Read) | SMS auto-replies, scheduled SMS, URGENT keyword detection |
| Contacts | Match callers to VIPs / saved contacts / unknown numbers |
| Notifications (+ Notification Access) | Read WhatsApp notifications to send quick-reply auto-responses |
| Do Not Disturb Access | Let starred contacts ring through silent mode |
| Calendar (Read) | Calendar Focus Sync — detect Busy events |
| Location (optional) | Share your location via the #location keyword, only when you enable it |
| Bluetooth (optional) | Drive Mode auto-reply when connected to your car |

---

## Source & License

Pause is 100% open source under the MIT License: https://github.com/karanraj-ux/pause

*Pause — your attention, defended.*
