# bKash SMS to WhatsApp Auto Forwarder

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%E2%80%9336%29-3DDC84.svg?style=flat&logo=android)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material%203-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat)]()
[![Visibility](https://img.shields.io/badge/Visibility-Public-success.svg?style=flat)]()

An automated Android background application that intercepts incoming **bKash** transaction SMS notifications, parses transaction metadata (TrxID, Amount, Type, Counterpart, Balance, Fee, Reference), deduplicates via a local database, and securely dispatches them to a designated **WhatsApp** recipient using the **official WhatsApp Business Cloud API** (Meta Graph API) or custom webhooks.

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

### 4. WhatsApp Group & Multi-Provider Dispatch Engine
- **WhatsApp Group Forwarding**: Seamlessly forward directly into any WhatsApp Group via Group JIDs (e.g. `120363040377656518@g.us`).
- **Exact Copy-Paste (Raw SMS) Mode**: Forwards the exact verbatim bKash transaction message received on your phone without modifying or reformatting the text.
- **Structured Card Mode**: Alternative option to format transactions with icons, breakdown, and timestamps.
- **TextMeBot Integration**: Instant WhatsApp Group & 1-to-1 message dispatching with zero server setup.
- **Custom Webhook / WhatsApp Gateway Support**: Full compatibility with UltraMsg, Green API, Evolution API, Baileys, and custom HTTP Webhooks.
- **Meta WhatsApp Business Cloud API (Official)**: Direct integration with Meta Graph API using permanent System User Access Tokens.
- **Interactive In-App Share & Copy**: One-tap buttons to copy the raw bKash SMS or share directly to WhatsApp.
- **Test WhatsApp Message**: Test your connection directly to your WhatsApp Group or phone with instant delivery feedback.

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

## ⚙️ Configuration & Permission Guide

### 🛡️ Android 13 / 14 / 15 / 16: "App was denied access to SMS" Fix
On modern Android versions, Google automatically locks sensitive permissions (SMS) on downloaded APKs under a security feature called **Restricted Settings**. You can easily unlock it in 10 seconds:

1. Open phone **Settings** ➔ **Apps** ➔ **bKash Forwarder** (or tap **"How to Allow"** ➔ **"Open App Settings Now"** directly inside the app).
2. Look at the top-right corner and tap the **3 vertical dots (⋮)** (or **"More"**).
3. Tap **"Allow restricted settings"** and confirm your fingerprint or screen lock PIN.
4. Tap **Permissions** ➔ **SMS** ➔ select **Allow**.
5. Return to the app — direct SMS forwarding is now fully unlocked!

---

### 🔔 Alternative: Notification Interception Engine
If you prefer not to unlock restricted SMS settings or if your device firmware restricts SMS access:
- Simply tap **"Turn On"** under **Notification Interception** in the dashboard.
- This enables `BKashNotificationListenerService` which captures incoming bKash transaction alerts directly from your SMS app (Google Messages, Samsung Messages) and the official bKash app.
- Both engines can be active simultaneously — built-in database deduplication guarantees **zero duplicate messages**.

---

## 🚀 Setup Steps

1. **Install APK**: Download and install [`bKash_SMS_Forwarder_v1.0.apk`](./bKash_SMS_Forwarder_v1.0.apk) on your Android phone.
2. **Enable Channel**: Follow the steps above to unlock SMS or enable Notification Access.
3. **Exempt from Battery Saver**: Tap **Whitelist** to exempt the app from OEM battery optimizations.
4. **Configure WhatsApp**:
   - Tap **⚙️ Settings** on the top right.
   - Enter your **WhatsApp Recipient Number** (format: `88017XXXXXXXX`).
   - Enter your Meta **Phone Number ID** & **Access Token** (or select **Custom Webhook** for CallMeBot / private gateway).
   - Tap **Send Test WhatsApp Message** to verify your setup.
   - Tap **Save Settings**.
5. **Activate**: Toggle the **Auto Forwarding Service** switch to **ON**.

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
