package com.example.lotteryapp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.example.lotteryapp.data.entity.Raffle

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateRaffle: () -> Unit,
    onOpenRaffle: (String) -> Unit,
    onEditRaffle: (String) -> Unit
) {
    val raffles by viewModel.activeRaffles.collectAsState()
    var raffleToDelete by remember { mutableStateOf<Raffle?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRaffle) {
                Icon(Icons.Filled.Add, contentDescription = "Crear rifa")
            }
        }
    ) { paddingValues ->
        if (raffles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay rifas activas. Tocá + para crear una.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(raffles, key = { it.id }) { raffle ->
                    RaffleRow(
                        raffle = raffle,
                        onClick = { onOpenRaffle(raffle.id) },
                        onEdit = { onEditRaffle(raffle.id) },
                        onDelete = { raffleToDelete = raffle }
                    )
                }
            }
        }
    }

    raffleToDelete?.let { raffle ->
        AlertDialog(
            onDismissRequest = { raffleToDelete = null },
            title = { Text("Eliminar rifa") },
            text = { Text("¿Seguro que querés eliminar \"${raffle.name}\"? Esta acción no se puede deshacer y se perderán todos sus boletos.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRaffle(raffle)
                    raffleToDelete = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { raffleToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun RaffleRow(
    raffle: Raffle,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(raffle.name) },
            supportingContent = { Text(raffle.prizeName) },
            trailingContent = {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            },
            modifier = Modifier.padding(4.dp)
        )
    }
}