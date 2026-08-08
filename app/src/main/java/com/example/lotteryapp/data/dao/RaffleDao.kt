package com.example.lotteryapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Delete
    suspend fun delete(raffle: Raffle)

    @Query("SELECT * FROM raffles WHERE id = :raffleId")
    suspend fun getById(raffleId: String): Raffle?

    @Query("SELECT * FROM raffles WHERE status = :status ORDER BY drawDate ASC")
    fun getByStatus(status: RaffleStatus): Flow<List<Raffle>>

    @Query("SELECT * FROM raffles WHERE status = :status AND name LIKE '%' || :query || '%' ORDER BY drawDate ASC")
    fun searchByStatus(status: RaffleStatus, query: String): Flow<List<Raffle>>

}