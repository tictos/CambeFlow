package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.example.model.CurrencyCatalog
import com.example.ui.components.CreateAlertDialog
import com.example.ui.components.CurrencyPickerModal
import com.example.ui.components.DualFlagPill
import com.example.ui.components.InteractiveChart
import com.example.ui.theme.*
import com.example.viewmodel.TradeFlowViewModel
import java.util.Locale

@Composable
fun TrendsScreen(
    viewModel: TradeFlowViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPair by viewModel.selectedPair.collectAsState()
    val timeFrame by viewModel.selectedTimeFrame.collectAsState()
    val isCandlestick by viewModel.isCandlestickMode.collectAsState()
    val isChartComputing by viewModel.isChartComputing.collectAsState()
    val chartPoints by viewModel.chartPoints.collectAsState()
    val candleData by viewModel.candleStickData.collectAsState()

    var showCreateAlertModal by remember { mutableStateOf(false) }
    var showPairSelectorModal by remember { mutableStateOf(false) }

    if (showCreateAlertModal) {
        CreateAlertDialog(
            initialBase = selectedPair.baseCurrency,
            initialTarget = selectedPair.targetCurrency,
            currentRate = selectedPair.rate,
            onDismiss = { showCreateAlertModal = false },
            onCreateAlert = { base, target, rate, dir, note ->
                viewModel.createAlert(base, target, rate, dir, note)
                showCreateAlertModal = false
            }
        )
    }

    if (showPairSelectorModal) {
        CurrencyPickerModal(
            selectedCurrency = selectedPair.baseCurrency,
            onCurrencySelected = { newBase ->
                val newPair = selectedPair.copy(
                    baseCurrency = newBase,
                    rate = viewModel.getLiveConversionRate(newBase.code, selectedPair.targetCurrency.code)
                )
                viewModel.selectPairForTrends(newPair)
            },
            onDismiss = { showPairSelectorModal = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header Section: Pair Title + Dual Flags + Price & Change + Alert Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DualFlagPill(
                        baseCurrency = selectedPair.baseCurrency,
                        targetCurrency = selectedPair.targetCurrency
                    )
                    Text(
                        text = selectedPair.symbol,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedPair.displayName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "%.4f", selectedPair.rate),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val isPositive = selectedPair.change24h >= 0
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", selectedPair.change24h)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositive) Secondary else Tertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPositive) Secondary.copy(alpha = 0.15f) else Tertiary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Alert Button
                IconButton(
                    onClick = { showCreateAlertModal = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        .testTag("trend_add_alert_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAlert,
                        contentDescription = "Ajouter alerte",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Interactive Chart Component
        InteractiveChart(
            points = chartPoints,
            candles = candleData,
            selectedTimeFrame = timeFrame,
            onTimeFrameSelected = { viewModel.setTimeFrame(it) },
            isCandlestickMode = isCandlestick,
            onToggleCandlestick = { viewModel.toggleCandlestickMode(it) },
            currentRate = selectedPair.rate,
            isPositive = selectedPair.isPositive,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Period Statistics Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceContainerHigh.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Statistiques de la Période (${timeFrame.periodName})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Plus Haut (High)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.US, "%.4f", selectedPair.high24h),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Plus Bas (Low)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.US, "%.4f", selectedPair.low24h),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ouverture (Open)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format(Locale.US, "%.4f", selectedPair.open24h),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volatilité Marché", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = selectedPair.volatility,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPair.isPositive) Secondary else Tertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action CTA Card: Trade this Pair
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceContainer.copy(alpha = 0.8f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapCalls,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Convertir ${selectedPair.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Exécutez une conversion instantanée au taux interbancaire.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                Button(
                    onClick = { viewModel.selectPairForConverter(selectedPair) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("trend_trade_cta_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Échanger ${selectedPair.baseCurrency.code} ➔ ${selectedPair.targetCurrency.code}",
                        fontWeight = FontWeight.Bold,
                        color = OnPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
