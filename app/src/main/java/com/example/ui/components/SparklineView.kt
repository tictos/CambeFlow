package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Secondary
import com.example.ui.theme.Tertiary

@Composable
fun SparklineView(
    data: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = if (isPositive) Secondary else Tertiary

    Canvas(
        modifier = modifier
            .width(88.dp)
            .height(28.dp)
    ) {
        if (data.size < 2) return@Canvas

        val minVal = data.minOrNull() ?: 0.0
        val maxVal = data.maxOrNull() ?: 1.0
        val range = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)

        val path = Path()

        for (i in data.indices) {
            val x = i * stepX
            val normalizedY = 1.0 - ((data[i] - minVal) / range)
            // Keep padding within canvas
            val y = (normalizedY * (height - 8f) + 4f).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevNormalizedY = 1.0 - ((data[i - 1] - minVal) / range)
                val prevY = (prevNormalizedY * (height - 8f) + 4f).toFloat()

                // Cubic bezier smooth curve
                val controlX1 = prevX + (x - prevX) / 2f
                val controlY1 = prevY
                val controlX2 = prevX + (x - prevX) / 2f
                val controlY2 = y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
