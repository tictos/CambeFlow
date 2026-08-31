package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.Primary
import com.example.ui.theme.Secondary
import com.example.ui.theme.SurfaceContainerHigh
import com.example.viewmodel.AsyncStreamStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeFlowTopBar(
    title: String = "Cambe Flow",
    hasUnreadAlerts: Boolean = false,
    streamStatus: AsyncStreamStatus? = null,
    onSyncClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Pulsing animation for the async live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_cambeflow_logo_1788212572616),
                    contentDescription = "Cambe Flow Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.testTag("app_title")
                    )

                    // Live asynchronous stream indicator
                    if (streamStatus != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .scale(if (streamStatus.isStreaming) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(if (streamStatus.isStreaming) Color(0xFF00E676) else Color(0xFFFFA000))
                            )
                            Text(
                                text = if (streamStatus.isStreaming) "Flux Live (${streamStatus.latencyMs}ms)" else "Flux Suspendu",
                                fontSize = 10.sp,
                                color = if (streamStatus.isStreaming) Color(0xFF00E676) else Color(0xFFFFA000),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Right: Quick Async Refresh & Notification Bell
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Async Manual Sync Button
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .testTag("sync_rates_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Synchroniser",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Notification Bell Button
                IconButton(
                    onClick = onAlertsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceContainerHigh)
                        .testTag("notification_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (hasUnreadAlerts) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Alertes",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (hasUnreadAlerts) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .align(Alignment.TopEnd)
                                    .clip(CircleShape)
                                    .background(Secondary)
                            )
                        }
                    }
                }
            }
        }
    }
}
