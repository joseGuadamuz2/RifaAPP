package com.example.lotteryapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lotteryapp.data.dao.CancellationHistoryDao
import com.example.lotteryapp.data.dao.RaffleDao
import com.example.lotteryapp.data.dao.TicketDao
import com.example.lotteryapp.data.entity.CancellationHistory
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.Ticket

@Database(
    entities = [Raffle::class, Ticket::class, CancellationHistory::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun raffleDao(): RaffleDao
    abstract fun ticketDao(): TicketDao
    abstract fun cancellationHistoryDao(): CancellationHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE raffles ADD COLUMN modality TEXT NOT NULL DEFAULT 'SENCILLA'"
                )
                db.execSQL(
                    "ALTER TABLE raffles ADD COLUMN groupSize INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}