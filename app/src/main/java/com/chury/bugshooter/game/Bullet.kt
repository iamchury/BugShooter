package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameObject
import com.chury.bugshooter.engine.Vector2

data class Bullet(
    val id: Int,
    override val position: Vector2,
    override val radius: Float,
    val speed: Float,
) : GameObject {
    fun update(deltaSeconds: Float): Bullet {
        return copy(position = position.copy(y = position.y - speed * deltaSeconds))
    }
}
