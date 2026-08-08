package com.example.lotteryapp.ui.raffle

import androidx.compose.animation.*
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleStatus
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.util.ImageSharingHelper
import com.example.lotteryapp.util.WhatsAppSender
import kotlinx.coroutines.launch

private val ColorAvailable = Color(0xFFF5F5F5)
private val ColorReserved = Color(0xFFFFD54F)
private val ColorSold = Color(0xFF66BB6A)
private val ColorSelectedBorder = Color(0xFF1976D2)

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
    var showGroupSellDialog by remember { mutableStateOf(false) }
    var showQuickSellDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val isRaffleActive = raffle?.status == RaffleStatus.ACTIVE

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
                            Text("Limpiar (${selectedIds.size})", fontWeight = FontWeight.Bold)
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
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            raffle?.let { 
                RaffleHeaderSaaS(
                    raffle = it,
                    onEditClick = { onEditRaffle(it.id) },
                    onOpenSoldTickets = onOpenSoldTickets
                )
            }
            LegendRow()
            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)
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
            
            BottomActionPanel(
                sold = tickets.count { it.status == TicketStatus.SOLD },
                reserved = tickets.count { it.status == TicketStatus.RESERVED },
                available = tickets.count { it.status == TicketStatus.AVAILABLE },
                total = tickets.size,
                isRaffleActive = isRaffleActive,
                selectedCount = selectedIds.size,
                onButtonClick = {
                    if (selectedIds.isEmpty()) showQuickSellDialog = true 
                    else showGroupSellDialog = true 
                }
            )
        }
    }

    if (showGroupSellDialog) {
        val selectedTickets = tickets.filter { selectedIds.contains(it.id) }
        GroupSellDialog(
            tickets = selectedTickets,
            onDismiss = { showGroupSellDialog = false },
            onConfirm = { name, phone, status ->
                viewModel.sellOrReserveGroup(selectedTickets, name, phone, status)
                selectedIds = emptySet()
                showGroupSellDialog = false
            }
        )
    }

    if (showQuickSellDialog) {
        val availableTickets = tickets.filter { it.status == TicketStatus.AVAILABLE }
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
    isRaffleActive: Boolean,
    selectedCount: Int,
    onButtonClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp, 
        shadowElevation = 12.dp, 
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val progress = if (total > 0) sold.toFloat() / total.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress }, 
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), 
                color = ColorSold, 
                trackColor = ColorAvailable
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceAround) {
                    SummaryStatSmall(label = "Pagados", value = sold.toString(), color = ColorSold)
                    SummaryStatSmall(label = "Apartados", value = reserved.toString(), color = ColorReserved)
                    SummaryStatSmall(label = "Libres", value = available.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                if (isRaffleActive) {
                    Spacer(Modifier.width(16.dp))
                    Button(
                        onClick = onButtonClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCount == 0) MaterialTheme.colorScheme.primary else ColorSelectedBorder
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            if (selectedCount == 0) Icons.Default.Bolt else Icons.Default.CheckCircle, 
                            null, 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedCount == 0) "VENTA RÁPIDA" else "CONFIRMAR ($selectedCount)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
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
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSold),
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
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = ColorReserved.copy(alpha = 0.2f), contentColor = Color(0xFF7B5E00)),
                        enabled = availableTickets.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(Modifier.width(8.dp))
                        Text("APARTAR (Reserva)")
                    }
                } else {
                    Text("Procesando como: ${if(selectedStatus == TicketStatus.SOLD) "VENTA" else "APARTADO"}", fontWeight = FontWeight.Bold, color = if(selectedStatus == TicketStatus.SOLD) ColorSold else Color(0xFF7B5E00))
                    OutlinedTextField(value = buyerName, onValueChange = { buyerName = it }, label = { Text("Nombre del cliente") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(
                        value = buyerPhone, 
                        onValueChange = { if (it.length <= 8) buyerPhone = it.filter { c -> c.isDigit() } }, 
                        label = { Text("WhatsApp (8 dígitos)") }, 
                        isError = !isPhoneValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp), 
                        singleLine = true
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
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSold)
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(12.dp))
                        Text("VENDER (Ya pagado)", fontWeight = FontWeight.Bold)
                    }
                    
                    FilledTonalButton(
                        onClick = { selectedStatus = TicketStatus.RESERVED },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = ColorReserved.copy(alpha = 0.2f), contentColor = Color(0xFF7B5E00))
                    ) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(Modifier.width(12.dp))
                        Text("APARTAR (Reserva)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Modo: ${if(selectedStatus == TicketStatus.SOLD) "VENTA" else "APARTADO"}", fontWeight = FontWeight.Bold, color = if(selectedStatus == TicketStatus.SOLD) ColorSold else Color(0xFF7B5E00))
                    
                    OutlinedTextField(value = buyerName, onValueChange = { buyerName = it }, label = { Text("Nombre del cliente") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    OutlinedTextField(
                        value = buyerPhone, 
                        onValueChange = { if (it.length <= 8) buyerPhone = it.filter { c -> c.isDigit() } }, 
                        label = { Text("WhatsApp (8 dígitos)") }, 
                        isError = !isPhoneValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), 
                        modifier = Modifier.fillMaxWidth(), 
                        shape = RoundedCornerShape(12.dp), 
                        singleLine = true
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(20.dp)).clickable { onEditClick() }) {
                if (raffle.prizePhotoPath != null) {
                    AsyncImage(model = raffle.prizePhotoPath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(24.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                    }
                } else {
                    val stroke = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    Canvas(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                        drawRoundRect(color = Color.LightGray, style = stroke)
                    }
                    Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center).size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(raffle.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Surface(color = if(raffle.status == RaffleStatus.ACTIVE) ColorSold.copy(alpha = 0.1f) else Color.LightGray, shape = RoundedCornerShape(8.dp)) {
                        Text(if(raffle.status == RaffleStatus.ACTIVE) "ACTIVA" else "CERRADA", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = if(raffle.status == RaffleStatus.ACTIVE) ColorSold else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
                Text(raffle.prizeName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(onClick = onOpenSoldTickets, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Detalle de Ventas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TicketActionSheetContent(ticket: Ticket, canModify: Boolean, onToggleStatus: (Ticket) -> Unit, onFree: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, start = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Número ${ticket.number}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 16.dp))
        ListItem(headlineContent = { Text("Cliente: ${ticket.buyerName ?: "Sin nombre"}") }, supportingContent = { Text("Estado: ${if(ticket.status == TicketStatus.SOLD) "Vendido" else "Apartado"}") }, leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) })
        if (canModify) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (ticket.status == TicketStatus.RESERVED) {
                ListItem(headlineContent = { Text("Confirmar Pago (Marcar como Vendido)", fontWeight = FontWeight.Bold, color = ColorSold) }, leadingContent = { Icon(Icons.Default.CheckCircle, null, tint = ColorSold) }, modifier = Modifier.clickable { onToggleStatus(ticket) })
            } else {
                ListItem(headlineContent = { Text("Cambiar a Apartado", fontWeight = FontWeight.Bold, color = ColorReserved) }, leadingContent = { Icon(Icons.Default.Schedule, null, tint = ColorReserved) }, modifier = Modifier.clickable { onToggleStatus(ticket) })
            }
            ListItem(headlineContent = { Text("Liberar número (Borrar venta)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }, leadingContent = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) }, modifier = Modifier.clickable { onFree() })
        }
    }
}

@Composable
private fun LegendRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
        LegendItem(color = ColorAvailable, label = "Libre")
        Spacer(Modifier.width(20.dp))
        LegendItem(color = ColorReserved, label = "Apartado")
        Spacer(Modifier.width(20.dp))
        LegendItem(color = ColorSold, label = "Vendido")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color).border(0.5.dp, Color.Gray.copy(alpha = 0.3f), CircleShape))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SummaryStatSmall(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TicketCell(ticket: Ticket, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = when (ticket.status) {
        TicketStatus.AVAILABLE -> if (isSelected) ColorSelectedBorder.copy(alpha = 0.15f) else ColorAvailable
        TicketStatus.RESERVED -> ColorReserved
        TicketStatus.SOLD -> ColorSold
    }
    val textColor = if (ticket.status == TicketStatus.AVAILABLE && !isSelected) Color(0xFF616161) else Color.White
    Box(modifier = Modifier.padding(4.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(backgroundColor).border(width = if (isSelected) 3.dp else 0.dp, color = ColorSelectedBorder, shape = RoundedCornerShape(12.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = ticket.number, color = if (isSelected) ColorSelectedBorder else textColor, fontSize = 15.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
