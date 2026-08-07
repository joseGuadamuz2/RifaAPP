package com.example.lotteryapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RaffleDao {

    @Insert
    suspend fun insert(raffle: Raffle)

    @Update
    suspend fun update(raffle: Raffle)

    @Query("SELECT * FROM raffles WHERE id = :raffleId")
    suspend fun getById(raffleId: String): Raffle?

    @Query("SELECT * FROM raffles WHERE status = :status ORDER BY drawDate ASC")
    fun getByStatus(status: RaffleStatus): Flow<List<Raffle>>
}