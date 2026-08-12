package com.example.lotteryapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lotteryapp.data.entity.Winner
import kotlinx.coroutines.flow.Flow

@Dao
interface WinnerDao {

    @Insert
    suspend fun insert(winner: Winner)

    @Update
    suspend fun update(winner: Winner)

    @Delete
    suspend fun delete(winner: Winner)

    @Query("SELECT * FROM winners WHERE raffleId = :raffleId ORDER BY registeredAt DESC")
    fun getByRaffle(raffleId: String): Flow<List<Winner>>

    @Query("SELECT COUNT(*) FROM winners WHERE raffleId = :raffleId")
    suspend fun getCountByRaffle(raffleId: String): Int

    @Query("SELECT * FROM winners WHERE id = :id")
    suspend fun getById(id: String): Winner?

    @Query("SELECT * FROM winners WHERE raffleId = :raffleId AND winningNumber = :winningNumber LIMIT 1")
    suspend fun getByNumber(raffleId: String, winningNumber: String): Winner?
}