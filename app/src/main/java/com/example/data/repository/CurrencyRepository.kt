package com.example.data.repository

import com.example.data.local.AlertDao
import com.example.data.local.HistoryDao
import com.example.data.remote.ExchangeRateApiService
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin
import kotlin.random.Random

class CurrencyRepository(
    private val apiService: ExchangeRateApiService,
    private val alertDao: AlertDao,
    private val historyDao: HistoryDao
) {
    // In-memory cache of latest rates with USD as reference
    private val _rates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val rates = _rates.asStateFlow()

    private val _lastUpdated = MutableStateFlow(System.currentTimeMillis())
    val lastUpdated = _lastUpdated.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline = _isOffline.asStateFlow()

    val alerts: Flow<List<PriceAlert>> = alertDao.getAllAlerts()
    val history: Flow<List<ConversionRecord>> = historyDao.getAllHistory()

    init {
        // Initialize default baseline rates from catalog
        val initialMap = CurrencyCatalog.currencies.associate { it.code to it.defaultRateToUSD }
        _rates.value = initialMap
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // Insert initial alerts matching the user's mockup if DB is empty
        val sampleAlerts = listOf(
            PriceAlert(
                baseCode = "EUR",
                targetCode = "USD",
                targetRate = 1.1050,
                currentRate = 1.0982,
                direction = AlertDirection.ABOVE,
                isEnabled = true,
                note = "Seuil de prise de profit EUR"
            ),
            PriceAlert(
                baseCode = "GBP",
                targetCode = "JPY",
                targetRate = 188.500,
                currentRate = 189.245,
                direction = AlertDirection.BELOW,
                isEnabled = true,
                note = "Support technique GBP/JPY"
            ),
            PriceAlert(
                baseCode = "AUD",
                targetCode = "USD",
                targetRate = 0.6650,
                currentRate = 0.6590,
                direction = AlertDirection.ABOVE,
                isEnabled = false,
                note = "Résistance hebdomadaire"
            )
        )

        // Seed initial history matching mockup
        val now = System.currentTimeMillis()
        val dayMillis = 86_400_000L
        val sampleHistory = listOf(
            ConversionRecord(
                fromCode = "USD",
                toCode = "EUR",
                fromAmount = 10000.0,
                toAmount = 9420.50,
                exchangeRate = 0.94205,
                timestamp = now - (dayMillis * 1),
                status = "Complété"
            ),
            ConversionRecord(
                fromCode = "GBP",
                toCode = "USD",
                fromAmount = 5000.0,
                toAmount = 6125.00,
                exchangeRate = 1.225,
                timestamp = now - (dayMillis * 2),
                status = "Complété"
            ),
            ConversionRecord(
                fromCode = "EUR",
                toCode = "JPY",
                fromAmount = 2500.0,
                toAmount = 398500.0,
                exchangeRate = 159.4,
                timestamp = now - (dayMillis * 3),
                status = "En attente"
            )
        )

        // Insert initial history if none exists
        try {
            for (alert in sampleAlerts) {
                alertDao.insertAlert(alert)
            }
            for (item in sampleHistory) {
                historyDao.insert(item)
            }
        } catch (e: Exception) {
            // ignore if already seeded
        }
    }

    suspend fun refreshRates(base: String = "USD") = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val response = apiService.getLatestRates(base)
            if (response.result == "success" && response.rates != null) {
                _rates.value = response.rates
                _lastUpdated.value = System.currentTimeMillis()
                _isOffline.value = false
            } else {
                _isOffline.value = true
            }
        } catch (e: Exception) {
            _isOffline.value = true
        } finally {
            _isLoading.value = false
        }
    }

    fun getRate(fromCode: String, toCode: String): Double {
        if (fromCode == toCode) return 1.0
        val currentRates = _rates.value
        val fromRateToUSD = currentRates[fromCode] ?: CurrencyCatalog.find(fromCode).defaultRateToUSD
        val toRateToUSD = currentRates[toCode] ?: CurrencyCatalog.find(toCode).defaultRateToUSD

        if (fromRateToUSD <= 0.0) return 1.0
        // Since rates are relative to USD (1 USD = X Currency), from -> to is (toRate / fromRate)
        return toRateToUSD / fromRateToUSD
    }

    fun convertAmount(amount: Double, fromCode: String, toCode: String): Double {
        val rate = getRate(fromCode, toCode)
        return amount * rate
    }

    fun getMarketPairs(): List<MarketPair> {
        val pairsConfig = listOf(
            Pair("EUR", "USD") to 0.45,
            Pair("GBP", "USD") to -0.12,
            Pair("USD", "JPY") to 0.85,
            Pair("AUD", "USD") to -0.34,
            Pair("USD", "CAD") to 0.18,
            Pair("USD", "CHF") to -0.08,
            Pair("EUR", "GBP") to 0.22,
            Pair("EUR", "JPY") to 1.15,
            Pair("GBP", "JPY") to -0.42,
            Pair("USD", "CNY") to 0.05,
            Pair("USD", "INR") to -0.02,
            Pair("EUR", "CHF") to 0.14
        )

        return pairsConfig.map { (pair, change) ->
            val (baseCode, targetCode) = pair
            val base = CurrencyCatalog.find(baseCode)
            val target = CurrencyCatalog.find(targetCode)
            val rate = getRate(baseCode, targetCode)
            val high = rate * (1.0 + (kotlin.math.abs(change) * 0.008 + 0.003))
            val low = rate * (1.0 - (kotlin.math.abs(change) * 0.008 + 0.003))
            val open = rate * (1.0 - change / 100.0)
            val volatility = if (kotlin.math.abs(change) > 0.5) "Élevée (${String.format(Locale.US, "%.2f", kotlin.math.abs(change))}%)"
            else "Modérée (${String.format(Locale.US, "%.2f", kotlin.math.abs(change))}%)"

            val sparkline = generateSparkline(rate, change >= 0)

            MarketPair(
                baseCurrency = base,
                targetCurrency = target,
                rate = rate,
                change24h = change,
                high24h = high,
                low24h = low,
                open24h = open,
                volatility = volatility,
                sparkline = sparkline
            )
        }
    }

    private fun generateSparkline(currentRate: Double, isPositive: Boolean): List<Double> {
        val points = mutableListOf<Double>()
        val count = 12
        val trend = if (isPositive) 0.008 else -0.008
        val base = currentRate * (1.0 - trend)
        for (i in 0 until count) {
            val progress = i.toDouble() / (count - 1)
            val noise = (sin(i * 1.5) * 0.002) + ((Random.nextDouble() - 0.5) * 0.001)
            val value = base + (trend * currentRate * progress) + noise
            points.add(value)
        }
        points[count - 1] = currentRate
        return points
    }

    fun getChartPoints(pair: String, timeFrame: TimeFrame, currentRate: Double): List<ChartPoint> {
        val count = when (timeFrame) {
            TimeFrame.ONE_HOUR -> 12
            TimeFrame.ONE_DAY -> 24
            TimeFrame.ONE_WEEK -> 7
            TimeFrame.ONE_MONTH -> 30
            TimeFrame.ONE_YEAR -> 12
        }

        val points = mutableListOf<ChartPoint>()
        val calendar = Calendar.getInstance()
        val timeStep = when (timeFrame) {
            TimeFrame.ONE_HOUR -> Calendar.MINUTE to 5
            TimeFrame.ONE_DAY -> Calendar.HOUR_OF_DAY to 1
            TimeFrame.ONE_WEEK -> Calendar.DAY_OF_YEAR to 1
            TimeFrame.ONE_MONTH -> Calendar.DAY_OF_YEAR to 1
            TimeFrame.ONE_YEAR -> Calendar.MONTH to 1
        }

        val baseVolatility = currentRate * 0.015
        val seed = pair.hashCode().toLong()
        val random = Random(seed + timeFrame.ordinal)

        // Generate smooth realistic trajectory
        var walker = currentRate * (1.0 + (random.nextDouble() - 0.5) * 0.02)
        val rawValues = mutableListOf<Double>()
        for (i in 0 until count) {
            rawValues.add(walker)
            walker += (random.nextDouble() - 0.48) * (baseVolatility / kotlin.math.sqrt(count.toDouble()))
        }
        rawValues[rawValues.size - 1] = currentRate

        val timeFormat = when (timeFrame) {
            TimeFrame.ONE_HOUR -> SimpleDateFormat("HH:mm", Locale.getDefault())
            TimeFrame.ONE_DAY -> SimpleDateFormat("HH:mm", Locale.getDefault())
            TimeFrame.ONE_WEEK -> SimpleDateFormat("EEE", Locale.getDefault())
            TimeFrame.ONE_MONTH -> SimpleDateFormat("d MMM", Locale.getDefault())
            TimeFrame.ONE_YEAR -> SimpleDateFormat("MMM", Locale.getDefault())
        }

        calendar.add(timeStep.first, -timeStep.second * count)
        for (i in 0 until count) {
            calendar.add(timeStep.first, timeStep.second)
            points.add(
                ChartPoint(
                    timestamp = calendar.timeInMillis,
                    value = rawValues[i],
                    label = timeFormat.format(calendar.time)
                )
            )
        }
        return points
    }

    fun getCandleStickData(pair: String, timeFrame: TimeFrame, currentRate: Double): List<CandleStickData> {
        val points = getChartPoints(pair, timeFrame, currentRate)
        val candles = mutableListOf<CandleStickData>()
        val random = Random(pair.hashCode() + 42)

        for (i in points.indices) {
            val point = points[i]
            val prevClose = if (i == 0) point.value * 0.998 else candles[i - 1].close
            val open = prevClose
            val close = point.value
            val spread = kotlin.math.abs(close - open) + (currentRate * 0.001)
            val high = kotlin.math.max(open, close) + (random.nextDouble() * spread * 0.8)
            val low = kotlin.math.min(open, close) - (random.nextDouble() * spread * 0.8)

            candles.add(
                CandleStickData(
                    timestamp = point.timestamp,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    label = point.label
                )
            )
        }
        return candles
    }

    suspend fun getChartPointsAsync(pair: String, timeFrame: TimeFrame, currentRate: Double): List<ChartPoint> = withContext(Dispatchers.Default) {
        getChartPoints(pair, timeFrame, currentRate)
    }

    suspend fun getCandleStickDataAsync(pair: String, timeFrame: TimeFrame, currentRate: Double): List<CandleStickData> = withContext(Dispatchers.Default) {
        getCandleStickData(pair, timeFrame, currentRate)
    }

    suspend fun applyMicroTick(volatilityFactor: Double = 0.00015): Long = withContext(Dispatchers.Default) {
        val current = _rates.value.toMutableMap()
        if (current.isNotEmpty()) {
            val random = Random.Default
            // Pick 3-6 currencies to apply subtle live tick
            val keys = current.keys.toList()
            val sampleSize = minOf(6, keys.size)
            for (i in 0 until sampleSize) {
                val key = keys[random.nextInt(keys.size)]
                if (key != "USD") {
                    val oldVal = current[key] ?: continue
                    val delta = (random.nextDouble() - 0.495) * volatilityFactor * oldVal
                    current[key] = maxOf(0.000001, oldVal + delta)
                }
            }
            _rates.value = current
            _lastUpdated.value = System.currentTimeMillis()
        }
        System.currentTimeMillis()
    }

    suspend fun checkAndTriggerAlerts(): List<PriceAlert> = withContext(Dispatchers.IO) {
        val allAlerts = alertDao.getAllAlertsOnce()
        val triggered = mutableListOf<PriceAlert>()
        val currentRates = _rates.value

        for (alert in allAlerts) {
            if (alert.isEnabled && !alert.isTriggered) {
                val baseRate = currentRates[alert.baseCode] ?: CurrencyCatalog.find(alert.baseCode).defaultRateToUSD
                val targetRate = currentRates[alert.targetCode] ?: CurrencyCatalog.find(alert.targetCode).defaultRateToUSD
                val liveRate = if (baseRate > 0) targetRate / baseRate else 1.0

                val isTriggeredNow = when (alert.direction) {
                    AlertDirection.ABOVE -> liveRate >= alert.targetRate
                    AlertDirection.BELOW -> liveRate <= alert.targetRate
                }

                if (isTriggeredNow) {
                    val updated = alert.copy(
                        isTriggered = true,
                        triggeredAt = System.currentTimeMillis(),
                        currentRate = liveRate
                    )
                    alertDao.updateAlert(updated)
                    triggered.add(updated)
                } else if (kotlin.math.abs(liveRate - alert.currentRate) > 0.00001) {
                    alertDao.updateAlert(alert.copy(currentRate = liveRate))
                }
            }
        }
        triggered
    }

    // Room Alert Operations
    suspend fun saveAlert(alert: PriceAlert): Long = withContext(Dispatchers.IO) {
        alertDao.insertAlert(alert)
    }

    suspend fun toggleAlert(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        alertDao.setAlertEnabled(id, enabled)
    }

    suspend fun deleteAlert(id: Long) = withContext(Dispatchers.IO) {
        alertDao.deleteAlertById(id)
    }

    // Room History Operations
    suspend fun recordConversion(record: ConversionRecord): Long = withContext(Dispatchers.IO) {
        historyDao.insert(record)
    }

    suspend fun deleteHistoryRecord(id: Long) = withContext(Dispatchers.IO) {
        historyDao.deleteById(id)
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }
}
