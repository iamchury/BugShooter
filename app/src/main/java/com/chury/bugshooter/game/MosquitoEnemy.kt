package com.chury.bugshooter.game

import com.chury.bugshooter.engine.Vector2

data class MosquitoEnemy(
    override val id: Int,
    override val position: Vector2,
    override val radius: Float,
    override val speed: Float,
) : Enemy {
    override fun update(deltaSeconds: Float): MosquitoEnemy {
        return copy(position = position.copy(y = position.y + speed * deltaSeconds))
    }
}
