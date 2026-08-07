package com.example.lotteryapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.lotteryapp.data.entity.CancellationHistory

@Dao
interface CancellationHistoryDao {

    @Insert
    suspend fun insert(record: CancellationHistory)

    @Query("SELECT * FROM cancellation_history WHERE ticketId = :ticketId ORDER BY cancellationDate DESC")
    suspend fun getByTicket(ticketId: String): List<CancellationHistory>
}