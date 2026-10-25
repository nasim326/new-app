package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 24.dp,
    centerTitle: String = "",
    centerSubtitle: String = ""
) {
    val total = slices.sumOf { it.value }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = size.toPx() - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val chartSize = Size(diameter, diameter)

            if (total <= 0.0 || slices.isEmpty()) {
                // Empty placeholder ring
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = chartSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            } else {
                var currentAngle = -90f
                slices.forEach { slice ->
                    val sweepAngle = ((slice.value / total) * 360f).toFloat() * progress.value
                    if (sweepAngle > 0.5f) {
                        drawArc(
                            color = slice.color,
                            startAngle = currentAngle,
                            sweepAngle = sweepAngle - 2f, // subtle gap
                            useCenter = false,
                            topLeft = topLeft,
                            size = chartSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    currentAngle += sweepAngle
                }
            }
        }

        if (centerTitle.isNotEmpty() || centerSubtitle.isNotEmpty()) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (centerTitle.isNotEmpty()) {
                    Text(
                        text = centerTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }
                if (centerSubtitle.isNotEmpty()) {
                    Text(
                        text = centerSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
