package com.chury.bugshooter.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
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
import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2
import com.chury.bugshooter.game.BugShooterGame
import com.chury.bugshooter.game.BossMosquito
import com.chury.bugshooter.game.EnemyBullet
import com.chury.bugshooter.game.Enemy
import com.chury.bugshooter.game.EnemyKind
import com.chury.bugshooter.game.Explosion
import com.chury.bugshooter.game.PowerUp
import com.chury.bugshooter.game.PowerUpType
import com.chury.bugshooter.game.SoundController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin

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
    val previousScore = remember { mutableStateOf(state.score) }
    val previousHits = remember { mutableStateOf(state.hits) }
    val previousGameOver = remember { mutableStateOf(state.isGameOver) }

    LaunchedEffect(Unit) {
        var step = 0
        while (isActive) {
            if (game.state.isGameOver) {
                SoundController.stopBackground()
                delay(300)
            } else {
                SoundController.playBackgroundBeat(step++)
                delay(if (step % 4 == 0) 520 else 360)
            }
        }
    }

    LaunchedEffect(state.score) {
        if (state.score > previousScore.value) {
            SoundController.playEnemyHit()
        }
        previousScore.value = state.score
    }

    LaunchedEffect(state.hits) {
        if (state.hits > previousHits.value && !state.isGameOver) {
            SoundController.playPlayerDeath()
        }
        previousHits.value = state.hits
    }

    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver && !previousGameOver.value) {
            SoundController.playPlayerDeath()
        }
        previousGameOver.value = state.isGameOver
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101820))
            .onSizeChanged { game.setScreenSize(Vector2(it.width.toFloat(), it.height.toFloat())) }
            .pointerInput(game) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    game.startTouch(Vector2(down.position.x, down.position.y))

                    var pressed: Boolean
                    do {
                        val event = awaitPointerEvent()
                        val activeChange = event.changes.lastOrNull { it.pressed }
                        pressed = event.changes.any { it.pressed }
                        if (activeChange != null) {
                            event.changes.forEach { it.consume() }
                            game.moveTouch(Vector2(activeChange.position.x, activeChange.position.y))
                        }
                    } while (pressed)

                    game.endTouch()
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
                shieldCharges = state.shieldCharges,
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
            state.bosses.forEach { boss ->
                drawBossMosquito(boss)
            }
            state.enemyBullets.forEach { bullet ->
                drawEnemyBullet(bullet)
            }
            state.powerUps.forEach { powerUp ->
                drawPowerUp(powerUp)
            }
            state.explosions.forEach { explosion ->
                drawExplosion(explosion)
            }
            drawBossHpBars(state.bosses)
        }

        Text(
            text = "Stage ${state.currentStage}   Level ${state.difficultyLevel}   Life Energy ${state.lives}/${GameConfig.MaxLives}   Score ${state.score}   Hits ${state.hits}   Combo ${state.combo} x${comboMultiplier(state.combo)}",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        )

        Text(
            text = "Pattern: ${state.currentPatternName}   ${activePowerUpText(state.doubleShotSeconds, state.tripleShotSeconds, state.rapidFireSeconds, state.bulletSpeedMultiplier, state.shieldCharges)}",
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 42.dp),
        )

        Text(
            text = "Misses ${state.misses}   Next +${state.carryOverEnemies}",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 62.dp),
        )

        Button(
            onClick = game::reset,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Text("Restart")
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

private fun comboMultiplier(combo: Int): Int {
    return when {
        combo >= 50 -> 5
        combo >= 25 -> 3
        combo >= 10 -> 2
        else -> 1
    }
}

private fun activePowerUpText(
    doubleShotSeconds: Float,
    tripleShotSeconds: Float,
    speedPowerSeconds: Float,
    bulletSpeedMultiplier: Float,
    shieldCharges: Int,
): String {
    val active = buildList {
        if (bulletSpeedMultiplier >= 1.5f) add("RED SPEED x1.5 ${formatSeconds(speedPowerSeconds)}")
        else if (bulletSpeedMultiplier >= 1.25f) add("YELLOW SPEED x1.25 ${formatSeconds(speedPowerSeconds)}")
        if (tripleShotSeconds > 0f) add("PINK TRIPLE ${formatSeconds(tripleShotSeconds)}")
        else if (doubleShotSeconds > 0f) add("GREEN DOUBLE")
        if (shieldCharges > 0) add("SHIELD $shieldCharges")
    }
    return if (active.isEmpty()) "Power: NONE" else "Power: ${active.joinToString("  ")}"
}

private fun formatSeconds(seconds: Float): String {
    val tenths = (seconds.coerceAtLeast(0f) * 10f).toInt()
    return "${tenths / 10}.${tenths % 10}s"
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
    shieldCharges: Int,
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
    if (shieldCharges > 0) {
        drawCircle(
            color = if (shieldCharges >= 5) {
                Color(0xFFAB47BC).copy(alpha = 0.55f)
            } else {
                Color(0xFF42A5F5).copy(alpha = 0.55f)
            },
            radius = size.x * 0.72f,
            center = Offset(position.x, position.y),
            style = Stroke(width = size.y * 0.1f),
        )
    }
}

private fun DrawScope.drawMosquito(enemy: Enemy) {
    // Later, this is the swap point for drawing R.drawable.mosquito instead of a shape.
    val center = Offset(enemy.position.x, enemy.position.y)
    val r = enemy.radius
    val flap = wingFlap(enemy.elapsedSeconds, enemy.formationIndex)
    val wingHeight = r * (0.52f + flap * 0.5f)
    val wingAlpha = 0.36f + flap * 0.34f
    when (enemy.enemyKind) {
        EnemyKind.FLY -> {
            drawFly(enemy)
            return
        }
        EnemyKind.HONEY_BEE -> {
            drawBee(enemy, isWasp = false)
            return
        }
        EnemyKind.WASP -> {
            drawBee(enemy, isWasp = true)
            return
        }
        EnemyKind.WHITE_BUTTERFLY -> {
            drawButterfly(enemy, isSwallowtail = false)
            return
        }
        EnemyKind.SWALLOWTAIL_BUTTERFLY -> {
            drawButterfly(enemy, isSwallowtail = true)
            return
        }
        EnemyKind.STAG_BEETLE -> {
            drawStagBeetle(enemy)
            return
        }
        EnemyKind.MOSQUITO -> Unit
    }

    drawOval(
        color = Color.White.copy(alpha = wingAlpha),
        topLeft = Offset(center.x - r * 1.24f, center.y - r * (0.82f + flap * 0.24f)),
        size = Size(r * 1.18f, wingHeight),
    )
    drawOval(
        color = Color.White.copy(alpha = wingAlpha),
        topLeft = Offset(center.x + r * 0.06f, center.y - r * (0.82f + (1f - flap) * 0.24f)),
        size = Size(r * 1.18f, r * (0.52f + (1f - flap) * 0.5f)),
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

private fun DrawScope.drawFly(enemy: Enemy) {
    val center = Offset(enemy.position.x, enemy.position.y)
    val r = enemy.radius
    val flap = wingFlap(enemy.elapsedSeconds, enemy.formationIndex)
    val wingHeight = r * (0.5f + flap * 0.55f)
    val wingAlpha = 0.3f + flap * 0.42f
    drawOval(
        color = Color.White.copy(alpha = wingAlpha),
        topLeft = Offset(center.x - r * 1.18f, center.y - r * (0.78f + flap * 0.24f)),
        size = Size(r * 1.15f, wingHeight),
    )
    drawOval(
        color = Color.White.copy(alpha = wingAlpha),
        topLeft = Offset(center.x + r * 0.03f, center.y - r * (0.78f + (1f - flap) * 0.24f)),
        size = Size(r * 1.15f, r * (0.5f + (1f - flap) * 0.55f)),
    )
    drawOval(
        color = Color(0xFF263238),
        topLeft = Offset(center.x - r * 0.42f, center.y - r * 0.45f),
        size = Size(r * 0.84f, r * 1.05f),
    )
    drawCircle(
        color = Color(0xFF37474F),
        radius = r * 0.34f,
        center = Offset(center.x, center.y - r * 0.48f),
    )
    drawCircle(
        color = Color(0xFF76FF03),
        radius = r * 0.13f,
        center = Offset(center.x - r * 0.13f, center.y - r * 0.52f),
    )
    drawCircle(
        color = Color(0xFF76FF03),
        radius = r * 0.13f,
        center = Offset(center.x + r * 0.13f, center.y - r * 0.52f),
    )
    repeat(3) { legIndex ->
        val y = center.y - r * 0.05f + legIndex * r * 0.24f
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(center.x - r * 0.25f, y),
            end = Offset(center.x - r * 0.78f, y + r * 0.12f),
            strokeWidth = r * 0.07f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(center.x + r * 0.25f, y),
            end = Offset(center.x + r * 0.78f, y + r * 0.12f),
            strokeWidth = r * 0.07f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawBee(enemy: Enemy, isWasp: Boolean) {
    val center = Offset(enemy.position.x, enemy.position.y)
    val r = enemy.radius
    val flap = wingFlap(enemy.elapsedSeconds, enemy.formationIndex)
    val wingAlpha = 0.34f + flap * 0.42f
    val bodyColor = if (isWasp) Color(0xFFFFC107) else Color(0xFFFFD54F)
    val stripeColor = if (isWasp) Color(0xFF212121) else Color(0xFF4E342E)
    val eyeColor = if (isWasp) Color(0xFFE53935) else Color(0xFF263238)

    drawOval(
        color = Color.White.copy(alpha = wingAlpha),
        topLeft = Offset(center.x - r * 1.35f, center.y - r * (0.98f + flap * 0.24f)),
        size = Size(r * 1.35f, r * (0.68f + flap * 0.5f)),
    )
    drawOval(
        color = Color.White.copy(alpha = wingAlpha),
        topLeft = Offset(center.x, center.y - r * (0.98f + (1f - flap) * 0.24f)),
        size = Size(r * 1.35f, r * (0.68f + (1f - flap) * 0.5f)),
    )
    drawOval(
        color = bodyColor,
        topLeft = Offset(center.x - r * 0.34f, center.y - r * 0.55f),
        size = Size(r * 0.68f, r * 1.3f),
    )
    repeat(4) { index ->
        val stripeY = center.y - r * 0.34f + index * r * 0.28f
        drawLine(
            color = stripeColor,
            start = Offset(center.x - r * 0.28f, stripeY),
            end = Offset(center.x + r * 0.28f, stripeY + r * 0.06f),
            strokeWidth = r * 0.1f,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(
        color = stripeColor,
        radius = r * 0.32f,
        center = Offset(center.x, center.y - r * 0.62f),
    )
    drawCircle(eyeColor, r * 0.09f, Offset(center.x - r * 0.11f, center.y - r * 0.66f))
    drawCircle(eyeColor, r * 0.09f, Offset(center.x + r * 0.11f, center.y - r * 0.66f))
    drawLine(
        color = Color(0xFFFF7043),
        start = Offset(center.x, center.y + r * 0.72f),
        end = Offset(center.x, center.y + r * 1.05f),
        strokeWidth = r * 0.08f,
        cap = StrokeCap.Round,
    )
    repeat(3) { legIndex ->
        val y = center.y - r * 0.1f + legIndex * r * 0.25f
        drawLine(Color(0xFF6D4C41), Offset(center.x - r * 0.24f, y), Offset(center.x - r * 0.86f, y + r * 0.18f), r * 0.06f, cap = StrokeCap.Round)
        drawLine(Color(0xFF6D4C41), Offset(center.x + r * 0.24f, y), Offset(center.x + r * 0.86f, y + r * 0.18f), r * 0.06f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawButterfly(enemy: Enemy, isSwallowtail: Boolean) {
    val center = Offset(enemy.position.x, enemy.position.y)
    val r = enemy.radius
    val flap = wingFlap(enemy.elapsedSeconds, enemy.formationIndex)
    val wingLift = r * (0.12f + flap * 0.28f)
    val topColor = if (isSwallowtail) Color(0xFFFFB74D) else Color(0xFFF5F5F5)
    val bottomColor = if (isSwallowtail) Color(0xFFFF7043) else Color(0xFFE3F2FD)
    val lineColor = if (isSwallowtail) Color(0xFF212121) else Color(0xFF90CAF9)
    val leftTop = Path().apply {
        moveTo(center.x - r * 0.08f, center.y - r * 0.35f)
        cubicTo(center.x - r * 1.35f, center.y - r * 1.35f - wingLift, center.x - r * 1.65f, center.y - r * 0.1f, center.x - r * 0.35f, center.y + r * 0.1f)
        close()
    }
    val rightTop = Path().apply {
        moveTo(center.x + r * 0.08f, center.y - r * 0.35f)
        cubicTo(center.x + r * 1.35f, center.y - r * 1.35f - wingLift, center.x + r * 1.65f, center.y - r * 0.1f, center.x + r * 0.35f, center.y + r * 0.1f)
        close()
    }
    val leftBottom = Path().apply {
        moveTo(center.x - r * 0.1f, center.y + r * 0.05f)
        cubicTo(center.x - r * 1.2f, center.y + r * 0.15f, center.x - r * 1.05f, center.y + r * 1.2f + wingLift, center.x - r * 0.25f, center.y + r * 0.55f)
        close()
    }
    val rightBottom = Path().apply {
        moveTo(center.x + r * 0.1f, center.y + r * 0.05f)
        cubicTo(center.x + r * 1.2f, center.y + r * 0.15f, center.x + r * 1.05f, center.y + r * 1.2f + wingLift, center.x + r * 0.25f, center.y + r * 0.55f)
        close()
    }
    drawPath(leftTop, topColor.copy(alpha = 0.84f))
    drawPath(rightTop, topColor.copy(alpha = 0.84f))
    drawPath(leftBottom, bottomColor.copy(alpha = 0.8f))
    drawPath(rightBottom, bottomColor.copy(alpha = 0.8f))
    drawOval(
        color = Color(0xFF3E2723),
        topLeft = Offset(center.x - r * 0.16f, center.y - r * 0.65f),
        size = Size(r * 0.32f, r * 1.35f),
    )
    drawLine(lineColor, Offset(center.x - r * 0.85f, center.y - r * 0.45f), Offset(center.x - r * 0.25f, center.y - r * 0.08f), r * 0.06f)
    drawLine(lineColor, Offset(center.x + r * 0.85f, center.y - r * 0.45f), Offset(center.x + r * 0.25f, center.y - r * 0.08f), r * 0.06f)
    if (isSwallowtail) {
        drawCircle(Color(0xFF212121), r * 0.12f, Offset(center.x - r * 0.72f, center.y + r * 0.12f))
        drawCircle(Color(0xFF212121), r * 0.12f, Offset(center.x + r * 0.72f, center.y + r * 0.12f))
    }
}

private fun DrawScope.drawStagBeetle(enemy: Enemy) {
    val center = Offset(enemy.position.x, enemy.position.y)
    val r = enemy.radius
    val sway = sin(enemy.elapsedSeconds * 8f + enemy.formationIndex) * r * 0.08f
    drawOval(
        color = Color(0xFF4E342E),
        topLeft = Offset(center.x - r * 0.48f, center.y - r * 0.48f + sway),
        size = Size(r * 0.96f, r * 1.15f),
    )
    drawCircle(
        color = Color(0xFF3E2723),
        radius = r * 0.34f,
        center = Offset(center.x, center.y - r * 0.62f + sway),
    )
    drawLine(Color(0xFFD7CCC8), Offset(center.x - r * 0.18f, center.y - r * 0.82f), Offset(center.x - r * 0.75f, center.y - r * 1.25f), r * 0.12f, cap = StrokeCap.Round)
    drawLine(Color(0xFFD7CCC8), Offset(center.x + r * 0.18f, center.y - r * 0.82f), Offset(center.x + r * 0.75f, center.y - r * 1.25f), r * 0.12f, cap = StrokeCap.Round)
    drawLine(Color(0xFFD7CCC8), Offset(center.x - r * 0.75f, center.y - r * 1.25f), Offset(center.x - r * 0.55f, center.y - r * 0.9f), r * 0.08f, cap = StrokeCap.Round)
    drawLine(Color(0xFFD7CCC8), Offset(center.x + r * 0.75f, center.y - r * 1.25f), Offset(center.x + r * 0.55f, center.y - r * 0.9f), r * 0.08f, cap = StrokeCap.Round)
    repeat(3) { index ->
        val y = center.y - r * 0.2f + index * r * 0.28f
        drawLine(Color(0xFFBCAAA4), Offset(center.x - r * 0.34f, y), Offset(center.x - r * 0.9f, y + r * 0.18f), r * 0.07f, cap = StrokeCap.Round)
        drawLine(Color(0xFFBCAAA4), Offset(center.x + r * 0.34f, y), Offset(center.x + r * 0.9f, y + r * 0.18f), r * 0.07f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawBossMosquito(boss: BossMosquito) {
    val bodyBob = sin(boss.elapsedSeconds * 6f) * boss.radius * 0.05f
    val proxy = object : Enemy {
        override val id = boss.id
        override val enemyKind = boss.enemyKind
        override val groupId = -1
        override val formationPattern = com.chury.bugshooter.game.FormationPattern.CIRCLE_LEFT
        override val formationIndex = 0
        override val elapsedSeconds = boss.elapsedSeconds
        override val basePosition = boss.basePosition
        override val position = boss.position.copy(y = boss.position.y + bodyBob)
        override val radius = boss.radius
        override val speed = 0f
        override fun update(deltaSeconds: Float): Enemy = this
    }
    drawCircle(
        color = Color(0xFFFFD54F).copy(alpha = 0.12f + wingFlap(boss.elapsedSeconds, 0) * 0.12f),
        radius = boss.radius * 1.55f,
        center = Offset(boss.position.x, boss.position.y),
    )
    drawMosquito(proxy)
    drawCircle(
        color = Color(0xFFFFD54F).copy(alpha = 0.25f),
        radius = boss.radius * 1.3f,
        center = Offset(boss.position.x, boss.position.y),
        style = Stroke(width = boss.radius * 0.12f),
    )
}

private fun wingFlap(elapsedSeconds: Float, index: Int): Float {
    return ((sin(elapsedSeconds * 22f + index * 0.7f) + 1f) / 2f).coerceIn(0f, 1f)
}

private fun DrawScope.drawBossHpBars(bosses: List<BossMosquito>) {
    bosses.forEach { boss ->
        drawBossHpBar(boss)
    }
}

private fun DrawScope.drawBossHpBar(boss: BossMosquito) {
    val barWidth = (boss.radius * 2.15f).coerceIn(size.width * 0.18f, size.width * 0.42f)
    val barHeight = (boss.radius * 0.14f).coerceIn(6f, 12f)
    val left = (boss.position.x - barWidth / 2f).coerceIn(8f, size.width - barWidth - 8f)
    val top = (boss.position.y - boss.radius * 1.55f).coerceAtLeast(76f)
    val progress = boss.hp.toFloat() / boss.maxHp.toFloat()
    drawRect(
        color = Color.White.copy(alpha = 0.22f),
        topLeft = Offset(left, top),
        size = Size(barWidth, barHeight),
    )
    drawRect(
        color = Color(0xFFE53935),
        topLeft = Offset(left, top),
        size = Size(barWidth * progress, barHeight),
    )
}

private fun DrawScope.drawEnemyBullet(bullet: EnemyBullet) {
    drawCircle(
        color = Color(0xFFFF7043),
        radius = bullet.radius,
        center = Offset(bullet.position.x, bullet.position.y),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = bullet.radius * 0.45f,
        center = Offset(bullet.position.x, bullet.position.y),
    )
}

private fun DrawScope.drawPowerUp(powerUp: PowerUp) {
    val color = when (powerUp.type) {
        PowerUpType.SPEED_2 -> Color(0xFFFFCA28)
        PowerUpType.SPEED_4 -> Color(0xFFE53935)
        PowerUpType.DOUBLE_SHOT -> Color(0xFF66BB6A)
        PowerUpType.TRIPLE_SHOT -> Color(0xFFFF5CA8)
        PowerUpType.SHIELD_1 -> Color(0xFF42A5F5)
        PowerUpType.SHIELD_2 -> Color(0xFFAB47BC)
    }
    drawCircle(
        color = color,
        radius = powerUp.radius,
        center = Offset(powerUp.position.x, powerUp.position.y),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = powerUp.radius * 0.45f,
        center = Offset(powerUp.position.x, powerUp.position.y),
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
