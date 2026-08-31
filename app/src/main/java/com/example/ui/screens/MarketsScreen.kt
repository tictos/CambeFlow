package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Refresh
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
import com.example.model.MarketPair
import com.example.ui.components.DualFlagPill
import com.example.ui.components.SparklineView
import com.example.ui.theme.*
import com.example.viewmodel.TradeFlowViewModel
import java.util.Locale

@Composable
fun MarketsScreen(
    viewModel: TradeFlowViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.marketSearchQuery.collectAsState()
    val filterCategory by viewModel.marketFilterCategory.collectAsState()
    val isLoading by viewModel.isLoadingRates.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val streamStatus by viewModel.asyncStreamStatus.collectAsState()

    val marketPairs by viewModel.filteredMarketPairs.collectAsState()
    val categories = listOf("Tous", "Majeures", "Forex", "Favoris")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Asynchronous Stream Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (streamStatus.isStreaming) Color(0xFF00E676) else Color(0xFFFFA000))
                    )
                    Text(
                        text = if (streamStatus.isStreaming) "Flux Asynchrone Actif (${streamStatus.latencyMs}ms)" else "Flux Asynchrone en Pause",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tick #${streamStatus.tickCount}",
                        fontSize = 11.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setMarketSearchQuery(it) },
            placeholder = {
                Text(
                    text = "Rechercher une paire (ex. EUR/USD)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Recherche",
                    tint = Primary
                )
            },
            trailingIcon = {
                IconButton(onClick = { viewModel.refreshRates() }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Rafraîchir",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .testTag("market_search_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainerHigh,
                unfocusedContainerColor = SurfaceContainerHigh,
                focusedBorderColor = Primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = filterCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setMarketFilterCategory(category) },
                    label = { Text(category, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = OnPrimary,
                        containerColor = SurfaceContainerLow,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("market_chip_${category.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pairs List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (marketPairs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune paire trouvée pour \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(marketPairs) { pair ->
                    MarketPairCard(
                        pair = pair,
                        onClick = { viewModel.selectPairForTrends(pair) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketPairCard(
    pair: MarketPair,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .testTag("pair_card_${pair.baseCurrency.code}_${pair.targetCurrency.code}"),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerHigh.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Flag avatars + Symbol + Percentage badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DualFlagPill(
                        baseCurrency = pair.baseCurrency,
                        targetCurrency = pair.targetCurrency
                    )

                    Column {
                        Text(
                            text = pair.symbol,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = pair.displayName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Trend percentage pill
                val isPositive = pair.change24h >= 0
                val badgeBg = if (isPositive) Secondary.copy(alpha = 0.18f) else Tertiary.copy(alpha = 0.18f)
                val badgeColor = if (isPositive) Secondary else Tertiary

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", kotlin.math.abs(pair.change24h))}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom row: Current Rate + Sparkline Graph
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = String.format(Locale.US, "%.4f", pair.rate),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                SparklineView(
                    data = pair.sparkline,
                    isPositive = pair.isPositive
                )
            }
        }
    }
}
