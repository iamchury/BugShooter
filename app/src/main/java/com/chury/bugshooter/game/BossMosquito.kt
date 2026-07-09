package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameObject
import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2
import kotlin.math.sin

data class BossMosquito(
    val id: Int,
    val enemyKind: EnemyKind,
    val hp: Int,
    val maxHp: Int,
    val basePosition: Vector2,
    override val position: Vector2,
    override val radius: Float,
    val screenSize: Vector2,
    val speed: Float,
    val elapsedSeconds: Float = 0f,
) : GameObject {
    fun update(deltaSeconds: Float): BossMosquito {
        val nextElapsed = elapsedSeconds + deltaSeconds
        val travel = screenSize.x * 0.28f
        val verticalBob = sin(nextElapsed * 2.2f) * screenSize.y * 0.035f
        val nextPosition = Vector2(
            x = basePosition.x + sin(nextElapsed * 1.4f) * travel,
            y = basePosition.y + speed * nextElapsed + verticalBob,
        )
        if (nextPosition.y - radius > screenSize.y) {
            val restartPosition = Vector2(
                x = nextPosition.x.coerceIn(radius, screenSize.x - radius),
                y = -radius * 2f,
            )
            return copy(
                basePosition = restartPosition,
                position = restartPosition,
                elapsedSeconds = 0f,
            )
        }
        return copy(
            elapsedSeconds = nextElapsed,
            position = nextPosition,
        )
    }

    val escaped: Boolean
        get() = position.y - radius > screenSize.y

    fun reenterFromTop(): BossMosquito {
        val restartPosition = Vector2(
            x = position.x.coerceIn(radius, screenSize.x - radius),
            y = -radius * 2f,
        )
        return copy(
            basePosition = restartPosition,
            position = restartPosition,
            elapsedSeconds = 0f,
        )
    }

    fun damage(amount: Int): BossMosquito {
        return copy(hp = (hp - amount).coerceAtLeast(0))
    }
}
