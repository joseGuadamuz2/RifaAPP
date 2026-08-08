package com.example.lotteryapp.ui.raffle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.util.WhatsAppSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoldTicketsScreen(
    viewModel: SoldTicketsViewModel,
    onBack: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val raffle by viewModel.raffle.collectAsState()
    var entryToCancel by remember { mutableStateOf<SaleEntry?>(null) }
    var entryToEditPhone by remember { mutableStateOf<SaleEntry?>(null) }
    var entryToToggleStatus by remember { mutableStateOf<SaleEntry?>(null) }
    var statusFilter by remember { mutableStateOf<TicketStatus?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de ventas") },
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
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Buscar por nombre o número") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Todos") }
                )
                FilterChip(
                    selected = statusFilter == TicketStatus.SOLD,
                    onClick = { statusFilter = TicketStatus.SOLD },
                    label = { Text("Vendidos") }
                )
                FilterChip(
                    selected = statusFilter == TicketStatus.RESERVED,
                    onClick = { statusFilter = TicketStatus.RESERVED },
                    label = { Text("Apartados") }
                )
            }

            val filteredEntries = statusFilter?.let { filter ->
                entries.filter { it.status == filter }
            } ?: entries

            if (filteredEntries.isEmpty()) {
                Text(
                    text = "No hay ventas registradas todavía.",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn {
                    items(filteredEntries, key = { it.groupId ?: it.tickets.first().id }) { entry ->
                        val currentRaffle = raffle
                        SaleEntryCard(
                            entry = entry,
                            onToggleStatus = { entryToToggleStatus = entry },
                            onResend = {
                                if (currentRaffle != null) {
                                    WhatsAppSender.sendTextReceipt(context, currentRaffle, entry.tickets)
                                }
                            },
                            onCancel = { entryToCancel = entry },
                            onEditPhone = { entryToEditPhone = entry }
                        )
                    }
                }
            }
        }
    }

    entryToToggleStatus?.let { entry ->
        val isSold = entry.status == TicketStatus.SOLD
        val newStatusLabel = if (isSold) "Apartado" else "Vendido"
        val actionLabel = if (isSold) "Marcar como apartado" else "Confirmar venta"

        AlertDialog(
            onDismissRequest = { entryToToggleStatus = null },
            title = { Text("Cambiar estado") },
            text = {
                Text("¿Deseas cambiar el estado de los boletos ${entry.numbers.joinToString(", ")} a $newStatusLabel?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleStatus(entry)
                    entryToToggleStatus = null
                }) {
                    Text(actionLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToToggleStatus = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    entryToCancel?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToCancel = null },
            title = { Text("Cancelar venta") },
            text = {
                val label = if (entry.tickets.size > 1) "los boletos" else "el boleto"
                Text("¿Seguro que querés cancelar $label ${entry.numbers.joinToString(", ")}? Volverán a estar disponibles.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelEntry(entry)
                    entryToCancel = null
                }) {
                    Text("Cancelar venta")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToCancel = null }) {
                    Text("Volver")
                }
            }
        )
    }

    entryToEditPhone?.let { entry ->
        EditPhoneDialog(
            currentPhone = entry.buyerPhone,
            onDismiss = { entryToEditPhone = null },
            onConfirm = { newPhone ->
                viewModel.editPhone(entry, newPhone)
                entryToEditPhone = null
            }
        )
    }
}

@Composable
private fun SaleEntryCard(
    entry: SaleEntry,
    onToggleStatus: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit,
    onEditPhone: () -> Unit
) {
    val statusLabel = if (entry.status == TicketStatus.SOLD) "Vendido" else "Apartado"
    val toggleLabel = if (entry.status == TicketStatus.SOLD) "Apartar" else "Vender"
    val hasPhone = !entry.buyerPhone.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Boletos: ${entry.numbers.joinToString(", ")}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = entry.buyerName ?: "")
                    if (hasPhone) {
                        Text(text = entry.buyerPhone ?: "")
                    }
                }
                AssistChip(onClick = {}, label = { Text(statusLabel) })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val buttonPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                val fontSize = 11.sp

                if (hasPhone) {
                    FilledTonalButton(
                        onClick = onResend,
                        modifier = Modifier.weight(1f),
                        contentPadding = buttonPadding
                    ) {
                        Text("Reenviar", fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                FilledTonalButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.weight(1f),
                    contentPadding = buttonPadding
                ) {
                    Text(toggleLabel, fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                FilledTonalButton(
                    onClick = onEditPhone,
                    modifier = Modifier.weight(1f),
                    contentPadding = buttonPadding
                ) {
                    Text("Editar", fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                FilledTonalButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    contentPadding = buttonPadding
                ) {
                    Text("Borrar", fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun EditPhoneDialog(
    currentPhone: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var phone by remember { mutableStateOf(currentPhone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar teléfono") },
        text = {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Teléfono (opcional)") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(phone.ifBlank { null }) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
