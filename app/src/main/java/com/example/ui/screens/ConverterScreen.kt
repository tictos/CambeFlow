package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Currency
import com.example.ui.components.ConversionSuccessDialog
import com.example.ui.components.CurrencyPickerModal
import com.example.ui.components.KeypadView
import com.example.ui.components.SingleFlagBadge
import com.example.ui.theme.*
import com.example.viewmodel.ConverterUiState
import com.example.viewmodel.TradeFlowViewModel
import java.util.Locale

@Composable
fun ConverterScreen(
    viewModel: TradeFlowViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.converterState.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val streamStatus by viewModel.asyncStreamStatus.collectAsState()
    val rates by viewModel.rates.collectAsState()

    var showSourcePicker by remember { mutableStateOf(false) }
    var showTargetPicker by remember { mutableStateOf(false) }

    val liveRate = remember(state.sourceCurrency, state.targetCurrency, rates) {
        viewModel.getLiveConversionRate(state.sourceCurrency.code, state.targetCurrency.code)
    }
    val inputAmount = state.inputAmountText.toDoubleOrNull() ?: 0.0
    val convertedAmount = inputAmount * liveRate

    var swapRotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation by animateFloatAsState(targetValue = swapRotation, label = "swap_anim")

    if (showSourcePicker) {
        CurrencyPickerModal(
            selectedCurrency = state.sourceCurrency,
            onCurrencySelected = { viewModel.setSourceCurrency(it) },
            onDismiss = { showSourcePicker = false }
        )
    }

    if (showTargetPicker) {
        CurrencyPickerModal(
            selectedCurrency = state.targetCurrency,
            onCurrencySelected = { viewModel.setTargetCurrency(it) },
            onDismiss = { showTargetPicker = false }
        )
    }

    if (state.showSuccessDialog && state.lastConvertedRecord != null) {
        ConversionSuccessDialog(
            record = state.lastConvertedRecord!!,
            onDismiss = { viewModel.dismissSuccessDialog() }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Ambient background glow effect
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .size(280.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Primary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Rate Info Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Taux interbancaire moyen",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "1 ${state.sourceCurrency.code} = ${String.format(Locale.US, "%.4f", liveRate)} ${state.targetCurrency.code}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Secondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isOffline) {
                            "Mode hors-ligne"
                        } else if (streamStatus.isStreaming) {
                            "Direct (Tick #${streamStatus.tickCount})"
                        } else {
                            "Mis à jour"
                        },
                        fontSize = 11.sp,
                        color = if (streamStatus.isStreaming && !isOffline) Color(0xFF00E676) else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Conversion Box Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Source Currency Input Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                Primary.copy(alpha = 0.35f),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceContainer.copy(alpha = 0.85f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top Row: Currency Picker + Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceContainerHigh.copy(alpha = 0.8f))
                                        .clickable { showSourcePicker = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("source_currency_selector"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SingleFlagBadge(currency = state.sourceCurrency, size = 26)
                                    Text(
                                        text = state.sourceCurrency.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = "Solde: $12,450.00",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Bottom Row: Symbol + Amount Text
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.sourceCurrency.symbol,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (state.inputAmountText.isEmpty()) "0" else state.inputAmountText,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    modifier = Modifier.testTag("source_amount_display")
                                )
                            }
                        }
                    }

                    // Target Currency Output Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceContainerHigh.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top Row: Currency Picker
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceContainerLow)
                                        .clickable { showTargetPicker = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("target_currency_selector"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SingleFlagBadge(currency = state.targetCurrency, size = 26)
                                    Text(
                                        text = state.targetCurrency.code,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Text(
                                    text = state.targetCurrency.frenchName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Bottom Row: Symbol + Converted Amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.targetCurrency.symbol,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = String.format(Locale.US, "%,.2f", convertedAmount),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Secondary,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                    modifier = Modifier.testTag("converted_amount_display")
                                )
                            }
                        }
                    }
                }

                // Central Floating Swap Button
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer)
                        .border(3.dp, DarkBackground, CircleShape)
                        .clickable {
                            swapRotation += 180f
                            viewModel.swapCurrencies()
                        }
                        .testTag("swap_currencies_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Inverser",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(animatedRotation)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tactile Keypad
            KeypadView(
                onDigitClick = { viewModel.onKeypadDigit(it) },
                onBackspaceClick = { viewModel.onKeypadBackspace() },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: Review Conversion
            Button(
                onClick = { viewModel.executeConversion() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .testTag("review_conversion_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Effectuer la Conversion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
