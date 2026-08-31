package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Primary
import com.example.ui.theme.Secondary
import com.example.ui.theme.SecondaryContainer
import com.example.ui.theme.SurfaceContainer
import com.example.viewmodel.AppScreen

data class NavItem(
    val screen: AppScreen,
    val frenchLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val navItems = listOf(
    NavItem(
        screen = AppScreen.CONVERTER,
        frenchLabel = "Convertir",
        selectedIcon = Icons.Filled.CurrencyExchange,
        unselectedIcon = Icons.Outlined.CurrencyExchange
    ),
    NavItem(
        screen = AppScreen.MARKETS,
        frenchLabel = "Marchés",
        selectedIcon = Icons.Filled.QueryStats,
        unselectedIcon = Icons.Outlined.QueryStats
    ),
    NavItem(
        screen = AppScreen.TRENDS,
        frenchLabel = "Tendances",
        selectedIcon = Icons.AutoMirrored.Filled.TrendingUp,
        unselectedIcon = Icons.AutoMirrored.Outlined.TrendingUp
    ),
    NavItem(
        screen = AppScreen.ALERTS,
        frenchLabel = "Alertes",
        selectedIcon = Icons.Filled.NotificationsActive,
        unselectedIcon = Icons.Outlined.NotificationsActive
    ),
    NavItem(
        screen = AppScreen.HISTORY,
        frenchLabel = "Historique",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )
)

@Composable
fun TradeFlowBottomNav(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = SurfaceContainer.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentScreen == item.screen
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) {
                                Modifier.background(SecondaryContainer.copy(alpha = 0.35f))
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple()
                        ) {
                            onScreenSelected(item.screen)
                        }
                        .padding(vertical = 6.dp)
                        .testTag("nav_tab_${item.screen.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.frenchLabel,
                        tint = if (isSelected) Secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.frenchLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
