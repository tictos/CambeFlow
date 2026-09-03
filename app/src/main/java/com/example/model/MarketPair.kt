package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeRateApiResponse(
    @Json(name = "result") val result: String? = null,
    @Json(name = "base_code") val baseCode: String? = null,
    @Json(name = "rates") val rates: Map<String, Double>? = null,
    @Json(name = "time_last_update_utc") val timeLastUpdate: String? = null
)

data class ChartPoint(
    val timestamp: Long,
    val value: Double,
    val label: String
)

data class CandleStickData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val label: String
)

enum class TimeFrame(val label: String, val periodName: String) {
    ONE_HOUR("1H", "1 Heure"),
    ONE_DAY("1D", "1 Jour"),
    ONE_WEEK("1W", "1 Semaine"),
    ONE_MONTH("1M", "1 Mois"),
    ONE_YEAR("1Y", "1 An")
}

data class MarketPair(
    val baseCurrency: Currency,
    val targetCurrency: Currency,
    val rate: Double,
    val change24h: Double, // in percentage, e.g. +0.45 or -0.12
    val high24h: Double,
    val low24h: Double,
    val open24h: Double,
    val volatility: String, // e.g. "Faible (0.32%)", "Moyenne (0.55%)", "Élevée (0.85%)"
    val sparkline: List<Double>,
    val isFavorite: Boolean = false
) {
    val symbol: String get() = "${baseCurrency.code}/${targetCurrency.code}"
    val displayName: String get() = "${baseCurrency.frenchName} / ${targetCurrency.frenchName}"
    val isPositive: Boolean get() = change24h >= 0
}
