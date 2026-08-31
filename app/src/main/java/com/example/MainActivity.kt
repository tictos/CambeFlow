package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.components.TradeFlowBottomNav
import com.example.ui.components.TradeFlowTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.TradeFlowViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TradeFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val alerts by viewModel.alerts.collectAsState()
                val streamStatus by viewModel.asyncStreamStatus.collectAsState()
                val triggeredAlert by viewModel.triggeredAlertMessage.collectAsState()
                val hasActiveAlerts = alerts.any { it.isEnabled }
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(triggeredAlert) {
                    triggeredAlert?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.dismissTriggeredAlert()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TradeFlowTopBar(
                            title = "Cambe Flow",
                            hasUnreadAlerts = hasActiveAlerts,
                            streamStatus = streamStatus,
                            onSyncClick = { viewModel.triggerAsyncSync() },
                            onAlertsClick = { viewModel.navigateTo(AppScreen.ALERTS) },
                            onProfileClick = { viewModel.navigateTo(AppScreen.SETTINGS) }
                        )
                    },
                    bottomBar = {
                        TradeFlowBottomNav(
                            currentScreen = currentScreen,
                            onScreenSelected = { screen -> viewModel.navigateTo(screen) }
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { screen ->
                            when (screen) {
                                AppScreen.CONVERTER -> ConverterScreen(viewModel = viewModel)
                                AppScreen.MARKETS -> MarketsScreen(viewModel = viewModel)
                                AppScreen.TRENDS -> TrendsScreen(viewModel = viewModel)
                                AppScreen.ALERTS -> AlertsScreen(viewModel = viewModel)
                                AppScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                                AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
