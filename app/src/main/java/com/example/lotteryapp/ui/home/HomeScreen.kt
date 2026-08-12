package com.example.lotteryapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateRaffle: () -> Unit,
    onOpenRaffle: (String) -> Unit,
    onEditRaffle: (String) -> Unit
) {
    val raffleItems by viewModel.raffleItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var raffleToDelete by remember { mutableStateOf<Raffle?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = viewModel::onSearchQueryChange,
                                placeholder = { Text("Buscar rifa...") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        } else {
                            Text("Mis Rifas", fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) viewModel.onSearchQueryChange("")
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                                contentDescription = "Buscar"
                            )
                        }
                    }
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { viewModel.onTabSelected(0) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Activas")
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { viewModel.onTabSelected(1) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Finalizadas")
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateRaffle,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva rifa") }
            )
        }
    ) { paddingValues ->
        if (raffleItems.isEmpty()) {
            EmptyState(
                query = searchQuery,
                tab = selectedTab,
                onCreateClick = onCreateRaffle,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(raffleItems, key = { it.raffle.id }) { item ->
                    RaffleCard(
                        item = item,
                        onClick = { onOpenRaffle(item.raffle.id) },
                        onEdit = { onEditRaffle(item.raffle.id) },
                        onDelete = { raffleToDelete = item.raffle }
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
                Button(
                    onClick = {
                        viewModel.deleteRaffle(raffle)
                        raffleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
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
private fun RaffleCard(
    item: RaffleItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val raffle = item.raffle
    var menuExpanded by remember { mutableStateOf(false) }
    val isClosed = raffle.status == RaffleStatus.CLOSED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatusChip(status = raffle.status)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = raffle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = raffle.prizeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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
                            onClick = { menuExpanded = false; onEdit() },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = { menuExpanded = false; onDelete() },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isClosed) "Sorteo completado" else "Progreso de ventas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.soldCount}/${item.totalCount}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { item.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun StatusChip(status: RaffleStatus) {
    val isActive = status == RaffleStatus.ACTIVE
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(color = containerColor, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = if (isActive) "ACTIVA" else "CERRADA",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun EmptyState(
    query: String,
    tab: Int,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (query.isEmpty()) Icons.Filled.Add else Icons.Filled.Search,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (query.isEmpty()) "No tienes rifas aún" else "Sin resultados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (query.isNotEmpty()) {
                "No encontramos nada que coincida con \"$query\"."
            } else if (tab == 0) {
                "Creá tu primera rifa para empezar a vender números."
            } else {
                "Todavía no tenés rifas finalizadas."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (query.isEmpty() && tab == 0) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateClick) {
                Text("Crear primera rifa")
            }
        }
    }
}