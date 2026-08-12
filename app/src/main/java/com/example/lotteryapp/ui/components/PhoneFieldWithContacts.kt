package com.example.lotteryapp.ui.components

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PhoneFieldWithContacts(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "WhatsApp (8 dígitos)",
    placeholder: String = "Ej: 88888888",
    isError: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickingContact by remember { mutableStateOf(false) }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pickingContact = false
        if (result.resultCode == Activity.RESULT_OK && result.data?.data != null) {
            val uri = result.data?.data
            scope.launch {
                try {
                    context.contentResolver.query(
                        uri!!,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val raw = cursor.getString(0) ?: return@use
                            onValueChange(raw.filter { it.isDigit() }.takeLast(8))
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }.take(8)) },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        trailingIcon = {
            IconButton(onClick = {
                pickingContact = true
                contactsLauncher.launch(
                    Intent(
                        Intent.ACTION_PICK,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                    )
                )
            }) {
                Icon(
                    Icons.Filled.Contacts,
                    contentDescription = "Elegir de contactos",
                    tint = if (pickingContact) androidx.compose.ui.graphics.Color.Gray else androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    )
}