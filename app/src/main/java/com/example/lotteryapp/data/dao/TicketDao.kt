package com.example.lotteryapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    @Insert
    suspend fun insertAll(tickets: List<Ticket>)

    @Update
    suspend fun update(ticket: Ticket)

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getById(id: String): Ticket?

    @Query("SELECT * FROM tickets WHERE raffleId = :raffleId ORDER BY number ASC")
    fun getByRaffle(raffleId: String): Flow<List<Ticket>>

    @Query(
        """
        SELECT * FROM tickets 
        WHERE raffleId = :raffleId 
        AND status != 'AVAILABLE'
        AND (buyerName LIKE '%' || :query || '%' OR number LIKE '%' || :query || '%' OR buyerPhone LIKE '%' || :query || '%')
        ORDER BY number ASC
        """
    )
    fun searchSoldOrReserved(raffleId: String, query: String): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE raffleId = :raffleId AND number = :number LIMIT 1")
    suspend fun getByNumber(raffleId: String, number: String): Ticket?

    @Query("SELECT * FROM tickets WHERE groupId = :groupId")
    suspend fun getByGroupId(groupId: String): List<Ticket>

    @Query("SELECT COUNT(*) FROM tickets WHERE raffleId = :raffleId")
    fun getCountByRaffle(raffleId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM tickets WHERE raffleId = :raffleId AND status = :status")
    fun getCountByStatus(raffleId: String, status: TicketStatus): Flow<Int>

}