package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class MosquitoEnemy(
    override val id: Int,
    override val enemyKind: EnemyKind = EnemyKind.MOSQUITO,
    override val groupId: Int,
    override val formationPattern: FormationPattern,
    override val formationIndex: Int,
    val formationCount: Int,
    val formationSpacing: Float,
    override val basePosition: Vector2,
    override val position: Vector2,
    override val radius: Float,
    override val speed: Float,
    val screenSize: Vector2,
    override val elapsedSeconds: Float = 0f,
) : Enemy {
    override fun update(deltaSeconds: Float): MosquitoEnemy {
        val nextElapsed = elapsedSeconds + deltaSeconds
        return copy(
            elapsedSeconds = nextElapsed,
            position = positionFor(nextElapsed),
        )
    }

    private fun positionFor(elapsed: Float): Vector2 {
        val minSize = screenSize.x.coerceAtMost(screenSize.y)
        val down = speed * elapsed
        val indexOffset = formationIndex - (formationCount - 1) / 2f
        val rowOffset = indexOffset * formationSpacing
        val centerBase = basePosition.copy(x = basePosition.x - rowOffset)
        val phase = elapsed * 3.2f
        val zigzag = minSize * GameConfig.ZigzagAmplitudeRatio * zigzagValue(elapsed)
        val waveY = minSize * GameConfig.WaveAmplitudeRatio * sin(phase * 1.4f)
        val circleRadius = minSize * GameConfig.CircleRadiusRatio
        val spiralStartRadius = minSize * GameConfig.SpiralRadiusRatio
        val rowWave = minSize * GameConfig.FormationRowWaveRatio *
            sin(elapsed * 2.1f + formationIndex * 0.65f)

        val center = when (formationPattern) {
            FormationPattern.STRAIGHT_DOWN,
            FormationPattern.LINE_HORIZONTAL -> Vector2(centerBase.x, centerBase.y + down)

            FormationPattern.DIAGONAL_LEFT -> Vector2(
                x = centerBase.x - down * 0.35f,
                y = centerBase.y + down,
            )

            FormationPattern.DIAGONAL_RIGHT -> Vector2(
                x = centerBase.x + down * 0.35f,
                y = centerBase.y + down,
            )

            FormationPattern.ZIGZAG -> Vector2(
                x = centerBase.x + zigzag,
                y = centerBase.y + down,
            )

            FormationPattern.S_CURVE -> Vector2(
                x = centerBase.x + minSize * GameConfig.ZigzagAmplitudeRatio * 1.35f * sin(elapsed * 2.2f),
                y = centerBase.y + down,
            )

            FormationPattern.CIRCLE_LEFT,
            FormationPattern.CIRCLE_RIGHT,
            FormationPattern.SPIRAL_DOWN -> Vector2(centerBase.x, centerBase.y + down)

            FormationPattern.WAVE_VERTICAL -> Vector2(
                x = centerBase.x,
                y = centerBase.y + down + waveY,
            )
        }

        val localOffset = localOffsetForPattern(
            elapsed = elapsed,
            rowOffset = rowOffset,
            rowWave = rowWave,
            circleRadius = circleRadius,
            spiralStartRadius = spiralStartRadius,
        )

        return Vector2(
            x = wrapX(center.x + localOffset.x),
            y = center.y + localOffset.y,
        )
    }

    private fun localOffsetForPattern(
        elapsed: Float,
        rowOffset: Float,
        rowWave: Float,
        circleRadius: Float,
        spiralStartRadius: Float,
    ): Vector2 {
        val indexAngle = formationIndex * (2f * PI.toFloat() / formationCount.coerceAtLeast(1))
        return when (formationPattern) {
            FormationPattern.CIRCLE_LEFT -> {
                val angle = elapsed * 3.1f + indexAngle
                Vector2(cos(angle) * circleRadius, sin(angle) * circleRadius)
            }

            FormationPattern.CIRCLE_RIGHT -> {
                val angle = -elapsed * 3.1f + indexAngle
                Vector2(cos(angle) * circleRadius, sin(angle) * circleRadius)
            }

            FormationPattern.SPIRAL_DOWN -> {
                val spiralRadius = (spiralStartRadius * (1f - elapsed * 0.08f)).coerceAtLeast(circleRadius * 0.35f)
                val angle = elapsed * 4f + indexAngle
                Vector2(cos(angle) * spiralRadius, sin(angle) * spiralRadius)
            }

            FormationPattern.DIAGONAL_LEFT -> tiltedRow(rowOffset, angle = -0.55f)
            FormationPattern.DIAGONAL_RIGHT -> tiltedRow(rowOffset, angle = 0.55f)
            FormationPattern.ZIGZAG -> tiltedRow(rowOffset, angle = sin(elapsed * 5f) * GameConfig.FormationTiltAmplitude)
                .let { it.copy(y = it.y + rowWave) }

            FormationPattern.S_CURVE -> tiltedRow(rowOffset, angle = sin(elapsed * 2.2f) * GameConfig.FormationTiltAmplitude)
                .let { it.copy(y = it.y + rowWave) }

            FormationPattern.WAVE_VERTICAL -> Vector2(rowOffset, rowWave * 1.6f)
            FormationPattern.STRAIGHT_DOWN,
            FormationPattern.LINE_HORIZONTAL -> Vector2(rowOffset, 0f)
        }
    }

    private fun tiltedRow(rowOffset: Float, angle: Float): Vector2 {
        return Vector2(
            x = rowOffset * cos(angle),
            y = rowOffset * sin(angle),
        )
    }

    private fun wrapX(x: Float): Float {
        val width = screenSize.x
        val span = width + radius * 2f
        var wrapped = x + radius
        while (wrapped < 0f) wrapped += span
        while (wrapped > span) wrapped -= span
        return wrapped - radius
    }

    private fun zigzagValue(elapsed: Float): Float {
        val period = 0.85f
        val t = (elapsed % period) / period
        return when {
            t < 0.25f -> t * 4f
            t < 0.75f -> 2f - t * 4f
            else -> t * 4f - 4f
        }
    }
}
