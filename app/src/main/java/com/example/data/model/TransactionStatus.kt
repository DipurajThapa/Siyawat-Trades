package com.example.data.model

enum class TransactionStatus(val label: String) {
    PENDING_VERIFICATION("Pending Bank Verification"),
    VERIFIED("Verified & Credited"),
    COMPLETED("Completed"),
    FLAGGED("Flagged / Attention Req."),
    CANCELLED("Cancelled"),
    REVERSED("Reversed")
}
