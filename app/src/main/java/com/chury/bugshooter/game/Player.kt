package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameObject
import com.chury.bugshooter.engine.Vector2

data class Player(
    override val position: Vector2,
    val size: Vector2,
) : GameObject {
    override val radius: Float
        get() = size.x / 2f

    fun moveHorizontally(deltaX: Float, screenWidth: Float): Player {
        val halfWidth = size.x / 2f
        val nextX = (position.x + deltaX).coerceIn(halfWidth, screenWidth - halfWidth)
        return copy(position = position.copy(x = nextX))
    }
}
