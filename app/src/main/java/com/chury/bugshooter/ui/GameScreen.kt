package com.chury.bugshooter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chury.bugshooter.engine.Vector2
import com.chury.bugshooter.game.BugShooterGame
import com.chury.bugshooter.game.Enemy
import com.chury.bugshooter.game.Explosion
import kotlinx.coroutines.isActive

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    game: BugShooterGame = remember { BugShooterGame() },
) {
    LaunchedEffect(game) {
        var previousFrameNanos = withFrameNanos { it }
        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val deltaSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
            previousFrameNanos = frameNanos
            game.update(deltaSeconds.coerceAtMost(0.05f))
        }
    }

    val state = game.state

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
            .onSizeChanged { game.setScreenSize(Vector2(it.width.toFloat(), it.height.toFloat())) }
            .pointerInput(game) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    game.movePlayerBy(dragAmount.x)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSpaceBackground()
            val playerIsFlashing = state.playerHitFlashSeconds > 0f &&
                (state.playerHitFlashSeconds * 14).toInt() % 2 == 0
            drawPlayer(
                position = state.player.position,
                size = state.player.size,
                isFlashing = playerIsFlashing,
            )
            state.bullets.forEach { bullet ->
                drawCircle(
                    color = Color(0xFFFFF176),
                    radius = bullet.radius,
                    center = Offset(bullet.position.x, bullet.position.y),
                )
            }
            state.enemies.forEach { enemy ->
                drawMosquito(enemy)
            }
            state.explosions.forEach { explosion ->
                drawExplosion(explosion)
            }
        }

        Text(
            text = "Score ${state.score}   Lives ${state.lives}   Hits ${state.hits}   Misses ${state.misses}",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )

        Text(
            text = "Pattern: ${state.currentPatternName}",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 42.dp),
        )

        Button(
            onClick = {
                if (state.isGameOver) {
                    game.reset()
                } else {
                    game.fireBullet()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Text(if (state.isGameOver) "Restart" else "Fire")
        }

        if (state.isGameOver) {
            Text(
                text = "Game Over",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private fun DrawScope.drawSpaceBackground() {
    drawRect(Color(0xFF050A16))
    repeat(70) { index ->
        val x = ((index * 47) % size.width.toInt().coerceAtLeast(1)).toFloat()
        val y = ((index * 89) % size.height.toInt().coerceAtLeast(1)).toFloat()
        val radius = if (index % 5 == 0) 1.6f else 0.8f
        drawCircle(
            color = Color.White.copy(alpha = if (index % 3 == 0) 0.32f else 0.16f),
            radius = radius,
            center = Offset(x, y),
        )
    }
}

private fun DrawScope.drawPlayer(
    position: Vector2,
    size: Vector2,
    isFlashing: Boolean,
) {
    val x = position.x
    val y = position.y
    val w = size.x
    val h = size.y
    val bodyColor = if (isFlashing) Color.White else Color(0xFF1976D2)
    val lightBlue = if (isFlashing) Color.White else Color(0xFF64B5F6)
    val darkBlue = if (isFlashing) Color(0xFFBBDEFB) else Color(0xFF0D47A1)

    val leftWing = Path().apply {
        moveTo(x - w * 0.08f, y - h * 0.05f)
        lineTo(x - w * 0.52f, y + h * 0.2f)
        lineTo(x - w * 0.38f, y + h * 0.38f)
        lineTo(x - w * 0.08f, y + h * 0.2f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(x + w * 0.08f, y - h * 0.05f)
        lineTo(x + w * 0.52f, y + h * 0.2f)
        lineTo(x + w * 0.38f, y + h * 0.38f)
        lineTo(x + w * 0.08f, y + h * 0.2f)
        close()
    }
    val body = Path().apply {
        moveTo(x, y - h * 0.58f)
        lineTo(x - w * 0.18f, y + h * 0.32f)
        lineTo(x, y + h * 0.5f)
        lineTo(x + w * 0.18f, y + h * 0.32f)
        close()
    }
    val tailLeft = Path().apply {
        moveTo(x - w * 0.12f, y + h * 0.28f)
        lineTo(x - w * 0.32f, y + h * 0.56f)
        lineTo(x - w * 0.08f, y + h * 0.48f)
        close()
    }
    val tailRight = Path().apply {
        moveTo(x + w * 0.12f, y + h * 0.28f)
        lineTo(x + w * 0.32f, y + h * 0.56f)
        lineTo(x + w * 0.08f, y + h * 0.48f)
        close()
    }

    drawPath(leftWing, darkBlue)
    drawPath(rightWing, darkBlue)
    drawPath(tailLeft, lightBlue)
    drawPath(tailRight, lightBlue)
    drawPath(body, bodyColor)
    drawLine(
        color = Color.White.copy(alpha = 0.75f),
        start = Offset(x, y - h * 0.45f),
        end = Offset(x, y + h * 0.25f),
        strokeWidth = w * 0.04f,
        cap = StrokeCap.Round,
    )
    drawOval(
        color = Color(0xFF90CAF9).copy(alpha = if (isFlashing) 1f else 0.82f),
        topLeft = Offset(x - w * 0.08f, y - h * 0.23f),
        size = Size(w * 0.16f, h * 0.28f),
    )
    drawLine(
        color = Color(0xFFFF7043),
        start = Offset(x - w * 0.08f, y + h * 0.48f),
        end = Offset(x - w * 0.08f, y + h * 0.7f),
        strokeWidth = w * 0.05f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFFF7043),
        start = Offset(x + w * 0.08f, y + h * 0.48f),
        end = Offset(x + w * 0.08f, y + h * 0.7f),
        strokeWidth = w * 0.05f,
        cap = StrokeCap.Round,
    )

    if (isFlashing) {
        drawCircle(
            color = Color(0xFFFF5252).copy(alpha = 0.45f),
            radius = size.x * 0.75f,
            center = Offset(position.x, position.y),
            style = Stroke(width = size.y * 0.18f),
        )
    }
}

private fun DrawScope.drawMosquito(enemy: Enemy) {
    // Later, this is the swap point for drawing R.drawable.mosquito instead of a shape.
    val center = Offset(enemy.position.x, enemy.position.y)
    val r = enemy.radius

    drawOval(
        color = Color.White.copy(alpha = 0.62f),
        topLeft = Offset(center.x - r * 0.9f, center.y - r * 0.85f),
        size = Size(r * 0.85f, r * 0.75f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.62f),
        topLeft = Offset(center.x + r * 0.05f, center.y - r * 0.85f),
        size = Size(r * 0.85f, r * 0.75f),
    )

    drawLine(
        color = Color(0xFFB71C1C),
        start = Offset(center.x - r * 0.2f, center.y + r * 0.55f),
        end = Offset(center.x - r * 0.45f, center.y + r * 1.1f),
        strokeWidth = r * 0.12f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFB71C1C),
        start = Offset(center.x + r * 0.2f, center.y + r * 0.55f),
        end = Offset(center.x + r * 0.45f, center.y + r * 1.1f),
        strokeWidth = r * 0.12f,
        cap = StrokeCap.Round,
    )

    drawOval(
        color = Color(0xFF424242),
        topLeft = Offset(center.x - r * 0.28f, center.y - r * 0.65f),
        size = Size(r * 0.56f, r * 1.35f),
    )
    drawCircle(
        color = Color(0xFF616161),
        radius = r * 0.33f,
        center = Offset(center.x, center.y - r * 0.55f),
    )
    drawCircle(
        color = Color(0xFFE53935),
        radius = r * 0.12f,
        center = Offset(center.x - r * 0.11f, center.y - r * 0.62f),
    )
    drawCircle(
        color = Color(0xFFE53935),
        radius = r * 0.12f,
        center = Offset(center.x + r * 0.11f, center.y - r * 0.62f),
    )
    drawLine(
        color = Color(0xFFEEEEEE),
        start = Offset(center.x, center.y - r * 0.85f),
        end = Offset(center.x, center.y - r * 1.35f),
        strokeWidth = r * 0.08f,
        cap = StrokeCap.Round,
    )

    repeat(3) { legIndex ->
        val y = center.y - r * 0.1f + legIndex * r * 0.26f
        val spread = r * (0.72f + legIndex * 0.14f)
        drawLine(
            color = Color(0xFFBDBDBD),
            start = Offset(center.x - r * 0.18f, y),
            end = Offset(center.x - spread, y + r * 0.18f),
            strokeWidth = r * 0.07f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFFBDBDBD),
            start = Offset(center.x + r * 0.18f, y),
            end = Offset(center.x + spread, y + r * 0.18f),
            strokeWidth = r * 0.07f,
            cap = StrokeCap.Round,
        )
    }

    drawLine(
        color = Color(0xFFEF5350),
        start = Offset(center.x, center.y + r * 0.05f),
        end = Offset(center.x, center.y + r * 0.75f),
        strokeWidth = r * 0.08f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawExplosion(explosion: Explosion) {
    val progress = explosion.progress
    val alpha = 1f - progress
    val outerRadius = explosion.maxRadius * progress.coerceAtLeast(0.2f)
    val innerRadius = outerRadius * 0.55f
    drawCircle(
        color = Color(0xFFFFD54F).copy(alpha = alpha),
        radius = outerRadius,
        center = Offset(explosion.position.x, explosion.position.y),
        style = Stroke(width = explosion.maxRadius * 0.12f),
    )
    drawCircle(
        color = Color(0xFFFF7043).copy(alpha = alpha * 0.8f),
        radius = innerRadius,
        center = Offset(explosion.position.x, explosion.position.y),
        style = Stroke(width = explosion.maxRadius * 0.08f),
    )
}
