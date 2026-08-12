package com.example.lotteryapp.ui.raffle

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.ui.components.PhoneFieldWithContacts
import com.example.lotteryapp.util.PdfClientRow
import com.example.lotteryapp.util.PdfReportHelper
import com.example.lotteryapp.util.WhatsAppSender
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoldTicketsScreen(
    viewModel: SoldTicketsViewModel,
    onBack: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val entries by viewModel.entries.collectAsState()
    val buyers by viewModel.buyers.collectAsState()
    val raffle by viewModel.raffle.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var entryToCancel by remember { mutableStateOf<SaleEntry?>(null) }
    var entryToEditPhone by remember { mutableStateOf<SaleEntry?>(null) }
    var entryToToggleStatus by remember { mutableStateOf<SaleEntry?>(null) }
    var statusFilter by remember { mutableStateOf<TicketStatus?>(null) }
    
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR")).apply {
        maximumFractionDigits = 0
    }

    // Cálculos para Dashboard
    val perGroup = raffle?.modality == RaffleModality.GROUPS
    val unitsFor = { entry: SaleEntry ->
        if (perGroup) 1 else entry.tickets.size
    }
    val totalRecaudado = entries.filter { it.status == TicketStatus.SOLD }.sumOf { unitsFor(it) * (raffle?.ticketPrice ?: 0.0) }
    val boletosVendidos = entries.filter { it.status == TicketStatus.SOLD }.sumOf { it.tickets.size }
    val boletosPendientes = entries.filter { it.status == TicketStatus.RESERVED }.sumOf { it.tickets.size }
    val montoPendiente = entries.filter { it.status == TicketStatus.RESERVED }.sumOf { unitsFor(it) * (raffle?.ticketPrice ?: 0.0) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Administrador", fontWeight = FontWeight.ExtraBold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val pdfRows = buyers.sortedByDescending { it.totalPaid }.map { buyer ->
                                PdfClientRow(
                                    name = buyer.name,
                                    numbers = buyer.allNumbers.joinToString(", "),
                                    total = currencyFormat.format(buyer.totalPaid + buyer.totalPending)
                                )
                            }
                            PdfReportHelper.generateAndSharePdf(
                                context = context,
                                raffleName = raffle?.name ?: "Rifa",
                                totalRecaudado = currencyFormat.format(totalRecaudado),
                                montoPendiente = currencyFormat.format(montoPendiente),
                                boletosVendidos = boletosVendidos.toString(),
                                boletosPendientes = boletosPendientes.toString(),
                                clientes = pdfRows
                            )
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = {
                            val report = buildString {
                                appendLine("📊 *REPORTE DE VENTAS* 📊")
                                appendLine("*Rifa:* ${raffle?.name?.uppercase()}")
                                appendLine("━━━━━━━━━━━━━━━━")
                                appendLine("💰 *RECAUDADO:* ${currencyFormat.format(totalRecaudado)}")
                                appendLine("⏳ *POR COBRAR:* ${currencyFormat.format(montoPendiente)}")
                                appendLine("🎟️ *PAGADOS:* $boletosVendidos")
                                appendLine("🎟️ *PENDIENTES:* $boletosPendientes")
                                appendLine("━━━━━━━━━━━━━━━━")
                                appendLine("\n📝 *DETALLE DE CLIENTES:*")
                                buyers.sortedByDescending { it.totalPaid }.forEach { buyer ->
                                    val statusIcon = if (buyer.totalPending > 0) "⏳" else "✅"
                                    appendLine("$statusIcon *${buyer.name}*")
                                    appendLine("   Números: ${buyer.allNumbers.joinToString(", ")}")
                                    if (buyer.totalPending > 0) appendLine("   Debe: ${currencyFormat.format(buyer.totalPending)}")
                                }
                                appendLine("\n_Generado por Rifador_ 🎟️")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, report)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartir Reporte"))
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reporte", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            
            // DASHBOARD SaaS MEJORADO
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryStat(label = "Recaudado", value = currencyFormat.format(totalRecaudado), icon = Icons.Default.MonetizationOn, color = Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                    SummaryStat(label = "Por Cobrar", value = currencyFormat.format(montoPendiente), icon = Icons.Default.HourglassBottom, color = Color(0xFFD32F2F), modifier = Modifier.weight(1f))
                    SummaryStat(label = "Venta", value = "$boletosVendidos/100", icon = Icons.Default.ConfirmationNumber, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Ventas", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Directorio", fontWeight = FontWeight.Bold) })
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                Spacer(Modifier.height(16.dp))
                // BUSCADOR SaaS
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Buscar por cliente o número...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) { Icon(Icons.Default.Close, null) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )

                if (selectedTab == 0) {
                    SalesView(
                        entries = entries,
                        statusFilter = statusFilter,
                        onFilterChange = { statusFilter = it },
                        onToggleStatus = { entryToToggleStatus = it },
                        onEditPhone = { entryToEditPhone = it },
                        onCancel = { entryToCancel = it },
                        viewModel = viewModel,
                        raffle = raffle
                    )
                } else {
                    DirectoryView(buyers = buyers, raffle = raffle)
                }
            }
        }
    }

    // DIÁLOGOS (Sin cambios en lógica, solo pulido visual)
    entryToToggleStatus?.let { entry ->
        val isSold = entry.status == TicketStatus.SOLD
        AlertDialog(
            onDismissRequest = { entryToToggleStatus = null },
            title = { Text(if (isSold) "Revertir Pago" else "Confirmar Pago") },
            text = { Text("¿Deseas marcar los boletos ${entry.numbers.joinToString(", ")} como ${if (isSold) "Pendientes" else "Pagados"}?") },
            confirmButton = {
                Button(onClick = { viewModel.toggleStatus(entry); entryToToggleStatus = null }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { entryToToggleStatus = null }) { Text("Cancelar") } }
        )
    }

    entryToCancel?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToCancel = null },
            title = { Text("Eliminar Registro", color = MaterialTheme.colorScheme.error) },
            text = { Text("¿Estás seguro de liberar los números ${entry.numbers.joinToString(", ")}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(onClick = { viewModel.cancelEntry(entry); entryToCancel = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { entryToCancel = null }) { Text("Atrás") } }
        )
    }

    entryToEditPhone?.let { entry ->
        EditPhoneDialog(currentPhone = entry.buyerPhone, onDismiss = { entryToEditPhone = null }, onConfirm = { newPhone -> viewModel.editPhone(entry, newPhone); entryToEditPhone = null })
    }
}

@Composable
private fun SalesView(
    entries: List<SaleEntry>,
    statusFilter: TicketStatus?,
    onFilterChange: (TicketStatus?) -> Unit,
    onToggleStatus: (SaleEntry) -> Unit,
    onEditPhone: (SaleEntry) -> Unit,
    onCancel: (SaleEntry) -> Unit,
    viewModel: SoldTicketsViewModel,
    raffle: com.example.lotteryapp.data.entity.Raffle?
) {
    val context = LocalContext.current
    val filtered = remember(entries, statusFilter) {
        statusFilter?.let { filter -> entries.filter { it.status == filter } } ?: entries
    }

    Column {
        Row(modifier = Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = statusFilter == null, onClick = { onFilterChange(null) }, label = { Text("Todos") })
            FilterChip(selected = statusFilter == TicketStatus.SOLD, onClick = { onFilterChange(TicketStatus.SOLD) }, label = { Text("Pagados") })
            FilterChip(selected = statusFilter == TicketStatus.RESERVED, onClick = { onFilterChange(TicketStatus.RESERVED) }, label = { Text("Pendientes") })
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(filtered) { entry ->
                SaleEntryCardSaaS(
                    entry = entry,
                    onToggleStatus = { onToggleStatus(entry) },
                    onResend = { raffle?.let { WhatsAppSender.sendTextReceipt(context, it, entry.tickets) } },
                    onSendReminder = {
                        val msg = viewModel.getReminderMessage(entry)
                        WhatsAppSender.sendCustomMessage(context, entry.buyerPhone, msg)
                    },
                    onCancel = { onCancel(entry) },
                    onEditPhone = { onEditPhone(entry) }
                )
            }
        }
    }
}

@Composable
private fun DirectoryView(
    buyers: List<BuyerSummary>,
    raffle: com.example.lotteryapp.data.entity.Raffle?
) {
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CR")).apply {
        maximumFractionDigits = 0
    }
    
    if (buyers.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { 
            Text("Aún no tienes clientes registrados.", color = Color.Gray, textAlign = TextAlign.Center) 
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)) {
            items(buyers) { buyer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(buyer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${buyer.tickets.size} boletos totales", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        Text("Boletos: ${buyer.allNumbers.joinToString(", ")}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Inversión Total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(currencyFormat.format(buyer.totalPaid + buyer.totalPending), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (buyer.totalPending > 0) {
                                Button(
                                    onClick = {
                                        val msg = "Hola ${buyer.name}, tienes un saldo pendiente de ${currencyFormat.format(buyer.totalPending)} en la Rifa ${raffle?.name}. ¡Gracias!"
                                        WhatsAppSender.sendCustomMessage(context, buyer.phone, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cobrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                                    Text("AL DÍA", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = color)
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
    }
}

@Composable
private fun SaleEntryCardSaaS(
    entry: SaleEntry,
    onToggleStatus: () -> Unit,
    onResend: () -> Unit,
    onSendReminder: () -> Unit,
    onCancel: () -> Unit,
    onEditPhone: () -> Unit
) {
    val isSold = entry.status == TicketStatus.SOLD
    val hasPhone = !entry.buyerPhone.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = entry.buyerName ?: "Sin Nombre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "Boletos: ${entry.numbers.joinToString(", ")}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(
                    color = if (isSold) Color(0xFFE8F5E9) else Color(0xFFFFF3E0), 
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isSold) "PAGADO" else "PENDIENTE", 
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (isSold) Color(0xFF2E7D32) else Color(0xFFE65100), 
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (hasPhone) {
                    Button(
                        onClick = if (!isSold) onSendReminder else onResend, 
                        modifier = Modifier.weight(1f).height(38.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = if (!isSold) Color(0xFF25D366) else MaterialTheme.colorScheme.primary), 
                        shape = RoundedCornerShape(10.dp), 
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(if (!isSold) Icons.Default.NotificationsActive else Icons.Default.Share, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (!isSold) "Cobrar" else "Recibo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallActionButton(
                        icon = if (isSold) Icons.Default.SyncAlt else Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = if (isSold) Color.Gray else Color(0xFF2E7D32),
                        onClick = onToggleStatus
                    )
                    SmallActionButton(
                        icon = Icons.Default.Edit,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        onClick = onEditPhone
                    )
                    SmallActionButton(
                        icon = Icons.Default.DeleteOutline,
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error,
                        onClick = onCancel
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EditPhoneDialog(currentPhone: String?, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    var phone by remember { mutableStateOf(currentPhone ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actualizar Contacto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Puedes escribirlo o elegirlo desde tus contactos.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                PhoneFieldWithContacts(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "Ej: 88888888",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(phone.ifBlank { null }) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
