package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameObject

interface Enemy : GameObject {
    val id: Int
    val speed: Float

    fun update(deltaSeconds: Float): Enemy
}
