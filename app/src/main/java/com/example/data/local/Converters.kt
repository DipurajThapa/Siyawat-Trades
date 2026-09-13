package com.example.data.local

import androidx.room.TypeConverter
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
}
