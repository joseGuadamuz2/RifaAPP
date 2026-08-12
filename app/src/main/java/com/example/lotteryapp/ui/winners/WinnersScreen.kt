package com.example.lotteryapp.ui.winners

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.TicketStatus
import com.example.lotteryapp.data.entity.Winner
import com.example.lotteryapp.ui.components.PhoneFieldWithContacts
import com.example.lotteryapp.ui.raffle.SaleEntry
import com.example.lotteryapp.ui.theme.WhatsAppGreen
import com.example.lotteryapp.util.WhatsAppSender
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinnersScreen(
    viewModel: WinnersViewModel,
    onBack: () -> Unit
) {
    val winners by viewModel.winners.collectAsState()
    val saleEntries by viewModel.saleEntries.collectAsState()
    val raffle by viewModel.raffle.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val canRegisterWinner = raffle?.let {
        startOfDayMillis(it.drawDate) <= startOfDayMillis(System.currentTimeMillis())
    } ?: false

    var showRegisterDialog by remember { mutableStateOf(false) }
    var winnerToEdit by remember { mutableStateOf<Winner?>(null) }
    var winnerToDelete by remember { mutableStateOf<Winner?>(null) }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                title = { Text("Ganadores", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (canRegisterWinner) {
                        showRegisterDialog = true
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "El registro de ganadores se habilita a partir del día del sorteo."
                            )
                        }
                    }
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(if (canRegisterWinner) "Registrar ganador" else "El día del sorteo") },
                modifier = Modifier.alpha(if (canRegisterWinner) 1f else 0.55f)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            raffle?.let {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "🏆 ${it.prizeName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!canRegisterWinner) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "El registro de ganadores se habilita a partir del día del sorteo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (winners.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Aún no hay ganadores",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Registrá al ganador del sorteo cuando realicés el sorteo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(winners, key = { it.id }) { winner ->
                        WinnerCard(
                            winner = winner,
                            onNotify = {
                                raffle?.let { currentRaffle ->
                                    val launched = WhatsAppSender.sendWinnerNotification(context, currentRaffle, winner)
                                    if (launched) {
                                        viewModel.markNotified(winner)
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar("WhatsApp no está instalado") }
                                    }
                                }
                            },
                            onEdit = { winnerToEdit = winner },
                            onDelete = { winnerToDelete = winner }
                        )
                    }
                }
            }
        }
    }

    if (showRegisterDialog) {
        RegisterWinnerDialog(
            winner = null,
            raffle = raffle,
            soldEntries = saleEntries,
            onDismiss = { showRegisterDialog = false },
            onConfirm = { number, name, phone, prize ->
                viewModel.registerWinner(number, name, phone, prize)
                showRegisterDialog = false
            }
        )
    }

    winnerToEdit?.let { winner ->
        RegisterWinnerDialog(
            winner = winner,
            raffle = raffle,
            soldEntries = saleEntries,
            onDismiss = { winnerToEdit = null },
            onConfirm = { number, name, phone, prize ->
                viewModel.updateWinner(
                    winner.copy(
                        winningNumber = number,
                        buyerName = name,
                        buyerPhone = phone,
                        prizeName = prize
                    )
                )
                winnerToEdit = null
            }
        )
    }

    winnerToDelete?.let { winner ->
        AlertDialog(
            onDismissRequest = { winnerToDelete = null },
            title = { Text("Eliminar ganador", color = MaterialTheme.colorScheme.error) },
            text = { Text("¿Seguro que querés eliminar a ${winner.buyerName} con el número ${winner.winningNumber}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWinner(winner)
                        winnerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { winnerToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun WinnerCard(
    winner: Winner,
    onNotify: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR")) }
    val dateStr = dateFormat.format(Date(winner.registeredAt))
    val hasPhone = !winner.buyerPhone.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFFFF8E1),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = winner.winningNumber,
                                color = Color(0xFFB26A00),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = winner.buyerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Número ganador: ${winner.winningNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Surface(
                    color = if (winner.notified) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (winner.notified) "NOTIFICADO" else "PENDIENTE",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (winner.notified) Color(0xFF2E7D32) else Color(0xFF757575),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (hasPhone) {
                        WinnerDetailRow(Icons.Filled.Phone, winner.buyerPhone.orEmpty())
                    }
                    WinnerDetailRow(
                        Icons.Filled.Person,
                        buildString {
                            append("Premio: ")
                            append(winner.prizeName ?: "Sin premio")
                            winner.prizeAmount?.let { append(" · ₡${it.toInt()}") }
                        }
                    )
                    WinnerDetailRow(Icons.Filled.EmojiEvents, "Registrado: $dateStr")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (hasPhone) {
                        Button(
                            onClick = onNotify,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Surface(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WinnerDetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(13.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RegisterWinnerDialog(
    winner: Winner?,
    raffle: Raffle?,
    soldEntries: List<SaleEntry>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String?) -> Unit
) {
    var searchQuery by remember(winner) { mutableStateOf("") }
    var number by remember(winner) { mutableStateOf(winner?.winningNumber ?: "") }
    var buyerName by remember(winner) { mutableStateOf(winner?.buyerName ?: "") }
    var buyerPhone by remember(winner) { mutableStateOf(winner?.buyerPhone ?: "") }
    var prize by remember(winner, raffle) { mutableStateOf(winner?.prizeName ?: raffle?.prizeName ?: "") }

    val numberParts = number.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val numberValid = numberParts.isNotEmpty() && numberParts.all { it.length == 2 && it.all(Char::isDigit) }
    val nameValid = buyerName.isNotBlank()
    val phoneValid = buyerPhone.isEmpty() || buyerPhone.length == 8
    val formValid = numberValid && nameValid && phoneValid

    val filtered = remember(searchQuery, soldEntries) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.trim().lowercase()
            soldEntries.filter { entry ->
                entry.buyerName?.lowercase()?.contains(q) == true ||
                        entry.numbers.any { it.contains(q) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (winner == null) "Registrar ganador" else "Editar ganador", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Buscá el boleto vendido por número o nombre del participante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar boleto vendido...") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (searchQuery.isNotBlank()) {
                    if (filtered.isEmpty()) {
                        Text(
                            text = "Sin coincidencias en las ventas. Completá los datos manualmente para registrarlo como no vendido.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Seleccioná una venta:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filtered, key = { it.groupId ?: it.tickets.first().id }) { entry ->
                                Surface(
                                    onClick = {
                                        number = entry.numbers.joinToString(", ")
                                        buyerName = entry.buyerName ?: ""
                                        buyerPhone = entry.buyerPhone ?: ""
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.buyerName ?: "Sin nombre",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Números: ${entry.numbers.joinToString(", ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = if (entry.status == TicketStatus.SOLD) "PAGADO" else "APARTADO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (entry.status == TicketStatus.SOLD) Color(0xFF2E7D32) else Color(0xFFE65100)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                OutlinedTextField(
                    value = number,
                    onValueChange = { input ->
                        number = input.filter { it.isDigit() || it == ',' || it.isWhitespace() }.take(30)
                    },
                    label = { Text("Número ganador") },
                    placeholder = { Text("Ej: 07 o 05, 06") },
                    isError = number.isNotEmpty() && !numberValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = buyerName,
                    onValueChange = { if (it.all { c -> c.isLetterOrDigit() || c.isWhitespace() }) buyerName = it },
                    label = { Text("Nombre del ganador") },
                    isError = buyerName.isNotEmpty() && !nameValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                PhoneFieldWithContacts(
                    value = buyerPhone,
                    onValueChange = { buyerPhone = it },
                    isError = !phoneValid,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prize,
                    onValueChange = { prize = it },
                    label = { Text("Premio") },
                    placeholder = { Text(raffle?.prizeName ?: "Nombre del premio") },
                    supportingText = {
                        Text(
                            text = buildString {
                                append("Se completa con el premio de la rifa")
                                raffle?.prizeValue?.let { append(" · ₡${it.toInt()}") }
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        number.trim(),
                        buyerName,
                        buyerPhone.ifBlank { null },
                        prize.trim().ifBlank { null }
                    )
                },
                enabled = formValid
            ) {
                Text(if (winner == null) "Guardar" else "Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun startOfDayMillis(millis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}