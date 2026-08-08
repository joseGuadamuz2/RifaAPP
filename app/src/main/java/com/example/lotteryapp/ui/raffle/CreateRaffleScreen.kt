package com.example.lotteryapp.ui.raffle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lotteryapp.data.entity.RaffleSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRaffleScreen(
    viewModel: CreateRaffleViewModel,
    onBack: () -> Unit,
    onRaffleSaved: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR")) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onPrizePhotoChange(it.toString()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva rifa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Imagen del premio (opcional)
            Card(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.prizePhotoPath != null) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Imagen seleccionada — tocá para cambiarla")
                    } else {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Agregar imagen (opcional)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 2. Nombre de la rifa
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre de la rifa") },
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Nombre del premio
            OutlinedTextField(
                value = uiState.prizeName,
                onValueChange = viewModel::onPrizeNameChange,
                label = { Text("Nombre del premio") },
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Precio por boleto
            OutlinedTextField(
                value = uiState.ticketPrice,
                onValueChange = viewModel::onTicketPriceChange,
                label = { Text("Precio por boleto") },
                modifier = Modifier.fillMaxWidth()
            )

            // 5. Fecha del sorteo — ahora con apariencia real de input
            Box {
                OutlinedTextField(
                    value = uiState.drawDate?.let { dateFormat.format(Date(it)) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha del sorteo") },
                    trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                // Capa invisible encima del campo para capturar el toque sin
                // que el teclado intente abrirse (el campo es readOnly).
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showDatePicker = true }
                )
            }

            // 6. Tipo de sorteo
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

            // 7. Botón principal
            Button(
                onClick = viewModel::saveRaffle,
                enabled = uiState.isFormValid && !uiState.isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "Guardando..." else "Crear rifa")
            }
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
        val raffleId = uiState.createdRaffleId
        if (uiState.saveSuccess && raffleId != null) {
            onRaffleSaved(raffleId)
        }
    }
}