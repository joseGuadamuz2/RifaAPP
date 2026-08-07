package com.example.lotteryapp.ui.raffle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.util.WhatsAppSender
import androidx.compose.material3.ExperimentalMaterial3Api

private val ColorAvailable = Color(0xFFECEFF1)
private val ColorReserved = Color(0xFFFFD54F)
private val ColorSold = Color(0xFF66BB6A)
private val ColorSelectedBorder = Color(0xFF1976D2)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TicketGridScreen(
    viewModel: TicketGridViewModel,
    onBack: () -> Unit
) {
    val tickets by viewModel.tickets.collectAsState()
    val raffle by viewModel.raffle.collectAsState()
    val lastTransaction by viewModel.lastTransaction.collectAsState()
    var ticketToSell by remember { mutableStateOf<Ticket?>(null) }
    var ticketToView by remember { mutableStateOf<Ticket?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showGroupSellDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    val soldCount = tickets.count { it.status == TicketStatus.SOLD }
    val reservedCount = tickets.count { it.status == TicketStatus.RESERVED }
    val availableCount = tickets.count { it.status == TicketStatus.AVAILABLE }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = raffle?.name ?: "",
                            fontWeight = FontWeight.Bold
                        )
                        raffle?.prizeName?.let {
                            Text(text = it, fontSize = 13.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            LegendRow()
            HorizontalDivider()

            if (isSelectionMode) {
                SelectionBar(
                    count = selectedIds.size,
                    onCancel = { exitSelectionMode() },
                    onConfirm = { showGroupSellDialog = true }
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(tickets, key = { it.id }) { ticket ->
                    TicketCell(
                        ticket = ticket,
                        isSelected = selectedIds.contains(ticket.id),
                        onClick = {
                            if (isSelectionMode) {
                                if (ticket.status == TicketStatus.AVAILABLE) {
                                    selectedIds = if (selectedIds.contains(ticket.id)) {
                                        selectedIds - ticket.id
                                    } else {
                                        selectedIds + ticket.id
                                    }
                                }
                            } else {
                                if (ticket.status == TicketStatus.AVAILABLE) {
                                    ticketToSell = ticket
                                } else {
                                    ticketToView = ticket
                                }
                            }
                        },
                        onLongClick = {
                            if (ticket.status == TicketStatus.AVAILABLE) {
                                isSelectionMode = true
                                selectedIds = setOf(ticket.id)
                            }
                        }
                    )
                }
            }

            HorizontalDivider()
            SummaryBar(sold = soldCount, reserved = reservedCount, available = availableCount)
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

    if (showGroupSellDialog) {
        val selectedTickets = tickets.filter { selectedIds.contains(it.id) }
        GroupSellDialog(
            tickets = selectedTickets,
            onDismiss = { showGroupSellDialog = false },
            onConfirm = { buyerName, buyerPhone, status ->
                viewModel.sellOrReserveGroup(selectedTickets, buyerName, buyerPhone, status)
                showGroupSellDialog = false
                exitSelectionMode()
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
private fun LegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = ColorAvailable, label = "Disponible")
        LegendItem(color = ColorReserved, label = "Apartado")
        LegendItem(color = ColorSold, label = "Vendido")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun SummaryBar(sold: Int, reserved: Int, available: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SummaryItem(label = "Vendidos", value = sold, color = ColorSold)
        SummaryItem(label = "Apartados", value = reserved, color = ColorReserved)
        SummaryItem(label = "Disponibles", value = available, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(text = label, fontSize = 12.sp)
    }
}

@Composable
private fun SelectionBar(count: Int, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) {
            Text("Cancelar")
        }
        Text("$count seleccionados")
        Button(onClick = onConfirm, enabled = count > 0) {
            Text("Vender grupo")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TicketCell(
    ticket: Ticket,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = when (ticket.status) {
        TicketStatus.AVAILABLE -> ColorAvailable
        TicketStatus.RESERVED -> ColorReserved
        TicketStatus.SOLD -> ColorSold
    }
    val textColor = if (ticket.status == TicketStatus.AVAILABLE) Color(0xFF616161) else Color.White

    Box(
        modifier = Modifier
            .padding(3.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = ColorSelectedBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ticket.number,
            color = textColor,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
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
private fun GroupSellDialog(
    tickets: List<Ticket>,
    onDismiss: () -> Unit,
    onConfirm: (buyerName: String, buyerPhone: String?, status: TicketStatus) -> Unit
) {
    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    val numbers = tickets.sortedBy { it.number }.joinToString(", ") { it.number }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Boletos: $numbers") },
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