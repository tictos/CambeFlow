package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Currency
import com.example.model.CurrencyCatalog
import com.example.ui.theme.Primary
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceContainerLow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerModal(
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val popularCodes = listOf("USD", "EUR", "GBP", "JPY", "CAD", "CHF", "AUD", "MAD", "XOF")

    val filteredCurrencies = remember(searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            CurrencyCatalog.currencies
        } else {
            CurrencyCatalog.currencies.filter {
                it.code.lowercase().contains(query) ||
                        it.name.lowercase().contains(query) ||
                        it.frenchName.lowercase().contains(query) ||
                        it.symbol.lowercase().contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sélectionner une devise",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Rechercher (ex. EUR, Dollar, Franc...)", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("currency_search_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceContainerHigh,
                    unfocusedContainerColor = SurfaceContainerHigh,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Popular Chips
            Text(
                text = "Devises populaires",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(popularCodes) { code ->
                    val currency = CurrencyCatalog.find(code)
                    val isCurrent = currency.code == selectedCurrency.code

                    FilterChip(
                        selected = isCurrent,
                        onClick = {
                            onCurrencySelected(currency)
                            onDismiss()
                        },
                        label = {
                            Text("${currency.flag} ${currency.code}", fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = SurfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // All Currencies List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCurrencies) { currency ->
                    val isCurrent = currency.code == selectedCurrency.code

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) SurfaceContainerHigh else Color.Transparent)
                            .clickable {
                                onCurrencySelected(currency)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("currency_item_${currency.code}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SingleFlagBadge(currency = currency, size = 36)
                            Column {
                                Text(
                                    text = "${currency.code} (${currency.symbol})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isCurrent) Primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currency.frenchName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isCurrent) {
                            Text(
                                text = "Actuel",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }
        }
    }
}
