package com.example.lotteryapp.ui.raffle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.RaffleSource
import com.example.lotteryapp.repository.RaffleRepository
import com.example.lotteryapp.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val GROUP_SIZE_OPTIONS = listOf(2, 4, 5, 10, 20, 25, 50)

data class CreateRaffleUiState(
    val name: String = "",
    val prizeName: String = "",
    val ticketPrice: String = "",
    val source: RaffleSource = RaffleSource.LOTERIA_NACIONAL,
    val modality: RaffleModality = RaffleModality.SENCILLA,
    val groupSize: Int = 2,
    val drawDate: Long? = null,
    val prizePhotoPath: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val createdRaffleId: String? = null,
    val errorMessage: String? = null
) {
    val groupCount: Int
        get() = if (modality == RaffleModality.GROUPS) 100 / groupSize else 0

    val isFormValid: Boolean
        get() {
            val price = ticketPrice.toDoubleOrNull() ?: 0.0

            return name.isNotBlank() && prizeName.isNotBlank() &&
                    price in 5.0..1000000.0 &&
                    drawDate != null && DateUtils.isSameDayOrAfter(drawDate) &&
                    (modality == RaffleModality.SENCILLA || groupSize in GROUP_SIZE_OPTIONS)
        }
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

    fun onModalityChange(value: RaffleModality) {
        _uiState.value = _uiState.value.copy(modality = value)
    }

    fun onGroupSizeChange(value: Int) {
        _uiState.value = _uiState.value.copy(groupSize = value)
    }

    fun onDrawDateChange(value: Long) {
        _uiState.value = _uiState.value.copy(drawDate = value)
    }

    fun onPrizePhotoChange(uri: String?) {
        _uiState.value = _uiState.value.copy(prizePhotoPath = uri)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun saveRaffle() {
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
                val raffle = Raffle(
                    name = state.name.trim(),
                    prizeName = state.prizeName.trim(),
                    ticketPrice = price,
                    drawDate = date,
                    source = state.source,
                    modality = state.modality,
                    groupSize = state.groupSize,
                    prizePhotoPath = state.prizePhotoPath
                )
                repository.createRaffle(raffle)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true,
                    createdRaffleId = raffle.id
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Error al guardar la rifa: ${e.message}"
                )
            }
        }
    }

    class Factory(private val repository: RaffleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateRaffleViewModel(repository) as T
        }
    }
}