package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PoolTransactionDao {
    @Query("SELECT * FROM pool_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<PoolTransactionEntity>>

    @Query("SELECT * FROM pool_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): PoolTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: PoolTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<PoolTransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: PoolTransactionEntity)

    @Query("UPDATE pool_transactions SET status = :status, verifiedByEmail = :verifiedBy, approvalStatus = :approvalStatus, approvedByEmail = :verifiedBy, approvalTimestamp = :approvalTimestamp, approvalDate = :approvalDate, approvalNotes = :approvalNotes, approvalScreenshotUri = COALESCE(:approvalScreenshotUri, proofUri) WHERE id = :id")
    suspend fun updateApproval(
        id: Long,
        status: String,
        verifiedBy: String,
        approvalStatus: String,
        approvalTimestamp: Long,
        approvalDate: String,
        approvalNotes: String?,
        approvalScreenshotUri: String?
    )

    @Query("SELECT * FROM pool_transactions WHERE approvalStatus = 'APPROVED' OR status = 'VERIFIED' OR status = 'COMPLETED' ORDER BY timestamp DESC")
    fun getApprovedTransactions(): Flow<List<PoolTransactionEntity>>

    @Query("UPDATE pool_transactions SET status = :status, verifiedByEmail = :verifiedBy WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, verifiedBy: String)

    @Query("UPDATE pool_transactions SET driveFileId = :fileId, driveWebViewLink = :webViewLink WHERE id = :id")
    suspend fun updateDriveInfo(id: Long, fileId: String, webViewLink: String)

    @Query("DELETE FROM pool_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT * FROM pool_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(): List<PoolTransactionEntity>

    @Query("SELECT * FROM pool_transactions WHERE referenceNo = :ref LIMIT 1")
    suspend fun getByReferenceNo(ref: String): PoolTransactionEntity?

    @Query("UPDATE pool_transactions SET status = :status, reversalReason = :reason, reversedByEmail = :reversedBy, reversalTimestamp = :timestamp, notes = :notes WHERE id = :id")
    suspend fun updateReversal(
        id: Long,
        status: String,
        reason: String,
        reversedBy: String,
        timestamp: Long,
        notes: String
    )

    @Query("SELECT COUNT(*) FROM pool_transactions")
    suspend fun getCount(): Int

    @Query("SELECT * FROM pool_transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<PoolTransactionEntity>>

    @Query("SELECT * FROM pool_transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsBetweenSync(startTime: Long, endTime: Long): List<PoolTransactionEntity>

    @Query("SELECT * FROM pool_transactions WHERE timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsUpToSync(endTime: Long): List<PoolTransactionEntity>

    @Query("DELETE FROM pool_transactions")
    suspend fun clearAll()
}
