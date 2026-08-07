package com.example.lotteryapp.ui.raffle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

            if (entries.isEmpty()) {
                Text(
                    text = "No hay ventas registradas todavía.",
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn {
                    items(entries, key = { it.groupId ?: it.tickets.first().id }) { entry ->
                        val currentRaffle = raffle
                        SaleEntryCard(
                            entry = entry,
                            onToggleStatus = { viewModel.toggleStatus(entry) },
                            onResend = {
                                if (currentRaffle != null) {
                                    WhatsAppSender.sendTextReceipt(context, currentRaffle, entry.tickets)
                                }
                            },
                            onCancel = { entryToCancel = entry }
                        )
                    }
                }
            }
        }
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
}

@Composable
private fun SaleEntryCard(
    entry: SaleEntry,
    onToggleStatus: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit
) {
    val statusLabel = if (entry.status == TicketStatus.SOLD) "Vendido" else "Apartado"
    val toggleLabel = if (entry.status == TicketStatus.SOLD) "Marcar apartado" else "Marcar vendido"
    val hasPhone = !entry.buyerPhone.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Boletos: ${entry.numbers.joinToString(", ")}",
                fontWeight = FontWeight.Bold
            )
            Text(text = entry.buyerName ?: "")
            if (hasPhone) {
                Text(text = entry.buyerPhone ?: "")
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = {}, label = { Text(statusLabel) })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancelar")
                }
                TextButton(onClick = onToggleStatus) {
                    Text(toggleLabel)
                }
                if (hasPhone) {
                    TextButton(onClick = onResend) {
                        Text("Reenviar")
                    }
                }
            }
        }
    }
}