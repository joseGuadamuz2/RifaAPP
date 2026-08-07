package com.example.lotteryapp.ui.raffle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.util.WhatsAppSender

@Composable
fun TicketGridScreen(viewModel: TicketGridViewModel) {
    val tickets by viewModel.tickets.collectAsState()
    val raffle by viewModel.raffle.collectAsState()
    val lastTransaction by viewModel.lastTransaction.collectAsState()
    var ticketToSell by remember { mutableStateOf<Ticket?>(null) }
    var ticketToView by remember { mutableStateOf<Ticket?>(null) }
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(10),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(tickets, key = { it.id }) { ticket ->
            TicketCell(
                ticket = ticket,
                onClick = {
                    if (ticket.status == TicketStatus.AVAILABLE) {
                        ticketToSell = ticket
                    } else {
                        ticketToView = ticket
                    }
                }
            )
        }
    }

    ticketToSell?.let { ticket ->
        SellTicketDialog(
            ticket = ticket,
            onDismiss = { ticketToSell = null },
            onConfirm = { buyerName, buyerPhone, status ->
                viewModel.sellOrReserve(ticket, buyerName, buyerPhone, status)
                ticketToSell = null
            }
        )
    }

    ticketToView?.let { ticket ->
        TicketDetailDialog(
            ticket = ticket,
            onDismiss = { ticketToView = null },
            onCancel = {
                viewModel.cancel(ticket)
                ticketToView = null
            }
        )
    }

    val transaction = lastTransaction
    val currentRaffle = raffle
    if (transaction != null && currentRaffle != null) {
        val hasPhone = !transaction.firstOrNull()?.buyerPhone.isNullOrBlank()
        AlertDialog(
            onDismissRequest = { viewModel.clearLastTransaction() },
            title = { Text("Comprobante") },
            text = {
                Text(
                    if (hasPhone) "¿Enviar comprobante por WhatsApp?"
                    else "El comprador no tiene teléfono registrado, así que no se puede enviar por WhatsApp."
                )
            },
            confirmButton = {
                if (hasPhone) {
                    TextButton(onClick = {
                        WhatsAppSender.sendTextReceipt(context, currentRaffle, transaction)
                        viewModel.clearLastTransaction()
                    }) {
                        Text("Enviar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearLastTransaction() }) {
                    Text(if (hasPhone) "Ahora no" else "Cerrar")
                }
            }
        )
    }
}

@Composable
private fun TicketCell(ticket: Ticket, onClick: () -> Unit) {
    val backgroundColor = when (ticket.status) {
        TicketStatus.AVAILABLE -> Color(0xFFE0E0E0)
        TicketStatus.RESERVED -> Color(0xFFFFC107)
        TicketStatus.SOLD -> Color(0xFF4CAF50)
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = ticket.number)
    }
}

@Composable
private fun SellTicketDialog(
    ticket: Ticket,
    onDismiss: () -> Unit,
    onConfirm: (buyerName: String, buyerPhone: String?, status: TicketStatus) -> Unit
) {
    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Boleto #${ticket.number}") },
        text = {
            Column {
                OutlinedTextField(
                    value = buyerName,
                    onValueChange = { buyerName = it },
                    label = { Text("Nombre del comprador") }
                )
                OutlinedTextField(
                    value = buyerPhone,
                    onValueChange = { buyerPhone = it },
                    label = { Text("Teléfono (opcional)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (buyerName.isNotBlank()) {
                        onConfirm(buyerName, buyerPhone.ifBlank { null }, TicketStatus.SOLD)
                    }
                }
            ) {
                Text("Vender")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (buyerName.isNotBlank()) {
                        onConfirm(buyerName, buyerPhone.ifBlank { null }, TicketStatus.RESERVED)
                    }
                }
            ) {
                Text("Apartar")
            }
        }
    )
}

@Composable
private fun TicketDetailDialog(
    ticket: Ticket,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    val statusText = if (ticket.status == TicketStatus.SOLD) "Vendido" else "Apartado"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Boleto #${ticket.number}") },
        text = {
            Column {
                Text("Comprador: ${ticket.buyerName ?: ""}")
                if (!ticket.buyerPhone.isNullOrBlank()) {
                    Text("Teléfono: ${ticket.buyerPhone}")
                }
                Text("Estado: $statusText")
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar boleto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}