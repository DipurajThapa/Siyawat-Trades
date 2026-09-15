package com.example.data.model

enum class SettlementState(val label: String) {
    UNSETTLED("Unsettled"),
    IN_TRANSIT("In Transit"),
    SETTLED("Settled"),
    FAILED("Failed"),
    REVERSED("Reversed")
}
