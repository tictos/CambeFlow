package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConversionRecord
import com.example.model.CurrencyCatalog
import com.example.ui.theme.*
import com.example.viewmodel.TradeFlowViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: TradeFlowViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.history.collectAsState()
    val filter by viewModel.historyFilter.collectAsState()

    val filteredRecords = viewModel.getFilteredHistory(history)
    val filters = listOf("7 derniers jours", "Mois dernier", "3 derniers mois", "Tout")

    val totalVolumeUSD = filteredRecords.sumOf { record ->
        val fromRate = CurrencyCatalog.find(record.fromCode).defaultRateToUSD
        if (fromRate > 0) record.fromAmount / fromRate else record.fromAmount
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Title and Clear Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historique des Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (history.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearAllHistory() },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Effacer l'historique",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { item ->
                val isSelected = filter == item
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setHistoryFilter(item) },
                    label = { Text(item, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary,
                        containerColor = SurfaceContainerLow,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("history_chip_${item.take(3).lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Volume Summary Stats Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Volume ($filter)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%,.0f", totalVolumeUSD)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text(
                        text = "+12.4%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Secondary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Total Transactions:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${filteredRecords.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Frais Moyens:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("0.00%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Secondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History Records List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (filteredRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune transaction pour cette période",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredRecords) { record ->
                    HistoryRecordCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordCard(
    record: ConversionRecord
) {
    val fromCurr = CurrencyCatalog.find(record.fromCode)
    val toCurr = CurrencyCatalog.find(record.toCode)
    val dateFormat = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.FRENCH)
    val isPending = record.status.contains("attente", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .testTag("history_item_${record.id}"),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerHigh.copy(alpha = if (isPending) 0.4f else 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Pair + Status + Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHighest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPending) Icons.Default.Schedule else Icons.Default.CurrencyExchange,
                        contentDescription = null,
                        tint = if (isPending) MaterialTheme.colorScheme.outline else Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = record.pairText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = record.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPending) MaterialTheme.colorScheme.outline else Secondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isPending) SurfaceContainerLowest else Secondary.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = dateFormat.format(Date(record.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Right: Amounts
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "- ${fromCurr.symbol}${String.format(Locale.US, "%,.2f", record.fromAmount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "+ ${toCurr.symbol}${String.format(Locale.US, "%,.2f", record.toAmount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Secondary
                    )
                }
            }
        }
    }
}
