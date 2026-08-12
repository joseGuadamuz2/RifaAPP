package com.example.lotteryapp.ui.raffle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.repository.RaffleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SaleEntry(
    val tickets: List<Ticket>,
    val groupId: String?
) {
    val numbers: List<String> get() = tickets.map { it.number }.sorted()
    val buyerName: String? get() = tickets.firstOrNull()?.buyerName
    val buyerPhone: String? get() = tickets.firstOrNull()?.buyerPhone
    val status: TicketStatus get() = tickets.firstOrNull()?.status ?: TicketStatus.AVAILABLE
}

data class BuyerSummary(
    val name: String,
    val phone: String?,
    val tickets: List<Ticket>,
    val totalPending: Double,
    val totalPaid: Double
) {
    val allNumbers: List<String> get() = tickets.map { it.number }.sorted()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SoldTicketsViewModel(
    private val repository: RaffleRepository,
    private val raffleId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _raffle = MutableStateFlow<Raffle?>(null)
    val raffle: StateFlow<Raffle?> = _raffle

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _raffle.value = repository.getRaffleById(raffleId)
        }
    }

    // Lista de entradas (ventas individuales o grupales)
    val entries: StateFlow<List<SaleEntry>> = combine(_searchQuery, repository.getTicketsForRaffle(raffleId)) { query, allTickets ->
        val saleTickets = allTickets.filter { it.status != TicketStatus.AVAILABLE }
        val matched = if (query.isBlank()) {
            saleTickets
        } else {
            val q = query.trim()
            saleTickets.filter { ticket ->
                ticket.buyerName?.contains(q, ignoreCase = true) == true ||
                        ticket.buyerPhone?.contains(q, ignoreCase = true) == true ||
                        ticket.number.contains(q)
            }
        }
        val byGroup = allTickets.filter { it.groupId != null }.groupBy { it.groupId!! }
        buildEntries(matched, byGroup)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Directorio de Clientes (Agrupado por nombre/teléfono)
    val buyers: StateFlow<List<BuyerSummary>> = _raffle
        .flatMapLatest { raffle ->
            entries.map { saleEntries -> buildBuyers(saleEntries, raffle) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildBuyers(saleEntries: List<SaleEntry>, raffle: Raffle?): List<BuyerSummary> {
        val tickets = saleEntries.flatMap { it.tickets }
        val price = raffle?.ticketPrice ?: 0.0
        val perGroup = raffle?.modality == RaffleModality.GROUPS
        val groupSize = raffle?.groupSize?.coerceAtLeast(1) ?: 1

        return tickets.groupBy { (it.buyerName ?: "Anónimo") to (it.buyerPhone ?: "") }
            .map { (key, buyerTickets) ->
                BuyerSummary(
                    name = key.first,
                    phone = key.second.ifBlank { null },
                    tickets = buyerTickets,
                    totalPaid = if (perGroup) {
                        buyerTickets.count { it.status == TicketStatus.SOLD } / groupSize * price
                    } else {
                        buyerTickets.count { it.status == TicketStatus.SOLD } * price
                    },
                    totalPending = if (perGroup) {
                        buyerTickets.count { it.status == TicketStatus.RESERVED } / groupSize * price
                    } else {
                        buyerTickets.count { it.status == TicketStatus.RESERVED } * price
                    }
                )
            }.sortedByDescending { it.totalPaid + it.totalPending }
    }

    private fun buildEntries(matched: List<Ticket>, byGroup: Map<String, List<Ticket>>): List<SaleEntry> {
        val individualEntries = mutableListOf<SaleEntry>()
        val groupEntries = mutableListOf<SaleEntry>()
        val seenGroupIds = mutableSetOf<String>()

        for (ticket in matched) {
            val groupId = ticket.groupId
            if (groupId == null) {
                individualEntries.add(SaleEntry(tickets = listOf(ticket), groupId = null))
            } else if (groupId !in seenGroupIds) {
                seenGroupIds.add(groupId)
                val fullGroup = byGroup[groupId].orEmpty().sortedBy { it.number }
                groupEntries.add(SaleEntry(tickets = fullGroup, groupId = groupId))
            }
        }
        return (individualEntries + groupEntries).sortedBy { it.numbers.firstOrNull() }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleStatus(entry: SaleEntry) {
        val newStatus = if (entry.status == TicketStatus.SOLD) TicketStatus.RESERVED else TicketStatus.SOLD
        viewModelScope.launch {
            try {
                repository.changeTicketsStatus(entry.tickets, newStatus)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al cambiar estado: ${e.message}"
            }
        }
    }

    fun cancelEntry(entry: SaleEntry) {
        viewModelScope.launch {
            try {
                repository.cancelTickets(entry.tickets)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al cancelar venta: ${e.message}"
            }
        }
    }

    fun editPhone(entry: SaleEntry, newPhone: String?) {
        viewModelScope.launch {
            try {
                repository.updateBuyerPhone(entry.tickets, newPhone)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar teléfono: ${e.message}"
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun getReminderMessage(entry: SaleEntry): String {
        val raffle = _raffle.value
        val raffleName = raffle?.name ?: "la rifa"
        val perGroup = raffle?.modality == RaffleModality.GROUPS
        val total = if (perGroup) raffle!!.ticketPrice else entry.tickets.size * (raffle?.ticketPrice ?: 0.0)
        val unitLabel = if (perGroup) "por grupo" else "por boleto"
        return "Hola ${entry.buyerName}, te saludo de la Rifa $raffleName. Paso a recordarte el pago de tus números: ${entry.numbers.joinToString(", ")} ($unitLabel). El total es ₡${total.toInt()}. ¡Gracias!"
    }

    class Factory(private val repository: RaffleRepository, private val raffleId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SoldTicketsViewModel(repository, raffleId) as T
        }
    }
}
