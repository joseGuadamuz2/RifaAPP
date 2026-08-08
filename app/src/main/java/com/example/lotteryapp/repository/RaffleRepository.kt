package com.example.lotteryapp.repository

import com.example.lotteryapp.data.dao.CancellationHistoryDao
import com.example.lotteryapp.data.dao.RaffleDao
import com.example.lotteryapp.data.dao.TicketDao
import com.example.lotteryapp.data.entity.CancellationHistory
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleStatus
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RaffleRepository(
    private val raffleDao: RaffleDao,
    private val ticketDao: TicketDao,
    private val cancellationHistoryDao: CancellationHistoryDao
) {

    // --- Rifas ---

    suspend fun createRaffle(raffle: Raffle) {
        raffleDao.insert(raffle)
        val tickets = (0..99).map { number ->
            Ticket(
                raffleId = raffle.id,
                number = number.toString().padStart(2, '0')
            )
        }
        ticketDao.insertAll(tickets)
    }

    suspend fun getRaffleById(raffleId: String): Raffle? = raffleDao.getById(raffleId)

    suspend fun updateRaffle(raffle: Raffle) = raffleDao.update(raffle)

    suspend fun deleteRaffle(raffle: Raffle) = raffleDao.delete(raffle)
    fun getTicketsForRaffle(raffleId: String): Flow<List<Ticket>> =
        ticketDao.getByRaffle(raffleId)

    // --- Venta / apartado ---

    suspend fun sellOrReserveTicket(
        ticket: Ticket,
        buyerName: String,
        buyerPhone: String?,
        status: TicketStatus
    ): Ticket {
        val updated = ticket.copy(
            buyerName = buyerName,
            buyerPhone = buyerPhone,
            status = status
        )
        ticketDao.update(updated)
        return updated
    }

    suspend fun changeTicketStatus(ticket: Ticket, newStatus: TicketStatus): Ticket {
        val updated = ticket.copy(status = newStatus)
        ticketDao.update(updated)
        return updated
    }

    suspend fun updateBuyerPhone(tickets: List<Ticket>, newPhone: String?): List<Ticket> {
        val updated = tickets.map { it.copy(buyerPhone = newPhone) }
        updated.forEach { ticketDao.update(it) }
        return updated
    }


    suspend fun sellOrReserveGroup(
        tickets: List<Ticket>,
        buyerName: String,
        buyerPhone: String?,
        status: TicketStatus
    ): List<Ticket> {
        val groupId = UUID.randomUUID().toString()
        val updated = tickets.map { ticket ->
            ticket.copy(
                buyerName = buyerName,
                buyerPhone = buyerPhone,
                status = status,
                groupId = groupId
            )
        }
        updated.forEach { ticketDao.update(it) }
        return updated
    }

    // --- Cancelación ---

    suspend fun cancelTicket(ticket: Ticket) {
        cancellationHistoryDao.insert(
            CancellationHistory(
                ticketId = ticket.id,
                previousBuyerName = ticket.buyerName ?: "",
                previousBuyerPhone = ticket.buyerPhone,
                cancellationDate = System.currentTimeMillis()
            )
        )
        val reset = ticket.copy(
            buyerName = null,
            buyerPhone = null,
            status = TicketStatus.AVAILABLE,
            groupId = null,
            imageSent = false
        )
        ticketDao.update(reset)
    }

    // --- Registro de ventas ---

    fun searchSoldOrReserved(raffleId: String, query: String): Flow<List<Ticket>> =
        ticketDao.searchSoldOrReserved(raffleId, query)

    suspend fun getTicketsByGroup(groupId: String): List<Ticket> =
        ticketDao.getByGroupId(groupId)

    suspend fun changeTicketsStatus(tickets: List<Ticket>, newStatus: TicketStatus): List<Ticket> {
        val updated = tickets.map { it.copy(status = newStatus) }
        updated.forEach { ticketDao.update(it) }
        return updated
    }

    suspend fun cancelTickets(tickets: List<Ticket>) {
        tickets.forEach { cancelTicket(it) }
    }

    // --- Ganador ---

    suspend fun findWinner(raffleId: String, winningNumber: String): Ticket? {
        val ticket = ticketDao.getByNumber(raffleId, winningNumber)
        return if (ticket?.status == TicketStatus.SOLD) ticket else null
    }

    suspend fun closeRaffle(raffle: Raffle, winningNumber: String) {
        raffleDao.update(raffle.copy(winningNumber = winningNumber, status = RaffleStatus.CLOSED))
    }
    fun getActiveRaffles(): Flow<List<Raffle>> = raffleDao.getByStatus(RaffleStatus.ACTIVE)
}