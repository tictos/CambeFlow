package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CandleStickData
import com.example.model.ChartPoint
import com.example.model.TimeFrame
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun InteractiveChart(
    points: List<ChartPoint>,
    candles: List<CandleStickData>,
    selectedTimeFrame: TimeFrame,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    isCandlestickMode: Boolean,
    onToggleCandlestick: (Boolean) -> Unit,
    currentRate: Double,
    isPositive: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val themeColor = if (isPositive) Secondary else Tertiary
    val gradientStart = themeColor.copy(alpha = 0.35f)
    val gradientEnd = themeColor.copy(alpha = 0.0f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerHigh.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Controls: TimeFrame selector + Candlestick/Line toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TimeFrame.entries.forEach { tf ->
                        val isSelected = tf == selectedTimeFrame
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) SurfaceContainerHighest else Color.Transparent)
                                .clickable { onTimeFrameSelected(tf) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("timeframe_${tf.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tf.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Chart mode icons
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLow)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { onToggleCandlestick(true) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCandlestickMode) SurfaceContainerHighest else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CandlestickChart,
                            contentDescription = "Bougies",
                            tint = if (isCandlestickMode) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onToggleCandlestick(false) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isCandlestickMode) SurfaceContainerHighest else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Ligne",
                            tint = if (!isCandlestickMode) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hover / Dragged Scrubber Info Pill
            if (selectedIndex != null && selectedIndex!! in points.indices) {
                val point = points[selectedIndex!!]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sélectionné: ${point.label}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.4f", point.value),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }
            }

            // Main Canvas Area
            val minVal = if (isCandlestickMode && candles.isNotEmpty()) candles.minOf { it.low }
            else if (points.isNotEmpty()) points.minOf { it.value } else currentRate * 0.99
            val maxVal = if (isCandlestickMode && candles.isNotEmpty()) candles.maxOf { it.high }
            else if (points.isNotEmpty()) points.maxOf { it.value } else currentRate * 1.01
            val range = if (maxVal - minVal == 0.0) 0.001 else (maxVal - minVal)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(Locale.US, "%.4f", maxVal),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.4f", (maxVal + minVal) / 2),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.4f", minVal),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }

                // Interactive Chart Drawing Area
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 42.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)
                        .pointerInput(points.size, isCandlestickMode) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val count = if (isCandlestickMode) candles.size else points.size
                                    if (count > 0) {
                                        val idx = ((offset.x / size.width) * count)
                                            .toInt()
                                            .coerceIn(0, count - 1)
                                        selectedIndex = idx
                                    }
                                }
                            )
                        }
                        .pointerInput(points.size, isCandlestickMode) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    change.consume()
                                    val count = if (isCandlestickMode) candles.size else points.size
                                    if (count > 0) {
                                        val idx = ((change.position.x / size.width) * count)
                                            .toInt()
                                            .coerceIn(0, count - 1)
                                        selectedIndex = idx
                                    }
                                },
                                onDragEnd = {
                                    // keep selection or null
                                }
                            )
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Draw grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = (canvasHeight / gridLines) * i
                        drawLine(
                            color = OutlineVariant.copy(alpha = 0.25f),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    if (isCandlestickMode && candles.isNotEmpty()) {
                        val candleWidth = (canvasWidth / candles.size) * 0.65f
                        val stepX = canvasWidth / candles.size

                        for (i in candles.indices) {
                            val candle = candles[i]
                            val centerX = (i * stepX) + (stepX / 2f)
                            val isBullish = candle.close >= candle.open
                            val candleColor = if (isBullish) Secondary else Tertiary

                            val highY = ((1.0 - (candle.high - minVal) / range) * canvasHeight).toFloat()
                            val lowY = ((1.0 - (candle.low - minVal) / range) * canvasHeight).toFloat()
                            val openY = ((1.0 - (candle.open - minVal) / range) * canvasHeight).toFloat()
                            val closeY = ((1.0 - (candle.close - minVal) / range) * canvasHeight).toFloat()

                            // Draw wick
                            drawLine(
                                color = candleColor,
                                start = Offset(centerX, highY),
                                end = Offset(centerX, lowY),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // Draw body
                            val topY = kotlin.math.min(openY, closeY)
                            val bodyHeight = kotlin.math.max(kotlin.math.abs(closeY - openY), 3f)
                            drawRect(
                                color = candleColor,
                                topLeft = Offset(centerX - (candleWidth / 2f), topY),
                                size = Size(candleWidth, bodyHeight)
                            )
                        }
                    } else if (points.size >= 2) {
                        val stepX = canvasWidth / (points.size - 1)
                        val linePath = Path()
                        val fillPath = Path()

                        for (i in points.indices) {
                            val x = i * stepX
                            val normalizedY = 1.0 - ((points[i].value - minVal) / range)
                            val y = (normalizedY * canvasHeight).toFloat()

                            if (i == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, canvasHeight)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (i - 1) * stepX
                                val prevNormalizedY = 1.0 - ((points[i - 1].value - minVal) / range)
                                val prevY = (prevNormalizedY * canvasHeight).toFloat()

                                val controlX1 = prevX + (x - prevX) / 2f
                                val controlY1 = prevY
                                val controlX2 = prevX + (x - prevX) / 2f
                                val controlY2 = y

                                linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                            }

                            if (i == points.size - 1) {
                                fillPath.lineTo(x, canvasHeight)
                                fillPath.close()
                            }
                        }

                        // Draw shaded area
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(gradientStart, gradientEnd),
                                startY = 0f,
                                endY = canvasHeight
                            )
                        )

                        // Draw stroke
                        drawPath(
                            path = linePath,
                            color = themeColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Current Price horizontal dotted indicator
                        val currentY = ((1.0 - (currentRate - minVal) / range) * canvasHeight).toFloat()
                        drawLine(
                            color = themeColor.copy(alpha = 0.7f),
                            start = Offset(0f, currentY),
                            end = Offset(canvasWidth, currentY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    }

                    // Draw vertical cursor if scrubbed
                    if (selectedIndex != null) {
                        val count = if (isCandlestickMode) candles.size else points.size
                        if (count > 0 && selectedIndex!! in 0 until count) {
                            val step = canvasWidth / (if (isCandlestickMode) count else (count - 1).coerceAtLeast(1))
                            val cursorX = if (isCandlestickMode) (selectedIndex!! * step) + (step / 2f) else selectedIndex!! * step

                            drawLine(
                                color = Primary,
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, canvasHeight),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }
                    }
                }
            }

            // X-Axis Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 42.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (points.isNotEmpty()) {
                    val displayIndices = listOf(0, points.size / 4, points.size / 2, (3 * points.size) / 4, points.size - 1)
                    displayIndices.filter { it in points.indices }.distinct().forEach { idx ->
                        Text(
                            text = points[idx].label,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
