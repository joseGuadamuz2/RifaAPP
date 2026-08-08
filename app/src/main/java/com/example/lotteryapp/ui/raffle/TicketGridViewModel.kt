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
    val raffle: StateFlow<Raffle?> = _raffle

    private val _lastTransaction = MutableStateFlow<List<Ticket>?>(null)
    val lastTransaction: StateFlow<List<Ticket>?> = _lastTransaction

    init {
        viewModelScope.launch {
            _raffle.value = repository.getRaffleById(raffleId)
        }
    }

    fun sellOrReserve(
        ticket: Ticket,
        buyerName: String,
        buyerPhone: String?,
        status: TicketStatus
    ) {
        viewModelScope.launch {
            val updated = repository.sellOrReserveTicket(ticket, buyerName, buyerPhone, status)
            _lastTransaction.value = listOf(updated)
        }
    }

    fun sellOrReserveGroup(
        tickets: List<Ticket>,
        buyerName: String,
        buyerPhone: String?,
        status: TicketStatus
    ) {
        viewModelScope.launch {
            val updated = repository.sellOrReserveGroup(tickets, buyerName, buyerPhone, status)
            _lastTransaction.value = updated
        }
    }

    fun clearLastTransaction() {
        _lastTransaction.value = null
    }

    fun cancel(ticket: Ticket) {
        viewModelScope.launch {
            repository.cancelTicket(ticket)
        }
    }

    fun changeStatus(ticket: Ticket, newStatus: TicketStatus) {
        viewModelScope.launch {
            repository.changeTicketStatus(ticket, newStatus)
        }
    }


    class Factory(
        private val repository: RaffleRepository,
        private val raffleId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TicketGridViewModel(repository, raffleId) as T
        }
    }

    fun editPhone(ticket: Ticket, newPhone: String?) {
        viewModelScope.launch {
            repository.updateBuyerPhone(listOf(ticket), newPhone)
        }
    }

}