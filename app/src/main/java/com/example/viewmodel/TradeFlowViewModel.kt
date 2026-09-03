package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TradeFlowDatabase
import com.example.data.remote.ExchangeRateApiService
import com.example.data.repository.CurrencyRepository
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.NumberFormat
import java.util.Locale

enum class AppScreen(val label: String, val title: String) {
    CONVERTER("Converter", "Convertisseur"),
    MARKETS("Markets", "Marchés"),
    TRENDS("Trends", "Tendances"),
    ALERTS("Alerts", "Alertes"),
    HISTORY("History", "Historique"),
    SETTINGS("Settings", "Paramètres")
}

data class ConverterUiState(
    val sourceCurrency: Currency = CurrencyCatalog.find("USD"),
    val targetCurrency: Currency = CurrencyCatalog.find("EUR"),
    val inputAmountText: String = "1000",
    val userBalance: Double = 12450.00,
    val showSuccessDialog: Boolean = false,
    val lastConvertedRecord: ConversionRecord? = null
)

data class AsyncStreamStatus(
    val isStreaming: Boolean = true,
    val autoSyncIntervalSec: Int = 5,
    val tickCount: Long = 0,
    val latencyMs: Long = 24,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val isAutoSyncRunning: Boolean = true,
    val lastTickTimestamp: Long = System.currentTimeMillis()
)

class TradeFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TradeFlowDatabase.getDatabase(application)
    private val apiService = ExchangeRateApiService.create()
    private val repository = CurrencyRepository(
        apiService = apiService,
        alertDao = db.alertDao(),
        historyDao = db.historyDao()
    )

    // Asynchronous jobs
    private var liveStreamJob: Job? = null
    private var autoSyncJob: Job? = null
    private var alertSentinelJob: Job? = null
    private var chartComputationJob: Job? = null

    // Asynchronous Stream State
    private val _asyncStreamStatus = MutableStateFlow(AsyncStreamStatus())
    val asyncStreamStatus = _asyncStreamStatus.asStateFlow()

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.CONVERTER)
    val currentScreen = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Converter State
    private val _converterState = MutableStateFlow(ConverterUiState())
    val converterState = _converterState.asStateFlow()

    val rates: StateFlow<Map<String, Double>> = repository.rates
    val isLoadingRates: StateFlow<Boolean> = repository.isLoading
    val isOffline: StateFlow<Boolean> = repository.isOffline
    val lastUpdated: StateFlow<Long> = repository.lastUpdated

    val alerts: StateFlow<List<PriceAlert>> = repository.alerts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val history: StateFlow<List<ConversionRecord>> = repository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Markets Search & Filtering
    private val _marketSearchQuery = MutableStateFlow("")
    val marketSearchQuery = _marketSearchQuery.asStateFlow()

    private val _marketFilterCategory = MutableStateFlow("Tous")
    val marketFilterCategory = _marketFilterCategory.asStateFlow()

    // Asynchronous reactive Flow combining query debounce, category & live rates
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val filteredMarketPairs: StateFlow<List<MarketPair>> = combine(
        repository.rates,
        _marketSearchQuery.debounce(200).distinctUntilChanged(),
        _marketFilterCategory
    ) { _, query, category ->
        withContext(Dispatchers.Default) {
            val pairs = repository.getMarketPairs()
            val cleanQuery = query.trim().lowercase()

            pairs.filter { pair ->
                val matchesQuery = cleanQuery.isEmpty() ||
                        pair.symbol.lowercase().contains(cleanQuery) ||
                        pair.baseCurrency.name.lowercase().contains(cleanQuery) ||
                        pair.targetCurrency.name.lowercase().contains(cleanQuery) ||
                        pair.displayName.lowercase().contains(cleanQuery)

                val matchesCategory = when (category) {
                    "Majeures" -> listOf("EUR/USD", "GBP/USD", "USD/JPY", "USD/CHF", "USD/CAD", "AUD/USD").contains(pair.symbol)
                    "Forex" -> true
                    "Favoris" -> pair.isFavorite || listOf("EUR/USD", "GBP/USD", "USD/JPY").contains(pair.symbol)
                    else -> true
                }

                matchesQuery && matchesCategory
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.getMarketPairs()
    )

    // Trends State
    private val _selectedPair = MutableStateFlow(
        MarketPair(
            baseCurrency = CurrencyCatalog.find("EUR"),
            targetCurrency = CurrencyCatalog.find("USD"),
            rate = 1.0842,
            change24h = 0.24,
            high24h = 1.0885,
            low24h = 1.0792,
            open24h = 1.0810,
            volatility = "Élevée (0.85%)",
            sparkline = listOf(1.081, 1.083, 1.082, 1.085, 1.083, 1.086, 1.0842)
        )
    )
    val selectedPair = _selectedPair.asStateFlow()

    private val _selectedTimeFrame = MutableStateFlow(TimeFrame.ONE_WEEK)
    val selectedTimeFrame = _selectedTimeFrame.asStateFlow()

    private val _isCandlestickMode = MutableStateFlow(false)
    val isCandlestickMode = _isCandlestickMode.asStateFlow()

    // Asynchronous Chart Data
    private val _chartPoints = MutableStateFlow<List<ChartPoint>>(emptyList())
    val chartPoints = _chartPoints.asStateFlow()

    private val _candleStickData = MutableStateFlow<List<CandleStickData>>(emptyList())
    val candleStickData = _candleStickData.asStateFlow()

    private val _isChartComputing = MutableStateFlow(false)
    val isChartComputing = _isChartComputing.asStateFlow()

    // History filter
    private val _historyFilter = MutableStateFlow("7 derniers jours")
    val historyFilter = _historyFilter.asStateFlow()

    // Alert Creation Modal State
    private val _showCreateAlertDialog = MutableStateFlow(false)
    val showCreateAlertDialog = _showCreateAlertDialog.asStateFlow()

    // Notifications / Alerts trigger snackbar
    private val _triggeredAlertMessage = MutableStateFlow<String?>(null)
    val triggeredAlertMessage = _triggeredAlertMessage.asStateFlow()

    init {
        // Seed initial data asynchronously on IO thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.seedInitialDataIfEmpty()
                repository.refreshRates()
            } catch (t: Throwable) {
                // Fallback gracefully to offline state
            }
        }

        // Launch concurrent background engines
        startLiveStreamTicker()
        startAutoSyncLoop()
        startAlertSentinelLoop()
        recomputeChartDataAsync()
    }

    fun dismissTriggeredAlert() {
        _triggeredAlertMessage.value = null
    }

    // ==========================================
    // ASYNCHRONOUS COROUTINE ENGINES
    // ==========================================

    private fun startLiveStreamTicker() {
        liveStreamJob?.cancel()
        liveStreamJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    if (_asyncStreamStatus.value.isStreaming) {
                        val start = System.currentTimeMillis()
                        repository.applyMicroTick()
                        val latency = (System.currentTimeMillis() - start).coerceAtLeast(12) + (10..22).random()

                        _asyncStreamStatus.update { current ->
                            current.copy(
                                tickCount = current.tickCount + 1,
                                latencyMs = latency,
                                lastTickTimestamp = System.currentTimeMillis()
                            )
                        }

                        // Update selected pair rate dynamically
                        val curPair = _selectedPair.value
                        val newRate = repository.getRate(curPair.baseCurrency.code, curPair.targetCurrency.code)
                        if (kotlin.math.abs(newRate - curPair.rate) > 0.000001) {
                            _selectedPair.value = curPair.copy(rate = newRate)
                        }
                    }
                } catch (t: Throwable) {
                    // Safe guard against background calculation issues
                }
                delay(2000L) // 2s asynchronous tick interval
            }
        }
    }

    private fun startAutoSyncLoop() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val intervalSec = _asyncStreamStatus.value.autoSyncIntervalSec
                    if (intervalSec > 0 && _asyncStreamStatus.value.isAutoSyncRunning) {
                        delay(intervalSec * 1000L)
                        repository.refreshRates()
                        _asyncStreamStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
                    } else {
                        delay(5000L)
                    }
                } catch (t: Throwable) {
                    delay(5000L)
                }
            }
        }
    }

    private fun startAlertSentinelLoop() {
        alertSentinelJob?.cancel()
        alertSentinelJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    delay(3000L)
                    val triggered = repository.checkAndTriggerAlerts()
                    if (triggered.isNotEmpty()) {
                        val first = triggered.first()
                        _triggeredAlertMessage.value = "🚨 Alerte Déclenchée : ${first.baseCode}/${first.targetCode} a atteint ${String.format(Locale.US, "%.4f", first.currentRate)} !"
                    }
                } catch (t: Throwable) {
                    // Safe guard
                }
            }
        }
    }

    private fun recomputeChartDataAsync() {
        chartComputationJob?.cancel()
        chartComputationJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                _isChartComputing.value = true
                val pair = _selectedPair.value
                val timeFrame = _selectedTimeFrame.value
                val currentRate = pair.rate

                val points = repository.getChartPointsAsync(pair.symbol, timeFrame, currentRate)
                val candles = repository.getCandleStickDataAsync(pair.symbol, timeFrame, currentRate)

                _chartPoints.value = points
                _candleStickData.value = candles
            } catch (t: Throwable) {
                // Fallback gracefully
            } finally {
                _isChartComputing.value = false
            }
        }
    }

    // Toggle and configure Asynchrony from UI
    fun toggleLiveStream(enabled: Boolean) {
        _asyncStreamStatus.update { it.copy(isStreaming = enabled) }
    }

    fun setAutoSyncInterval(seconds: Int) {
        _asyncStreamStatus.update {
            it.copy(
                autoSyncIntervalSec = seconds,
                isAutoSyncRunning = seconds > 0
            )
        }
    }

    fun triggerAsyncSync() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshRates()
            _asyncStreamStatus.update { it.copy(lastSyncTimestamp = System.currentTimeMillis()) }
        }
    }

    // ==========================================
    // CONVERTER LOGIC
    // ==========================================

    fun onKeypadDigit(digit: String) {
        val currentText = _converterState.value.inputAmountText
        if (digit == ".") {
            if (!currentText.contains(".")) {
                _converterState.value = _converterState.value.copy(
                    inputAmountText = if (currentText.isEmpty()) "0." else "$currentText."
                )
            }
        } else {
            val newText = if (currentText == "0") digit else currentText + digit
            if (newText.length <= 10) {
                _converterState.value = _converterState.value.copy(inputAmountText = newText)
            }
        }
    }

    fun onKeypadBackspace() {
        val currentText = _converterState.value.inputAmountText
        if (currentText.isNotEmpty()) {
            val newText = currentText.dropLast(1)
            _converterState.value = _converterState.value.copy(
                inputAmountText = if (newText.isEmpty()) "0" else newText
            )
        }
    }

    fun onKeypadClear() {
        _converterState.value = _converterState.value.copy(inputAmountText = "0")
    }

    fun swapCurrencies() {
        val curr = _converterState.value
        _converterState.value = curr.copy(
            sourceCurrency = curr.targetCurrency,
            targetCurrency = curr.sourceCurrency
        )
    }

    fun setSourceCurrency(currency: Currency) {
        _converterState.value = _converterState.value.copy(sourceCurrency = currency)
    }

    fun setTargetCurrency(currency: Currency) {
        _converterState.value = _converterState.value.copy(targetCurrency = currency)
    }

    fun getLiveConversionRate(fromCode: String, toCode: String): Double {
        return repository.getRate(fromCode, toCode)
    }

    fun executeConversion() {
        val state = _converterState.value
        val amount = state.inputAmountText.toDoubleOrNull() ?: 0.0
        if (amount <= 0) return

        val rate = repository.getRate(state.sourceCurrency.code, state.targetCurrency.code)
        val convertedAmount = amount * rate

        val record = ConversionRecord(
            fromCode = state.sourceCurrency.code,
            toCode = state.targetCurrency.code,
            fromAmount = amount,
            toAmount = convertedAmount,
            exchangeRate = rate,
            timestamp = System.currentTimeMillis(),
            status = "Complété"
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.recordConversion(record)
            withContext(Dispatchers.Main) {
                _converterState.value = state.copy(
                    showSuccessDialog = true,
                    lastConvertedRecord = record
                )
            }
        }
    }

    fun dismissSuccessDialog() {
        _converterState.value = _converterState.value.copy(showSuccessDialog = false)
    }

    fun refreshRates() {
        triggerAsyncSync()
    }

    // ==========================================
    // MARKETS LOGIC
    // ==========================================

    fun setMarketSearchQuery(query: String) {
        _marketSearchQuery.value = query
    }

    fun setMarketFilterCategory(category: String) {
        _marketFilterCategory.value = category
    }

    fun getFilteredMarketPairs(): List<MarketPair> {
        return filteredMarketPairs.value
    }

    // ==========================================
    // TRENDS LOGIC
    // ==========================================

    fun selectPairForTrends(pair: MarketPair) {
        _selectedPair.value = pair
        _currentScreen.value = AppScreen.TRENDS
        recomputeChartDataAsync()
    }

    fun selectPairForConverter(pair: MarketPair) {
        _converterState.value = _converterState.value.copy(
            sourceCurrency = pair.baseCurrency,
            targetCurrency = pair.targetCurrency
        )
        _currentScreen.value = AppScreen.CONVERTER
    }

    fun setTimeFrame(timeFrame: TimeFrame) {
        _selectedTimeFrame.value = timeFrame
        recomputeChartDataAsync()
    }

    fun toggleCandlestickMode(enabled: Boolean) {
        _isCandlestickMode.value = enabled
    }

    fun getChartPoints(): List<ChartPoint> {
        return chartPoints.value
    }

    fun getCandleStickData(): List<CandleStickData> {
        return candleStickData.value
    }

    // ==========================================
    // ALERTS LOGIC
    // ==========================================

    fun openCreateAlertDialog() {
        _showCreateAlertDialog.value = true
    }

    fun closeCreateAlertDialog() {
        _showCreateAlertDialog.value = false
    }

    fun createAlert(
        baseCode: String,
        targetCode: String,
        targetRate: Double,
        direction: AlertDirection,
        note: String
    ) {
        val currentRate = repository.getRate(baseCode, targetCode)
        val alert = PriceAlert(
            baseCode = baseCode,
            targetCode = targetCode,
            targetRate = targetRate,
            currentRate = currentRate,
            direction = direction,
            isEnabled = true,
            note = note.ifBlank { "Alerte $baseCode/$targetCode" }
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveAlert(alert)
            withContext(Dispatchers.Main) {
                _showCreateAlertDialog.value = false
            }
        }
    }

    fun toggleAlert(id: Long, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleAlert(id, isEnabled)
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlert(id)
        }
    }

    // ==========================================
    // HISTORY LOGIC
    // ==========================================

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun getFilteredHistory(records: List<ConversionRecord>): List<ConversionRecord> {
        val now = System.currentTimeMillis()
        val dayMillis = 86_400_000L
        return when (_historyFilter.value) {
            "7 derniers jours" -> records.filter { it.timestamp >= now - (7 * dayMillis) }
            "Mois dernier" -> records.filter { it.timestamp >= now - (30 * dayMillis) }
            "3 derniers mois" -> records.filter { it.timestamp >= now - (90 * dayMillis) }
            else -> records
        }
    }

    fun deleteHistoryRecord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryRecord(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllHistory()
        }
    }
}
