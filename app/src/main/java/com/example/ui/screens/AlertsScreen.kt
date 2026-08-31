package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import com.example.model.AlertDirection
import com.example.model.CurrencyCatalog
import com.example.model.PriceAlert
import com.example.ui.components.CreateAlertDialog
import com.example.ui.theme.*
import com.example.viewmodel.TradeFlowViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    viewModel: TradeFlowViewModel,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val showCreateDialog by viewModel.showCreateAlertDialog.collectAsState()

    val activeAlerts = alerts.filter { it.isEnabled }
    val triggeredAlerts = alerts.filter { it.isTriggered }

    if (showCreateDialog) {
        CreateAlertDialog(
            onDismiss = { viewModel.closeCreateAlertDialog() },
            onCreateAlert = { base, target, rate, dir, note ->
                viewModel.createAlert(base, target, rate, dir, note)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
        ) {
            // Header: Active Alerts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alertes Actives",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${activeAlerts.size} Actives",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }
            }

            if (alerts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAlert,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Aucune alerte configurée",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Créez une alerte pour être notifié lorsque le taux atteint votre cible.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(alerts) { alert ->
                    val liveRate = viewModel.getLiveConversionRate(alert.baseCode, alert.targetCode)
                    AlertCard(
                        alert = alert,
                        liveRate = liveRate,
                        onToggle = { viewModel.toggleAlert(alert.id, it) },
                        onDelete = { viewModel.deleteAlert(alert.id) }
                    )
                }
            }

            // Triggered Alerts / Derniers Déclenchements
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Derniers Déclenchements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TriggeredAlertItem(
                            pair = "USD/CAD a atteint 1.3500",
                            dateText = "Hier, 14:30"
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        TriggeredAlertItem(
                            pair = "EUR/GBP a atteint 0.8550",
                            dateText = "12 Oct, 09:15"
                        )
                    }
                }
            }
        }

        // Floating Action Button to Create Alert
        FloatingActionButton(
            onClick = { viewModel.openCreateAlertDialog() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .testTag("fab_create_alert"),
            containerColor = Primary,
            contentColor = OnPrimary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddAlert,
                    contentDescription = "Créer une Alerte"
                )
                Text(
                    text = "Créer une Alerte",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: PriceAlert,
    liveRate: Double,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isAbove = alert.direction == AlertDirection.ABOVE
    val targetColor = if (isAbove) Secondary else Tertiary
    val progress = alert.calculateProgress(liveRate)
    val opacity = if (alert.isEnabled) 1.0f else 0.55f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .testTag("alert_card_${alert.pairSymbol.replace('/', '_')}"),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerHigh.copy(alpha = 0.6f * opacity)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Base -> Target badges + Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = alert.baseCode,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = alert.targetCode,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (alert.note.isNotEmpty()) {
                        Text(
                            text = alert.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = alert.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = SurfaceContainerHighest
                        ),
                        modifier = Modifier.testTag("alert_switch_${alert.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle Row: Target vs Current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isAbove) "Cible à la hausse" else "Cible à la baisse",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.4f", alert.targetRate),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = targetColor
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Actuel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.4f", liveRate),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar towards target
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerLowest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .clip(CircleShape)
                        .background(if (alert.isEnabled) targetColor else MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Composable
private fun TriggeredAlertItem(
    pair: String,
    dateText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Secondary,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(
                text = pair,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dateText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
