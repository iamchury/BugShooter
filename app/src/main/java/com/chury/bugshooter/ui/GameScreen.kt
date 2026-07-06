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
                drawMosquitoPlaceholder(enemy)
            }
            state.explosions.forEach { explosion ->
                drawExplosion(explosion)
            }
        }

        Text(
            text = "Score ${state.score}   Lives ${state.lives}   Hits ${state.hits}",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlayer(
    position: Vector2,
    size: Vector2,
    isFlashing: Boolean,
) {
    val path = Path().apply {
        moveTo(position.x, position.y - size.y / 2f)
        lineTo(position.x - size.x / 2f, position.y + size.y / 2f)
        lineTo(position.x + size.x / 2f, position.y + size.y / 2f)
        close()
    }
    drawPath(path = path, color = if (isFlashing) Color.White else Color(0xFF4FC3F7))
    if (isFlashing) {
        drawCircle(
            color = Color(0xFFFF5252).copy(alpha = 0.45f),
            radius = size.x * 0.75f,
            center = Offset(position.x, position.y),
            style = Stroke(width = size.y * 0.18f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMosquitoPlaceholder(enemy: Enemy) {
    // Later, this is the swap point for drawing R.drawable.mosquito instead of a shape.
    drawOval(
        color = Color(0xFFE57373),
        topLeft = Offset(enemy.position.x - enemy.radius, enemy.position.y - enemy.radius * 0.75f),
        size = Size(enemy.radius * 2f, enemy.radius * 1.5f),
    )
    drawCircle(
        color = Color(0xFFEF9A9A),
        radius = enemy.radius * 0.35f,
        center = Offset(enemy.position.x, enemy.position.y - enemy.radius * 0.15f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawExplosion(explosion: Explosion) {
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
