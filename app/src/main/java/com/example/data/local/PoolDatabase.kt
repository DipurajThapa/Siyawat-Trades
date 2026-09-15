package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PoolTransactionEntity::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PoolDatabase : RoomDatabase() {
    abstract fun transactionDao(): PoolTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: PoolDatabase? = null

        fun getDatabase(context: Context): PoolDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PoolDatabase::class.java,
                    "usdt_pool_ledger.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
