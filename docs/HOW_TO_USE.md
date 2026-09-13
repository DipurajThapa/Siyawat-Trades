# Siyawat Trades — User Manual & Operations Guide

This manual covers day-to-day operations for administrators and members using the Siyawat Trades treasury ledger application.

---

## Table of Contents
1. [User Roles & Access Levels](#1-user-roles--access-levels)
2. [Adding Capital / Money Transfer](#2-adding-capital--money-transfer)
3. [Transaction Confirmation Screenshots & OCR](#3-transaction-confirmation-screenshots--ocr)
4. [Suspicious Activity & Verification System](#4-suspicious-activity--verification-system)
5. [Tracking the Pool Treasury Lifecycle](#5-tracking-the-pool-treasury-lifecycle)
6. [Disbursing Profits & Overdraw Safeguards](#6-disbursing-profits--overdraw-safeguards)
7. [Reversals, Corrections & Audit Trail](#7-reversals-corrections--audit-trail)
8. [Exporting Ledger Reports (CSV)](#8-exporting-ledger-reports-csv)

---

## 1. User Roles & Access Levels

The application supports multiple user profiles switchable from the top user chip:
- **Pool Administrator (`dipuraj.thapa@gmail.com`)**:
  - Authorized to execute fund disbursements and profit distributions.
  - Reviews pending deposits and verifies suspicious activity flags.
  - Can reverse erroneous entries with required audit justifications.
  - Manages stage transitions (Bank to Exchange, Crypto conversion, P2P cycles).
- **Contributing Member / Investor**:
  - Record capital contributions with wire advice slips.
  - Inspect equity stake and share of pool returns.
  - View overall pool health and audit logs.

---

## 2. Adding Capital / Money Transfer

1. Tap **"+ Add Money"** on the dashboard.
2. **Select Transfer Currency**: Choose between **AED**, **USD**, or **INR**. The screen displays the locked conversion rate (e.g. `1 USD = 3.6725 AED`).
3. **Enter Amount**: Type the transfer amount or use the quick preset chips (e.g. `AED 2,000`, `AED 5,000`, `AED 10,000`, `AED 25,000`).
4. **Enter Bank Reference Number**: Provide the wire transfer confirmation code or slip reference.
5. **Attach Screenshot Proof**: (See Section 3 below).
6. **Submit**: Click **Submit Transfer**. The entry is timestamped, locked, and recorded in the Room ledger.

---

## 3. Transaction Confirmation Screenshots & OCR

Every deposit strictly requires an image receipt before submission:
- **Upload via Photo Picker**: Tap **"Upload Screenshot"** to select any PNG, JPG, or WEBP confirmation image from your device gallery.
- **On-Device OCR Extraction**:
  - The embedded vision engine scans the image for numerical amounts and reference codes.
  - If found, an **"Extracted Data Review"** card will appear.
  - If your fields were empty, the system automatically auto-populates the detected values. You can also tap **"Apply"** to replace existing values.
- **Instant Digital Slip Generation**: If conducting an offline or cash-to-bank settlement, tap **"Generate Slip"** to render a cryptographic receipt bitmap with verified timestamp, user identity, and transaction UUID.

---

## 4. Suspicious Activity & Verification System

To prevent duplicate submissions or fraudulent receipts, the analyzer performs continuous consistency checks:
- **Missing Information**: If a screenshot is blurry, cropped, or lacks crucial fields, the app prompts:
  - *"Transaction amount could not be found in the uploaded image."*
  - *"Transaction ID could not be found in the uploaded image."*
  - *"Both transaction amount and transaction ID could not be found in the uploaded image."*
  - You can tap **"Replace Screenshot"** to submit a clearer image or manually adjust the fields.
- **Suspicious Flagging**:
  - **Duplicate Reference ID**: The system checks local ledger history and flags reuse of past transaction IDs.
  - **Amount Discrepancy**: If you type `15,000` but the slip reads `5,000`, a warning banner alerts the user.
  - **Malformed References**: Extremely short or dummy reference numbers (e.g. `TEST`) are flagged.
  - **Action**: Entries are classified as *"Potentially suspicious — requires verification."* and submitted with audit tags for manual administrator verification.

---

## 5. Tracking the Pool Treasury Lifecycle

The central ledger tracks transactions through 6 distinct stages:
1. **Capital Injection**: Contributor funds arriving in the central banking institution.
2. **Bank to Exchange**: Liquid capital transferred to Binance / OKX fiat gateways.
3. **Exchange to Crypto**: Fiat converted into stable collateral (USDT / USDC).
4. **P2P Trading & Arbitrage**: Capital utilized in algorithmic market-making cycles.
5. **Profit Realization**: Closed trading yield.
6. **Disbursement**: Principal or dividend returns paid out to contributors.

---

## 6. Disbursing Profits & Overdraw Safeguards

1. Open the Admin Actions panel.
2. Tap **"Disburse Funds"**.
3. The system computes the current **Available Liquid Balance**.
4. **Overdraw Guard**: If the entered disbursement exceeds available liquid capital, the system immediately disables submission and prompts an overdraw alert to prevent balance insolvency.
5. Confirming disbursement creates an audit entry and updates pool balances.

---

## 7. Reversals, Corrections & Audit Trail

- **No Silent Deletions**: In adherence to financial auditing standards, past transactions cannot be erased without a trace.
- **Audited Reversal**: An administrator can mark a transaction as **"REVERSED"**.
- **Mandatory Reason**: The user must provide an explanatory note (e.g., *"Duplicate wire advice receipt submitted by depositor"*).
- The ledger retains both the original entry and the reversal record in historical logs.

---

## 8. Exporting Ledger Reports (CSV)

1. Navigate to the **Audit / History** section.
2. Tap **"Export Ledger (CSV)"**.
3. A formatted CSV document is compiled, containing:
   - Transaction UUID
   - Execution Timestamp (ISO 8601)
   - Stage & Category
   - User Name & Email
   - Amount (Fiat & Normalized USD)
   - Currency Code & Locked Exchange Rate
   - Bank Reference Number
   - Verification Status & Audit Remarks
4. The CSV can be saved locally or shared directly to accounting software (Excel, Google Sheets).
