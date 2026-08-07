package com.example.lotteryapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.lotteryapp.data.dao.RaffleDao
import com.example.lotteryapp.data.dao.TicketDao
import com.example.lotteryapp.data.entity.CancellationHistory
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.dao.CancellationHistoryDao

@Database(
    entities = [Raffle::class, Ticket::class, CancellationHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun raffleDao(): RaffleDao
    abstract fun ticketDao(): TicketDao
    abstract fun cancellationHistoryDao(): CancellationHistoryDao
}