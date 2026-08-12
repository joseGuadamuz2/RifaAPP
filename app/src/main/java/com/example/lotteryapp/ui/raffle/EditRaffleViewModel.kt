package com.example.lotteryapp.ui.raffle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.RaffleSource
import com.example.lotteryapp.repository.RaffleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditRaffleUiState(
    val name: String = "",
    val prizeName: String = "",
    val prizePhotoPath: String? = null,
    val ticketPrice: String = "",
    val source: RaffleSource = RaffleSource.LOTERIA_NACIONAL,
    val modality: RaffleModality = RaffleModality.SENCILLA,
    val groupSize: Int = 1,
    val drawDate: Long? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class EditRaffleViewModel(
    private val repository: RaffleRepository,
    private val raffleId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditRaffleUiState())
    val uiState: StateFlow<EditRaffleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val raffle = repository.getRaffleById(raffleId)
            if (raffle != null) {
                _uiState.value = EditRaffleUiState(
                    name = raffle.name,
                    prizeName = raffle.prizeName,
                    prizePhotoPath = raffle.prizePhotoPath,
                    ticketPrice = raffle.ticketPrice.toString(),
                    source = raffle.source,
                    modality = raffle.modality,
                    groupSize = raffle.groupSize,
                    drawDate = raffle.drawDate,
                    isLoading = false
                )
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onPrizeNameChange(value: String) {
        _uiState.value = _uiState.value.copy(prizeName = value)
    }

    fun onPrizePhotoChange(value: String?) {
        _uiState.value = _uiState.value.copy(prizePhotoPath = value)
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

    fun saveChanges() {
        val state = _uiState.value
        val price = state.ticketPrice.toDoubleOrNull()
        val date = state.drawDate

        if (state.name.isBlank() || state.prizeName.isBlank() || price == null || date == null) {
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            val existing = repository.getRaffleById(raffleId) ?: return@launch
            repository.updateRaffle(
                existing.copy(
                    name = state.name,
                    prizeName = state.prizeName,
                    prizePhotoPath = state.prizePhotoPath,
                    ticketPrice = price,
                    drawDate = date,
                    source = state.source
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
        }
    }

    class Factory(
        private val repository: RaffleRepository,
        private val raffleId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditRaffleViewModel(repository, raffleId) as T
        }
    }
}
