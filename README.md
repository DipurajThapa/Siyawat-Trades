# Siyawat Trades (سيواة للتجارة)
### Multi-Currency Capital Pool & Algorithmic Trading Treasury Ledger

[![Android CI](https://img.shields.io/badge/Platform-Android_14+-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Database](https://img.shields.io/badge/Storage-Room_SQLite-00758F?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Tests](https://img.shields.io/badge/Unit_Tests-16%2F16_Passed-success)](https://github.com/)

---

## 📌 Overview

**Siyawat Trades** is an offline-first Android application designed for high-integrity capital pooling, multi-currency treasury reconciliation, and algorithmic trading fund management. Built with **Jetpack Compose (Material Design 3)**, **Kotlin Coroutines & StateFlow**, and **Room Database**, the app implements Six Sigma financial math, rate-locking mechanisms, cryptographic-style audit logs, and on-device receipt vision/OCR verification.

---

## ✨ Key Features

### 1. Multi-Currency Treasury Management (AED, USD, INR)
- **Real-Time & Historical Rate Locking**: Every capital injection locks the exact foreign exchange rate at the second of transaction execution, ensuring immutable book value reproducibility.
- **Native AED Support**: Clean, un-duplicated currency notation throughout the interface (e.g. `AED 25,000`).
- **Dynamic Valuation**: Pool totals, capital contributions, and trading yields are aggregated in real-time.

### 2. Six Sigma Zero-Loss Ledger
- **Stage Lifecycle**:
  - `Capital Injection` (Pool contributor deposits)
  - `Bank to Exchange` (Wire transfer to Binance / OKX)
  - `Exchange to Crypto` (USDT / USDC conversion)
  - `P2P Trading` (Arbitrage & algorithmic trading cycle)
  - `Profit / Loss Realization`
  - `Distribution / Payout` (Liquid profit payouts with overdraw prevention)
- **Overdraw Protection**: Automatic validation prevents admin distributions that exceed current available pool cash.

### 3. Screenshot OCR & Fraud Detection
- **Android Photo Picker**: Zero-permission media selection adhering strictly to modern Google Play privacy guidelines (`PickVisualMedia`).
- **On-Device Vision & OCR**: Detects transaction amounts and bank reference IDs automatically from bank wire advice slips and exchange confirmations.
- **Intelligent Field Extraction**: Automatically populates transaction fields with review cards.
- **Missing Information Prompts**: Displays clear notifications if the amount or reference number cannot be determined with high confidence.
- **Suspicious Activity Flagging**:
  - Detects duplicate transaction IDs already recorded in the system.
  - Flags mismatches between entered amounts and screenshot values.
  - Detects malformed or test reference IDs.
  - Flags entries as *"Potentially suspicious — requires verification"* for administrator audit.

### 4. Immutable Audit Trail & Administrative Governance
- **Role-Based Access Control**:
  - **Admin**: Disburse returns, approve/verify pending transactions, manage members, perform rollbacks.
  - **Member / Investor**: View pool ledger, submit deposits, review personal equity and historical yield.
- **Mandatory Reversal Justifications**: Transactions cannot be arbitrarily deleted; status alterations and reversals require an audited reason.
- **Export Capabilities**: Complete CSV ledger export for accounting and regulatory compliance.

---

## 🏗 Architecture & Tech Stack

```
com.example/
├── data/
│   ├── local/            # Room Database, DAOs, Entity models
│   ├── model/            # Domain models (Currencies, Transaction stages, Users)
│   └── repository/       # Repository layer with transactional state management
├── ui/
│   ├── components/       # Material 3 UI components (Cards, Dialogs, OCR reviews)
│   ├── theme/            # Material Design 3 color palette, typography & shapes
│   ├── MainScreen.kt     # Primary pool dashboard & ledger navigation
│   └── PoolViewModel.kt  # StateFlow reactive view models
└── util/
    ├── FormatUtils.kt                 # Financial currency formatting & math
    ├── ProofReceiptGenerator.kt       # High-fidelity digital slip generator
    └── TransactionScreenshotAnalyzer.kt # Vision & OCR text analysis engine
```

- **Architecture**: Single Activity, MVVM (Model-View-ViewModel) with Unidirectional Data Flow (UDF).
- **Concurrency**: Kotlin Coroutines & StateFlow.
- **Persistence**: Room DB with SQLite.
- **Image Handling**: Coil & Android Canvas.
- **Testing**: Robolectric (JVM SDK 36), Roborazzi (Screenshot verification), and JUnit 4.

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17 or JDK 21
- Android SDK 34 / 35 (Android 14 / 15)

### Build via Command Line
```bash
# Clone the repository
git clone https://github.com/your-org/siyawat-trades.git
cd siyawat-trades

# Build the Debug APK
gradle assembleDebug

# Run all unit and Robolectric tests
gradle :app:testDebugUnitTest

# Verify UI Screenshot tests (Roborazzi)
gradle :app:verifyRoborazziDebug
```

---

## 🧪 Testing Matrix

All 16 unit, integration, and screenshot tests pass with zero errors or failures:

| Test Class | Test Count | Description |
| :--- | :---: | :--- |
| `TransactionScreenshotAnalyzerTest` | 7 | Validates OCR amount extraction, reference matching, missing notifications, and suspicious flag classification. |
| `PoolLedgerTest` | 6 | Validates ledger math, admin overdraw limits, rate locking, reversal reasons, and CSV exports. |
| `GreetingScreenshotTest` | 1 | Roborazzi visual regression test. |
| `ExampleRobolectricTest` | 1 | Android resource bundle and context verification. |
| `ExampleUnitTest` | 1 | Basic JVM execution sanity test. |

---

## 📖 User Guide

For detailed step-by-step instructions on depositing funds, verifying screenshots, and exporting audit ledgers, see [docs/HOW_TO_USE.md](docs/HOW_TO_USE.md).

---

## 🔒 Security & Privacy

- **No Broad Storage Permissions**: The app does not request `READ_EXTERNAL_STORAGE` or `READ_MEDIA_IMAGES`. It uses the Android system Photo Picker.
- **Local Data Confidentiality**: Financial records reside securely on-device within private application sandboxing.
- **Non-Destructive Auditing**: Reversals append an audit trail entry rather than erasing ledger history.

---

## 📄 License
Internal proprietary license for **Siyawat Trades**. All rights reserved.
