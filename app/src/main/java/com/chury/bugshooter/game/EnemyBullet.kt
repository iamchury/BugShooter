package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameObject
import com.chury.bugshooter.engine.Vector2

data class EnemyBullet(
    val id: Int,
    override val position: Vector2,
    override val radius: Float,
    val speed: Float,
) : GameObject {
    fun update(deltaSeconds: Float): EnemyBullet {
        return copy(position = position.copy(y = position.y + speed * deltaSeconds))
    }
}
