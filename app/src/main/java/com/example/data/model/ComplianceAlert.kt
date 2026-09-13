package com.example.data.model

enum class AlertSeverity {
    CRITICAL, // Red visual flag
    WARNING,  // Amber/Yellow visual flag
    INFO      // Blue/Teal visual flag
}

data class ComplianceAlert(
    val id: String,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val actionText: String? = null,
    val affectedTransactionIds: List<Long> = emptyList(),
    val targetStageFilter: TransactionStage? = null
)
