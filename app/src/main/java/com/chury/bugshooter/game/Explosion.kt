package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2

data class Explosion(
    val id: Int,
    val position: Vector2,
    val maxRadius: Float,
    val ageSeconds: Float = 0f,
    val lifetimeSeconds: Float = GameConfig.ExplosionLifetimeSeconds,
) {
    val progress: Float
        get() = (ageSeconds / lifetimeSeconds).coerceIn(0f, 1f)

    val isFinished: Boolean
        get() = ageSeconds >= lifetimeSeconds

    fun update(deltaSeconds: Float): Explosion {
        return copy(ageSeconds = ageSeconds + deltaSeconds)
    }
}
