package com.example.lotteryapp.ui.raffle

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.RaffleSource
import com.example.lotteryapp.util.ImageSharingHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRaffleScreen(
    viewModel: CreateRaffleViewModel,
    onBack: () -> Unit,
    onRaffleSaved: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-CR")) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var modalityMenuExpanded by remember { mutableStateOf(false) }
    var groupSizeMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val persistedPath = ImageSharingHelper.persistPickedImage(context, it)
            if (persistedPath != null) viewModel.onPrizePhotoChange(persistedPath)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva rifa", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. COMPONENTE DE IMAGEN DINÁMICO
            Card(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (uiState.prizePhotoPath != null) {
                        AsyncImage(
                            model = uiState.prizePhotoPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = CircleShape,
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp).size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val stroke = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        val outlineColor = MaterialTheme.colorScheme.outlineVariant
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(color = outlineColor, style = stroke)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Agregar imagen (opcional)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. CAMPOS DEL FORMULARIO
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nombre de la rifa") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = uiState.prizeName,
                onValueChange = viewModel::onPrizeNameChange,
                label = { Text("Nombre del premio") },
                leadingIcon = { Icon(Icons.Filled.EmojiEvents, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            OutlinedTextField(
                value = uiState.ticketPrice,
                onValueChange = viewModel::onTicketPriceChange,
                label = { Text(if (uiState.modality == RaffleModality.GROUPS) "Precio por grupo" else "Precio por boleto") },
                prefix = { Text("₡ ") },
                leadingIcon = { Icon(Icons.Filled.Payments, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            // CAMPO DE FECHA REDISEÑADO
            Box {
                OutlinedTextField(
                    value = uiState.drawDate?.let { dateFormat.format(Date(it)) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha del sorteo") },
                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showDatePicker = true }
                )
            }

            // TIPO DE SORTEO
            ExposedDropdownMenuBox(
                expanded = sourceMenuExpanded,
                onExpandedChange = { sourceMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = when(uiState.source) {
                        RaffleSource.LOTERIA_NACIONAL -> "Lotería Nacional"
                        RaffleSource.CHANCES -> "Chances"
                        else -> "Otro"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de sorteo") },
                    leadingIcon = { Icon(Icons.Filled.Casino, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = sourceMenuExpanded,
                    onDismissRequest = { sourceMenuExpanded = false }
                ) {
                    RaffleSource.entries.filter { it != RaffleSource.OTRO }.forEach { source ->
                        DropdownMenuItem(
                            text = { Text(source.name.replace("_", " ")) },
                            onClick = {
                                viewModel.onSourceChange(source)
                                sourceMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MODALIDAD DE LA RIFA
            ExposedDropdownMenuBox(
                expanded = modalityMenuExpanded,
                onExpandedChange = { modalityMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (uiState.modality == RaffleModality.GROUPS) "Por grupos" else "Sencilla",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Modalidad") },
                    leadingIcon = { Icon(Icons.Filled.Groups, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modalityMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(
                    expanded = modalityMenuExpanded,
                    onDismissRequest = { modalityMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sencilla") },
                        onClick = {
                            viewModel.onModalityChange(RaffleModality.SENCILLA)
                            modalityMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Por grupos") },
                        onClick = {
                            viewModel.onModalityChange(RaffleModality.GROUPS)
                            modalityMenuExpanded = false
                        }
                    )
                }
            }

            if (uiState.modality == RaffleModality.GROUPS) {
                // TAMAÑO DEL GRUPO
                ExposedDropdownMenuBox(
                    expanded = groupSizeMenuExpanded,
                    onExpandedChange = { groupSizeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${uiState.groupSize} números",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tamaño del grupo") },
                        leadingIcon = { Icon(Icons.Filled.GridView, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupSizeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = groupSizeMenuExpanded,
                        onDismissRequest = { groupSizeMenuExpanded = false }
                    ) {
                        GROUP_SIZE_OPTIONS.forEach { size ->
                            DropdownMenuItem(
                                text = { Text("${size} número${if (size > 1) "s" else ""} · ${100 / size} grupos") },
                                onClick = {
                                    viewModel.onGroupSizeChange(size)
                                    groupSizeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = "Se crearán ${uiState.groupCount} grupos aleatorios de ${uiState.groupSize} números cada uno (100 boletos en total).",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. BOTÓN PRINCIPAL
            Button(
                onClick = viewModel::saveRaffle,
                enabled = uiState.isFormValid && !uiState.isSaving,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("CREAR RIFA", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.drawDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDrawDateChange(it) }
                    showDatePicker = false
                }) { Text("Confirmar") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    LaunchedEffect(uiState.saveSuccess) {
        val raffleId = uiState.createdRaffleId
        if (uiState.saveSuccess && raffleId != null) {
            onRaffleSaved(raffleId)
        }
    }
}
