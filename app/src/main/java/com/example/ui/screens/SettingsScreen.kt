package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.viewmodel.TradeFlowViewModel

@Composable
fun SettingsScreen(
    viewModel: TradeFlowViewModel,
    modifier: Modifier = Modifier
) {
    val streamStatus by viewModel.asyncStreamStatus.collectAsState()
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }
    var showOfflineModeBanner by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Paramètres & Préférences",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Profile / Status Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_cambeflow_logo_1788212572616),
                    contentDescription = "Cambe Flow Icon",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Primary, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Column {
                    Text(
                        text = "Cambe Flow Pro",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Moteur Réactif & Asynchrone (Coroutines)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Moteur Asynchrone & Données en Temps Réel
        Text(
            text = "MOTEUR ASYNCHRONE & TEMPS RÉEL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchRow(
                    icon = Icons.Default.Bolt,
                    title = "Flux en Direct (Asynchrone)",
                    subtitle = "Micro-fluctuations temps réel via Flow",
                    checked = streamStatus.isStreaming,
                    onCheckedChange = { viewModel.toggleLiveStream(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                // Auto-sync interval selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intervalle de synchronisation API",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (streamStatus.autoSyncIntervalSec > 0) "${streamStatus.autoSyncIntervalSec}s" else "Désactivé",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 15, 30, 0).forEach { sec ->
                            val label = if (sec == 0) "Off" else "${sec}s"
                            val isSelected = streamStatus.autoSyncIntervalSec == sec
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAutoSyncInterval(sec) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = OnPrimary,
                                    containerColor = SurfaceContainerLow,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SettingsActionRow(
                    icon = Icons.Default.CloudSync,
                    title = "Forcer l'actualisation asynchrone",
                    subtitle = "Exécuter la requête sur Dispatchers.IO",
                    onClick = { viewModel.refreshRates() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Diagnostics Card
        Text(
            text = "DIAGNOSTICS & PERFORMANCES ASYNCHRONES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainer.copy(alpha = 0.85f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Latence mesurée :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${streamStatus.latencyMs} ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Ticks asynchrones générés :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("#${streamStatus.tickCount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Architecture d'exécution :", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Coroutines & StateFlow", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Interface & Retour Haptique
        Text(
            text = "INTERFACE & EXPÉRIENCE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsSwitchRow(
                    icon = Icons.Default.TouchApp,
                    title = "Vibrations clavier",
                    subtitle = "Retour tactile lors de la saisie numérique",
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = { hapticFeedbackEnabled = it }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                SettingsSwitchRow(
                    icon = Icons.Default.WifiOff,
                    title = "Mode Hors-Ligne automatique",
                    subtitle = "Utilise les taux mis en cache en absence de réseau",
                    checked = showOfflineModeBanner,
                    onCheckedChange = { showOfflineModeBanner = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
