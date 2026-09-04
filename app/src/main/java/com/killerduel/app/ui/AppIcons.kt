package com.killerduel.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Les quelques pictogrammes du jeu, tracés à la main : six icônes ne justifient
 * pas d'embarquer une bibliothèque entière, et le trait reste raccord avec la grille.
 */
enum class AppIcon { Back, Pause, Play, Undo, Erase, Pencil, Hint, Trophy, Stats, Settings, Calendar }

@Composable
fun AppIconView(
    icon: AppIcon,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.09f
        val stroke = Stroke(width = w, cap = StrokeCap.Round)
        when (icon) {
            AppIcon.Back -> drawBack(tint, s, stroke)
            AppIcon.Pause -> drawPause(tint, s)
            AppIcon.Play -> drawPlay(tint, s)
            AppIcon.Undo -> drawUndo(tint, s, stroke)
            AppIcon.Erase -> drawErase(tint, s)
            AppIcon.Pencil -> drawPencil(tint, s)
            AppIcon.Hint -> drawHint(tint, s, stroke)
            AppIcon.Trophy -> drawTrophy(tint, s, stroke)
            AppIcon.Stats -> drawStats(tint, s)
            AppIcon.Settings -> drawSettings(tint, s, stroke)
            AppIcon.Calendar -> drawCalendar(tint, s, stroke)
        }
    }
}

private fun DrawScope.drawBack(tint: Color, s: Float, stroke: Stroke) {
    val path = Path().apply {
        moveTo(s * 0.62f, s * 0.2f)
        lineTo(s * 0.34f, s * 0.5f)
        lineTo(s * 0.62f, s * 0.8f)
    }
    drawPath(path, tint, style = stroke)
}

private fun DrawScope.drawPause(tint: Color, s: Float) {
    val barWidth = s * 0.17f
    drawRect(tint, Offset(s * 0.28f, s * 0.22f), Size(barWidth, s * 0.56f))
    drawRect(tint, Offset(s * 0.55f, s * 0.22f), Size(barWidth, s * 0.56f))
}

private fun DrawScope.drawPlay(tint: Color, s: Float) {
    val path = Path().apply {
        moveTo(s * 0.32f, s * 0.2f)
        lineTo(s * 0.78f, s * 0.5f)
        lineTo(s * 0.32f, s * 0.8f)
        close()
    }
    drawPath(path, tint)
}

private fun DrawScope.drawUndo(tint: Color, s: Float, stroke: Stroke) {
    drawArc(
        color = tint,
        startAngle = 150f,
        sweepAngle = 250f,
        useCenter = false,
        topLeft = Offset(s * 0.2f, s * 0.24f),
        size = Size(s * 0.6f, s * 0.55f),
        style = stroke
    )
    val head = Path().apply {
        moveTo(s * 0.16f, s * 0.24f)
        lineTo(s * 0.24f, s * 0.44f)
        lineTo(s * 0.42f, s * 0.32f)
        close()
    }
    drawPath(head, tint)
}

private fun DrawScope.drawErase(tint: Color, s: Float) {
    rotate(-38f, Offset(s / 2f, s / 2f)) {
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.24f, s * 0.34f),
            size = Size(s * 0.52f, s * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.06f)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(s * 0.24f, s * 0.34f),
            size = Size(s * 0.22f, s * 0.32f)
        )
    }
}

private fun DrawScope.drawPencil(tint: Color, s: Float) {
    rotate(0f) {
        val body = Path().apply {
            moveTo(s * 0.24f, s * 0.76f)
            lineTo(s * 0.32f, s * 0.55f)
            lineTo(s * 0.66f, s * 0.21f)
            lineTo(s * 0.79f, s * 0.34f)
            lineTo(s * 0.45f, s * 0.68f)
            close()
        }
        drawPath(body, tint)
        val tip = Path().apply {
            moveTo(s * 0.24f, s * 0.76f)
            lineTo(s * 0.32f, s * 0.55f)
            lineTo(s * 0.45f, s * 0.68f)
            close()
        }
        drawPath(tip, tint.copy(alpha = 0.55f))
    }
}

private fun DrawScope.drawHint(tint: Color, s: Float, stroke: Stroke) {
    drawCircle(tint, radius = s * 0.24f, center = Offset(s / 2f, s * 0.42f))
    drawLine(
        tint, Offset(s * 0.4f, s * 0.72f), Offset(s * 0.6f, s * 0.72f),
        strokeWidth = stroke.width, cap = StrokeCap.Round
    )
    drawLine(
        tint, Offset(s * 0.43f, s * 0.83f), Offset(s * 0.57f, s * 0.83f),
        strokeWidth = stroke.width, cap = StrokeCap.Round
    )
}

private fun DrawScope.drawTrophy(tint: Color, s: Float, stroke: Stroke) {
    val cup = Path().apply {
        moveTo(s * 0.3f, s * 0.18f)
        lineTo(s * 0.7f, s * 0.18f)
        lineTo(s * 0.64f, s * 0.55f)
        lineTo(s * 0.36f, s * 0.55f)
        close()
    }
    drawPath(cup, tint)
    drawArc(
        tint, 90f, 180f, false,
        topLeft = Offset(s * 0.14f, s * 0.2f), size = Size(s * 0.2f, s * 0.26f), style = stroke
    )
    drawArc(
        tint, 270f, 180f, false,
        topLeft = Offset(s * 0.66f, s * 0.2f), size = Size(s * 0.2f, s * 0.26f), style = stroke
    )
    drawRect(tint, Offset(s * 0.45f, s * 0.55f), Size(s * 0.1f, s * 0.16f))
    drawRoundRect(
        tint, Offset(s * 0.3f, s * 0.71f), Size(s * 0.4f, s * 0.11f),
        androidx.compose.ui.geometry.CornerRadius(s * 0.03f)
    )
}

private fun DrawScope.drawCalendar(tint: Color, s: Float, stroke: Stroke) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(s * 0.16f, s * 0.24f),
        size = Size(s * 0.68f, s * 0.60f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.07f),
        style = stroke
    )
    drawRect(tint, Offset(s * 0.16f, s * 0.24f), Size(s * 0.68f, s * 0.13f))
    drawLine(tint, Offset(s * 0.33f, s * 0.14f), Offset(s * 0.33f, s * 0.28f), stroke.width, cap = StrokeCap.Round)
    drawLine(tint, Offset(s * 0.67f, s * 0.14f), Offset(s * 0.67f, s * 0.28f), stroke.width, cap = StrokeCap.Round)
    drawCircle(tint, radius = s * 0.06f, center = Offset(s * 0.5f, s * 0.60f))
}

private fun DrawScope.drawSettings(tint: Color, s: Float, stroke: Stroke) {
    drawCircle(tint, radius = s * 0.30f, center = Offset(s / 2f, s / 2f), style = stroke)
    drawCircle(tint, radius = s * 0.09f, center = Offset(s / 2f, s / 2f))
    for (i in 0 until 6) {
        val angle = Math.toRadians(i * 60.0)
        val cx = s / 2f + (s * 0.30f) * kotlin.math.cos(angle).toFloat()
        val cy = s / 2f + (s * 0.30f) * kotlin.math.sin(angle).toFloat()
        drawCircle(tint, radius = s * 0.055f, center = Offset(cx, cy))
    }
}

private fun DrawScope.drawStats(tint: Color, s: Float) {
    val bars = listOf(
        Rect(s * 0.2f, s * 0.55f, s * 0.35f, s * 0.82f),
        Rect(s * 0.42f, s * 0.34f, s * 0.57f, s * 0.82f),
        Rect(s * 0.64f, s * 0.18f, s * 0.79f, s * 0.82f)
    )
    bars.forEach {
        drawRoundRect(
            tint, Offset(it.left, it.top), Size(it.width, it.height),
            androidx.compose.ui.geometry.CornerRadius(s * 0.03f)
        )
    }
}
