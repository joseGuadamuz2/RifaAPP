package com.example.lotteryapp.ui.raffle

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryapp.data.entity.TicketStatus
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
    val totalRecaudado = entries.filter { it.status == TicketStatus.SOLD }.sumOf { it.tickets.size * (raffle?.ticketPrice ?: 0.0) }
    val boletosVendidos = entries.filter { it.status == TicketStatus.SOLD }.sumOf { it.tickets.size }
    val boletosPendientes = entries.filter { it.status == TicketStatus.RESERVED }.sumOf { it.tickets.size }
    val montoPendiente = entries.filter { it.status == TicketStatus.RESERVED }.sumOf { it.tickets.size * (raffle?.ticketPrice ?: 0.0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administrador SaaS", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
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
                                appendLine("   Numbers: ${buyer.allNumbers.joinToString(", ")}")
                                if (buyer.totalPending > 0) appendLine("   Debe: ${currencyFormat.format(buyer.totalPending)}")
                            }
                            appendLine("\n_Generado por LotteryApp_ 🎟️")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, report)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir Reporte"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // DASHBOARD SaaS MEJORADO
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryStat(label = "Recaudado", value = currencyFormat.format(totalRecaudado), icon = Icons.Default.MonetizationOn, color = Color(0xFF2E7D32))
                    SummaryStat(label = "Por Cobrar", value = currencyFormat.format(montoPendiente), icon = Icons.Default.HourglassBottom, color = Color(0xFFD32F2F))
                    SummaryStat(label = "Venta", value = "$boletosVendidos/100", icon = Icons.Default.ConfirmationNumber, color = MaterialTheme.colorScheme.primary)
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Ventas", fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Directorio", fontWeight = FontWeight.Bold) })
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
            Text("Aún no tienes clientes registrados.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center) 
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)) {
            items(buyers) { buyer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(buyer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${buyer.tickets.size} boletos totales", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        Text("Boletos: ${buyer.allNumbers.joinToString(", ")}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                        
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Inversión Total", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(currencyFormat.format(buyer.totalPaid + buyer.totalPending), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            }
                            if (buyer.totalPending > 0) {
                                Button(
                                    onClick = {
                                        val msg = "Hola ${buyer.name}, tienes un saldo pendiente de ${currencyFormat.format(buyer.totalPending)} en la Rifa ${raffle?.name}. ¡Gracias!"
                                        WhatsAppSender.sendCustomMessage(context, buyer.phone, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cobrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                AssistChip(onClick = {}, label = { Text("AL DÍA", fontWeight = FontWeight.Bold) }, leadingIcon = { Icon(Icons.Default.Verified, null, Modifier.size(18.dp), Color(0xFF2E7D32)) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = color)
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = entry.buyerName ?: "Sin Nombre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Boletos: ${entry.numbers.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                }
                Surface(
                    color = if (isSold) Color(0xFFE8F5E9) else Color(0xFFFFF3E0), 
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSold) "PAGADO" else "PENDIENTE", 
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (isSold) Color(0xFF2E7D32) else Color(0xFFE65100), 
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isSold && hasPhone) {
                    Button(
                        onClick = onSendReminder, 
                        modifier = Modifier.weight(1.5f), 
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), 
                        shape = RoundedCornerShape(12.dp), 
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cobrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (isSold && hasPhone) {
                    FilledTonalButton(
                        onClick = onResend, 
                        modifier = Modifier.weight(1.5f), 
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Recibo", fontSize = 11.sp)
                    }
                }

                IconButton(
                    onClick = onToggleStatus, 
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(if (isSold) Icons.Default.SyncAlt else Icons.Default.CheckCircle, null, tint = if (isSold) Color.Gray else Color(0xFF2E7D32))
                }
                IconButton(
                    onClick = onEditPhone, 
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onCancel, 
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                }
            }
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
            OutlinedTextField(
                value = phone, 
                onValueChange = { if (it.length <= 8) phone = it.filter { c -> c.isDigit() } }, 
                label = { Text("WhatsApp (8 dígitos)") }, 
                placeholder = { Text("Ej: 88888888") }, 
                modifier = Modifier.fillMaxWidth(), 
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
            )
        },
        confirmButton = { Button(onClick = { onConfirm(phone.ifBlank { null }) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
