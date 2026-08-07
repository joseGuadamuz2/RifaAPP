package com.example.lotteryapp.ui.raffle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus

@Composable
fun SoldTicketsScreen(viewModel: SoldTicketsViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.results.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text("Buscar por nombre o número") },
            modifier = Modifier.fillMaxWidth()
        )

        LazyColumn {
            items(results, key = { it.id }) { ticket ->
                SoldTicketRow(ticket)
                Divider()
            }
        }
    }
}

@Composable
private fun SoldTicketRow(ticket: Ticket) {
    val statusLabel = when (ticket.status) {
        TicketStatus.SOLD -> "Vendido"
        TicketStatus.RESERVED -> "Apartado"
        TicketStatus.AVAILABLE -> "" // no debería aparecer acá, la consulta ya los excluye
    }

    ListItem(
        headlineContent = { Text("#${ticket.number} — ${ticket.buyerName ?: ""}") },
        supportingContent = { Text(statusLabel) }
    )
}