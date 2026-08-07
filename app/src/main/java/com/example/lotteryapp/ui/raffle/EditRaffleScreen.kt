package com.example.lotteryapp.ui.raffle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lotteryapp.data.entity.RaffleSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRaffleScreen(
    viewModel: EditRaffleViewModel,
    onRaffleUpdated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Editar rifa",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nombre de la rifa") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.prizeName,
            onValueChange = viewModel::onPrizeNameChange,
            label = { Text("Nombre del premio") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.ticketPrice,
            onValueChange = viewModel::onTicketPriceChange,
            label = { Text("Precio por boleto") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = uiState.drawDate?.let { dateFormat.format(Date(it)) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha del sorteo") },
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = { showDatePicker = true }) {
            Text("Cambiar fecha del sorteo")
        }

        ExposedDropdownMenuBox(
            expanded = sourceMenuExpanded,
            onExpandedChange = { sourceMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = if (uiState.source == RaffleSource.LOTERIA_NACIONAL) "Lotería Nacional" else "Chances",
                onValueChange = {},
                readOnly = true,
                label = { Text("Sorteo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceMenuExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            DropdownMenu(
                expanded = sourceMenuExpanded,
                onDismissRequest = { sourceMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Lotería Nacional") },
                    onClick = {
                        viewModel.onSourceChange(RaffleSource.LOTERIA_NACIONAL)
                        sourceMenuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Chances") },
                    onClick = {
                        viewModel.onSourceChange(RaffleSource.CHANCES)
                        sourceMenuExpanded = false
                    }
                )
            }
        }

        Button(
            onClick = viewModel::saveChanges,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isSaving) "Guardando..." else "Guardar cambios")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.drawDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDrawDateChange(it) }
                    showDatePicker = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onRaffleUpdated()
        }
    }
}