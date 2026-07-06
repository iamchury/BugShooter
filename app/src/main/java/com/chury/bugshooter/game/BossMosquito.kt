package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameObject
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
    val elapsedSeconds: Float = 0f,
) : GameObject {
    fun update(deltaSeconds: Float): BossMosquito {
        val nextElapsed = elapsedSeconds + deltaSeconds
        val travel = screenSize.x * 0.28f
        return copy(
            elapsedSeconds = nextElapsed,
            position = basePosition.copy(x = basePosition.x + sin(nextElapsed * 1.4f) * travel),
        )
    }

    fun damage(amount: Int): BossMosquito {
        return copy(hp = (hp - amount).coerceAtLeast(0))
    }
}
