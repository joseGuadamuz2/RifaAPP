package com.example.lotteryapp.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.RaffleStatus
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.repository.RaffleRepository
import com.example.lotteryapp.util.ImageSharingHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RaffleItem(
    val raffle: Raffle,
    val soldCount: Int,
    val totalCount: Int,
    val collectedAmount: Double // Nueva propiedad
) {
    val progress: Float get() = if (totalCount > 0) soldCount.toFloat() / totalCount else 0f
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: RaffleRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTab = MutableStateFlow(0) // 0: Activas, 1: Finalizadas
    val selectedTab: StateFlow<Int> = _selectedTab

    init {
        checkAndCloseExpiredRaffles()
    }

    private fun checkAndCloseExpiredRaffles() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                // Buscamos rifas activas que ya pasaron su fecha
                val activeRaffles = repository.getRafflesByStatus(RaffleStatus.ACTIVE, "").first()
                activeRaffles.forEach { raffle ->
                    if (raffle.drawDate < now) {
                        repository.updateRaffle(raffle.copy(status = RaffleStatus.CLOSED))
                    }
                }
            } catch (_: Exception) { }
        }
    }

    val raffleItems: StateFlow<List<RaffleItem>> = combine(_searchQuery, _selectedTab) { query: String, tab: Int ->
        val status = if (tab == 0) RaffleStatus.ACTIVE else RaffleStatus.CLOSED
        repository.getRafflesByStatus(status, query)
    }.flatMapLatest { flow: Flow<List<Raffle>> -> flow }
        .flatMapLatest { raffleList: List<Raffle> ->
            if (raffleList.isEmpty()) {
                flowOf(emptyList<RaffleItem>())
            } else {
                val itemFlows: List<Flow<RaffleItem>> = raffleList.map { raffle ->
                    combine(
                        repository.getTicketsCount(raffle.id),
                        repository.getTicketsCountByStatus(raffle.id, TicketStatus.SOLD),
                        repository.getSoldGroupCount(raffle.id)
                    ) { total: Int, sold: Int, soldGroups: Int ->
                        val isGroupMode = raffle.modality == RaffleModality.GROUPS
                        val unitTotal = if (isGroupMode) total.div(raffle.groupSize.coerceAtLeast(1)) else total
                        val unitSold = if (isGroupMode) soldGroups else sold
                        RaffleItem(
                            raffle = raffle,
                            soldCount = unitSold,
                            totalCount = unitTotal,
                            collectedAmount = unitSold * raffle.ticketPrice
                        )
                    }
                }
                combine(itemFlows) { items: Array<RaffleItem> -> items.toList() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun deleteRaffle(raffle: Raffle) {
        viewModelScope.launch {
            repository.deleteRaffle(raffle)
        }
    }

    fun shareAvailableNumbers(context: Context, raffle: Raffle) {
        viewModelScope.launch {
            val tickets = repository.getTicketsForRaffle(raffle.id).first()
            ImageSharingHelper.shareAvailableNumbers(context, raffle, tickets)
        }
    }

    class Factory(private val repository: RaffleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
