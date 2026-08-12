package com.example.lotteryapp.ui.winners

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.data.entity.Winner
import com.example.lotteryapp.repository.RaffleRepository
import com.example.lotteryapp.ui.raffle.SaleEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WinnersViewModel(
    private val repository: RaffleRepository,
    private val raffleId: String
) : ViewModel() {

    val winners: StateFlow<List<Winner>> = repository.getWinnersForRaffle(raffleId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Ventas (vendidos/apartados) para buscar al ganador
    val saleEntries: StateFlow<List<SaleEntry>> = repository.getTicketsForRaffle(raffleId)
        .map { tickets -> buildSaleEntries(tickets) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun buildSaleEntries(tickets: List<Ticket>): List<SaleEntry> {
        val matched = tickets.filter { it.status != TicketStatus.AVAILABLE }
        val byGroup = tickets.filter { it.groupId != null }.groupBy { it.groupId!! }
        val individual = mutableListOf<SaleEntry>()
        val groups = mutableListOf<SaleEntry>()
        val seenGroupIds = mutableSetOf<String>()

        for (ticket in matched) {
            val groupId = ticket.groupId
            if (groupId == null) {
                individual.add(SaleEntry(tickets = listOf(ticket), groupId = null))
            } else if (groupId !in seenGroupIds) {
                seenGroupIds.add(groupId)
                val fullGroup = byGroup[groupId].orEmpty().sortedBy { it.number }
                groups.add(SaleEntry(tickets = fullGroup, groupId = groupId))
            }
        }
        return (individual + groups).sortedBy { it.numbers.firstOrNull() }
    }

    private val _raffle = MutableStateFlow<Raffle?>(null)
    val raffle: StateFlow<Raffle?> = _raffle.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _raffle.value = repository.getRaffleById(raffleId)
        }
    }

    fun registerWinner(
        winningNumber: String,
        buyerName: String,
        buyerPhone: String?,
        prize: String?
    ) {
        viewModelScope.launch {
            try {
                val raffle = repository.getRaffleById(raffleId)
                repository.registerWinner(
                    Winner(
                        raffleId = raffleId,
                        winningNumber = winningNumber,
                        buyerName = buyerName,
                        buyerPhone = buyerPhone,
                        prizeName = prize,
                        prizeAmount = raffle?.prizeValue
                    )
                )
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al registrar ganador: ${e.message}"
            }
        }
    }

    fun deleteWinner(winner: Winner) {
        viewModelScope.launch {
            try {
                repository.deleteWinner(winner)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar ganador: ${e.message}"
            }
        }
    }

    fun updateWinner(winner: Winner) {
        viewModelScope.launch {
            try {
                repository.updateWinner(winner)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar ganador: ${e.message}"
            }
        }
    }

    fun markNotified(winner: Winner) {
        viewModelScope.launch {
            try {
                repository.markWinnerNotified(winner)
            } catch (_: Exception) {
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    class Factory(
        private val repository: RaffleRepository,
        private val raffleId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WinnersViewModel(repository, raffleId) as T
        }
    }
}