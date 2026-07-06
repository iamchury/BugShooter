package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameObject

interface Enemy : GameObject {
    val id: Int
    val groupId: Int
    val formationPattern: FormationPattern
    val formationIndex: Int
    val elapsedSeconds: Float
    val basePosition: com.chury.bugshooter.engine.Vector2
    val speed: Float

    fun update(deltaSeconds: Float): Enemy
}
