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
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.RaffleStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateRaffle: () -> Unit,
    onOpenRaffle: (String) -> Unit,
    onOpenSoldTickets: (String) -> Unit,
    onEditRaffle: (String) -> Unit
) {
    val raffleItems by viewModel.raffleItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var raffleToDelete by remember { mutableStateOf<Raffle?>(null) }

    val context = LocalContext.current

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
                modifier = Modifier.padding(paddingValues).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(raffleItems, key = { it.raffle.id }) { item ->
                    RaffleCard(
                        item = item,
                        onClick = { onOpenRaffle(item.raffle.id) },
                        onSell = { onOpenRaffle(item.raffle.id) },
                        onOpenSoldTickets = { onOpenSoldTickets(item.raffle.id) },
                        onShare = { viewModel.shareAvailableNumbers(context, item.raffle) },
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
    onSell: () -> Unit,
    onOpenSoldTickets: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val raffle = item.raffle
    var menuExpanded by remember { mutableStateOf(false) }
    val isClosed = raffle.status == RaffleStatus.CLOSED
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.forLanguageTag("es-ES")) }
    val dateStr = dateFormat.format(Date(raffle.drawDate))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = raffle.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = raffle.prizeName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(status = raffle.status)
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Opciones", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Editar") },
                                onClick = { menuExpanded = false; onEdit() },
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar") },
                                onClick = { menuExpanded = false; onDelete() },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isClosed) "Sorteo completado" else "Progreso de ventas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.soldCount}/${item.totalCount}",
                    style = MaterialTheme.typography.labelSmall,
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
                    .height(6.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Detalle de la venta: precio, fecha y modalidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HomeDetailItem(Icons.Filled.Payments, "₡${raffle.ticketPrice.toInt()}")
                HomeDetailItem(Icons.Filled.Event, dateStr)
                HomeDetailItem(
                    if (raffle.modality == RaffleModality.GROUPS) Icons.Filled.Groups else Icons.Filled.ConfirmationNumber,
                    if (raffle.modality == RaffleModality.GROUPS) "${raffle.groupSize} números" else "Sencilla"
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Sección de venta y compartir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSell,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isClosed,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Payments, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("VENDER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                FilledTonalButton(
                    onClick = onOpenSoldTickets,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.MonetizationOn, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("VENTAS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(38.dp).background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    )
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeDetailItem(icon: ImageVector, value: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusChip(status: RaffleStatus) {
    val isActive = status == RaffleStatus.ACTIVE
    val containerColor = if (isActive) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
    val contentColor = if (isActive) Color(0xFF2E7D32) else Color(0xFF757575)

    Surface(color = containerColor, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = if (isActive) "ACTIVA" else "CERRADA",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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