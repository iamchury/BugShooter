package com.chury.bugshooter.game

import com.chury.bugshooter.engine.GameConfig
import com.chury.bugshooter.engine.Vector2

data class GameState(
    val screenSize: Vector2 = Vector2.Zero,
    val player: Player = Player(Vector2.Zero, Vector2.Zero),
    val bullets: List<Bullet> = emptyList(),
    val enemies: List<Enemy> = emptyList(),
    val explosions: List<Explosion> = emptyList(),
    val score: Int = 0,
    val lives: Int = GameConfig.InitialLives,
    val hits: Int = 0,
    val misses: Int = 0,
    val fireCooldownSeconds: Float = 0f,
    val spawnTimerSeconds: Float = 0f,
    val playerHitFlashSeconds: Float = 0f,
    val currentGroupId: Int? = null,
    val currentPatternName: String = "NONE",
    val isGameOver: Boolean = false,
)
