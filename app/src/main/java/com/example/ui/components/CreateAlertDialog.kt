package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertDirection
import com.example.model.Currency
import com.example.model.CurrencyCatalog
import com.example.ui.theme.Primary
import com.example.ui.theme.Secondary
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.Tertiary

@Composable
fun CreateAlertDialog(
    initialBase: Currency = CurrencyCatalog.find("EUR"),
    initialTarget: Currency = CurrencyCatalog.find("USD"),
    currentRate: Double = 1.0942,
    onDismiss: () -> Unit,
    onCreateAlert: (baseCode: String, targetCode: String, targetRate: Double, direction: AlertDirection, note: String) -> Unit
) {
    var baseCurrency by remember { mutableStateOf(initialBase) }
    var targetCurrency by remember { mutableStateOf(initialTarget) }
    var targetRateText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.4f", currentRate * 1.01)) }
    var direction by remember { mutableStateOf(AlertDirection.ABOVE) }
    var noteText by remember { mutableStateOf("") }

    var showBasePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    if (showBasePicker) {
        CurrencyPickerModal(
            selectedCurrency = baseCurrency,
            onCurrencySelected = { baseCurrency = it },
            onDismiss = { showBasePicker = false }
        )
    }

    if (showTargetPicker) {
        CurrencyPickerModal(
            selectedCurrency = targetCurrency,
            onCurrencySelected = { targetCurrency = it },
            onDismiss = { showTargetPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddAlert,
                    contentDescription = null,
                    tint = Primary
                )
                Text(
                    text = "Créer une Alerte de Prix",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Currency Pair Selector
                Text(
                    text = "Paire de devises",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showBasePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("${baseCurrency.flag} ${baseCurrency.code}")
                    }

                    Text("➔", fontWeight = FontWeight.Bold, color = Primary)

                    OutlinedButton(
                        onClick = { showTargetPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("${targetCurrency.flag} ${targetCurrency.code}")
                    }
                }

                // Direction: Above or Below
                Text(
                    text = "Condition de déclenchement",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = direction == AlertDirection.ABOVE,
                        onClick = { direction = AlertDirection.ABOVE },
                        label = { Text("▲ À la hausse", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Secondary.copy(alpha = 0.2f),
                            selectedLabelColor = Secondary,
                            containerColor = SurfaceContainerHigh
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = direction == AlertDirection.BELOW,
                        onClick = { direction = AlertDirection.BELOW },
                        label = { Text("▼ À la baisse", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Tertiary.copy(alpha = 0.2f),
                            selectedLabelColor = Tertiary,
                            containerColor = SurfaceContainerHigh
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Target Rate Input
                OutlinedTextField(
                    value = targetRateText,
                    onValueChange = { targetRateText = it },
                    label = { Text("Taux cible") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_target_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Note / Label
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optionnel)") },
                    placeholder = { Text("Ex: Objectif de vente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = targetRateText.toDoubleOrNull()
                    if (rate != null && rate > 0) {
                        onCreateAlert(baseCurrency.code, targetCurrency.code, rate, direction, noteText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.testTag("submit_alert_button")
            ) {
                Text("Activer l'alerte", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
