package com.example.lotteryapp.ui.raffle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.RaffleSource
import com.example.lotteryapp.data.entity.RaffleStatus
import com.example.lotteryapp.repository.RaffleRepository
import com.example.lotteryapp.util.DateUtils
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
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean
        get() {
            val price = ticketPrice.toDoubleOrNull() ?: 0.0

            return name.isNotBlank() && prizeName.isNotBlank() &&
                    price in 5.0..1000000.0 &&
                    drawDate != null && DateUtils.isSameDayOrAfter(drawDate)
        }
}

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

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun saveChanges() {
        val state = _uiState.value
        val price = state.ticketPrice.toDoubleOrNull()
        val date = state.drawDate

        if (!state.isFormValid || price == null || date == null) {
            _uiState.value = state.copy(errorMessage = "Por favor verifica los datos. El precio debe estar entre ₡5 y ₡1,000,000 y la fecha no puede ser anterior a hoy.")
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val existing = repository.getRaffleById(raffleId) ?: return@launch
                // Si se cambia la fecha de una rifa cerrada, se reabre (ACTIVA)
                val dateChanged = existing.drawDate != date
                val newStatus = if (existing.status == RaffleStatus.CLOSED && dateChanged) {
                    RaffleStatus.ACTIVE
                } else {
                    existing.status
                }
                repository.updateRaffle(
                    existing.copy(
                        name = state.name.trim(),
                        prizeName = state.prizeName.trim(),
                        prizePhotoPath = state.prizePhotoPath,
                        ticketPrice = price,
                        drawDate = date,
                        source = state.source,
                        status = newStatus
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Error al actualizar la rifa: ${e.message}"
                )
            }
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
