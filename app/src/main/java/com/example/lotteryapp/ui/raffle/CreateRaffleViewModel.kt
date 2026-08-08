package com.example.lotteryapp.ui.raffle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleSource
import com.example.lotteryapp.repository.RaffleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateRaffleUiState(
    val name: String = "",
    val prizeName: String = "",
    val ticketPrice: String = "",
    val source: RaffleSource = RaffleSource.LOTERIA_NACIONAL,
    val drawDate: Long? = null,
    val prizePhotoPath: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val createdRaffleId: String? = null
) {
    val isFormValid: Boolean
        get() = name.isNotBlank() && prizeName.isNotBlank() &&
                ticketPrice.toDoubleOrNull() != null && drawDate != null
}

class CreateRaffleViewModel(
    private val repository: RaffleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRaffleUiState())
    val uiState: StateFlow<CreateRaffleUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onPrizeNameChange(value: String) {
        _uiState.value = _uiState.value.copy(prizeName = value)
    }

    fun onTicketPriceChange(value: String) {
        _uiState.value = _uiState.value.copy(ticketPrice = value)
    }

    fun onSourceChange(value: RaffleSource) {
        _uiState.value = _uiState.value.copy(source = value)
    }

    fun onDrawDateChange(value: Long) {
        _uiState.value = _uiState.value.copy(drawDate = value)
    }

    fun onPrizePhotoChange(uri: String?) {
        _uiState.value = _uiState.value.copy(prizePhotoPath = uri)
    }

    fun saveRaffle() {
        val state = _uiState.value
        val price = state.ticketPrice.toDoubleOrNull()
        val date = state.drawDate

        if (!state.isFormValid || price == null || date == null) return

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val raffle = Raffle(
                name = state.name,
                prizeName = state.prizeName,
                ticketPrice = price,
                drawDate = date,
                source = state.source,
                prizePhotoPath = state.prizePhotoPath
            )
            repository.createRaffle(raffle)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = true,
                createdRaffleId = raffle.id
            )
        }
    }

    class Factory(private val repository: RaffleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateRaffleViewModel(repository) as T
        }
    }
}