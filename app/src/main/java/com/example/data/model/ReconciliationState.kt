package com.example.data.model

enum class ReconciliationState(val label: String) {
    UNRECONCILED("Unreconciled"),
    PENDING_USER_CONFIRM("Awaiting User Confirm"),
    CONFIRMED_BY_USER("Confirmed by User"),
    CONFIRMED_BY_TIMEOUT("Auto-Confirmed (72h SLA)"),
    DISPUTED("Disputed / Attention"),
    RECONCILED("Fully Reconciled")
}
