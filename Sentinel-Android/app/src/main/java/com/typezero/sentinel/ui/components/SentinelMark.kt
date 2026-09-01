package com.typezero.sentinel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.typezero.sentinel.ui.theme.SentinelCyan
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simplified Sentinel mark used inside the UI.
 *
 * The six outer nodes represent the observed network. The radar at the center represents
 * Sentinel correlating what it can discover and observe. The full-detail artwork lives in
 * drawable-nodpi/sentinel_brand.png; this geometric version stays legible at small sizes.
 */
@Composable
fun SentinelMark(
    modifier: Modifier = Modifier,
    color: Color = SentinelCyan
) {
    Canvas(modifier = modifier) {
        val c = center
        val unit = size.minDimension
        val outerRadius = unit * 0.39f
        val radarRadius = unit * 0.25f
        val nodeRadius = unit * 0.047f
        val steel = Color(0xFF6C7E89)

        val points = (0 until 6).map { i ->
            val angle = Math.toRadians((-90.0 + i * 60.0))
            Offset(
                x = c.x + cos(angle).toFloat() * outerRadius,
                y = c.y + sin(angle).toFloat() * outerRadius
            )
        }

        // Network frame.
        points.forEachIndexed { i, start ->
            val end = points[(i + 1) % points.size]
            drawLine(
                color = steel,
                start = start,
                end = end,
                strokeWidth = unit * 0.026f,
                cap = StrokeCap.Round
            )
        }

        // Observation links from the outer network to the central radar.
        points.forEach { node ->
            val dx = node.x - c.x
            val dy = node.y - c.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy)
            val inner = Offset(
                c.x + dx / len * radarRadius * 1.02f,
                c.y + dy / len * radarRadius * 1.02f
            )
            val outer = Offset(
                node.x - dx / len * nodeRadius * 1.35f,
                node.y - dy / len * nodeRadius * 1.35f
            )
            drawLine(
                color = color.copy(alpha = 0.62f),
                start = inner,
                end = outer,
                strokeWidth = unit * 0.018f,
                cap = StrokeCap.Round
            )
        }

        // Radar rings.
        drawCircle(
            color = Color(0xFF0A1116),
            radius = radarRadius * 1.12f,
            center = c
        )
        drawCircle(color = color, radius = radarRadius, center = c, style = Stroke(unit * 0.022f))
        drawCircle(color = color.copy(alpha = 0.55f), radius = radarRadius * 0.58f, center = c, style = Stroke(unit * 0.012f))
        drawCircle(color = color.copy(alpha = 0.45f), radius = radarRadius * 0.30f, center = c, style = Stroke(unit * 0.010f))

        // Sweep field and beam.
        val arcRadius = radarRadius * 0.93f
        drawArc(
            color = color.copy(alpha = 0.22f),
            startAngle = -90f,
            sweepAngle = 48f,
            useCenter = true,
            topLeft = Offset(c.x - arcRadius, c.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f)
        )
        val beamAngle = Math.toRadians(-42.0)
        val beamEnd = Offset(
            c.x + cos(beamAngle).toFloat() * arcRadius,
            c.y + sin(beamAngle).toFloat() * arcRadius
        )
        drawLine(color = Color(0xFFD7FAFF), start = c, end = beamEnd, strokeWidth = unit * 0.016f, cap = StrokeCap.Round)
        drawCircle(color = Color(0xFFD7FAFF), radius = unit * 0.023f, center = c)

        // Six observed nodes.
        points.forEach { node ->
            drawCircle(color = Color(0xFF14222A), radius = nodeRadius * 1.45f, center = node)
            drawCircle(color = steel, radius = nodeRadius * 1.45f, center = node, style = Stroke(unit * 0.014f))
            drawCircle(color = color, radius = nodeRadius * 0.68f, center = node)
        }
    }
}
