package com.chury.bugshooter.game

data class EnemyFormation(
    val groupId: Int,
    val pattern: FormationPattern,
    val enemyCount: Int,
    val speed: Float,
)
