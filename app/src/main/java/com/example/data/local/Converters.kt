package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ReconciliationState
import com.example.data.model.RecordState
import com.example.data.model.SettlementState
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus

class Converters {
    @TypeConverter
    fun fromTransactionStage(value: TransactionStage?): String? = value?.name

    @TypeConverter
    fun toTransactionStage(value: String?): TransactionStage? =
        value?.let { runCatching { TransactionStage.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromTransactionStatus(value: TransactionStatus?): String? = value?.name

    @TypeConverter
    fun toTransactionStatus(value: String?): TransactionStatus? =
        value?.let { runCatching { TransactionStatus.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromRecordState(value: RecordState?): String? = value?.name

    @TypeConverter
    fun toRecordState(value: String?): RecordState? =
        value?.let { runCatching { RecordState.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromSettlementState(value: SettlementState?): String? = value?.name

    @TypeConverter
    fun toSettlementState(value: String?): SettlementState? =
        value?.let { runCatching { SettlementState.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromReconciliationState(value: ReconciliationState?): String? = value?.name

    @TypeConverter
    fun toReconciliationState(value: String?): ReconciliationState? =
        value?.let { runCatching { ReconciliationState.valueOf(it) }.getOrNull() }
}
