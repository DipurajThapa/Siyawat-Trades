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

### 1. 3-Tier Decoupled Lifecycle State Machine
Financial integrity is enforced by decoupling transaction stages into three independent dimensions:
- **Record State (Internal Authorization)**:
  - `SUBMITTED`: Deposit or movement logged by member/operator.
  - `PENDING_SECOND_APPROVAL`: Triggered automatically for high-value transactions (≥ $10,000 USD).
  - `APPROVED`: Authorized by admin (or dual admins for high-value transfers).
  - `REJECTED`: Declined by administrator with mandatory explanation.
  - `CORRECTION_REQUESTED`: Sent back to submitter for revision.
  - `VOIDED`: Cancelled prior to settlement execution.
- **Settlement State (External Bank/Crypto Execution)**:
  - `UNSETTLED`: Awaiting external fund transfer by treasury desk.
  - `IN_TRANSIT`: External wire or exchange transfer in process.
  - `SETTLED`: Confirmed with mandatory Bank UTR or Blockchain TxHash reference.
  - `FAILED`: External wire bounced or rejected by banking rail.
  - `REVERSED`: Compensating reversal transaction executed.
- **Reconciliation State (End-to-End Delivery & Disputes)**:
  - `PENDING_USER_CONFIRM`: Settlement recorded; 72-hour countdown window opened for beneficiary receipt confirmation.
  - `CONFIRMED_BY_USER`: Beneficiary explicitly verified funds in destination account.
  - `CONFIRMED_BY_TIMEOUT`: Auto-reconciled after 72-hour SLA window without dispute.
  - `DISPUTED`: Beneficiary flagged non-delivery or amount discrepancy.
  - `RECONCILED` / `UNRECONCILED`: Admin resolved dispute via bank tracer investigation.

### 2. High-Value Dual-Control Governance (Maker-Checker)
- Any transaction with a converted value ≥ **$10,000 USD** automatically mandates two distinct administrative signatures.
- **Maker**: First administrator approves the transaction details and attaches banking authorization.
- **Checker**: A separate, distinct administrator must review and counter-sign before funds can be settled. The Maker cannot self-approve as Checker.

### 3. FIFO USDT Inventory Engine & Realized P&L
- **First-In, First-Out (FIFO) Lot Accounting**: Every crypto acquisition (USDT purchase) creates an immutable lot with its acquisition timestamp, rate, quantity, and cost basis.
- **Automated Lot Liquidation**: When selling or distributing USDT, the engine consumes oldest lots first, computing exact cost basis and realized profit or loss.
- **Visual Lot Inspector**: Admins and members can inspect discrete active inventory lots, remaining quantities, and blended average costs via the **FIFO Lot Audit Dialog**.

### 4. Robust Camera, Screenshots Folder & OCR Vision
- **Zero-Crash FileProvider**: Standardized Android FileProvider architecture supporting app-internal cache, external cache, and dedicated screenshots directories.
- **SAF Persistence Isolation**: Selected receipts and camera captures are safely duplicated to `context.cacheDir`, preventing URI permission drops.
- **Fallback Image Decoders**: Resilient fallback to Android `BitmapFactory` ensures flawless OCR across Samsung, OnePlus, Xiaomi, and Google Pixel screenshot formats.
- **International & Indian Numbering Formats**: ML Kit regex parses standard (`150,000.00`) and Indian lakhs/crores (`1,50,000.00`) accurately.

### 5. Mobile-Ergonomic Fullscreen Experience
- **Display Cutout Integration**: Seamless edge-to-edge support utilizing `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`.
- **Adaptive System Bar Hiding**: Fullscreen toggle in the ergonomic bottom action bar hides system status and navigation bars with swipe-to-reveal gesture support.
- **Clean Action Bar Layout**: Account switcher, cloud backup, and fullscreen controls positioned in the bottom navigation bar for comfortable one-handed mobile operation.

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
