package com.example.lotteryapp.ui.raffle

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.RaffleSource
import com.example.lotteryapp.data.entity.RaffleStatus
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.ui.components.PhoneFieldWithContacts
import com.example.lotteryapp.ui.theme.TicketAvailable
import com.example.lotteryapp.ui.theme.TicketReservedBg
import com.example.lotteryapp.ui.theme.TicketSelected
import com.example.lotteryapp.ui.theme.TicketSoldBg
import com.example.lotteryapp.ui.theme.TicketSelectedBorder
import com.example.lotteryapp.util.ImageSharingHelper
import com.example.lotteryapp.util.WhatsAppSender
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TicketGridScreen(
    viewModel: TicketGridViewModel,
    onBack: () -> Unit,
    onOpenSoldTickets: () -> Unit,
    onEditRaffle: (String) -> Unit
) {
    val tickets by viewModel.tickets.collectAsState()
    val raffle by viewModel.raffle.collectAsState()
    val lastTransaction by viewModel.lastTransaction.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var selectedTicketForDetails by remember { mutableStateOf<Ticket?>(null) }
    var selectedGroupForDetails by remember { mutableStateOf<List<Ticket>?>(null) }
    var showGroupSellDialog by remember { mutableStateOf(false) }
    var showQuickSellDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isRaffleActive = raffle?.status == RaffleStatus.ACTIVE
    val isGroupMode = raffle?.modality == RaffleModality.GROUPS

    val groups: List<List<Ticket>> = remember(tickets, isGroupMode) {
        if (isGroupMode) {
            tickets.groupBy { it.groupId ?: it.id }
                .values
                .map { it.sortedBy { t -> t.number } }
                .sortedBy { it.first().number }
        } else {
            emptyList()
        }
    }
    val selectedGroupCount = if (isGroupMode && selectedIds.isNotEmpty()) 1 else 0

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, withDismissAction = true)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Panel de Rifa", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text(
                                "Limpiar (${if (isGroupMode) selectedGroupCount else selectedIds.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            raffle?.let { currentRaffle ->
                                ImageSharingHelper.shareAvailableNumbers(context, currentRaffle, tickets)
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            raffle?.let {
                RaffleHeaderSaaS(
                    raffle = it,
                    onEditClick = { onEditRaffle(it.id) },
                    onOpenSoldTickets = onOpenSoldTickets
                )
            }
            LegendRow()
            if (isGroupMode) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(groups, key = { group -> group.joinToString(",") { it.id } }) { group ->
                        val gStatus = groupStatusOf(group)
                        val isSelected = group.all { selectedIds.contains(it.id) }
                        GroupCell(
                            group = group,
                            index = groups.indexOf(group),
                            groupCount = groups.size,
                            status = gStatus,
                            isSelected = isSelected,
                            onClick = {
                                if (gStatus == TicketStatus.AVAILABLE && isRaffleActive) {
                                    selectedIds = group.map { it.id }.toSet()
                                    showGroupSellDialog = true
                                } else {
                                    selectedGroupForDetails = group
                                }
                            }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(10),
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(tickets, key = { it.id }) { ticket ->
                        val isSelected = selectedIds.contains(ticket.id)
                        TicketCell(
                            ticket = ticket,
                            isSelected = isSelected,
                            onClick = {
                                if (ticket.status == TicketStatus.AVAILABLE && isRaffleActive) {
                                    selectedIds = if (isSelected) selectedIds - ticket.id else selectedIds + ticket.id
                                } else {
                                    selectedTicketForDetails = ticket
                                }
                            }
                        )
                    }
                }
            }

            // Panel de Acción Integrado al fondo (Diseño compacto sin barra de progreso)
            BottomActionPanel(
                sold = tickets.count { it.status == TicketStatus.SOLD },
                reserved = tickets.count { it.status == TicketStatus.RESERVED },
                available = tickets.count { it.status == TicketStatus.AVAILABLE },
                total = if (isGroupMode) groups.size else tickets.size,
                mode = if (isGroupMode) RaffleModality.GROUPS else RaffleModality.SENCILLA,
                isRaffleActive = isRaffleActive,
                selectedCount = if (isGroupMode) selectedGroupCount else selectedIds.size,
                onButtonClick = {
                    if (isGroupMode) showGroupSellDialog = true
                    else if (selectedIds.isEmpty()) showQuickSellDialog = true
                    else showGroupSellDialog = true
                }
            )
        }
    }

    if (showGroupSellDialog) {
        val selectedTickets = tickets.filter { selectedIds.contains(it.id) }
        GroupSellDialog(
            tickets = selectedTickets,
            onDismiss = {
                showGroupSellDialog = false
                selectedIds = emptySet()
            },
            onConfirm = { name, phone, status ->
                viewModel.sellOrReserveGroup(selectedTickets, name, phone, status)
                selectedIds = emptySet()
                showGroupSellDialog = false
            }
        )
    }

    if (showQuickSellDialog) {
        QuickSellDialog(
            allTickets = tickets,
            onDismiss = { showQuickSellDialog = false },
            onConfirm = { selTickets, name, phone, status ->
                viewModel.sellOrReserveGroup(selTickets, name, phone, status)
                showQuickSellDialog = false
            }
        )
    }

    if (selectedTicketForDetails != null) {
        ModalBottomSheet(onDismissRequest = { selectedTicketForDetails = null }) {
            TicketActionSheetContent(
                ticket = selectedTicketForDetails!!,
                canModify = isRaffleActive,
                onToggleStatus = {
                    val newStatus = if (it.status == TicketStatus.SOLD) TicketStatus.RESERVED else TicketStatus.SOLD
                    viewModel.changeStatus(it, newStatus)
                    selectedTicketForDetails = null
                },
                onFree = {
                    viewModel.cancel(selectedTicketForDetails!!)
                    selectedTicketForDetails = null
                    scope.launch { snackbarHostState.showSnackbar("Número liberado") }
                }
            )
        }
    }

    if (selectedGroupForDetails != null) {
        val group = selectedGroupForDetails!!
        ModalBottomSheet(onDismissRequest = { selectedGroupForDetails = null }) {
            GroupActionSheetContent(
                group = group,
                canModify = isRaffleActive,
                onToggleStatus = {
                    val entryTickets = group
                    val newStatus = if (entryTickets.first().status == TicketStatus.SOLD) TicketStatus.RESERVED else TicketStatus.SOLD
                    viewModel.changeTicketsStatus(entryTickets, newStatus)
                    selectedGroupForDetails = null
                },
                onFree = {
                    viewModel.cancelGroup(group)
                    selectedGroupForDetails = null
                    scope.launch { snackbarHostState.showSnackbar("Grupo liberado") }
                }
            )
        }
    }

    lastTransaction?.let { transaction ->
        raffle?.let { currentRaffle ->
            val hasPhone = !transaction.firstOrNull()?.buyerPhone.isNullOrBlank()
            AlertDialog(
                onDismissRequest = { viewModel.clearLastTransaction() },
                title = { Text("¡Operación Exitosa!", fontWeight = FontWeight.Bold) },
                text = { Text(if (hasPhone) "¿Deseas enviar el recibo por WhatsApp ahora?" else "Datos guardados correctamente.") },
                confirmButton = {
                    if (hasPhone) {
                        Button(onClick = {
                            WhatsAppSender.sendTextReceipt(context, currentRaffle, transaction)
                            viewModel.clearLastTransaction()
                        }) { Text("Enviar WhatsApp") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.clearLastTransaction() }) { Text("Cerrar") }
                }
            )
        }
    }
}

@Composable
private fun BottomActionPanel(
    sold: Int,
    reserved: Int,
    available: Int,
    total: Int,
    mode: RaffleModality,
    isRaffleActive: Boolean,
    selectedCount: Int,
    onButtonClick: () -> Unit
) {
    val isGroupMode = mode == RaffleModality.GROUPS
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryStatSmall(
                    label = "VENDIDO",
                    value = sold.toString(),
                    color = TicketSoldBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatSmall(
                    label = "RESERVA",
                    value = reserved.toString(),
                    color = TicketReservedBg,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatSmall(
                    label = "LIBRE",
                    value = available.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
            }

            if (isRaffleActive) {
                Button(
                    onClick = onButtonClick,
                    enabled = !isGroupMode || selectedCount > 0,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCount == 0) MaterialTheme.colorScheme.primary else TicketSelectedBorder
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        when {
                            isGroupMode && selectedCount == 0 -> Icons.Default.Groups
                            selectedCount > 0 -> Icons.Default.CheckCircle
                            else -> Icons.Default.Bolt
                        },
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            isGroupMode && selectedCount == 0 -> "SELECCIONA UN GRUPO"
                            selectedCount > 0 -> "CONFIRMAR ($selectedCount)"
                            else -> "VENTA RÁPIDA"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSellDialog(
    allTickets: List<Ticket>,
    onDismiss: () -> Unit,
    onConfirm: (List<Ticket>, String, String?, TicketStatus) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<TicketStatus?>(null) }

    val (availableTickets, unavailableMsg) = remember(input, allTickets) {
        val typed = input.split(Regex("[\\s,.]+")).map { it.trim().padStart(2, '0') }.filter { it.isNotEmpty() && it.all { c -> c.isDigit() } }.distinct()
        val available = allTickets.filter { it.number in typed && it.status == TicketStatus.AVAILABLE }
        val unavailable = typed.filter { num -> allTickets.none { it.number == num && it.status == TicketStatus.AVAILABLE } }
        val msg = if (unavailable.isNotEmpty()) "No disponibles: ${unavailable.joinToString(", ")}" else null
        available to msg
    }

    val isPhoneValid = buyerPhone.isEmpty() || buyerPhone.length == 8
    val isFormValid = buyerName.isNotBlank() && isPhoneValid && availableTickets.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedStatus == null) "¿Qué desea hacer?" else "Datos del Cliente", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedStatus == null) {
                    Text("Ingrese los números:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Números (ej: 05, 12, 88)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    unavailableMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { selectedStatus = TicketStatus.SOLD },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TicketSoldBg),
                        enabled = availableTickets.isNotEmpty()
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("VENDER (Pagado)")
                    }

                    FilledTonalButton(
                        onClick = { selectedStatus = TicketStatus.RESERVED },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = TicketReservedBg.copy(alpha = 0.2f), contentColor = Color(0xFF7B5E00)),
                        enabled = availableTickets.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(Modifier.width(8.dp))
                        Text("APARTAR (Reserva)")
                    }
                } else {
                    Text("Procesando como: ${if(selectedStatus == TicketStatus.SOLD) "VENTA" else "APARTADO"}", fontWeight = FontWeight.Bold, color = if(selectedStatus == TicketStatus.SOLD) TicketSoldBg else Color(0xFF7B5E00))
                    OutlinedTextField(
                        value = buyerName,
                        onValueChange = { if (it.all { c -> c.isLetterOrDigit() || c.isWhitespace() }) buyerName = it },
                        label = { Text("Nombre del cliente") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    PhoneFieldWithContacts(
                        value = buyerPhone,
                        onValueChange = { buyerPhone = it },
                        isError = !isPhoneValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (selectedStatus != null) {
                Button(
                    onClick = { onConfirm(availableTickets, buyerName, buyerPhone.ifBlank { null }, selectedStatus!!) },
                    enabled = isFormValid
                ) {
                    Text("Finalizar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (selectedStatus != null) selectedStatus = null else onDismiss() }) {
                Text(if (selectedStatus != null) "Atrás" else "Cancelar")
            }
        }
    )
}

@Composable
private fun GroupSellDialog(
    tickets: List<Ticket>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, TicketStatus) -> Unit
) {
    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf<TicketStatus?>(null) }

    val isPhoneValid = buyerPhone.isEmpty() || buyerPhone.length == 8
    val isFormValid = buyerName.isNotBlank() && isPhoneValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedStatus == null) "¿Vender o Apartar?" else "Datos del Cliente", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (selectedStatus == null) {
                    Text("Números: ${tickets.joinToString(", ") { it.number }}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)

                    Button(
                        onClick = { selectedStatus = TicketStatus.SOLD },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TicketSoldBg)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(12.dp))
                        Text("VENDER (Ya pagado)", fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = { selectedStatus = TicketStatus.RESERVED },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = TicketReservedBg.copy(alpha = 0.2f), contentColor = Color(0xFF7B5E00))
                    ) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(Modifier.width(12.dp))
                        Text("APARTAR (Reserva)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Procesando como: ${if(selectedStatus == TicketStatus.SOLD) "VENTA" else "APARTADO"}", fontWeight = FontWeight.Bold, color = if(selectedStatus == TicketStatus.SOLD) TicketSoldBg else Color(0xFF7B5E00))

                    Text(
                        text = "Números: ${tickets.joinToString(", ") { it.number }}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    OutlinedTextField(
                        value = buyerName,
                        onValueChange = { if (it.all { c -> c.isLetterOrDigit() || c.isWhitespace() }) buyerName = it },
                        label = { Text("Nombre del cliente") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    PhoneFieldWithContacts(
                        value = buyerPhone,
                        onValueChange = { buyerPhone = it },
                        isError = !isPhoneValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (selectedStatus != null) {
                Button(
                    onClick = { onConfirm(buyerName, buyerPhone.ifBlank { null }, selectedStatus!!) },
                    enabled = isFormValid
                ) {
                    Text("Finalizar Venta")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (selectedStatus != null) selectedStatus = null else onDismiss() }) {
                Text(if (selectedStatus != null) "Atrás" else "Cancelar")
            }
        }
    )
}

@Composable
private fun RaffleHeaderSaaS(raffle: Raffle, onEditClick: () -> Unit, onOpenSoldTickets: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM", Locale.forLanguageTag("es-ES"))
    val dateStr = dateFormat.format(Date(raffle.drawDate))
    val sourceStr = when(raffle.source) {
        RaffleSource.LOTERIA_NACIONAL -> "Nal."
        RaffleSource.CHANCES -> "Chances"
        RaffleSource.SORTEO -> "Especial"
        RaffleSource.MANUAl -> "Manual"
        RaffleSource.OTRO -> "Otro"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).clickable { onEditClick() }) {
                    if (raffle.prizePhotoPath != null) {
                        AsyncImage(model = raffle.prizePhotoPath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(raffle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(raffle.prizeName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(color = if(raffle.status == RaffleStatus.ACTIVE) Color(0xFFE8F5E9) else Color(0xFFF5F5F5), shape = RoundedCornerShape(4.dp)) {
                    Text(if(raffle.status == RaffleStatus.ACTIVE) "ACTIVA" else "CERRADA", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if(raffle.status == RaffleStatus.ACTIVE) Color(0xFF2E7D32) else Color(0xFF757575), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RaffleDetailItemCompact(Icons.Default.Payments, "₡${raffle.ticketPrice.toInt()}", Modifier.weight(1f))
                    RaffleDetailItemCompact(Icons.Default.Event, dateStr, Modifier.weight(1f))
                    RaffleDetailItemCompact(Icons.Default.ConfirmationNumber, sourceStr, Modifier.weight(1f))
                }

                TextButton(
                    onClick = onOpenSoldTickets,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("VENTAS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RaffleDetailItemCompact(icon: ImageVector, value: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Spacer(Modifier.width(4.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TicketActionSheetContent(ticket: Ticket, canModify: Boolean, onToggleStatus: (Ticket) -> Unit, onFree: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Número ${ticket.number}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 16.dp))
        ListItem(headlineContent = { Text("Cliente: ${ticket.buyerName ?: "Sin nombre"}") }, supportingContent = { Text("Estado: ${if(ticket.status == TicketStatus.SOLD) "Vendido" else "Apartado"}") }, leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) })
        if (canModify) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            if (ticket.status == TicketStatus.RESERVED) {
                ListItem(headlineContent = { Text("Confirmar Pago (Marcar como Vendido)", fontWeight = FontWeight.Bold, color = TicketSoldBg) }, leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = TicketSoldBg) }, modifier = Modifier.clickable { onToggleStatus(ticket) })
            } else {
                ListItem(headlineContent = { Text("Cambiar a Apartado", fontWeight = FontWeight.Bold, color = TicketReservedBg) }, leadingContent = { Icon(Icons.Default.Schedule, null, tint = TicketReservedBg) }, modifier = Modifier.clickable { onToggleStatus(ticket) })
            }
            ListItem(headlineContent = { Text("Liberar número (Borrar venta)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }, leadingContent = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { onFree() })
        }
    }
}

@Composable
private fun LegendRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
        LegendItem(color = TicketAvailable, label = "Libre")
        LegendItem(color = TicketReservedBg, label = "Apartado")
        LegendItem(color = TicketSoldBg, label = "Vendido")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color).border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun SummaryStatSmall(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color, maxLines = 1)
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), letterSpacing = 0.5.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TicketCell(ticket: Ticket, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = when (ticket.status) {
        TicketStatus.AVAILABLE -> if (isSelected) TicketSelectedBorder.copy(alpha = 0.1f) else Color.White
        TicketStatus.RESERVED -> TicketReservedBg
        TicketStatus.SOLD -> TicketSoldBg
    }
    val textColor = when (ticket.status) {
        TicketStatus.AVAILABLE -> if (isSelected) TicketSelectedBorder else Color(0xFF424242)
        else -> Color.White
    }
    
    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) TicketSelectedBorder else Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ticket.number,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected || ticket.status != TicketStatus.AVAILABLE) FontWeight.ExtraBold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCell(
    group: List<Ticket>,
    index: Int,
    groupCount: Int,
    status: TicketStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (status) {
        TicketStatus.AVAILABLE -> if (isSelected) TicketSelectedBorder.copy(alpha = 0.1f) else Color.White
        TicketStatus.RESERVED -> TicketReservedBg
        TicketStatus.SOLD -> TicketSoldBg
    }
    val textColor = when (status) {
        TicketStatus.AVAILABLE -> if (isSelected) TicketSelectedBorder else Color(0xFF424242)
        else -> Color.White
    }
    val showRange = group.size > 10
    val numbersText = group.joinToString(" - ") { it.number }

    Box(
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) TicketSelectedBorder else Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
            Text(
                text = "G${index + 1}/$groupCount",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                maxLines = 1
            )
            if (showRange) {
                Text(
                    text = "${group.size} números",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    maxLines = 1
                )
                Text(
                    text = numbersText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = numbersText,
                    fontSize = if (group.size >= 8) 9.sp else 13.sp,
                    fontWeight = if (isSelected || status != TicketStatus.AVAILABLE) FontWeight.ExtraBold else FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun groupStatusOf(group: List<Ticket>): TicketStatus {
    if (group.isEmpty()) return TicketStatus.AVAILABLE
    return when {
        group.all { it.status == TicketStatus.SOLD } -> TicketStatus.SOLD
        group.all { it.status == TicketStatus.AVAILABLE } -> TicketStatus.AVAILABLE
        else -> TicketStatus.RESERVED
    }
}

@Composable
private fun GroupActionSheetContent(
    group: List<Ticket>,
    canModify: Boolean,
    onToggleStatus: () -> Unit,
    onFree: () -> Unit
) {
    val first = group.firstOrNull() ?: return
    val soldStatus = first.status == TicketStatus.SOLD
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Grupo · ${group.size} números",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text = group.joinToString(" - ") { it.number },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ListItem(
            headlineContent = { Text("Cliente: ${first.buyerName ?: "Sin nombre"}") },
            supportingContent = { Text("Estado: ${if (soldStatus) "Vendido" else "Apartado"}") },
            leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
        )
        if (canModify) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            if (first.status == TicketStatus.RESERVED) {
                ListItem(headlineContent = { Text("Confirmar Pago (Marcar como Vendido)", fontWeight = FontWeight.Bold, color = TicketSoldBg) }, leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = TicketSoldBg) }, modifier = Modifier.clickable { onToggleStatus() })
            } else {
                ListItem(headlineContent = { Text("Cambiar a Apartado", fontWeight = FontWeight.Bold, color = TicketReservedBg) }, leadingContent = { Icon(Icons.Default.Schedule, null, tint = TicketReservedBg) }, modifier = Modifier.clickable { onToggleStatus() })
            }
            ListItem(headlineContent = { Text("Liberar grupo (Borrar venta)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }, leadingContent = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { onFree() })
        }
    }
}