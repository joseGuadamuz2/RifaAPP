package com.example.lotteryapp.ui.raffle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.repository.RaffleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TicketGridViewModel(
    private val repository: RaffleRepository,
    private val raffleId: String
) : ViewModel() {

    val tickets: StateFlow<List<Ticket>> = repository.getTicketsForRaffle(raffleId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _raffle = MutableStateFlow<Raffle?>(null)
    val raffle: StateFlow<Raffle?> = _raffle.asStateFlow()

    private val _lastTransaction = MutableStateFlow<List<Ticket>?>(null)
    val lastTransaction: StateFlow<List<Ticket>?> = _lastTransaction.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _raffle.value = repository.getRaffleById(raffleId)
        }
    }

    fun sellOrReserveGroup(
        tickets: List<Ticket>,
        buyerName: String,
        buyerPhone: String?,
        status: TicketStatus
    ) {
        viewModelScope.launch {
            repository.sellOrReserveGroup(tickets, buyerName, buyerPhone, status)
                .onSuccess { updated ->
                    _lastTransaction.value = updated
                    _errorMessage.value = null
                }
                .onFailure { exception ->
                    _errorMessage.value = exception.message
                }
        }
    }

    fun changeStatus(ticket: Ticket, newStatus: TicketStatus) {
        viewModelScope.launch {
            repository.changeTicketStatus(ticket, newStatus)
                .onFailure { exception ->
                    _errorMessage.value = exception.message
                }
        }
    }

    fun cancel(ticket: Ticket) {
        viewModelScope.launch {
            try {
                repository.cancelTicket(ticket)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al liberar ticket: ${e.message}"
            }
        }
    }

    fun cancelGroup(group: List<Ticket>) {
        viewModelScope.launch {
            try {
                repository.cancelTickets(group)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al liberar grupo: ${e.message}"
            }
        }
    }

    fun changeTicketsStatus(tickets: List<Ticket>, newStatus: TicketStatus) {
        viewModelScope.launch {
            try {
                repository.changeTicketsStatus(tickets, newStatus)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al cambiar estado: ${e.message}"
            }
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun clearLastTransaction() { _lastTransaction.value = null }

    class Factory(
        private val repository: RaffleRepository,
        private val raffleId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TicketGridViewModel(repository, raffleId) as T
        }
    }
}
