package com.example.lotteryapp.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lotteryapp.data.entity.Raffle
import androidx.compose.foundation.clickable

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateRaffle: () -> Unit,
    onOpenRaffle: (String) -> Unit
) {
    val raffles by viewModel.activeRaffles.collectAsState()

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
                    RaffleRow(raffle = raffle, onClick = { onOpenRaffle(raffle.id) })
                }
            }
        }
    }
}

@Composable
private fun RaffleRow(raffle: Raffle, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(raffle.name) },
            supportingContent = { Text(raffle.prizeName) },
            modifier = Modifier.padding(4.dp)
        )
    }
}