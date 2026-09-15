package com.example.data.model

enum class RecordState(val label: String) {
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    CORRECTION_REQUESTED("Correction Requested"),
    PENDING_SECOND_APPROVAL("Pending 2nd Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    VOIDED("Voided"),
    COMPLETED("Completed")
}
