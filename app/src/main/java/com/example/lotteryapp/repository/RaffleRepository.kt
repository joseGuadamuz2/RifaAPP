package com.example.lotteryapp.repository

import com.example.lotteryapp.data.dao.CancellationHistoryDao
import com.example.lotteryapp.data.dao.RaffleDao
import com.example.lotteryapp.data.dao.TicketDao
import com.example.lotteryapp.data.entity.CancellationHistory
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleModality
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

    // --- Gestión de Rifas ---
    suspend fun createRaffle(raffle: Raffle) {
        raffleDao.insert(raffle)
        val numbers = (0..99).map { it.toString().padStart(2, '0') }
        val tickets = if (raffle.modality == RaffleModality.GROUPS) {
            val size = raffle.groupSize.coerceIn(1, 100)
            numbers.shuffled()
                .chunked(size)
                .map { groupNumbers ->
                    val groupId = UUID.randomUUID().toString()
                    groupNumbers.map { number ->
                        Ticket(raffleId = raffle.id, number = number, groupId = groupId)
                    }
                }
                .flatten()
        } else {
            numbers.map { number ->
                Ticket(raffleId = raffle.id, number = number)
            }
        }
        ticketDao.insertAll(tickets)
    }

    suspend fun getRaffleById(raffleId: String): Raffle? = raffleDao.getById(raffleId)
    suspend fun updateRaffle(raffle: Raffle) = raffleDao.update(raffle)
    suspend fun deleteRaffle(raffle: Raffle) = raffleDao.delete(raffle)
    
    fun getTicketsForRaffle(raffleId: String): Flow<List<Ticket>> = ticketDao.getByRaffle(raffleId)

    // --- Venta y Apartado con Validación Atómica ---
    suspend fun sellOrReserveGroup(
        tickets: List<Ticket>,
        buyerName: String,
        buyerPhone: String?,
        status: TicketStatus
    ): Result<List<Ticket>> {
        val unavailableNumbers = mutableListOf<String>()
        
        // 🛡️ VALIDACIÓN DE DISPONIBILIDAD (CONCURRENCIA)
        for (t in tickets) {
            val current = ticketDao.getById(t.id)
            if (current == null || current.status != TicketStatus.AVAILABLE) {
                unavailableNumbers.add(t.number)
            }
        }

        if (unavailableNumbers.isNotEmpty()) {
            val msg = if (unavailableNumbers.size == 1) 
                "El número ${unavailableNumbers[0]} ya no está disponible."
            else 
                "Los números ${unavailableNumbers.joinToString(", ")} ya han sido vendidos o apartados por otro usuario."
            return Result.failure(Exception(msg))
        }

        // PROCESAMIENTO SaaS (Agrupación en una sola transacción visual)
        // En modo grupos se preserva el groupId original; en sencilla se crea uno nuevo
        val firstGroupId = tickets.firstOrNull()?.groupId
        val groupId = if (firstGroupId != null && tickets.all { it.groupId == firstGroupId }) {
            firstGroupId
        } else {
            UUID.randomUUID().toString()
        }
        val updated = tickets.map { ticket ->
            ticket.copy(
                buyerName = buyerName,
                buyerPhone = buyerPhone,
                status = status,
                groupId = groupId
            )
        }
        updated.forEach { ticketDao.update(it) }
        return Result.success(updated)
    }

    suspend fun changeTicketStatus(ticket: Ticket, newStatus: TicketStatus): Result<Ticket> {
        val current = ticketDao.getById(ticket.id) ?: return Result.failure(Exception("Ticket no encontrado"))
        if (current.status == TicketStatus.AVAILABLE) {
            return Result.failure(Exception("No se puede cambiar el estado de un número libre sin datos de comprador."))
        }
        val updated = current.copy(status = newStatus)
        ticketDao.update(updated)
        return Result.success(updated)
    }

    suspend fun cancelTicket(ticket: Ticket) {
        cancellationHistoryDao.insert(
            CancellationHistory(
                ticketId = ticket.id,
                previousBuyerName = ticket.buyerName ?: "",
                previousBuyerPhone = ticket.buyerPhone,
                cancellationDate = System.currentTimeMillis()
            )
        )
        ticketDao.update(ticket.copy(
            buyerName = null,
            buyerPhone = null,
            status = TicketStatus.AVAILABLE,
            groupId = null
        ))
    }

    // --- Consultas ---
    fun searchSoldOrReserved(raffleId: String, query: String): Flow<List<Ticket>> =
        ticketDao.searchSoldOrReserved(raffleId, query)

    suspend fun getTicketsByGroup(groupId: String): List<Ticket> =
        ticketDao.getByGroupId(groupId)

    suspend fun changeTicketsStatus(tickets: List<Ticket>, newStatus: TicketStatus) {
        tickets.forEach { ticketDao.update(it.copy(status = newStatus)) }
    }

    suspend fun cancelTickets(tickets: List<Ticket>) {
        val sharedGroupId = tickets.firstOrNull()?.groupId
            ?.takeIf { gid -> tickets.all { it.groupId == gid } }
        tickets.forEach { ticket ->
            cancellationHistoryDao.insert(
                CancellationHistory(
                    ticketId = ticket.id,
                    previousBuyerName = ticket.buyerName ?: "",
                    previousBuyerPhone = ticket.buyerPhone,
                    cancellationDate = System.currentTimeMillis()
                )
            )
            ticketDao.update(ticket.copy(
                buyerName = null,
                buyerPhone = null,
                status = TicketStatus.AVAILABLE,
                groupId = sharedGroupId
            ))
        }
    }

    suspend fun updateBuyerPhone(tickets: List<Ticket>, newPhone: String?) {
        tickets.forEach { ticketDao.update(it.copy(buyerPhone = newPhone)) }
    }

    fun getActiveRaffles(): Flow<List<Raffle>> = raffleDao.getByStatus(RaffleStatus.ACTIVE)

    fun getRafflesByStatus(status: RaffleStatus, query: String): Flow<List<Raffle>> {
        return if (query.isBlank()) {
            raffleDao.getByStatus(status)
        } else {
            raffleDao.searchByStatus(status, query)
        }
    }

    fun getTicketsCount(raffleId: String): Flow<Int> = ticketDao.getCountByRaffle(raffleId)

    fun getTicketsCountByStatus(raffleId: String, status: TicketStatus): Flow<Int> =
        ticketDao.getCountByStatus(raffleId, status)

    fun getSoldGroupCount(raffleId: String): Flow<Int> = ticketDao.getSoldGroupCount(raffleId)
}
