package com.example.lotteryapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleStatus
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.repository.RaffleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RaffleItem(
    val raffle: Raffle,
    val soldCount: Int,
    val totalCount: Int
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
                        repository.getTicketsCountByStatus(raffle.id, TicketStatus.SOLD)
                    ) { total: Int, sold: Int -> RaffleItem(raffle, sold, total) }
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

    class Factory(private val repository: RaffleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
