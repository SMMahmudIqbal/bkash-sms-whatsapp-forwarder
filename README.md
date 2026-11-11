# bKash SMS to WhatsApp Auto Forwarder

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%E2%80%9336%29-3DDC84.svg?style=flat&logo=android)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material%203-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Private%20%2F%20Proprietary-red.svg?style=flat)]()

An automated, private Android background application that intercepts incoming **bKash** transaction SMS notifications, parses transaction metadata (TrxID, Amount, Type, Counterpart, Balance, Fee, Reference), deduplicates via a local database, and securely dispatches them to a designated **WhatsApp** recipient using the **official WhatsApp Business Cloud API** (Meta Graph API) or custom webhooks.

---

## 🚀 Key Features

### 1. Reliable bKash SMS Interception & Parsing
- Detects SMS originating from bKash official senders (`bKash`, `16247`, `BKASH`).
- Precision regex extracts:
  - **Transaction ID (TrxID)**: e.g. `BL49XXK98`
  - **Transaction Type**: *Cash In*, *Send Money*, *Payment Received*, *Remittance*, *Add Money*, *Mobile Recharge*, *Cash Out*.
  - **Amount**: ৳ Currency value formatted cleanly.
  - **Counterpart Number/Name**: Originating or destination party.
  - **New Balance & Fee**: Real-time account balance tracking.
  - **Reference**: Invoice or note references if provided.
- Ignores all non-bKash personal or commercial SMS automatically.

### 2. 🛡️ Strict Security & Privacy Guard
- **Zero OTP/PIN Leakage**: The parser runs a mandatory security audit on every incoming message. Any message containing sensitive credentials (e.g. `OTP`, `verification code`, `PIN`, `password`, `do not share`) is **immediately dropped** and will **never** be forwarded or logged.
- Only verified financial transactions containing valid Transaction IDs (TrxID) are processed.
- Credentials stored securely in private application preferences.

### 3. Duplicate Forwarding Prevention
- Built-in SQLite database with a `UNIQUE` index constraint on `trx_id`.
- If an incoming SMS contains an already processed Transaction ID, it is flagged as `IGNORED_DUPLICATE` to prevent multiple WhatsApp alerts for the same transaction.

### 4. Dual WhatsApp Dispatch Engine
- **Meta WhatsApp Business Cloud API (Official)**: Direct integration with `https://graph.facebook.com/v21.0/{phone_number_id}/messages` using permanent System User Access Tokens.
- **Custom Webhook / Gateway Support**: Full compatibility with CallMeBot or private WhatsApp gateway servers (Baileys, Evolution API, Green API, HTTP Webhooks).
- **Test WhatsApp Message**: Test your connection directly from the settings dialog with instant delivery feedback.

### 5. 24/7 Background Persistence & Resilience
- **Foreground Monitoring Service**: Runs continuously with a low-priority ongoing notification.
- **Reboot Auto-Restart**: Automatically resumes monitoring after phone restarts or updates (`BOOT_COMPLETED`).
- **Battery Optimization Whitelist**: Integrated one-tap prompt to exempt the application from aggressive OEM task killers (MIUI, OneUI, ColorOS).
- **Offline Queuing & Retry**: Failed dispatches due to transient network drops are automatically enqueued in `WorkManager` with exponential backoff and a `NetworkType.CONNECTED` constraint.

### 6. Minimalist Dashboard UI
- Clean, distraction-free Material 3 dark/light responsive interface inspired by Nothing OS and Linear.
- Real-time service status banner (`Active & Listening` / `Paused`).
- Stat counters: **Total Detected**, **Successfully Forwarded**, and **Pending/Failed**.
- Full transaction history log with status badges and detail view.
- Manual one-tap retry for individual failed records or bulk retry.

---

## 📱 Screenshots & Architecture

```
┌────────────────────────────────────────────────────────┐
│                   bKash SMS Received                   │
│   (From: bKash / 16247, Body: Tk 1,500.00 TrxID: ...)  │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
              ┌───────────────────────────┐
              │    Security Blacklist     │──► [Contains OTP/PIN?] ──► DROPPED
              └─────────────┬─────────────┘
                            │ Passed
                            ▼
              ┌───────────────────────────┐
              │     BKashSmsParser        │──► Extracts TrxID, Amount, Type
              └─────────────┬─────────────┘
                            │
                            ▼
              ┌───────────────────────────┐
              │   Database Deduplication  │──► [Already in DB?] ──► SKIPPED
              └─────────────┬─────────────┘
                            │ New TrxID
                            ▼
              ┌───────────────────────────┐
              │    WhatsApp Cloud API     │──► Dispatch formatted markdown
              └─────────────┬─────────────┘
                            │
            ┌───────────────┴───────────────┐
            │                               │
        [Success]                       [Failure]
            ▼                               ▼
    Mark Status: SENT              Mark: FAILED / Enqueue WorkManager
```

---

## ⚙️ Configuration Guide

1. **Install APK**: Download and install `bKash_SMS_Forwarder_v1.0.apk` onto your Android phone.
2. **Grant Permissions**:
   - Tap **Grant** on the dashboard to allow `RECEIVE_SMS` and `READ_SMS`.
   - On Android 13+, allow notification posting so the background monitoring service stays alive.
   - Tap **Whitelist** to exempt the app from battery optimization.
3. **Configure WhatsApp**:
   - Tap **⚙️ Settings** on the top right.
   - Enter your **WhatsApp Recipient Number** (format: `88017XXXXXXXX`).
   - Choose your delivery mode:
     - **Meta Cloud API**: Enter your `Phone Number ID` and `Access Token` from your Meta Developer App.
     - **Custom Webhook**: Enter your gateway URL or CallMeBot URL.
   - Tap **Send Test WhatsApp Message** to verify your setup.
   - Tap **Save Settings**.
4. **Activate**: Toggle the **Auto Forwarding Service** switch to **ON**. The app will now forward all incoming bKash transaction alerts automatically without any manual interaction.

---

## 🛠️ Tech Stack & Standards

- **Language**: Kotlin 2.2+ (JVM 17)
- **UI Framework**: Jetpack Compose Material 3
- **Networking**: OkHttp 4.12.0
- **Storage**: Android SQLite Database (`SQLiteOpenHelper`) with Unique TrxID Constraints
- **Background Engine**: Android Foreground Service (`foregroundServiceType="dataSync"`), BroadcastReceiver, WakeLock
- **Offline Retries**: AndroidX WorkManager 2.10.0
- **Target SDK**: Android API 36 (Android 15/16 ready), Min SDK: API 26 (Android 8.0+)

---

## 👨‍💻 Author

Developed by **S. M. Mahmud Iqbal**  
*Email: smmahmudiqbal@gmail.com*
